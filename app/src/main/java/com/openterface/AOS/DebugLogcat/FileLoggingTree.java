package com.openterface.AOS.DebugLogcat;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.BatteryManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import timber.log.Timber;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class FileLoggingTree extends Timber.Tree {
    private static final String TAG = "FileLoggingTree";
    private static final List<String> logList = new ArrayList<>();
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault());
    private final File logFile;
    private final Application.ActivityLifecycleCallbacks lifecycleCallbacks;
    private final Context context;
    private OnLogAddedListener logAddedListener;

    public interface OnLogAddedListener {
        void onLogAdded(String logEntry);
    }

    public void setOnLogAddedListener(OnLogAddedListener listener) {
        this.logAddedListener = listener;
    }

    public FileLoggingTree(Application application) {
        this.context = application;
        // Use application-specific directories
        File logDir = new File(context.getExternalFilesDir(null), "logs");
        if (!logDir.exists()) {
            boolean created = logDir.mkdirs();
            Log.d(TAG, "Log directory created: " + created);
        }
        logFile = new File(logDir, "app_log.txt");
        Log.d(TAG, "Log file path: " + logFile.getAbsolutePath());

        // register Activity Life cycle callback
        lifecycleCallbacks = new Application.ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
                Timber.i("Activity Created: %s", activity.getClass().getSimpleName());
                logSystemInfo(activity);
            }

            @Override
            public void onActivityStarted(Activity activity) {
//                Timber.i("Activity Started: %s", activity.getClass().getSimpleName());
            }

            @Override
            public void onActivityResumed(Activity activity) {
//                Timber.i("Activity Resumed: %s", activity.getClass().getSimpleName());
//                logNetworkInfo(activity);
//                logBatteryInfo(activity);
            }

            @Override
            public void onActivityPaused(Activity activity) {
                Timber.i("Activity Paused: %s", activity.getClass().getSimpleName());
            }

            @Override
            public void onActivityStopped(Activity activity) {
                Timber.i("Activity Stopped: %s", activity.getClass().getSimpleName());
            }

            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
                Timber.i("Activity SaveInstanceState: %s", activity.getClass().getSimpleName());
            }

            @Override
            public void onActivityDestroyed(Activity activity) {
                Timber.i("Activity Destroyed: %s", activity.getClass().getSimpleName());
            }
        };
        application.registerActivityLifecycleCallbacks(lifecycleCallbacks);
    }

    private void logSystemInfo(Activity activity) {
        // Recording system information
        Timber.i("System Info: Android %s, SDK %d", 
            android.os.Build.VERSION.RELEASE, 
            android.os.Build.VERSION.SDK_INT);
        Timber.i("Device Info: %s %s", 
            android.os.Build.MANUFACTURER, 
            android.os.Build.MODEL);
        Timber.i("Memory Info: Total=%dMB, Available=%dMB", 
            Runtime.getRuntime().totalMemory() / (1024 * 1024),
            Runtime.getRuntime().freeMemory() / (1024 * 1024));
    }

    private void logNetworkInfo(Activity activity) {
        try {
            ConnectivityManager cm = (ConnectivityManager) activity.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm != null) {
                NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
                if (activeNetwork != null) {
                    Timber.i("Network Info: Type=%s, Connected=%b", 
                        activeNetwork.getTypeName(), 
                        activeNetwork.isConnected());
                } else {
                    Timber.i("Network Info: No active network");
                }
            }
        } catch (SecurityException e) {
            Timber.w("No permission to access network info");
        } catch (Exception e) {
            Timber.e(e, "Error getting network info");
        }
    }

    private void logBatteryInfo(Activity activity) {
        try {
            IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            Intent batteryStatus = activity.registerReceiver(null, ifilter);
            if (batteryStatus != null) {
                int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
                float batteryPct = level * 100 / (float)scale;
                Timber.i("Battery Info: Level=%.1f%%", batteryPct);
            }
        } catch (Exception e) {
            Timber.e(e, "Error getting battery info");
        }
    }

    @Override
    protected void log(int priority, String tag, String message, Throwable t) {
        String logEntry = String.format("%s [%s] %s: %s", 
            DATE_FORMAT.format(new Date()),
            getPriorityString(priority),
            tag,
            message);
        
        if (t != null) {
            logEntry += "\n" + Log.getStackTraceString(t);
        }
        
        logList.add(logEntry);
        Log.d(TAG, "New log entry: " + logEntry);

        try {
            FileWriter writer = new FileWriter(logFile, true);
            writer.append(logEntry).append("\n");
            writer.flush();
            writer.close();
        } catch (IOException e) {
            Log.e(TAG, "Error writing to log file", e);
        }

        // Notify listener if exists
        if (logAddedListener != null) {
            logAddedListener.onLogAdded(logEntry);
        }
    }

    private String getPriorityString(int priority) {
        switch (priority) {
            case Log.VERBOSE: return "V";
            case Log.DEBUG: return "D";
            case Log.INFO: return "I";
            case Log.WARN: return "W";
            case Log.ERROR: return "E";
            case Log.ASSERT: return "A";
            default: return "?";
        }
    }

    public static List<String> getLogList() {
        return new ArrayList<>(logList);
    }

    public File getLogFile() {
        return logFile;
    }

    public Context getContext() {
        return context;
    }

    public void clearLogs() {
        logList.clear();
        try {
            FileWriter writer = new FileWriter(logFile, false);
            writer.write("");
            writer.flush();
            writer.close();
        } catch (IOException e) {
            Log.e(TAG, "Error clearing log file", e);
        }
    }
}