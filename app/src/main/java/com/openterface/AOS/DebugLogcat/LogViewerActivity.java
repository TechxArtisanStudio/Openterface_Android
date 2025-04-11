package com.openterface.AOS.DebugLogcat;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.openterface.AOS.R;

import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import timber.log.Timber;

public class LogViewerActivity extends Dialog {
    private final FileLoggingTree fileLoggingTree;
    private TextView logTextView;
    private Timer refreshTimer;
    private static final long REFRESH_INTERVAL = 1000;
    private final Handler handler = new Handler(Looper.getMainLooper());

    public LogViewerActivity(@NonNull Context context, FileLoggingTree fileLoggingTree) {
        super(context);
        this.fileLoggingTree = fileLoggingTree;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_log_viewer);

        // Initialize view
        logTextView = findViewById(R.id.log_text_view);
        Button exportButton = findViewById(R.id.export_button);
        Button closeButton = findViewById(R.id.close_button);
        Button clearButton = findViewById(R.id.clear_button);

        // Display log content
        refreshLogs();

        // Set export button click event
        exportButton.setOnClickListener(v -> exportLogs());

        // Set the close button click event
        closeButton.setOnClickListener(v -> dismiss());

        // Set clear button click event
        clearButton.setOnClickListener(v -> clearLogs());

        // Set log added listener
        fileLoggingTree.setOnLogAddedListener(logEntry -> {
            handler.post(() -> {
                String currentText = logTextView.getText().toString();
                logTextView.setText(currentText + logEntry + "\n");
                // Scroll to bottom
                int scrollAmount = logTextView.getLayout().getLineTop(logTextView.getLineCount()) - logTextView.getHeight();
                if (scrollAmount > 0) {
                    logTextView.scrollTo(0, scrollAmount);
                }
            });
        });

        // Start auto refresh timer
        startAutoRefresh();
    }

    private void startAutoRefresh() {
        refreshTimer = new Timer();
        refreshTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                handler.post(() -> refreshLogs());
            }
        }, REFRESH_INTERVAL, REFRESH_INTERVAL);
    }

    @Override
    public void dismiss() {
        // Stop auto refresh timer when dialog is dismissed
        if (refreshTimer != null) {
            refreshTimer.cancel();
            refreshTimer = null;
        }
        // Remove log added listener
        fileLoggingTree.setOnLogAddedListener(null);
        super.dismiss();
    }

    private void refreshLogs() {
        List<String> logs = fileLoggingTree.getLogList();
        StringBuilder logContent = new StringBuilder();
        for (String log : logs) {
            logContent.append(log).append("\n");
        }
        logTextView.setText(logContent.toString());
        // Roll to the bottom
        logTextView.post(() -> {
            int scrollAmount = logTextView.getLayout().getLineTop(logTextView.getLineCount()) - logTextView.getHeight();
            if (scrollAmount > 0) {
                logTextView.scrollTo(0, scrollAmount);
            }
        });
    }

    private void exportLogs() {
        try {
            // Use application-specific directories
            File exportDir = new File(fileLoggingTree.getContext().getExternalFilesDir(null), "exported_logs");
            if (!exportDir.exists()) {
                exportDir.mkdirs();
            }

            File exportFile = new File(exportDir,
                    "exported_log_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date()) + ".txt");

            FileWriter writer = new FileWriter(exportFile);
            for (String log : fileLoggingTree.getLogList()) {
                writer.append(log).append("\n");
            }
            writer.flush();
            writer.close();

            Toast.makeText(getContext(), "Logs have been exported to: " + exportFile.getAbsolutePath(), Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(getContext(), "exported fail: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            Timber.e(e, "Failure to export logs");
        }
    }

    private void clearLogs() {
        fileLoggingTree.clearLogs();
        refreshLogs();
        Toast.makeText(getContext(), "Log cleared", Toast.LENGTH_SHORT).show();
    }
}