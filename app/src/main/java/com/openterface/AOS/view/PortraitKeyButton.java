package com.openterface.AOS.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.openterface.AOS.R;

/**
 * 竖屏键盘按键 - 支持两部分显示
 * - 主标签（居中显示字母/符号）
 * - 右上角提示（显示长按可选字符，如数字/符号）
 *
 * 参考 KeyCMD 的 BasicPhysicalKeyboardView 实现
 */
public class PortraitKeyButton extends FrameLayout {

    private TextView mainLabel;
    private TextView cornerHintView;
    private String cornerHintText = null;

    public PortraitKeyButton(Context context) {
        super(context);
        init();
    }

    public PortraitKeyButton(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public PortraitKeyButton(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        // 设置不可点击（由父级处理点击事件）
        setClickable(false);
        setFocusable(false);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        // 从布局中获取 TextView
        mainLabel = findViewById(R.id.key_main_label);
        cornerHintView = findViewById(R.id.key_corner_hint);
    }

    /**
     * 设置主标签（居中的字母/符号）
     */
    public void setMainLabel(String text) {
        if (mainLabel != null) {
            mainLabel.setText(text);
        }
    }

    /**
     * 获取主标签
     */
    public CharSequence getMainLabelText() {
        return mainLabel != null ? mainLabel.getText() : "";
    }

    /**
     * 设置右上角提示（数字/符号）
     */
    public void setCornerHint(String hint) {
        this.cornerHintText = hint;
        if (cornerHintView != null) {
            if (hint != null && !hint.isEmpty()) {
                cornerHintView.setText(hint);
                cornerHintView.setVisibility(VISIBLE);
            } else {
                cornerHintView.setVisibility(GONE);
            }
        }
    }

    /**
     * 获取右上角提示
     */
    public String getCornerHint() {
        return cornerHintText;
    }
}
