package com.openterface.AOS.view;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import com.openterface.AOS.R;

/**
 * 竖屏键盘自定义 View
 * 动态加载 toolbar 和三个键盘面板
 * 解决面板可见性问题和 findViewById 失败问题
 */
public class PortraitKeyboardView extends LinearLayout {

    private static final String TAG = "PortraitKeyboardView";

    // 面板 - 只保留 System 和 Function（ShortCut 已移至顶部工具栏）
    private View systemPanel;
    private View functionPanel;

    // Toolbar 按钮 - 移除 shortCutBtn
    private ImageButton systemBtn;
    private Button functionBtn;
    private ImageButton closeBtn;
    private Button ctrlBtn;
    private Button shiftBtn;
    private Button altBtn;
    private Button winBtn;

    // 回调
    private OnCloseListener closeListener;

    public interface OnCloseListener {
        void onClose();
    }

    public PortraitKeyboardView(Context context) {
        super(context);
        init(context);
    }

    public PortraitKeyboardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public PortraitKeyboardView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    public void setOnCloseListener(OnCloseListener listener) {
        this.closeListener = listener;
    }

    private void init(Context context) {
        setOrientation(VERTICAL);
        setBackgroundColor(0xFFFFFFFF);

        Log.d(TAG, "init: loading keyboard panels (System + Function only)");

        // 只加载 System 和 Function 面板（ShortCut 已移至顶部工具栏）
        systemPanel = loadPanel(context, R.layout.system_button);
        functionPanel = loadPanel(context, R.layout.function_button);

        Log.d(TAG, "init: panels loaded - System=" + systemPanel + ", Function=" + functionPanel);

        // Default: show System panel
        showSystemPanel();

        Log.d(TAG, "init: done, child count = " + getChildCount());

        // Log child views info
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            Log.d(TAG, "init: child[" + i + "] - class: " + child.getClass().getSimpleName() +
                  ", visibility: " + child.getVisibility() +
                  ", height: " + child.getHeight() +
                  ", width: " + child.getWidth());
        }
    }

    private View loadPanel(Context context, int layoutResId) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View panel = inflater.inflate(layoutResId, this, false);
        addView(panel);
        Log.d(TAG, "loadPanel: 加载面板 " + context.getResources().getResourceName(layoutResId));
        return panel;
    }

    private void setupToolbarButtons() {
        if (systemBtn != null) {
            systemBtn.setOnClickListener(v -> showSystemPanel());
        }
        if (functionBtn != null) {
            functionBtn.setOnClickListener(v -> showFunctionPanel());
        }
        // shortCutBtn 已移除 - ShortCut 功能移至顶部工具栏
        if (closeBtn != null) {
            closeBtn.setOnClickListener(v -> {
                if (closeListener != null) {
                    closeListener.onClose();
                }
            });
        }
    }

    private void showSystemPanel() {
        if (systemPanel != null) systemPanel.setVisibility(VISIBLE);
        if (functionPanel != null) functionPanel.setVisibility(GONE);
        // shortCutPanel 已移除
        Log.d(TAG, "showSystemPanel: 显示 System 面板");
    }

    private void showFunctionPanel() {
        if (systemPanel != null) systemPanel.setVisibility(GONE);
        if (functionPanel != null) functionPanel.setVisibility(VISIBLE);
        // shortCutPanel 已移除
        Log.d(TAG, "showFunctionPanel: 显示 Function 面板");
    }
}
