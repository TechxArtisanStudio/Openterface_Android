package com.openterface.AOS.test;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;

import com.openterface.AOS.R;
import com.openterface.AOS.view.PortraitKeyboardView;

/**
 * 竖屏键盘 UI 测试类
 * 测试键盘模块的 UI 显示和基本功能
 */
public class KeyboardUITest {
    private static final String TAG = "KeyboardUITest";

    private Context context;
    private PortraitKeyboardView keyboardView;
    private TestResults results = new TestResults();

    public KeyboardUITest(Context context) {
        this.context = context;
    }

    /**
     * 运行所有测试
     */
    public TestResults runAllTests() {
        Log.d(TAG, "========== 开始竖屏键盘 UI 测试 ==========");

        try {
            testKeyboardViewInflation();
            testToolbarButtons();
            testPanelSwitching();
            testPanelLayout();
        } catch (Exception e) {
            results.addFailure("Unexpected error: " + e.getMessage());
            Log.e(TAG, "测试过程中发生错误", e);
        }

        Log.d(TAG, "========== 测试完成 ==========");
        Log.d(TAG, "总计: " + results.getTotalTests());
        Log.d(TAG, "通过: " + results.getPassedTests());
        Log.d(TAG, "失败: " + results.getFailedTests());

        return results;
    }

    /**
     * 测试 1: 键盘 View 是否能正确加载
     */
    private void testKeyboardViewInflation() {
        Log.d(TAG, "\n[测试 1] 键盘 View 加载测试");

        try {
            keyboardView = new PortraitKeyboardView(context);

            if (keyboardView == null) {
                results.addFailure("keyboardView 为 null");
                return;
            }

            // 检查基本属性
            if (keyboardView.getOrientation() != LinearLayout.VERTICAL) {
                results.addFailure("keyboardView 方向不是 VERTICAL");
                return;
            }

            int childCount = keyboardView.getChildCount();
            if (childCount == 0) {
                results.addFailure("keyboardView 没有子视图");
                return;
            }

            Log.d(TAG, "✓ keyboardView 创建成功");
            Log.d(TAG, "  - 子视图数量: " + childCount);
            Log.d(TAG, "  - 方向: VERTICAL");
            results.addPass();

        } catch (Exception e) {
            results.addFailure("keyboardView 加载失败: " + e.getMessage());
            Log.e(TAG, "加载失败", e);
        }
    }

    /**
     * 测试 2: 工具栏按钮是否存在
     */
    private void testToolbarButtons() {
        Log.d(TAG, "\n[测试 2] 工具栏按钮测试");

        if (keyboardView == null) {
            results.addFailure("keyboardView 未初始化，跳过测试");
            return;
        }

        try {
            // 检查关键按钮
            checkButton(R.id.KeyBoard_System, "System");
            checkButton(R.id.KeyBoard_Opacity, "Opacity");
            checkButton(R.id.KeyBoard_TouchPad, "TouchPad");
            checkButton(R.id.KeyBoard_ShortCut, "ShortCut");
            checkButton(R.id.KeyBoard_Function, "Function");
            checkButton(R.id.KeyBoard_Ctrl, "Ctrl");
            checkButton(R.id.KeyBoard_Shift, "Shift");
            checkButton(R.id.KeyBoard_Alt, "Alt");
            checkButton(R.id.KeyBoard_Win, "Win");
            checkButton(R.id.KeyBoard_Close, "Close");

            if (results.getFailedTests() == 0) {
                results.addPass();
            }

        } catch (Exception e) {
            results.addFailure("工具栏测试失败: " + e.getMessage());
            Log.e(TAG, "测试失败", e);
        }
    }

    private void checkButton(int id, String name) {
        View button = keyboardView.findViewById(id);
        if (button == null) {
            results.addFailure("找不到按钮: " + name);
            Log.w(TAG, "✗ 未找到按钮: " + name);
        } else {
            Log.d(TAG, "✓ 找到按钮: " + name);
        }
    }

    /**
     * 测试 3: 面板切换功能
     */
    private void testPanelSwitching() {
        Log.d(TAG, "\n[测试 3] 面板切换测试");

        if (keyboardView == null) {
            results.addFailure("keyboardView 未初始化，跳过测试");
            return;
        }

        try {
            // 获取面板
            View systemPanel = keyboardView.findViewById(R.id.Fragment_KeyBoard_System);
            View functionPanel = keyboardView.findViewById(R.id.Fragment_KeyBoard_Function);
            View shortCutPanel = keyboardView.findViewById(R.id.Fragment_KeyBoard_ShortCut);

            if (systemPanel == null || functionPanel == null || shortCutPanel == null) {
                results.addFailure("某个面板未找到");
                return;
            }

            // 测试初始状态（应该显示 System 面板）
            if (systemPanel.getVisibility() != View.VISIBLE) {
                results.addFailure("初始状态：System 面板未显示");
                return;
            }
            if (functionPanel.getVisibility() != View.GONE ||
                shortCutPanel.getVisibility() != View.GONE) {
                results.addFailure("初始状态：其他面板未隐藏");
                return;
            }
            Log.d(TAG, "✓ 初始状态正确：System 面板显示");

            results.addPass();

        } catch (Exception e) {
            results.addFailure("面板切换测试失败: " + e.getMessage());
            Log.e(TAG, "测试失败", e);
        }
    }

    /**
     * 测试 4: 面板布局参数
     */
    private void testPanelLayout() {
        Log.d(TAG, "\n[测试 4] 面板布局测试");

        if (keyboardView == null) {
            results.addFailure("keyboardView 未初始化，跳过测试");
            return;
        }

        try {
            View systemPanel = keyboardView.findViewById(R.id.Fragment_KeyBoard_System);
            if (systemPanel == null) {
                results.addFailure("System 面板未找到");
                return;
            }

            // 检查键盘 View 总尺寸
            int keyboardWidth = keyboardView.getWidth();
            int keyboardHeight = keyboardView.getHeight();
            Log.d(TAG, "✓ 键盘 View 尺寸: " + keyboardWidth + "x" + keyboardHeight + "px");

            if (keyboardWidth <= 0 || keyboardHeight <= 0) {
                results.addFailure("键盘 View 尺寸无效");
                return;
            }

            results.addPass();

        } catch (Exception e) {
            results.addFailure("布局测试失败: " + e.getMessage());
            Log.e(TAG, "测试失败", e);
        }
    }

    /**
     * 测试结果类
     */
    public static class TestResults {
        private int passedTests = 0;
        private int failedTests = 0;
        private java.util.List<String> failures = new java.util.ArrayList<>();

        public void addPass() {
            passedTests++;
        }

        public void addFailure(String message) {
            failedTests++;
            failures.add(message);
        }

        public int getTotalTests() {
            return passedTests + failedTests;
        }

        public int getPassedTests() {
            return passedTests;
        }

        public int getFailedTests() {
            return failedTests;
        }

        public java.util.List<String> getFailures() {
            return failures;
        }

        public boolean isAllPassed() {
            return failedTests == 0;
        }
    }
}
