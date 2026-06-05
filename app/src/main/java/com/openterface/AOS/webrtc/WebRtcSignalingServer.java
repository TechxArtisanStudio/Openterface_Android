package com.openterface.AOS.webrtc;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import fi.iki.elonen.NanoHTTPD;

/**
 * Lightweight HTTP signaling server for WebRTC.
 * Handles SDP offer/answer exchange via simple REST endpoints.
 *
 * Endpoints:
 * POST /offer - Submit SDP offer, receive SDP answer + ICE candidates
 * GET  /sdp   - Poll for the latest SDP answer (fallback)
 * GET  /status - Check server status and connection state
 * POST /ice   - Submit ICE candidates
 */
public class WebRtcSignalingServer extends NanoHTTPD {
    private static final String TAG = "WebRtcSignalingServer";

    public interface SignalingCallback {
        void onOfferReceived(String sdp);
        void onIceCandidateReceived(String candidateJson);
    }

    private SignalingCallback callback;
    private String pendingAnswer;
    private String pendingIceCandidates;
    private String connectionState = "idle";

    public WebRtcSignalingServer(int port, SignalingCallback callback) {
        super(port);
        this.callback = callback;
        Log.i(TAG, "Signaling server created on port " + port);
    }

    @Override
    public void start() throws IOException {
        super.start(5000, false); // 5s timeout, daemon thread
        Log.i(TAG, "Signaling server started on port " + getListeningPort());
    }

    public int getActualPort() {
        return getListeningPort();
    }

    private Context context;

    public void setContext(Context context) {
        this.context = context;
    }

    @Override
    public Response serve(IHTTPSession session) {
        Method method = session.getMethod();
        String uri = session.getUri();

        Log.d(TAG, "Request: " + method + " " + uri);

        try {
            // Static file serving first
            if (uri.equals("/") || uri.equals("/index.html")) {
                return serveAsset("webrtc/index.html", "text/html");
            }
            if (uri.startsWith("/assets/")) {
                String assetPath = "webrtc" + uri;
                String mimeType = getMimeType(uri);
                return serveAsset(assetPath, mimeType);
            }
            if (uri.equals("/keymod.js")) {
                return serveAsset("webrtc/keymod.js", "application/javascript");
            }
            if (uri.equals("/keymod.wasm")) {
                return serveAsset("webrtc/keymod.wasm", "application/wasm");
            }

            // API endpoints
            switch (uri) {
                case "/offer":
                    if (method == Method.POST) {
                        return handleOffer(session);
                    }
                    return newFixedLengthResponse(Response.Status.METHOD_NOT_ALLOWED, MIME_PLAINTEXT,
                            "Only POST allowed");

                case "/ice":
                    if (method == Method.POST) {
                        return handleIce(session);
                    }
                    return newFixedLengthResponse(Response.Status.METHOD_NOT_ALLOWED, MIME_PLAINTEXT,
                            "Only POST allowed");

                case "/sdp":
                    if (method == Method.GET) {
                        return handleGetSdp();
                    }
                    return newFixedLengthResponse(Response.Status.METHOD_NOT_ALLOWED, MIME_PLAINTEXT,
                            "Only GET allowed");

                case "/status":
                    return handleStatus();

                case "/input":
                    if (method == Method.POST) {
                        return handleInput(session);
                    }
                    return newFixedLengthResponse(Response.Status.METHOD_NOT_ALLOWED, MIME_PLAINTEXT,
                            "Only POST allowed");

                default:
                    // SPA fallback - serve index.html for unknown routes
                    return serveAsset("webrtc/index.html", "text/html");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error handling request", e);
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT,
                    "Error: " + e.getMessage());
        }
    }

    private Response serveAsset(String assetPath, String mimeType) {
        if (context == null) {
            Log.e(TAG, "Context not set, cannot serve assets");
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT,
                    "Server not initialized");
        }

        try {
            AssetManager assets = context.getAssets();
            InputStream is = assets.open(assetPath);

            // Read entire asset into byte array
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int read;
            while ((read = is.read(buffer)) != -1) {
                baos.write(buffer, 0, read);
            }
            is.close();

            byte[] data = baos.toByteArray();
            Log.d(TAG, "Serving asset: " + assetPath + " (" + data.length + " bytes, " + mimeType + ")");

            return newFixedLengthResponse(Response.Status.OK, mimeType,
                    new ByteArrayInputStream(data), data.length);

        } catch (IOException e) {
            Log.w(TAG, "Asset not found: " + assetPath);
            return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT,
                    "Asset not found: " + assetPath);
        }
    }

    private String getMimeType(String uri) {
        if (uri.endsWith(".html")) return "text/html";
        if (uri.endsWith(".js")) return "application/javascript";
        if (uri.endsWith(".css")) return "text/css";
        if (uri.endsWith(".wasm")) return "application/wasm";
        if (uri.endsWith(".json")) return "application/json";
        if (uri.endsWith(".png")) return "image/png";
        if (uri.endsWith(".jpg") || uri.endsWith(".jpeg")) return "image/jpeg";
        if (uri.endsWith(".svg")) return "image/svg+xml";
        return "application/octet-stream";
    }

    private Response handleOffer(IHTTPSession session) {
        try {
            String body = readBody(session);
            JSONObject json = new JSONObject(body);

            String sdp = json.optString("sdp", "");
            if (sdp.isEmpty()) {
                return newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_PLAINTEXT,
                        "Missing 'sdp' field");
            }

            Log.i(TAG, "Received SDP offer");
            connectionState = "offer_received";

            // Notify the WebRTC service to process the offer
            if (callback != null) {
                callback.onOfferReceived(sdp);
            }

            // Return waiting response - the answer will be set later
            JSONObject response = new JSONObject();
            response.put("status", "processing");
            response.put("message", "SDP offer received, answer will be provided at /sdp endpoint");

            return newFixedLengthResponse(Response.Status.OK, "application/json",
                    response.toString());

        } catch (JSONException e) {
            return newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_PLAINTEXT,
                    "Invalid JSON: " + e.getMessage());
        }
    }

    private Response handleIce(IHTTPSession session) {
        try {
            String body = readBody(session);
            JSONObject json = new JSONObject(body);

            if (callback != null) {
                callback.onIceCandidateReceived(body);
            }

            JSONObject response = new JSONObject();
            response.put("status", "ok");
            return newFixedLengthResponse(Response.Status.OK, "application/json",
                    response.toString());

        } catch (JSONException e) {
            return newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_PLAINTEXT,
                    "Invalid JSON: " + e.getMessage());
        }
    }

    private Response handleGetSdp() {
        JSONObject response = new JSONObject();
        try {
            if (pendingAnswer != null) {
                response.put("sdp", pendingAnswer);
                response.put("type", "answer");
                response.put("status", "ready");
            } else {
                response.put("status", "waiting");
                response.put("message", "No SDP answer available yet");
            }
        } catch (JSONException e) {
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT,
                    "Error: " + e.getMessage());
        }

        return newFixedLengthResponse(Response.Status.OK, "application/json",
                response.toString());
    }

    private Response handleStatus() {
        JSONObject response = new JSONObject();
        try {
            response.put("state", connectionState);
            response.put("port", getActualPort());
        } catch (JSONException e) {
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT,
                    "Error: " + e.getMessage());
        }
        return newFixedLengthResponse(Response.Status.OK, "application/json",
                response.toString());
    }

    private Response handleInput(IHTTPSession session) {
        // Fallback HTTP input endpoint (in case DataChannel isn't available)
        // Receives keyboard/mouse events as JSON
        try {
            String body = readBody(session);
            // The input would be routed through a separate callback
            JSONObject response = new JSONObject();
            response.put("status", "ok");
            return newFixedLengthResponse(Response.Status.OK, "application/json",
                    response.toString());
        } catch (JSONException e) {
            return newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_PLAINTEXT,
                    "Invalid JSON: " + e.getMessage());
        }
    }

    private Response handleGetIndex() {
        // Simple status page for browser testing
        String html = "<!DOCTYPE html>\n" +
                "<html><head><title>Openterface WebRTC</title></head>\n" +
                "<body>\n" +
                "<h1>Openterface Mini-KVM WebRTC Server</h1>\n" +
                "<p>Status: " + connectionState + "</p>\n" +
                "<p>Port: " + getActualPort() + "</p>\n" +
                "<p>Use a WebRTC client to connect via the /offer endpoint.</p>\n" +
                "</body></html>\n";
        return newFixedLengthResponse(Response.Status.OK, "text/html", html);
    }

    /**
     * Set the SDP answer to be returned by GET /sdp.
     */
    public void setPendingAnswer(String sdpAnswer) {
        this.pendingAnswer = sdpAnswer;
        this.connectionState = "answer_ready";
        Log.i(TAG, "SDP answer ready for client");
    }

    public String getPendingAnswer() {
        return pendingAnswer;
    }

    public void setConnectionState(String state) {
        this.connectionState = state;
    }

    public String getConnectionState() {
        return connectionState;
    }

    private String readBody(IHTTPSession session) {
        try {
            int contentLength = Integer.parseInt(
                    session.getHeaders().getOrDefault("content-length", "0"));
            if (contentLength == 0) {
                return "";
            }

            byte[] buffer = new byte[contentLength];
            InputStream inputStream = session.getInputStream();
            int totalRead = 0;
            while (totalRead < contentLength) {
                int read = inputStream.read(buffer, totalRead, contentLength - totalRead);
                if (read == -1) break;
                totalRead += read;
            }

            return new String(buffer, 0, totalRead, StandardCharsets.UTF_8);
        } catch (IOException e) {
            Log.e(TAG, "Error reading request body", e);
            return "";
        }
    }

    public void stopServer() {
        stop();
        Log.i(TAG, "Signaling server stopped");
    }
}
