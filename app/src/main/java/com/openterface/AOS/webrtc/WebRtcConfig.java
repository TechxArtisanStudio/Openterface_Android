package com.openterface.AOS.webrtc;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Manages WebRTC server configuration persistence.
 */
public class WebRtcConfig {
    private static final String PREFS_NAME = "webrtc_server_config";
    private static final String KEY_SIGNALLING_PORT = "webrtc_signalling_port";
    private static final String KEY_VIDEO_BITRATE = "webrtc_video_bitrate";
    private static final String KEY_VIDEO_WIDTH = "webrtc_video_width";
    private static final String KEY_VIDEO_HEIGHT = "webrtc_video_height";
    private static final String KEY_VIDEO_FPS = "webrtc_video_fps";
    private static final String KEY_STUN_SERVER = "webrtc_stun_server";
    private static final String KEY_ENABLE_TURN = "webrtc_enable_turn";
    private static final String KEY_TURN_SERVER = "webrtc_turn_server";
    private static final String KEY_TURN_USERNAME = "webrtc_turn_username";
    private static final String KEY_TURN_PASSWORD = "webrtc_turn_password";
    private static final String KEY_AUTO_START = "webrtc_auto_start";

    // Default values
    public static final int DEFAULT_SIGNALLING_PORT = 8080;
    public static final int DEFAULT_VIDEO_BITRATE = 2_000_000; // 2 Mbps
    public static final int DEFAULT_VIDEO_WIDTH = 1280;
    public static final int DEFAULT_VIDEO_HEIGHT = 720;
    public static final int DEFAULT_VIDEO_FPS = 30;
    public static final String DEFAULT_STUN_SERVER = "stun:stun.l.google.com:19302";

    private final SharedPreferences prefs;

    public WebRtcConfig(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public int getSignallingPort() {
        return prefs.getInt(KEY_SIGNALLING_PORT, DEFAULT_SIGNALLING_PORT);
    }

    public void setSignallingPort(int port) {
        prefs.edit().putInt(KEY_SIGNALLING_PORT, port).apply();
    }

    public int getVideoBitrate() {
        return prefs.getInt(KEY_VIDEO_BITRATE, DEFAULT_VIDEO_BITRATE);
    }

    public void setVideoBitrate(int bitrate) {
        prefs.edit().putInt(KEY_VIDEO_BITRATE, bitrate).apply();
    }

    public int getVideoWidth() {
        return prefs.getInt(KEY_VIDEO_WIDTH, DEFAULT_VIDEO_WIDTH);
    }

    public void setVideoWidth(int width) {
        prefs.edit().putInt(KEY_VIDEO_WIDTH, width).apply();
    }

    public int getVideoHeight() {
        return prefs.getInt(KEY_VIDEO_HEIGHT, DEFAULT_VIDEO_HEIGHT);
    }

    public void setVideoHeight(int height) {
        prefs.edit().putInt(KEY_VIDEO_HEIGHT, height).apply();
    }

    public int getVideoFps() {
        return prefs.getInt(KEY_VIDEO_FPS, DEFAULT_VIDEO_FPS);
    }

    public void setVideoFps(int fps) {
        prefs.edit().putInt(KEY_VIDEO_FPS, fps).apply();
    }

    public String getStunServer() {
        return prefs.getString(KEY_STUN_SERVER, DEFAULT_STUN_SERVER);
    }

    public void setStunServer(String server) {
        prefs.edit().putString(KEY_STUN_SERVER, server).apply();
    }

    public boolean isTurnEnabled() {
        return prefs.getBoolean(KEY_ENABLE_TURN, false);
    }

    public void setTurnEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_ENABLE_TURN, enabled).apply();
    }

    public String getTurnServer() {
        return prefs.getString(KEY_TURN_SERVER, "");
    }

    public void setTurnServer(String server) {
        prefs.edit().putString(KEY_TURN_SERVER, server).apply();
    }

    public String getTurnUsername() {
        return prefs.getString(KEY_TURN_USERNAME, "");
    }

    public void setTurnUsername(String username) {
        prefs.edit().putString(KEY_TURN_USERNAME, username).apply();
    }

    public String getTurnPassword() {
        return prefs.getString(KEY_TURN_PASSWORD, "");
    }

    public void setTurnPassword(String password) {
        prefs.edit().putString(KEY_TURN_PASSWORD, password).apply();
    }

    public boolean isAutoStart() {
        return prefs.getBoolean(KEY_AUTO_START, false);
    }

    public void setAutoStart(boolean autoStart) {
        prefs.edit().putBoolean(KEY_AUTO_START, autoStart).apply();
    }

    /**
     * Get the connection URL that clients should open in their browser.
     * Format: http://device-ip:signalling-port
     */
    public String getConnectionUrl(String deviceIp) {
        return "http://" + deviceIp + ":" + getSignallingPort();
    }
}
