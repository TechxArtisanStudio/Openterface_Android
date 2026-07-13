package com.openterface.AOS.activity;

import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.graphics.Color;

import com.openterface.AOS.R;
import com.openterface.AOS.test.KeyboardUITest;
import com.openterface.AOS.view.PortraitKeyboardView;

import java.util.List;

/**
 * Keyboard UI test Activity
 * For testing portrait keyboard UI display and functionality
 */
public class KeyboardTestActivity extends BaseActivity {
    private static final String TAG = "OP-UI";

    private LinearLayout testContainer;
    private TextView resultTextView;
    private PortraitKeyboardView keyboardView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Create test interface
        setupTestUI();

        // Run tests with delay to ensure UI is rendered
        testContainer.postDelayed(this::runTests, 500);
    }

    /**
     * Setup test interface
     */
    private void setupTestUI() {
        // Create root layout
        ScrollView scrollView = new ScrollView(this);
        scrollView.setLayoutParams(new ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        ));
        scrollView.setBackgroundColor(Color.WHITE);

        testContainer = new LinearLayout(this);
        testContainer.setOrientation(LinearLayout.VERTICAL);
        testContainer.setPadding(32, 32, 32, 32);

        // Add title
        TextView titleView = new TextView(this);
        titleView.setText(R.string.keyboard_test_title);
        titleView.setTextSize(24);
        titleView.setTextColor(Color.BLACK);
        titleView.setPadding(0, 0, 0, 32);
        testContainer.addView(titleView);

        // Add test area
        LinearLayout testArea = new LinearLayout(this);
        testArea.setOrientation(LinearLayout.VERTICAL);
        testArea.setBackgroundColor(Color.LTGRAY);
        testArea.setPadding(16, 16, 16, 16);
        testArea.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        // Create keyboard View
        keyboardView = new PortraitKeyboardView(this);
        keyboardView.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        testArea.addView(keyboardView);

        testContainer.addView(testArea);

        // Add result display area
        TextView resultLabel = new TextView(this);
        resultLabel.setText(R.string.test_results);
        resultLabel.setTextSize(18);
        resultLabel.setTextColor(Color.BLACK);
        resultLabel.setPadding(0, 32, 0, 16);
        testContainer.addView(resultLabel);

        resultTextView = new TextView(this);
        resultTextView.setTextSize(14);
        resultTextView.setTextColor(Color.BLACK);
        resultTextView.setPadding(16, 16, 16, 16);
        resultTextView.setBackgroundColor(Color.parseColor("#F0F0F0"));
        testContainer.addView(resultTextView);

        // Add retry test button
        Button retryButton = new Button(this);
        retryButton.setText(R.string.retry_test);
        retryButton.setOnClickListener(v -> runTests());
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        buttonParams.topMargin = 32;
        retryButton.setLayoutParams(buttonParams);
        testContainer.addView(retryButton);

        scrollView.addView(testContainer);
        setContentView(scrollView);
    }

    /**
     * Run all tests
     */
    private void runTests() {
        Log.d(TAG, "Starting keyboard UI tests...");

        resultTextView.setText(R.string.running_tests);

        KeyboardUITest test = new KeyboardUITest(KeyboardTestActivity.this);
        KeyboardUITest.TestResults results = test.runAllTests();

        // Display test results
        displayResults(results);

        Log.d(TAG, "Tests completed");
    }

    /**
     * Display test results
     */
    private void displayResults(KeyboardUITest.TestResults results) {
        StringBuilder sb = new StringBuilder();

        sb.append(getString(R.string.total_tests, results.getTotalTests())).append("\n");
        sb.append(getString(R.string.passed_tests, results.getPassedTests())).append("\n");
        sb.append(getString(R.string.failed_tests, results.getFailedTests())).append("\n\n");

        if (results.isAllPassed()) {
            sb.append(getString(R.string.all_tests_passed)).append("\n");
            resultTextView.setTextColor(Color.parseColor("#00AA00"));
        } else {
            sb.append(getString(R.string.some_tests_failed)).append("\n\n");
            sb.append(getString(R.string.failure_details)).append("\n");
            List<String> failures = results.getFailures();
            for (int i = 0; i < failures.size(); i++) {
                sb.append((i + 1)).append(". ").append(failures.get(i)).append("\n");
            }
            resultTextView.setTextColor(Color.parseColor("#AA0000"));
        }

        resultTextView.setText(sb.toString());

        // Scroll to result display area
        ScrollView parent = (ScrollView) resultTextView.getParent().getParent();
        parent.post(() -> parent.smoothScrollTo(0, resultTextView.getTop()));
    }
}
