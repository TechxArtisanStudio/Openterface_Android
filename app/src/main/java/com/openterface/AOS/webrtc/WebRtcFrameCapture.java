package com.openterface.AOS.webrtc;

import android.util.Log;

import com.openterface.AOS.ICameraHelper;
import com.serenegiant.usb.IFrameCallback;
import com.serenegiant.usb.UVCCamera;

import java.nio.ByteBuffer;

/**
 * Bridges UVC camera frames to the WebRTC server.
 * Uses IFrameCallback from libuvccamera to capture raw frames
 * and pushes them to WebRtcServerService.
 *
 * Similar pattern to VncFrameCapture, but feeds into WebRTC's
 * VideoCapturer pipeline instead of raw RGBX push.
 */
public class WebRtcFrameCapture {
    private static final String TAG = "OP-WEBRTC";

    private final int targetFps;
    private final long frameIntervalNs;
    private long lastFrameTimeNs = 0;

    private WebRtcServerService webRtcService;
    private int width;
    private int height;
    private ByteBuffer reuseBuffer;

    private final IFrameCallback frameCallback = new IFrameCallback() {
        @Override
        public void onFrame(ByteBuffer frame) {
            // Rate limiting
            long now = System.nanoTime();
            if (now - lastFrameTimeNs < frameIntervalNs) {
                return;
            }
            lastFrameTimeNs = now;

            // Only push if WebRTC service is running
            if (webRtcService == null || !webRtcService.isRunning()) {
                return;
            }

            int frameSize = frame.remaining();

            // Reuse buffer to avoid allocations
            if (reuseBuffer == null || reuseBuffer.capacity() < frameSize) {
                reuseBuffer = ByteBuffer.allocateDirect(frameSize);
            }

            // Copy frame data (frame is a direct buffer from JNI)
            frame.rewind();
            reuseBuffer.rewind();
            reuseBuffer.put(frame);
            reuseBuffer.rewind();

            // Push to WebRTC server
            webRtcService.onUvcFrame(reuseBuffer, width, height, now);
        }
    };

    /**
     * @param targetFps Target frame rate (0 = use 30)
     */
    public WebRtcFrameCapture(int targetFps) {
        this.targetFps = targetFps > 0 ? targetFps : 30;
        this.frameIntervalNs = 1_000_000_000L / this.targetFps;
    }

    /**
     * Start capturing frames from the camera and pushing to WebRTC server.
     * Must be called after the camera is opened.
     */
    public void start(ICameraHelper cameraHelper, WebRtcServerService webRtcService, int width, int height) {
        this.webRtcService = webRtcService;
        this.width = width;
        this.height = height;

        Log.i(TAG, "Starting WebRTC frame capture: " + width + "x" + height + " @ " + targetFps + " fps");

        // Start WebRTC video capturer
        webRtcService.startVideoCapture(width, height, targetFps);

        cameraHelper.setFrameCallback(frameCallback, UVCCamera.PIXEL_FORMAT_RGBX);
    }

    /**
     * Stop frame capture.
     */
    public void stop(ICameraHelper cameraHelper) {
        if (cameraHelper != null) {
            cameraHelper.setFrameCallback(null, 0);
        }
        reuseBuffer = null;
        Log.i(TAG, "WebRTC frame capture stopped");
    }
}
