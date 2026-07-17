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
    // Skip the first few frames after startup to let the camera pipeline
    // stabilize after pixel format change. Pushing unstable frames would
    // cause WebRTC's encoder to start with a poor keyframe (blurriness).
    private static final int STARTUP_SKIP_FRAMES = 5;

    private final int targetFps;
    private final long frameIntervalNs;
    private long lastFrameTimeNs = 0;

    // FPS monitoring
    private long lastFpsLogTimeNs = 0;
    private int framesDeliveredLastSecond = 0;
    private int framesDroppedLastSecond = 0;

    private WebRtcServerService webRtcService;
    private int width;
    private int height;
    private int rotation;
    private ByteBuffer reuseBuffer;
    private volatile int startupFramesSkipped = 0;

    private final IFrameCallback frameCallback = new IFrameCallback() {
        private int frameCount = 0;

        @Override
        public void onFrame(ByteBuffer frame) {
            frameCount++;
            long now = System.nanoTime();

            // FPS monitoring
            if (lastFpsLogTimeNs == 0) {
                lastFpsLogTimeNs = now;
            }
            long elapsed = now - lastFpsLogTimeNs;
            if (elapsed >= 1_000_000_000L) {
                Log.i(TAG, "FPS monitor: delivered=" + framesDeliveredLastSecond +
                        " dropped=" + framesDroppedLastSecond + " target=" + targetFps +
                        " (" + width + "x" + height + ")");
                framesDeliveredLastSecond = 0;
                framesDroppedLastSecond = 0;
                lastFpsLogTimeNs = now;
            }

            if (frameCount <= 3 || frameCount % 30 == 0) {
                Log.i(TAG, "Frame callback invoked: #" + frameCount + " buffer=" + (frame != null));
            }

            // Rate limiting
            if (now - lastFrameTimeNs < frameIntervalNs) {
                framesDroppedLastSecond++;
                return;
            }
            lastFrameTimeNs = now;

            // Skip initial frames after start() to let the camera pipeline
            // stabilize after setFrameCallback pixel format change.
            // Pushing unstable frames would give WebRTC encoder a poor keyframe.
            if (startupFramesSkipped < STARTUP_SKIP_FRAMES) {
                startupFramesSkipped++;
                framesDroppedLastSecond++;
                return;
            }

            // Only push if WebRTC service is running
            if (webRtcService == null || !webRtcService.isRunning()) {
                framesDroppedLastSecond++;
                Log.w(TAG, "WebRTC service not running, skipping frame");
                return;
            }

            // IMPORTANT: rewind BEFORE reading remaining() to ensure we get full buffer size
            frame.rewind();
            int frameSize = frame.remaining();
            if (frameSize <= 0) {
                Log.w(TAG, "Frame size is 0, skipping");
                return;
            }

            // Reuse buffer to avoid allocations
            if (reuseBuffer == null || reuseBuffer.capacity() < frameSize) {
                reuseBuffer = ByteBuffer.allocateDirect(frameSize);
            }

            // Copy frame data (frame is a direct buffer from JNI)
            // IMPORTANT: rewind frame again - remaining() above pushed position to the end
            frame.rewind();
            reuseBuffer.clear();
            reuseBuffer.put(frame);
            reuseBuffer.flip();

            // Push to WebRTC server
            webRtcService.onUvcFrame(reuseBuffer, width, height, now, rotation);
            framesDeliveredLastSecond++;

            if (frameCount <= 3 || frameCount % 30 == 0) {
                Log.i(TAG, "Frame pushed to WebRTC: #" + frameCount + " size=" + frameSize +
                        " " + width + "x" + height);
            }
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
    public void start(ICameraHelper cameraHelper, WebRtcServerService webRtcService, int width, int height, int rotation) {
        this.webRtcService = webRtcService;
        this.width = width;
        this.height = height;
        this.rotation = rotation;
        this.startupFramesSkipped = 0;  // Reset startup skip counter for fresh start

        Log.i(TAG, "Starting WebRTC frame capture: " + width + "x" + height + " @ " + targetFps + " fps, rotation=" + rotation);

        // Start WebRTC video capturer
        webRtcService.startVideoCapture(width, height, targetFps, rotation);

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
