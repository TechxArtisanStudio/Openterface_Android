package com.openterface.AOS.fragment;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.DialogFragment;

import com.openterface.AOS.R;
import com.openterface.AOS.vnc.VncServerConfig;
import com.openterface.AOS.vnc.VncServerService;

import java.util.List;

/**
 * Dialog fragment for VNC Server settings.
 */
public class VncServerSettingsDialogFragment extends DialogFragment {

    private static final String TAG = "VncSettingsDialog";

    private VncServerConfig config;
    private VncServerService vncService;

    // Encoding spinner adapter
    private ArrayAdapter<String> encodingAdapter;
    private ArrayAdapter<String> qualityAdapter;
    private ArrayAdapter<String> compressAdapter;

    // UI elements
    private TextView tvStatus;
    private TextView tvIpAddress;
    private EditText etPort;
    private EditText etPassword;
    private EditText etBindAddress;
    private Spinner spinnerEncoding;
    private Spinner spinnerQuality;
    private Spinner spinnerCompress;
    private SwitchCompat swAutoStart;
    private Button btnStart;
    private Button btnStop;

    // Callback listener
    private OnVncSettingsListener listener;

    public interface OnVncSettingsListener {
        void onServerStarted(int port, String password);
        void onServerStopped();
        void onSettingsChanged();
    }

    public void setVncService(VncServerService service) {
        this.vncService = service;
    }

    public void setListener(OnVncSettingsListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        config = new VncServerConfig(requireContext());

        View view = getLayoutInflater().inflate(R.layout.dialog_vnc_server_settings, null);

        initViews(view);
        loadSettings();
        updateUI();

        // Build dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        builder.setTitle(R.string.vnc_server_title);
        builder.setView(view);
        builder.setPositiveButton(R.string.vnc_server_close, (dialog, which) -> {
            saveSettings();
            dismiss();
        });

        return builder.create();
    }

    private void initViews(View view) {
        tvStatus = view.findViewById(R.id.vnc_status_text);
        tvIpAddress = view.findViewById(R.id.vnc_ip_address);
        etPort = view.findViewById(R.id.vnc_port);
        etPassword = view.findViewById(R.id.vnc_password);
        etBindAddress = view.findViewById(R.id.vnc_bind_address);
        spinnerEncoding = view.findViewById(R.id.vnc_encoding_spinner);
        spinnerQuality = view.findViewById(R.id.vnc_quality_spinner);
        spinnerCompress = view.findViewById(R.id.vnc_compress_spinner);
        swAutoStart = view.findViewById(R.id.vnc_auto_start);
        btnStart = view.findViewById(R.id.vnc_start_button);
        btnStop = view.findViewById(R.id.vnc_stop_button);

        // Populate encoding spinner
        encodingAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item,
                new String[]{
                        getString(R.string.vnc_encoding_auto),
                        getString(R.string.vnc_encoding_tight),
                        getString(R.string.vnc_encoding_zlib),
                        getString(R.string.vnc_encoding_zrle),
                        getString(R.string.vnc_encoding_hextile),
                });
        encodingAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerEncoding.setAdapter(encodingAdapter);

        // Populate quality spinner
        qualityAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item,
                new String[]{
                        getString(R.string.vnc_quality_auto),
                        getString(R.string.vnc_quality_low),
                        getString(R.string.vnc_quality_medium),
                        getString(R.string.vnc_quality_high),
                });
        qualityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerQuality.setAdapter(qualityAdapter);

        // Populate compress spinner
        compressAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item,
                new String[]{
                        getString(R.string.vnc_compress_auto),
                        getString(R.string.vnc_compress_fast),
                        getString(R.string.vnc_compress_medium),
                        getString(R.string.vnc_compress_max),
                });
        compressAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCompress.setAdapter(compressAdapter);

        btnStart.setOnClickListener(v -> startServer());
        btnStop.setOnClickListener(v -> stopServer());
    }

    private void loadSettings() {
        if (vncService != null) {
            List<String> ips = vncService.getDeviceIpAddresses();
            if (!ips.isEmpty()) {
                tvIpAddress.setText(String.join(", ", ips));
            } else {
                tvIpAddress.setText("No network connection");
            }
        }

        etPort.setText(String.valueOf(config.getPort()));
        etPassword.setText(config.getPassword());
        etBindAddress.setText(config.getBindAddress());
        swAutoStart.setChecked(config.isAutoStart());
        spinnerEncoding.setSelection(encodingPositionForValue(config.getEncoding()));
        spinnerQuality.setSelection(qualityPositionForValue(config.getQualityLevel()));
        spinnerCompress.setSelection(compressPositionForValue(config.getCompressLevel()));
    }

    private void saveSettings() {
        try {
            int port = Integer.parseInt(etPort.getText().toString());
            if (port > 0 && port < 65536) {
                config.setPort(port);
            }
        } catch (NumberFormatException e) {
            Log.w(TAG, "Invalid port number");
        }

        config.setPassword(etPassword.getText().toString());
        config.setBindAddress(etBindAddress.getText().toString());
        config.setAutoStart(swAutoStart.isChecked());
        config.setEncoding(encodingValueForPosition(spinnerEncoding.getSelectedItemPosition()));
        config.setQualityLevel(qualityValueForPosition(spinnerQuality.getSelectedItemPosition()));
        config.setCompressLevel(compressValueForPosition(spinnerCompress.getSelectedItemPosition()));

        if (listener != null) {
            listener.onSettingsChanged();
        }
    }

    private void startServer() {
        if (vncService == null) {
            Toast.makeText(requireContext(), "VNC Service not available", Toast.LENGTH_SHORT).show();
            return;
        }

        saveSettings();

        // Get current UVC resolution from MainActivity
        int width = getVncWidth();
        int height = getVncHeight();

        boolean success = vncService.startServer(
            config.getPassword(),
            config.getPort(),
            width,
            height,
            config.getEncoding(),
            config.getQualityLevel(),
            config.getCompressLevel()
        );

        if (success) {
            Toast.makeText(requireContext(), "VNC Server started", Toast.LENGTH_SHORT).show();
            if (listener != null) {
                listener.onServerStarted(config.getPort(), config.getPassword());
            }
        } else {
            Toast.makeText(requireContext(), "Failed to start VNC Server", Toast.LENGTH_SHORT).show();
        }

        updateUI();
    }

    private void stopServer() {
        if (vncService == null) return;

        vncService.stopServer();
        Toast.makeText(requireContext(), "VNC Server stopped", Toast.LENGTH_SHORT).show();
        if (listener != null) {
            listener.onServerStopped();
        }
        updateUI();
    }

    private void updateUI() {
        boolean running = vncService != null && vncService.isRunning();

        if (running) {
            tvStatus.setText("Running");
            tvStatus.setTextColor(getResources().getColor(R.color.green));
            btnStart.setVisibility(View.GONE);
            btnStop.setVisibility(View.VISIBLE);
            etPort.setEnabled(false);
            etPassword.setEnabled(false);
            // Encoding/quality/compress can only be set at server start time
            spinnerEncoding.setEnabled(false);
            spinnerQuality.setEnabled(false);
            spinnerCompress.setEnabled(false);
            swAutoStart.setEnabled(false);
        } else {
            tvStatus.setText("Stopped");
            tvStatus.setTextColor(getResources().getColor(R.color.purple_500));
            btnStart.setVisibility(View.VISIBLE);
            btnStop.setVisibility(View.GONE);
            etPort.setEnabled(true);
            etPassword.setEnabled(true);
            spinnerEncoding.setEnabled(true);
            spinnerQuality.setEnabled(true);
            spinnerCompress.setEnabled(true);
            swAutoStart.setEnabled(true);
        }
    }

    public void setPreviewSize(int width, int height) {
        this.previewWidth = width;
        this.previewHeight = height;
    }

    private int previewWidth = 1920;
    private int previewHeight = 1080;

    private int getVncWidth() {
        return previewWidth;
    }

    private int getVncHeight() {
        return previewHeight;
    }

    /**
     * Encoding spinner position to RFB encoding constant.
     * Position 0 = Auto, 1 = Tight, 2 = Zlib, 3 = ZRLE, 4 = Hextile
     */
    private int encodingValueForPosition(int position) {
        switch (position) {
            case 1: return VncServerConfig.ENCODING_TIGHT;
            case 2: return VncServerConfig.ENCODING_ZLIB;
            case 3: return VncServerConfig.ENCODING_ZRLE;
            case 4: return VncServerConfig.ENCODING_HEXTILE;
            default: return VncServerConfig.ENCODING_AUTO;
        }
    }

    /**
     * RFB encoding constant to spinner position.
     */
    private int encodingPositionForValue(int encoding) {
        switch (encoding) {
            case VncServerConfig.ENCODING_TIGHT: return 1;
            case VncServerConfig.ENCODING_ZLIB: return 2;
            case VncServerConfig.ENCODING_ZRLE: return 3;
            case VncServerConfig.ENCODING_HEXTILE: return 4;
            default: return 0; // Auto
        }
    }

    /**
     * Quality spinner position to quality level value.
     * Position 0 = Auto, 1 = Low, 2 = Medium, 3 = High
     */
    private int qualityValueForPosition(int position) {
        switch (position) {
            case 1: return VncServerConfig.QUALITY_LOW;
            case 2: return VncServerConfig.QUALITY_MEDIUM;
            case 3: return VncServerConfig.QUALITY_HIGH;
            default: return VncServerConfig.QUALITY_AUTO;
        }
    }

    /**
     * Quality level value to spinner position.
     */
    private int qualityPositionForValue(int quality) {
        switch (quality) {
            case VncServerConfig.QUALITY_LOW: return 1;
            case VncServerConfig.QUALITY_MEDIUM: return 2;
            case VncServerConfig.QUALITY_HIGH: return 3;
            default: return 0; // Auto
        }
    }

    /**
     * Compress spinner position to compress level value.
     * Position 0 = Auto, 1 = Fast, 2 = Medium, 3 = Max
     */
    private int compressValueForPosition(int position) {
        switch (position) {
            case 1: return VncServerConfig.COMPRESS_FAST;
            case 2: return VncServerConfig.COMPRESS_MEDIUM;
            case 3: return VncServerConfig.COMPRESS_MAX;
            default: return VncServerConfig.COMPRESS_AUTO;
        }
    }

    /**
     * Compress level value to spinner position.
     */
    private int compressPositionForValue(int compress) {
        switch (compress) {
            case VncServerConfig.COMPRESS_FAST: return 1;
            case VncServerConfig.COMPRESS_MEDIUM: return 2;
            case VncServerConfig.COMPRESS_MAX: return 3;
            default: return 0; // Auto
        }
    }
}
