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
 * Bridges the IFrameCallback NV12 frames from libuvccamera to WebRTC's VideoFrame pipeline.
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
     * The frame must be in PIXEL_FORMAT_NV12 format.
     *
     * @param nv12Buffer NV12 buffer from UVC camera
     * @param width      Frame width (must match capturer width)
     * @param height     Frame height (must match capturer height)
     * @param timestampNs Frame timestamp in nanoseconds
     * @param rotation   Frame rotation (0, 90, 180, or 270 degrees)
     */
    private int frameProcessedCount = 0;

    public void onFrame(ByteBuffer nv12Buffer, int width, int height, long timestampNs, int rotation) {
        if (!isRunning || nv12Buffer == null) {
            Log.w(TAG, "Frame dropped early: isRunning=" + isRunning + " buffer=" + (nv12Buffer != null));
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
                    " rotation=" + rotation + " bufferPosition=" + nv12Buffer.position() +
                    " bufferRemaining=" + nv12Buffer.remaining());
        }

        try {
            // Convert NV12 to I420 for WebRTC
            JavaI420Buffer i420Buffer = convertNv12ToI420(nv12Buffer, width, height);

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
     * Convert NV12 buffer to I420 (YUV420 planar) format for WebRTC.
     * NV12 has Y plane followed by interleaved UV plane.
     * I420 has separate Y, U, V planes.
     * This is much faster than RGBA conversion as it only involves memory rearrangement.
     */
    private JavaI420Buffer convertNv12ToI420(ByteBuffer nv12Buffer, int width, int height) {
        int ySize = width * height;
        int uvSize = ySize / 2; // UV interleaved data size

        // Allocate I420 buffer
        JavaI420Buffer i420Buffer = JavaI420Buffer.allocate(width, height);

        // Get direct buffers from I420 buffer
        ByteBuffer yPlane = i420Buffer.getDataY();
        ByteBuffer uPlane = i420Buffer.getDataU();
        ByteBuffer vPlane = i420Buffer.getDataV();

        int yStride = i420Buffer.getStrideY();
        int uStride = i420Buffer.getStrideU();
        int vStride = i420Buffer.getStrideV();

        // Make sure nv12 buffer is at position 0
        int originalPosition = nv12Buffer.position();
        nv12Buffer.rewind();

        // Check if NV12 buffer has expected size
        int expectedSize = ySize + uvSize; // Y + UV (interleaved)
        int actualSize = nv12Buffer.remaining();
        if (actualSize < expectedSize) {
            Log.e(TAG, "NV12 buffer too small: expected=" + expectedSize + " actual=" + actualSize);
            nv12Buffer.position(originalPosition);
            return null;
        }

        // Copy Y plane directly (same layout in NV12 and I420)
        byte[] yData = new byte[ySize];
        nv12Buffer.get(yData);
        for (int j = 0; j < height; j++) {
            yPlane.put(j * yStride, yData, j * width, width);
        }

        // De-interleave UV plane (NV12: UVUVUV... -> I420: UUU... VVV...)
        // NV12 UV data starts at offset ySize
        byte[] uvData = new byte[uvSize];
        nv12Buffer.get(uvData);

        // De-interleave U and V
        int uvWidth = width / 2;
        int uvHeight = height / 2;
        for (int j = 0; j < uvHeight; j++) {
            for (int i = 0; i < uvWidth; i++) {
                int uvIndex = j * width + i * 2; // NV12 UV is interleaved, row stride = width
                int uIndex = j * uStride + i;
                int vIndex = j * vStride + i;
                uPlane.put(uIndex, uvData[uvIndex]);
                vPlane.put(vIndex, uvData[uvIndex + 1]);
            }
        }

        nv12Buffer.position(originalPosition);

        // Diagnostic: verify I420 output
        if (frameProcessedCount <= 5) {
            yPlane.rewind();
            byte firstY = yPlane.get(0);
            byte midY = yPlane.get(ySize / 2);
            uPlane.rewind();
            byte firstU = uPlane.get(0);
            vPlane.rewind();
            byte firstV = vPlane.get(0);
            Log.i(TAG, "NV12->I420 diagnostic #" + frameProcessedCount +
                    " firstY=" + (firstY & 0xFF) +
                    " midY=" + (midY & 0xFF) +
                    " firstU=" + (firstU & 0xFF) +
                    " firstV=" + (firstV & 0xFF) +
                    " yStride=" + yStride + " uStride=" + uStride + " vStride=" + vStride);
        }

        return i420Buffer;
    }
}
