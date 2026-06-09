package com.openterface.AOS.vnc;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Manages VNC server configuration persistence.
 */
public class VncServerConfig {
    private static final String PREFS_NAME = "vnc_server_config";
    private static final String KEY_PORT = "vnc_port";
    private static final String KEY_PASSWORD = "vnc_password";
    private static final String KEY_AUTO_START = "vnc_auto_start";
    private static final String KEY_BIND_ADDRESS = "vnc_bind_address";
    private static final String KEY_ENCODING = "vnc_encoding";
    private static final String KEY_QUALITY = "vnc_quality";
    private static final String KEY_COMPRESS = "vnc_compress";

    // Encoding constants matching RFB protocol
    public static final int ENCODING_AUTO = -1;    // Client decides (default)
    public static final int ENCODING_TIGHT = 7;
    public static final int ENCODING_ZLIB = 6;
    public static final int ENCODING_ZRLE = 16;
    public static final int ENCODING_HEXTILE = 5;

    // Quality levels for Tight encoding (JPEG quality 0-100)
    public static final int QUALITY_AUTO = -1;     // Client decides
    public static final int QUALITY_LOW = 20;      // Best compression
    public static final int QUALITY_MEDIUM = 50;   // Balanced
    public static final int QUALITY_HIGH = 80;     // Best quality

    // Compression levels for Tight encoding (mapped to libvncserver internal levels 1-3)
    public static final int COMPRESS_AUTO = -1;    // Client decides
    public static final int COMPRESS_FAST = 1;     // Fastest (least CPU)
    public static final int COMPRESS_MEDIUM = 2;   // Balanced
    public static final int COMPRESS_MAX = 3;      // Maximum (most CPU)

    private final SharedPreferences prefs;

    public VncServerConfig(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public int getPort() {
        return prefs.getInt(KEY_PORT, 5900);
    }

    public void setPort(int port) {
        prefs.edit().putInt(KEY_PORT, port).apply();
    }

    public String getPassword() {
        return prefs.getString(KEY_PASSWORD, "");
    }

    public void setPassword(String password) {
        prefs.edit().putString(KEY_PASSWORD, password).apply();
    }

    public boolean isAutoStart() {
        return prefs.getBoolean(KEY_AUTO_START, false);
    }

    public void setAutoStart(boolean autoStart) {
        prefs.edit().putBoolean(KEY_AUTO_START, autoStart).apply();
    }

    public String getBindAddress() {
        return prefs.getString(KEY_BIND_ADDRESS, "0.0.0.0");
    }

    public void setBindAddress(String address) {
        prefs.edit().putString(KEY_BIND_ADDRESS, address).apply();
    }

    public int getEncoding() {
        return prefs.getInt(KEY_ENCODING, ENCODING_AUTO);
    }

    public void setEncoding(int encoding) {
        prefs.edit().putInt(KEY_ENCODING, encoding).apply();
    }

    public int getQualityLevel() {
        return prefs.getInt(KEY_QUALITY, QUALITY_MEDIUM);
    }

    public void setQualityLevel(int quality) {
        prefs.edit().putInt(KEY_QUALITY, quality).apply();
    }

    public int getCompressLevel() {
        return prefs.getInt(KEY_COMPRESS, COMPRESS_MEDIUM);
    }

    public void setCompressLevel(int compress) {
        prefs.edit().putInt(KEY_COMPRESS, compress).apply();
    }
}
