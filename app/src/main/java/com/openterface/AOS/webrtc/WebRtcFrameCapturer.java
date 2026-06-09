package com.openterface.AOS.webrtc;

import android.graphics.ImageFormat;
import android.util.Log;

import org.webrtc.JavaI420Buffer;
import org.webrtc.VideoFrame;
import org.webrtc.VideoCapturer;
import org.webrtc.CapturerObserver;
import org.webrtc.SurfaceTextureHelper;

import java.nio.ByteBuffer;

/**
 * Custom WebRTC VideoCapturer that feeds UVC camera frames into WebRTC.
 * Bridges the IFrameCallback RGBA frames from libuvccamera to WebRTC's VideoFrame pipeline.
 * WebRTC's internal encoder (MediaCodec H264) handles the rest automatically.
 */
public class WebRtcFrameCapturer implements VideoCapturer {
    private static final String TAG = "WebRtcFrameCapturer";

    private CapturerObserver capturerObserver;
    private SurfaceTextureHelper surfaceTextureHelper;
    private int width;
    private int height;
    private int targetFps;
    private long frameIntervalNs;
    private long lastFrameTimeNs = 0;

    private volatile boolean isRunning;

    /**
     * @param width  Frame width
     * @param height Frame height
     * @param fps    Target frame rate
     */
    public WebRtcFrameCapturer(int width, int height, int fps) {
        this.width = width;
        this.height = height;
        this.targetFps = fps > 0 ? fps : 30;
        this.frameIntervalNs = 1_000_000_000L / this.targetFps;
        Log.i(TAG, "Created capturer: " + width + "x" + height + " @ " + this.targetFps + "fps");
    }

    @Override
    public void initialize(SurfaceTextureHelper surfaceTextureHelper, android.content.Context context,
                           CapturerObserver capturerObserver) {
        this.capturerObserver = capturerObserver;
        this.surfaceTextureHelper = surfaceTextureHelper;
        this.isRunning = true;

        capturerObserver.onCapturerStarted(true);

        Log.i(TAG, "Capturer initialized");
    }

    @Override
    public void startCapture(int width, int height, int fps) {
        this.width = width;
        this.height = height;
        this.targetFps = fps;
        this.frameIntervalNs = 1_000_000_000L / fps;
        Log.i(TAG, "Start capture: " + width + "x" + height + " @ " + fps + "fps");
    }

    @Override
    public void stopCapture() {
        isRunning = false;
        if (capturerObserver != null) {
            capturerObserver.onCapturerStopped();
        }
        Log.i(TAG, "Capture stopped");
    }

    @Override
    public void changeCaptureFormat(int width, int height, int fps) {
        this.width = width;
        this.height = height;
        this.targetFps = fps;
        this.frameIntervalNs = 1_000_000_000L / fps;
        Log.i(TAG, "Format changed: " + width + "x" + height + " @ " + fps + "fps");
    }

    @Override
    public boolean isScreencast() {
        return false;
    }

    @Override
    public void dispose() {
        isRunning = false;
        capturerObserver = null;
        surfaceTextureHelper = null;
        Log.i(TAG, "Capturer disposed");
    }

    /**
     * Feed a frame from the UVC camera into WebRTC.
     * The frame must be in PIXEL_FORMAT_RGBX format.
     *
     * @param rgbaBuffer RGBA buffer from UVC camera
     * @param width      Frame width (must match capturer width)
     * @param height     Frame height (must match capturer height)
     * @param timestampNs Frame timestamp in nanoseconds
     */
    public void onFrame(ByteBuffer rgbaBuffer, int width, int height, long timestampNs) {
        if (!isRunning || capturerObserver == null || rgbaBuffer == null) {
            return;
        }

        // Rate limiting
        long now = System.nanoTime();
        if (now - lastFrameTimeNs < frameIntervalNs) {
            return;
        }
        lastFrameTimeNs = now;

        try {
            // Convert RGBA to I420 for WebRTC
            JavaI420Buffer i420Buffer = convertRgbaToI420(rgbaBuffer, width, height);

            // Create WebRTC VideoFrame
            VideoFrame videoFrame = new VideoFrame(i420Buffer, 0, timestampNs);

            // Pass to WebRTC
            capturerObserver.onFrameCaptured(videoFrame);

            // Release the frame
            videoFrame.release();

        } catch (Exception e) {
            Log.e(TAG, "Error processing frame", e);
        }
    }

    /**
     * Convert RGBA buffer to I420 (YUV420 planar) format for WebRTC.
     * This is the standard format WebRTC expects.
     */
    private JavaI420Buffer convertRgbaToI420(ByteBuffer rgbaBuffer, int width, int height) {
        int ySize = width * height;
        int uvSize = ySize / 4;

        // Allocate I420 buffer
        JavaI420Buffer i420Buffer = JavaI420Buffer.allocate(width, height);

        // Get direct buffers from I420 buffer
        ByteBuffer yPlane = i420Buffer.getDataY();
        ByteBuffer uPlane = i420Buffer.getDataU();
        ByteBuffer vPlane = i420Buffer.getDataV();

        int yStride = i420Buffer.getStrideY();
        int uStride = i420Buffer.getStrideU();
        int vStride = i420Buffer.getStrideV();

        // Make sure rgba buffer is at position 0
        int originalPosition = rgbaBuffer.position();
        rgbaBuffer.rewind();

        byte[] rgba = new byte[rgbaBuffer.remaining()];
        rgbaBuffer.get(rgba);
        rgbaBuffer.position(originalPosition);

        int frameSize = width * height;

        // Convert RGBA to I420
        int yPos = 0;
        int uPos = 0;
        int vPos = 0;

        for (int j = 0; j < height; j++) {
            int yLineOffset = (j * yStride);
            for (int i = 0; i < width; i++) {
                int rgbaIndex = (j * width + i) * 4;
                int r = rgba[rgbaIndex] & 0xFF;
                int g = rgba[rgbaIndex + 1] & 0xFF;
                int b = rgba[rgbaIndex + 2] & 0xFF;

                // Y component
                int y = ((66 * r + 129 * g + 25 * b + 128) >> 8) + 16;
                yPlane.put(yLineOffset + i, (byte) Math.max(16, Math.min(235, y)));

                // U and V components for every 2x2 block
                if (j % 2 == 0 && i % 2 == 0) {
                    int uvRow = j / 2;
                    int uvCol = i / 2;

                    // U = -0.169R - 0.331G + 0.5B + 128
                    int u = ((-38 * r - 74 * g + 112 * b + 128) >> 8) + 128;
                    uPlane.put(uvRow * uStride + uvCol, (byte) Math.max(0, Math.min(255, u)));

                    // V = 0.5R - 0.419G - 0.081B + 128
                    int v = ((112 * r - 94 * g - 18 * b + 128) >> 8) + 128;
                    vPlane.put(uvRow * vStride + uvCol, (byte) Math.max(0, Math.min(255, v)));
                }
            }
        }

        return i420Buffer;
    }
}
