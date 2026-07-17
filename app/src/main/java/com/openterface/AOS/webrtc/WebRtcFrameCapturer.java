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
    private static final String TAG = "OP-WEBRTC";

    private CapturerObserver capturerObserver;
    private SurfaceTextureHelper surfaceTextureHelper;
    private int width;
    private int height;
    private int targetFps;
    private long frameIntervalNs;
    private long lastFrameTimeNs = 0;
    private int rotation = 0;

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

    public void setRotation(int rotation) {
        this.rotation = rotation;
    }

    public boolean isRunning() {
        return isRunning;
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
     * @param rotation   Frame rotation (0, 90, 180, or 270 degrees)
     */
    private int frameProcessedCount = 0;
    // Reuse buffer for RGBA-to-I420 conversion to avoid per-frame heap allocations
    // (1920x1080 RGBA = 8MB per frame at 30fps = 240MB/s of GC pressure)
    private byte[] rgbaConversionBuffer;

    public void onFrame(ByteBuffer rgbaBuffer, int width, int height, long timestampNs, int rotation) {
        if (!isRunning || rgbaBuffer == null) {
            Log.w(TAG, "Frame dropped early: isRunning=" + isRunning + " buffer=" + (rgbaBuffer != null));
            return;
        }

        // Take a local snapshot to avoid race condition with dispose()
        CapturerObserver observer = capturerObserver;
        if (observer == null) {
            Log.w(TAG, "Frame dropped: capturerObserver is null (disposed?)");
            return;
        }

        // Rate limiting
        long now = System.nanoTime();
        if (now - lastFrameTimeNs < frameIntervalNs) {
            return;
        }
        lastFrameTimeNs = now;

        frameProcessedCount++;
        if (frameProcessedCount <= 3 || frameProcessedCount % 30 == 0) {
            Log.i(TAG, "Processing frame: #" + frameProcessedCount + " " + width + "x" + height +
                    " rotation=" + rotation + " bufferPosition=" + rgbaBuffer.position() +
                    " bufferRemaining=" + rgbaBuffer.remaining());
        }

        try {
            // Convert RGBA to I420 for WebRTC
            JavaI420Buffer i420Buffer = convertRgbaToI420(rgbaBuffer, width, height);

            if (i420Buffer == null) {
                Log.e(TAG, "I420 buffer is null!");
                return;
            }

            // Create WebRTC VideoFrame with rotation
            VideoFrame videoFrame = new VideoFrame(i420Buffer, rotation, timestampNs);

            // Pass to WebRTC
            observer.onFrameCaptured(videoFrame);

            // Release the frame
            videoFrame.release();

            if (frameProcessedCount <= 3 || frameProcessedCount % 30 == 0) {
                Log.i(TAG, "Frame delivered to WebRTC: #" + frameProcessedCount);
            }

        } catch (Exception e) {
            Log.e(TAG, "Error processing frame", e);
        }
    }

    /**
     * Convert RGBA buffer to I420 (YUV420 planar) format for WebRTC.
     * Reuses cached byte array to avoid per-frame heap allocations.
     */
    private JavaI420Buffer convertRgbaToI420(ByteBuffer rgbaBuffer, int width, int height) {
        JavaI420Buffer i420Buffer = JavaI420Buffer.allocate(width, height);

        ByteBuffer yPlane = i420Buffer.getDataY();
        ByteBuffer uPlane = i420Buffer.getDataU();
        ByteBuffer vPlane = i420Buffer.getDataV();

        int yStride = i420Buffer.getStrideY();
        int uStride = i420Buffer.getStrideU();
        int vStride = i420Buffer.getStrideV();

        int originalPosition = rgbaBuffer.position();
        rgbaBuffer.rewind();

        int frameSize = rgbaBuffer.remaining();
        // Reuse cached buffer to avoid allocating 8MB per frame (1920x1080 RGBA)
        if (rgbaConversionBuffer == null || rgbaConversionBuffer.length < frameSize) {
            rgbaConversionBuffer = new byte[frameSize];
        }
        rgbaBuffer.get(rgbaConversionBuffer, 0, frameSize);
        rgbaBuffer.position(originalPosition);

        byte[] rgba = rgbaConversionBuffer;

        // Pre-calculate constants for YUV conversion
        final int[] rCoeff = {66, -38, 112};
        final int[] gCoeff = {129, -74, -94};
        final int[] bCoeff = {25, 112, -18};
        final int[] offset = {16, 128, 128};

        // Process Y plane (full resolution)
        for (int j = 0; j < height; j++) {
            int yLineOffset = j * yStride;
            int rgbaRowStart = j * width * 4;
            for (int i = 0; i < width; i++) {
                int rgbaIndex = rgbaRowStart + i * 4;
                int r = rgba[rgbaIndex] & 0xFF;
                int g = rgba[rgbaIndex + 1] & 0xFF;
                int b = rgba[rgbaIndex + 2] & 0xFF;

                // Y = 0.257*R + 0.504*G + 0.098*B + 16
                int y = ((rCoeff[0] * r + gCoeff[0] * g + bCoeff[0] * b + 128) >> 8) + offset[0];
                yPlane.put(yLineOffset + i, (byte) Math.max(16, Math.min(235, y)));
            }
        }

        // Process U/V planes (quarter resolution)
        for (int j = 0; j < height; j += 2) {
            int uvRow = j / 2;
            int rgbaRowStart = j * width * 4;
            for (int i = 0; i < width; i += 2) {
                int rgbaIndex = rgbaRowStart + i * 4;
                int r = rgba[rgbaIndex] & 0xFF;
                int g = rgba[rgbaIndex + 1] & 0xFF;
                int b = rgba[rgbaIndex + 2] & 0xFF;

                // U = -0.148*R - 0.291*G + 0.439*B + 128
                int u = ((rCoeff[1] * r + gCoeff[1] * g + bCoeff[1] * b + 128) >> 8) + offset[1];
                // V = 0.439*R - 0.368*G - 0.071*B + 128
                int v = ((rCoeff[2] * r + gCoeff[2] * g + bCoeff[2] * b + 128) >> 8) + offset[2];

                uPlane.put(uvRow * uStride + i/2, (byte) Math.max(0, Math.min(255, u)));
                vPlane.put(uvRow * vStride + i/2, (byte) Math.max(0, Math.min(255, v)));
            }
        }

        return i420Buffer;
    }
}
