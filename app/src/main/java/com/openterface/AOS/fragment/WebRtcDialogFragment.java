package com.openterface.AOS.fragment;

import android.app.Dialog;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import androidx.appcompat.widget.SwitchCompat;

import com.openterface.AOS.R;
import com.openterface.AOS.webrtc.WebRtcConfig;
import com.openterface.AOS.webrtc.WebRtcServerService;

import java.util.List;

/**
 * Dialog fragment for WebRTC Server settings.
 * Allows starting/stopping the WebRTC server and configuring signaling port.
 */
public class WebRtcDialogFragment extends DialogFragment {

    private static final String TAG = "WebRtcDialog";

    private WebRtcConfig config;
    private WebRtcServerService webRtcService;

    // UI elements
    private TextView tvStatus;
    private TextView tvIpAddress;
    private TextView tvUrl;
    private EditText etPort;
    private EditText etFps;
    private EditText etBitrate;
    private SwitchCompat swAutoStart;
    private Button btnStart;
    private Button btnStop;

    // Callback listener
    private OnWebRtcSettingsListener listener;

    private int previewWidth = 1920;
    private int previewHeight = 1080;

    // VNC state for mutual exclusion validation
    private boolean vncAutoStartEnabled;
    private boolean vncServerRunning;

    public interface OnWebRtcSettingsListener {
        void onServerStarted();
        void onServerStopped();
        void onSettingsChanged();
    }

    public void setWebRtcService(WebRtcServerService service) {
        this.webRtcService = service;
    }

    public void setWebRtcConfig(WebRtcConfig config) {
        this.config = config;
    }

    public void setListener(OnWebRtcSettingsListener listener) {
        this.listener = listener;
    }

    public void setPreviewSize(int width, int height) {
        this.previewWidth = width;
        this.previewHeight = height;
    }

    public void setVncAutoStartEnabled(boolean enabled) {
        this.vncAutoStartEnabled = enabled;
    }

    public void setVncServerRunning(boolean running) {
        this.vncServerRunning = running;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        if (config == null) {
            config = new WebRtcConfig(requireContext());
        }

        View view = getLayoutInflater().inflate(R.layout.dialog_webrtc_server_settings, null);

        initViews(view);
        loadSettings();
        updateUI();

        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        builder.setTitle("WebRTC Server");
        builder.setView(view);
        builder.setPositiveButton("Close", (dialog, which) -> {
            saveSettings();
            dismiss();
        });

        return builder.create();
    }

    private void initViews(View view) {
        tvStatus = view.findViewById(R.id.webrtc_status_text);
        tvIpAddress = view.findViewById(R.id.webrtc_ip_address);
        tvUrl = view.findViewById(R.id.webrtc_connection_url);
        etPort = view.findViewById(R.id.webrtc_signaling_port);
        etFps = view.findViewById(R.id.webrtc_fps);
        etBitrate = view.findViewById(R.id.webrtc_bitrate);
        swAutoStart = view.findViewById(R.id.webrtc_auto_start);
        btnStart = view.findViewById(R.id.webrtc_start_button);
        btnStop = view.findViewById(R.id.webrtc_stop_button);

        swAutoStart.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked && (vncAutoStartEnabled || vncServerRunning)) {
                // VNC auto-start or VNC running, show conflict warning
                buttonView.setChecked(false);
                new AlertDialog.Builder(requireContext())
                    .setTitle("Cannot Enable Auto-Start")
                    .setMessage("VNC server is already configured to auto-start or is currently running. Only one server (VNC or WebRTC) can run at a time.\n\nPlease stop the VNC server and disable its auto-start first.")
                    .setPositiveButton("OK", null)
                    .show();
            }
        });

        btnStart.setOnClickListener(v -> startServer());
        btnStop.setOnClickListener(v -> stopServer());
    }

    private void loadSettings() {
        if (webRtcService != null) {
            List<String> ips = webRtcService.getDeviceIpAddresses();
            if (!ips.isEmpty()) {
                String ipText = String.join(", ", ips);
                tvIpAddress.setText(ipText);

                // Set connection URL using first IP
                String url = config.getConnectionUrl(ips.get(0));
                tvUrl.setText(url);
            } else {
                tvIpAddress.setText("No network connection");
                tvUrl.setText("N/A");
            }
        }

        if (config != null) {
            etPort.setText(String.valueOf(config.getSignallingPort()));
            etFps.setText(String.valueOf(config.getVideoFps()));
            etBitrate.setText(String.valueOf(config.getVideoBitrate() / 1000)); // Display as kbps
            swAutoStart.setChecked(config.isAutoStart());
        }
    }

    private void saveSettings() {
        if (config == null) return;

        try {
            int port = Integer.parseInt(etPort.getText().toString());
            if (port > 0 && port < 65536) {
                config.setSignallingPort(port);
            }
        } catch (NumberFormatException e) {
            Log.w(TAG, "Invalid port number");
        }

        try {
            int fps = Integer.parseInt(etFps.getText().toString());
            if (fps > 0 && fps <= 60) {
                config.setVideoFps(fps);
            }
        } catch (NumberFormatException e) {
            Log.w(TAG, "Invalid FPS");
        }

        try {
            int bitrate = Integer.parseInt(etBitrate.getText().toString());
            if (bitrate > 0) {
                config.setVideoBitrate(bitrate * 1000); // Convert kbps to bps
            }
        } catch (NumberFormatException e) {
            Log.w(TAG, "Invalid bitrate");
        }

        config.setAutoStart(swAutoStart.isChecked());

        if (listener != null) {
            listener.onSettingsChanged();
        }
    }

    private void startServer() {
        if (webRtcService == null) {
            Toast.makeText(requireContext(), "WebRTC Service not available", Toast.LENGTH_SHORT).show();
            return;
        }

        if (config == null) {
            config = new WebRtcConfig(requireContext());
        }

        saveSettings();

        boolean success = webRtcService.startServer(
            config.getSignallingPort(),
            config.getStunServer(),
            previewWidth,
            previewHeight,
            config.getVideoFps()
        );

        if (success) {
            Toast.makeText(requireContext(), "WebRTC Server started", Toast.LENGTH_SHORT).show();
            if (listener != null) {
                listener.onServerStarted();
            }
        } else {
            Toast.makeText(requireContext(), "Failed to start WebRTC Server", Toast.LENGTH_SHORT).show();
        }

        updateUI();
    }

    private void stopServer() {
        if (webRtcService == null) return;

        webRtcService.stopServer();
        Toast.makeText(requireContext(), "WebRTC Server stopped", Toast.LENGTH_SHORT).show();
        if (listener != null) {
            listener.onServerStopped();
        }
        updateUI();
    }

    private void updateUI() {
        boolean running = webRtcService != null && webRtcService.isRunning();

        if (running) {
            tvStatus.setText("Running");
            tvStatus.setTextColor(getResources().getColor(R.color.green));
            btnStart.setVisibility(View.GONE);
            btnStop.setVisibility(View.VISIBLE);
            etPort.setEnabled(false);
            etFps.setEnabled(false);
            etBitrate.setEnabled(false);
        } else {
            tvStatus.setText("Stopped");
            tvStatus.setTextColor(getResources().getColor(R.color.purple_500));
            btnStart.setVisibility(View.VISIBLE);
            btnStop.setVisibility(View.GONE);
            etPort.setEnabled(true);
            etFps.setEnabled(true);
            etBitrate.setEnabled(true);
        }

        // Set URL when running
        if (running && webRtcService != null) {
            String hostIp = "device";
            List<String> ips = webRtcService.getDeviceIpAddresses();
            if (!ips.isEmpty()) {
                hostIp = ips.get(0);
            }
            tvUrl.setText("http://" + hostIp + ":" + config.getSignallingPort());
        }
    }
}