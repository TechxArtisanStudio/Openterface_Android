package com.openterface.AOS.vnc;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import com.openterface.AOS.R;
import com.openterface.AOS.activity.MainActivity;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import androidx.core.app.NotificationCompat;

/**
 * Foreground service that manages the VNC server lifecycle.
 * Uses JNI to interface with libvncserver.
 */
public class VncServerService extends Service {
    private static final String TAG = "OP-VNC";
    private static final int NOTIFICATION_ID = 1001;
    private static final String CHANNEL_ID = "vnc_server_channel";

    // Current VNC server configuration
    private String password = "";
    private int port = 5900;
    private int width = 0;
    private int height = 0;
    private int encoding = VncServerConfig.ENCODING_AUTO;
    private int qualityLevel = VncServerConfig.QUALITY_MEDIUM;
    private int compressLevel = VncServerConfig.COMPRESS_MEDIUM;

    // Server state
    private long nativeHandle = 0;
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private final AtomicBoolean isClientConnected = new AtomicBoolean(false);
    private final AtomicReference<String> connectedClientIp = new AtomicReference<>("");

    // Callback for MainActivity
    private VncServerCallback callback;

    // JNI callbacks adapter
    private final Object vncCallbacks = new Object() {
        public void onClientConnected(String ip) {
            isClientConnected.set(true);
            connectedClientIp.set(ip);
            Log.i(TAG, "Client connected: " + ip);
            if (callback != null) callback.onClientConnected(ip);
            updateNotification(ip);
        }

        public void onClientDisconnected(String ip) {
            isClientConnected.set(false);
            connectedClientIp.set("");
            Log.i(TAG, "Client disconnected: " + ip);
            if (callback != null) callback.onClientDisconnected(ip);
            updateNotification(null);
        }

        public void onPointerEvent(int buttonMask, int x, int y) {
            if (callback != null) callback.onPointerEvent(buttonMask, x, y);
        }

        public void onKeyboardEvent(int keysym, boolean down) {
            if (callback != null) callback.onKeyboardEvent(keysym, down);
        }

        public void onEncodingChanged(String ip, String encoding) {
            Log.i(TAG, "Client " + ip + " using encoding: " + encoding);
            if (callback != null) callback.onEncodingChanged(ip, encoding);
        }
    };

    // Binder for local binding
    private final IBinder binder = new LocalBinder();

    public class LocalBinder extends Binder {
        public VncServerService getService() {
            return VncServerService.this;
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

    /**
     * Set the callback for VNC events.
     */
    public void setCallback(VncServerCallback cb) {
        this.callback = cb;
    }

    /**
     * Start the VNC server.
     *
     * @param password VNC password
     * @param port     Port to listen on
     * @param width    Framebuffer width (must match UVC resolution)
     * @param height   Framebuffer height
     * @param encoding Preferred encoding (ENCODING_AUTO for auto-negotiation)
     * @param quality  JPEG quality level for Tight encoding (-1 = auto, 0-100)
     * @param compress Compression level for Tight encoding (-1 = auto, 0-9)
     * @return true if started successfully
     */
    public boolean startServer(String password, int port, int width, int height, int encoding, int quality, int compress) {
        if (isRunning.get()) {
            Log.w(TAG, "Server is already running");
            return false;
        }

        this.password = password != null ? password : "";
        this.port = port;
        this.width = width;
        this.height = height;
        this.encoding = encoding;
        this.qualityLevel = quality;
        this.compressLevel = compress;

        Log.i(TAG, "Starting VNC server: port=" + port + ", " + width + "x" + height + ", encoding=" + encoding + ", quality=" + quality + ", compress=" + compress);

        nativeHandle = VncServerNative.vncServerStart(this.password, this.port, this.width, this.height, this.encoding, this.qualityLevel, this.compressLevel);
        if (nativeHandle == 0) {
            Log.e(TAG, "Failed to start VNC server");
            return false;
        }

        // Register callbacks
        VncServerNative.vncServerSetCallbacks(nativeHandle, vncCallbacks);

        isRunning.set(true);

        // Start foreground service with notification
        startForeground(NOTIFICATION_ID, createNotification(null));

        Log.i(TAG, "VNC server started");
        return true;
    }

    /**
     * Stop the VNC server.
     */
    public void stopServer() {
        if (!isRunning.get()) {
            return;
        }

        Log.i(TAG, "Stopping VNC server");
        isRunning.set(false);

        if (nativeHandle != 0) {
            VncServerNative.vncServerStop(nativeHandle);
            nativeHandle = 0;
        }

        stopForeground(true);
        Log.i(TAG, "VNC server stopped");
    }

    /**
     * Push a frame to the VNC server.
     *
     * @param frame  Direct ByteBuffer containing RGBA frame data
     * @param width  Frame width
     * @param height Frame height
     */
    public void pushFrame(java.nio.ByteBuffer frame, int width, int height) {
        if (!isRunning.get() || nativeHandle == 0) return;
        VncServerNative.vncServerPushFrame(nativeHandle, frame, width, height);
    }

    /**
     * Check if server is running.
     */
    public boolean isRunning() {
        return isRunning.get();
    }

    /**
     * Check if a client is connected.
     */
    public boolean isClientConnected() {
        return isClientConnected.get();
    }

    /**
     * Get the connected client IP.
     */
    public String getConnectedClientIp() {
        return connectedClientIp.get();
    }

    /**
     * Get the server port.
     */
    public int getPort() {
        return port;
    }

    /**
     * Get device IP addresses.
     */
    public List<String> getDeviceIpAddresses() {
        List<String> ips = new LinkedList<>();
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            for (NetworkInterface iface : Collections.list(interfaces)) {
                if (iface.isUp()) {
                    Enumeration<InetAddress> addresses = iface.getInetAddresses();
                    for (InetAddress addr : Collections.list(addresses)) {
                        if (!addr.isLoopbackAddress() && addr instanceof java.net.Inet4Address) {
                            ips.add(addr.getHostAddress());
                        }
                    }
                }
            }
        } catch (SocketException e) {
            Log.e(TAG, "Failed to get IP addresses", e);
        }
        return ips;
    }

    // --- Notification ---

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "VNC Server",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("VNC Server status");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private Notification createNotification(String connectedIp) {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        String content = connectedIp != null
                ? "Client connected: " + connectedIp
                : "Listening on port " + port + " (waiting for client)";

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("VNC Server")
                .setContentText(content)
                .setSmallIcon(android.R.drawable.ic_menu_share)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build();
    }

    private void updateNotification(String connectedIp) {
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID, createNotification(connectedIp));
        }
    }
}
