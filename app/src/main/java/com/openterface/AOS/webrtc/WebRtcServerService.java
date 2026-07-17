package com.openterface.AOS.webrtc;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.pm.ServiceInfo;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.openterface.AOS.R;
import com.openterface.AOS.activity.MainActivity;

import org.json.JSONObject;
import org.webrtc.*;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Foreground service that manages the WebRTC server lifecycle.
 * Sets up peer connection, signaling server, and handles video/input data flow.
 */
public class WebRtcServerService extends Service {
    private static final String TAG = "OP-WEBRTC";
    private static final int NOTIFICATION_ID = 1002;
    private static final String CHANNEL_ID = "webrtc_server_channel";

    // WebRTC
    private PeerConnectionFactory peerConnectionFactory;
    private PeerConnection peerConnection;
    private WebRtcFrameCapturer videoCapturer;
    private VideoSource videoSource;
    private VideoTrack videoTrack;
    private DataChannel dataChannel;
    private EglBase eglBase;

    // Signaling
    private WebRtcSignalingServer signalingServer;
    private int signalingPort;

    // State
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private final AtomicBoolean isConnected = new AtomicBoolean(false);
    private final AtomicReference<String> connectedClient = new AtomicReference<>("");

        // Video parameters
    private int targetFps;
    private int videoWidth;
    private int videoHeight;
    private int rotation;

    // Diagnostics
    private boolean enableDiagnostics = true;
    private Runnable diagnosticRunnable;

    // Callback
    private WebRtcCallback callback;

    // Main thread handler for UI updates
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    // Binder for local binding
    private final IBinder binder = new LocalBinder();

    public interface WebRtcCallback {
        void onClientConnected(String clientInfo);
        void onClientDisconnected(String clientInfo);
        void onServerError(String error);
        void onMouseEvent(int buttonMask, int x, int y, boolean pressed);
        void onKeyboardEvent(int keysym, boolean down, int modifier);
        void onConnectionStateChanged(String state);
    }

    public class LocalBinder extends Binder {
        public WebRtcServerService getService() {
            return WebRtcServerService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        Log.d(TAG, "Service created");
    }

    @Override
    public void onDestroy() {
        stopServer();
        super.onDestroy();
    }

    public void setCallback(WebRtcCallback cb) {
        this.callback = cb;
    }

    /**
     * Start the WebRTC server.
     *
     * @param signalingPort Port for HTTP signaling server
     * @param stunServer    STUN server URL (e.g., "stun:stun.l.google.com:19302")
     * @param width         Video width
     * @param height        Video height
     * @param fps           Target frame rate
     * @return true if started successfully
     */
    public boolean startServer(int signalingPort, String stunServer, int width, int height, int fps, int rotation) {
        if (isRunning.get()) {
            Log.w(TAG, "Server already running");
            return false;
        }

        this.signalingPort = signalingPort;
        this.targetFps = fps;
        this.videoWidth = width;
        this.videoHeight = height;
        this.rotation = rotation;
        Log.i(TAG, "Starting WebRTC server: port=" + signalingPort +
                ", video=" + width + "x" + height + "@" + fps + "fps" +
                ", rotation=" + rotation);

        try {
            // Initialize WebRTC
            if (!initializeWebRTC()) {
                Log.e(TAG, "Failed to initialize WebRTC");
                return false;
            }

            // Create peer connection
            if (!createPeerConnection(stunServer)) {
                Log.e(TAG, "Failed to create peer connection");
                cleanupWebRTC();
                return false;
            }

            // Start signaling server
            signalingServer = new WebRtcSignalingServer(signalingPort, new SignalingHandler());
            signalingServer.setContext(this);
            signalingServer.start();

            isRunning.set(true);

            // Start foreground service
            // Start foreground service with the correct foreground service type.
            // On Android 14+ (API 34+), explicitly specify the foregroundServiceType
            // declared in the manifest. On older Android versions, use the 2-arg form
            // which internally uses the manifest-declared type.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(NOTIFICATION_ID, createNotification(),
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE);
            } else {
                startForeground(NOTIFICATION_ID, createNotification());
            }

            Log.i(TAG, "WebRTC server started");
            return true;

        } catch (Exception e) {
            Log.e(TAG, "Failed to start server", e);
            cleanup();
            return false;
        }
    }

    /**
     * Stop the WebRTC server.
     */
    public void stopServer() {
        if (!isRunning.get()) {
            return;
        }

        Log.i(TAG, "Stopping WebRTC server");
        isRunning.set(false);

        if (signalingServer != null) {
            signalingServer.stopServer();
            signalingServer = null;
        }

        cleanup();

        stopForeground(true);
        Log.i(TAG, "WebRTC server stopped");
    }

    private boolean initializeWebRTC() {
        try {
            // Initialize WebRTC peer connection factory
            PeerConnectionFactory.InitializationOptions options =
                    PeerConnectionFactory.InitializationOptions.builder(getApplicationContext())
                            .setEnableInternalTracer(false)
                            .createInitializationOptions();
            PeerConnectionFactory.initialize(options);

            // Create EGL context for OpenGL rendering
            eglBase = EglBase.create();

            // Create peer connection factory
            peerConnectionFactory = PeerConnectionFactory.builder()
                    .setVideoDecoderFactory(new DefaultVideoDecoderFactory(eglBase.getEglBaseContext()))
                    .setVideoEncoderFactory(new DefaultVideoEncoderFactory(eglBase.getEglBaseContext(), true, true))
                    .createPeerConnectionFactory();

            Log.i(TAG, "WebRTC initialized");
            return true;

        } catch (Exception e) {
            Log.e(TAG, "Error initializing WebRTC", e);
            return false;
        }
    }

    /**
     * Diagnostic: Poll RTCP stats every 500ms for first 10s to verify encoder behavior.
     */
    private void startDiagnosticLogger() {
        if (!enableDiagnostics || peerConnection == null) return;

        diagnosticRunnable = new Runnable() {
            private int count = 0;

            @Override
            public void run() {
                if (count >= 20 || peerConnection == null) {
                    Log.i(TAG, "[DIAG] Diagnostic logging complete");
                    return;
                }
                peerConnection.getStats(report -> {
                    try {
                        for (RTCStats stats : report.getStatsMap().values()) {
                            if ("outbound-rtp".equals(stats.getType()) && stats.getMembers() != null) {
                                Object bytesSentObj = stats.getMembers().get("bytesSent");
                                Object framesObj = stats.getMembers().get("framesEncoded");
                                long bytesSent = bytesSentObj instanceof Number ? ((Number) bytesSentObj).longValue() : 0;
                                long framesEncoded = framesObj instanceof Number ? ((Number) framesObj).longValue() : 0;
                                Log.i(TAG, "[DIAG] T=" + (count * 500) + "ms bytesSent=" + bytesSent +
                                        " framesEncoded=" + framesEncoded);
                                break;
                            }
                        }
                    } catch (Exception e) {
                        Log.w(TAG, "[DIAG] Stats extraction error", e);
                    }
                });
                count++;
                mainHandler.postDelayed(this, 500);
            }
        };
        mainHandler.postDelayed(diagnosticRunnable, 500);
    }

    private boolean createPeerConnection(String stunServer) {
        try {
            // Configure ICE servers
            List<PeerConnection.IceServer> iceServers = new ArrayList<>();
            iceServers.add(PeerConnection.IceServer.builder(stunServer).createIceServer());

            // Create peer connection configuration
            PeerConnection.RTCConfiguration rtcConfig =
                    new PeerConnection.RTCConfiguration(iceServers);
            rtcConfig.iceTransportsType = PeerConnection.IceTransportsType.ALL;
            rtcConfig.bundlePolicy = PeerConnection.BundlePolicy.MAXBUNDLE;
            rtcConfig.rtcpMuxPolicy = PeerConnection.RtcpMuxPolicy.REQUIRE;
            rtcConfig.continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY;

            peerConnection = peerConnectionFactory.createPeerConnection(rtcConfig, peerObserver);

            // Note: DataChannel is created by browser client and received via onDataChannel
            // We don't create one here - we wait for the browser to create it

            // Create video source and track
            videoSource = peerConnectionFactory.createVideoSource(false);
            videoTrack = peerConnectionFactory.createVideoTrack("video0", videoSource);

            List<String> streamIds = new ArrayList<>();
            streamIds.add("stream0");
            peerConnection.addTrack(videoTrack, streamIds);

            Log.i(TAG, "Peer connection created");
            return true;

        } catch (Exception e) {
            Log.e(TAG, "Error creating peer connection", e);
            return false;
        }
    }

    /**
     * PeerConnection observer.
     * Implemented as a field to allow individual method overrides.
     */
    private final PeerConnection.Observer peerObserver = new PeerConnection.Observer() {
        @Override
        public void onSignalingChange(PeerConnection.SignalingState state) {
            Log.d(TAG, "Signaling state: " + state);
        }

        @Override
        public void onIceConnectionChange(PeerConnection.IceConnectionState state) {
            Log.d(TAG, "ICE connection state: " + state);
        }

        @Override
        public void onIceConnectionReceivingChange(boolean receiving) {
            Log.d(TAG, "ICE connection receiving: " + receiving);
        }

        @Override
        public void onIceGatheringChange(PeerConnection.IceGatheringState state) {
            Log.d(TAG, "ICE gathering state: " + state);
            // When gathering is complete, signal that the answer is ready with all candidates
            if (state == PeerConnection.IceGatheringState.COMPLETE && signalingServer != null) {
                Log.i(TAG, "ICE gathering complete, answer ready with all candidates");
                // The answer is already set via pendingAnswer, browser will poll for it with candidates
            }
        }

        @Override
        public void onIceCandidate(IceCandidate candidate) {
            Log.d(TAG, "New ICE candidate: " + candidate.sdp);
            // Send to browser via signaling server
            if (signalingServer != null) {
                signalingServer.addIceCandidate(candidate);
            }
        }

        @Override
        public void onIceCandidatesRemoved(IceCandidate[] candidates) {
        }

        @Override
        public void onAddStream(MediaStream stream) {
            Log.d(TAG, "Stream added");
        }

        @Override
        public void onRemoveStream(MediaStream stream) {
            Log.d(TAG, "Stream removed");
        }

        @Override
        public void onDataChannel(DataChannel dc) {
            Log.i(TAG, "Data channel received: " + dc.label());
            setupDataChannel(dc);
        }

        @Override
        public void onRenegotiationNeeded() {
            Log.d(TAG, "Renegotiation needed");
        }

        @Override
        public void onAddTrack(RtpReceiver receiver, MediaStream[] mediaStreams) {
            Log.d(TAG, "Track added");
        }
    };

    private void setupDataChannel(DataChannel dc) {
        dataChannel = dc;
        dataChannel.registerObserver(new DataChannel.Observer() {
            @Override
            public void onBufferedAmountChange(long previousAmount) {
            }

            @Override
            public void onStateChange() {
                Log.i(TAG, "Data channel state: " + (dataChannel != null ? dataChannel.state() : "null"));
            }

            @Override
            public void onMessage(DataChannel.Buffer buffer) {
                handleDataChannelMessage(buffer);
            }
        });
    }

    /**
     * Start video capture at target FPS.
     * Uses full resolution from the start for stable video output.
     */
    public void startVideoCapture(int width, int height, int fps, int rotation) {
        this.rotation = rotation;

        videoCapturer = new WebRtcFrameCapturer(width, height, fps);
        videoCapturer.setRotation(rotation);

        // Initialize capturer with WebRTC internals
        videoCapturer.initialize(null, getApplicationContext(),
                videoSource.getCapturerObserver());
        videoCapturer.startCapture(width, height, fps);

        Log.i(TAG, "Video capture started: " + width + "x" + height + " @ " + fps + "fps");
    }

    /**
     * Feed a frame from UVC camera into WebRTC.
     * Called from WebRtcFrameCapture (similar pattern to VNC).
     */
    public void onUvcFrame(ByteBuffer rgbaBuffer, int width, int height, long timestampNs, int rotation) {
        if (videoCapturer != null && isRunning.get()) {
            videoCapturer.onFrame(rgbaBuffer, width, height, timestampNs, rotation);
        }
    }

    /**
     * Handle incoming signaling offer from client.
     */
    private void handleOffer(String sdpOffer) {
        Log.i(TAG, "Processing SDP offer");

        try {
            SessionDescription offer = new SessionDescription(
                    SessionDescription.Type.OFFER, sdpOffer);

            peerConnection.setRemoteDescription(new SdpObserver() {
                @Override
                public void onCreateSuccess(SessionDescription sdp) {
                }

                @Override
                public void onSetSuccess() {
                    Log.i(TAG, "Remote description set");
                    createAnswer();
                }

                @Override
                public void onCreateFailure(String error) {
                    Log.e(TAG, "SDP creation failed: " + error);
                }

                @Override
                public void onSetFailure(String error) {
                    Log.e(TAG, "Failed to set remote description: " + error);
                }
            }, offer);

        } catch (Exception e) {
            Log.e(TAG, "Error handling offer", e);
        }
    }

    /**
     * Modify SDP to add video bitrate constraints for better initial quality.
     * This helps WebRTC encoder start with higher bitrate instead of ramping up slowly.
     */
    private SessionDescription modifySdpBitrate(SessionDescription sdp) {
        String description = sdp.description;

        // Add bandwidth limit (8 Mbps) for video
        // Insert b=AS:8000 after each m=video line
        String[] lines = description.split("\r\n");
        StringBuilder modified = new StringBuilder();

        for (String line : lines) {
            modified.append(line).append("\r\n");
            if (line.startsWith("m=video")) {
                // Add bandwidth limit for video
                modified.append("b=AS:8000\r\n");
            }
        }

        String modifiedDescription = modified.toString();
        Log.d(TAG, "Modified SDP with bitrate constraints");

        return new SessionDescription(sdp.type, modifiedDescription);
    }

    private void createAnswer() {
        MediaConstraints constraints = new MediaConstraints();

        peerConnection.createAnswer(new SdpObserver() {
            @Override
            public void onCreateSuccess(SessionDescription sdp) {
                Log.i(TAG, "Answer created");

                // Modify SDP to add bitrate constraints for better initial quality
                SessionDescription modifiedSdp = modifySdpBitrate(sdp);

                peerConnection.setLocalDescription(new SdpObserver() {
                    @Override
                    public void onCreateSuccess(SessionDescription sdp) {
                    }

                    @Override
                    public void onSetSuccess() {
                        Log.i(TAG, "Local description set");
                        if (signalingServer != null) {
                            signalingServer.setPendingAnswer(sdp.description);
                        }

                        // Diagnostic: Log stats for first 10s
                        startDiagnosticLogger();
                    }

                    @Override
                    public void onCreateFailure(String error) {
                    }

                    @Override
                    public void onSetFailure(String error) {
                        Log.e(TAG, "Failed to set local description: " + error);
                    }
                }, sdp);
            }

            @Override
            public void onSetSuccess() {
            }

            @Override
            public void onCreateFailure(String error) {
                Log.e(TAG, "Failed to create answer: " + error);
            }

            @Override
            public void onSetFailure(String error) {
            }
        }, constraints);
    }

    private void handleDataChannelMessage(DataChannel.Buffer buffer) {
        try {
            byte[] data = new byte[buffer.data.remaining()];
            buffer.data.get(data);
            String json = new String(data, "UTF-8");
            JSONObject message = new JSONObject(json);

            String type = message.optString("type");

            switch (type) {
                case "mouse":
                    int buttonMask = message.optInt("buttonMask", 0);
                    int x = message.optInt("x", 0);
                    int y = message.optInt("y", 0);
                    boolean pressed = message.optBoolean("pressed", false);
                    if (callback != null) {
                        mainHandler.post(() -> callback.onMouseEvent(buttonMask, x, y, pressed));
                    }
                    break;

                case "keyboard":
                    int keysym = message.optInt("keysym", 0);
                    boolean down = message.optBoolean("down", false);
                    int modifier = message.optInt("modifier", 0);
                    Log.i(TAG, "Keyboard event received: keysym=" + keysym + ", down=" + down + ", modifier=" + modifier);
                    if (callback != null) {
                        Log.i(TAG, "Dispatching to callback");
                        mainHandler.post(() -> callback.onKeyboardEvent(keysym, down, modifier));
                    } else {
                        Log.w(TAG, "No callback registered for keyboard event");
                    }
                    break;

                default:
                    Log.d(TAG, "Unknown message type: " + type);
            }

        } catch (Exception e) {
            Log.e(TAG, "Error handling data channel message", e);
        }
    }

    private void updateConnectionState(PeerConnection.PeerConnectionState state) {
        String stateStr = state.toString();
        if (callback != null) {
            mainHandler.post(() -> callback.onConnectionStateChanged(stateStr));
        }

        switch (state) {
            case CONNECTED:
                isConnected.set(true);
                connectedClient.set("WebRTC Client");
                if (callback != null) {
                    mainHandler.post(() -> callback.onClientConnected("WebRTC Client"));
                }
                updateNotification();
                break;
            case DISCONNECTED:
            case CLOSED:
            case FAILED:
                isConnected.set(false);
                if (callback != null && connectedClient.get() != null) {
                    mainHandler.post(() -> callback.onClientDisconnected(connectedClient.get()));
                }
                connectedClient.set("");
                updateNotification();
                break;
        }
    }

    private void cleanup() {
        if (videoCapturer != null) {
            videoCapturer.stopCapture();
            videoCapturer.dispose();
            videoCapturer = null;
        }

        if (dataChannel != null) {
            dataChannel.close();
            dataChannel = null;
        }

        if (videoTrack != null) {
            videoTrack.dispose();
            videoTrack = null;
        }

        if (videoSource != null) {
            videoSource.dispose();
            videoSource = null;
        }

        if (peerConnection != null) {
            peerConnection.close();
            peerConnection.dispose();
            peerConnection = null;
        }

        if (peerConnectionFactory != null) {
            peerConnectionFactory.dispose();
            peerConnectionFactory = null;
        }

        if (eglBase != null) {
            eglBase.release();
            eglBase = null;
        }

        Log.i(TAG, "WebRTC cleanup complete");
    }

    private void cleanupWebRTC() {
        // Separate cleanup just for WebRTC (before signaling)
    }

    // --- Getters ---

    public boolean isRunning() {
        return isRunning.get();
    }

    public boolean isClientConnected() {
        return isConnected.get();
    }

    public String getConnectedClient() {
        return connectedClient.get();
    }

    public int getSignalingPort() {
        return signalingPort;
    }

    public List<String> getDeviceIpAddresses() {
        List<String> ips = new ArrayList<>();
        try {
            for (NetworkInterface iface : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (iface.isUp() && !iface.isLoopback()) {
                    for (InetAddress addr : Collections.list(iface.getInetAddresses())) {
                        if (addr instanceof java.net.Inet4Address) {
                            ips.add(addr.getHostAddress());
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to get IP addresses", e);
        }
        return ips;
    }

    // --- Notification ---

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "WebRTC Server",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("WebRTC Server status");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private Notification createNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        String content = isConnected.get()
                ? "Client connected: " + connectedClient.get()
                : "Waiting for connection on port " + signalingPort;

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("WebRTC Server")
                .setContentText(content)
                .setSmallIcon(android.R.drawable.ic_menu_share)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build();
    }

    private void updateNotification() {
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID, createNotification());
        }
    }

    // --- Signaling handler ---

    private class SignalingHandler implements WebRtcSignalingServer.SignalingCallback {
        @Override
        public void onOfferReceived(String sdp) {
            mainHandler.post(() -> handleOffer(sdp));
        }

        @Override
        public void onIceCandidateReceived(String candidateJson) {
            try {
                JSONObject json = new JSONObject(candidateJson);
                String sdp = json.optString("candidate");
                String sdpMid = json.optString("sdpMid");
                int sdpMLineIndex = json.optInt("sdpMLineIndex");

                IceCandidate candidate = new IceCandidate(sdpMid, sdpMLineIndex, sdp);
                peerConnection.addIceCandidate(candidate);

            } catch (Exception e) {
                Log.e(TAG, "Error processing ICE candidate", e);
            }
        }
    }

    // --- SdpObserver abstract base class ---

    private abstract class SdpObserver implements org.webrtc.SdpObserver {
    }
}
