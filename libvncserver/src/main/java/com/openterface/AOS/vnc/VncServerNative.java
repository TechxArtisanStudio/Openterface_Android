package com.openterface.AOS.vnc;

import java.nio.ByteBuffer;

/**
 * JNI wrapper for libvncserver.
 * All native methods are backed by libvncserver C library.
 */
public class VncServerNative {

    static {
        System.loadLibrary("vncserver");
    }

    /**
     * Start the VNC server.
     *
     * @param password VNC password (can be empty for no auth)
     * @param port     Port to listen on (default 5900)
     * @param width    Framebuffer width
     * @param height   Framebuffer height
     * @return Native handle (0 on failure)
     */
    public static native long vncServerStart(String password, int port, int width, int height, int encoding, int qualityLevel, int compressLevel);

    /**
     * Stop the VNC server and free resources.
     *
     * @param handle Native handle from vncServerStart
     */
    public static native void vncServerStop(long handle);

    /**
     * Push a raw framebuffer to the VNC server.
     * Frame data must be in RGBA/RGBX format (4 bytes per pixel).
     *
     * @param handle Native handle
     * @param buffer Direct ByteBuffer containing frame data
     * @param width  Frame width
     * @param height Frame height
     */
    public static native void vncServerPushFrame(long handle, ByteBuffer buffer, int width, int height);

    /**
     * Check if the server is still running.
     *
     * @param handle Native handle
     * @return true if running
     */
    public static native boolean vncServerIsRunning(long handle);

    /**
     * Set the callback object for client events.
     *
     * @param handle    Native handle
     * @param callbacks Object implementing callback methods:
     *                  - onClientConnected(String ip)
     *                  - onClientDisconnected(String ip)
     *                  - onPointerEvent(int buttonMask, int x, int y)
     *                  - onKeyboardEvent(int keysym, boolean down)
     */
    public static native void vncServerSetCallbacks(long handle, Object callbacks);
}
