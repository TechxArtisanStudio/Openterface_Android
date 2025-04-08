package com.openterface.AOS.DebugLogcat;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
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

import timber.log.Timber;

public class LogViewerActivity extends Dialog {
    private final FileLoggingTree fileLoggingTree;
    private TextView logTextView;

    public LogViewerActivity(@NonNull Context context, FileLoggingTree fileLoggingTree) {
        super(context);
        this.fileLoggingTree = fileLoggingTree;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_log_viewer);

        // 初始化视图
        logTextView = findViewById(R.id.log_text_view);
        Button exportButton = findViewById(R.id.export_button);
        Button closeButton = findViewById(R.id.close_button);
        Button refreshButton = findViewById(R.id.refresh_button);

        // 显示日志内容
        refreshLogs();

        // 设置导出按钮点击事件
        exportButton.setOnClickListener(v -> exportLogs());

        // 设置关闭按钮点击事件
        closeButton.setOnClickListener(v -> dismiss());

        // 设置刷新按钮点击事件
        refreshButton.setOnClickListener(v -> refreshLogs());
    }

    private void refreshLogs() {
        List<String> logs = fileLoggingTree.getLogList();
        StringBuilder logContent = new StringBuilder();
        for (String log : logs) {
            logContent.append(log).append("\n");
        }
        logTextView.setText(logContent.toString());
        // 滚动到底部
        logTextView.post(() -> {
            int scrollAmount = logTextView.getLayout().getLineTop(logTextView.getLineCount()) - logTextView.getHeight();
            if (scrollAmount > 0) {
                logTextView.scrollTo(0, scrollAmount);
            }
        });
    }

    private void exportLogs() {
        try {
            // 使用应用专属目录
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

            Toast.makeText(getContext(), "日志已导出到: " + exportFile.getAbsolutePath(), Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(getContext(), "导出失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            Timber.e(e, "导出日志失败");
        }
    }
}