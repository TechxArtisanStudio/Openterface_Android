package com.openterface.AOS.vnc;

import android.util.Log;

import com.serenegiant.usb.IFrameCallback;
import com.serenegiant.usb.UVCCamera;
import com.openterface.AOS.ICameraHelper;

import java.nio.ByteBuffer;

/**
 * Bridges UVC camera frames to the VNC server.
 * Uses IFrameCallback from libuvccamera to capture raw frames
 * and pushes them to VncServerService.
 */
public class VncFrameCapture {
    private static final String TAG = "VncFrameCapture";

    private final int targetFps;
    private final long frameIntervalNs;
    private long lastFrameTimeNs = 0;

    private VncServerService vncService;
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

            // Only push if VNC service is running and has a client
            if (vncService == null || !vncService.isRunning()) {
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

            vncService.pushFrame(reuseBuffer, width, height);
        }
    };

    /**
     * @param targetFps Target frame rate (0 = use 15)
     */
    public VncFrameCapture(int targetFps) {
        this.targetFps = targetFps > 0 ? targetFps : 15;
        this.frameIntervalNs = 1_000_000_000L / this.targetFps;
    }

    /**
     * Start capturing frames from the camera and pushing to VNC server.
     * Must be called after the camera is opened.
     */
    public void start(ICameraHelper cameraHelper, VncServerService vncService, int width, int height) {
        this.vncService = vncService;
        this.width = width;
        this.height = height;

        Log.i(TAG, "Starting frame capture: " + width + "x" + height + " @ " + targetFps + " fps");

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
        Log.i(TAG, "Frame capture stopped");
    }
}
