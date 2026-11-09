/**
* @Title: ZoomLayoutDeal
* @Package com.openterface.AOS.drawerLayout
* @Description:
 * ========================================================================== *
 *                                                                            *
 *    This file is part of the Openterface Mini KVM App Android version       *
 *                                                                            *
 *    Copyright (C) 2024   <info@openterface.com>                             *
 *                                                                            *
 *    This program is free software: you can redistribute it and/or modify    *
 *    it under the terms of the GNU General Public License as published by    *
 *    the Free Software Foundation version 3.                                 *
 *                                                                            *
 *    This program is distributed in the hope that it will be useful, but     *
 *    WITHOUT ANY WARRANTY; without even the implied warranty of              *
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU        *
 *    General Public License for more details.                                *
 *                                                                            *
 *    You should have received a copy of the GNU General Public License       *
 *    along with this program. If not, see <http://www.gnu.org/licenses/>.    *
 *                                                                            *
 * ========================================================================== *
*/
package com.openterface.AOS.drawerLayout;

import android.annotation.SuppressLint;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import androidx.drawerlayout.widget.DrawerLayout;

import com.openterface.AOS.ICameraHelper;
import com.openterface.AOS.R;
import com.openterface.AOS.activity.MainActivity;
import com.openterface.AOS.databinding.ActivityMainBinding;
// import com.openterface.AOS.target.MouseManager;
import com.serenegiant.widget.AspectRatioSurfaceView;

public class ZoomLayoutDeal {
    private static final String TAG = "OP-UI";

    /**
     * Per-frame trace for the indicator/scroll mapping. This runs on every MOVE
     * event while the thumbnail is dragged, so the output stays off by default.
     */
    private static final boolean DEBUG_LAYOUT_LOG = false;

    /** Below this the indicator is not worth re-laying out during a pinch. */
    private static final int INDICATOR_SIZE_THRESHOLD = 3;
    private static MainActivity activity;
    private static DrawerLayout drawerLayout;
    private static View thumbnailContainer;
    private static View indicatorView;
    private static float ratioX;
    private static float ratioY;
    private final AspectRatioSurfaceView cameraViewSecond;
    private static ICameraHelper mCameraHelper;
    private ActivityMainBinding mBinding;
    private static int screenWidth;
    private static int screenHeight;

    private static ImageButton dragButton;
    private float lastTouchX, lastTouchY;

    // Drag offset to prevent indicator jumping when starting to drag
    private static float dragOffsetX = 0f;
    private static float dragOffsetY = 0f;

    private boolean isKeyboardScaled = false; // Track scaling state
    private static int originalWidth;
    private static int originalHeight; // Store original dimensions
    private LinearLayout keyBoardView;
    private ImageButton keyBoardZoomInOutButton;
    private ImageButton dragHandle;
    private boolean isPortraitMode = false; // Track if in portrait mode

    public static void getViewWidthHeight(int originalWidth, int originalHeight){

        ZoomLayoutDeal.originalWidth = originalWidth;
        ZoomLayoutDeal.originalHeight = originalHeight;
    }

    public ZoomLayoutDeal(MainActivity activity, ICameraHelper mCameraHelper, ActivityMainBinding mBinding) {
        this.activity = activity;
        this.mCameraHelper = mCameraHelper;
        this.mBinding = mBinding;
        cameraViewSecond = activity.findViewById(R.id.cameraViewSecond);
        drawerLayout = activity.findViewById(R.id.drawer_layout);
        thumbnailContainer = activity.findViewById(R.id.thumbnail_container);
        indicatorView = activity.findViewById(R.id.view_indicator);
        dragButton = activity.findViewById(R.id.drag_button);

        keyBoardZoomInOutButton = activity.findViewById(R.id.KeyBoard_ZoomInOut); // Initialize
        keyBoardView = activity.findViewById(R.id.KeyBoard_View); // Initialize
        dragHandle = activity.findViewById(R.id.drag_handle);

        // Check if in portrait mode
        isPortraitMode = activity.findViewById(R.id.module_selector_bar) != null;

        // Skip initialization if essential views are missing
        if (cameraViewSecond == null || thumbnailContainer == null || dragButton == null) {
            Log.d(TAG, "Essential views not found, skipping zoom setup");
            return;
        }

        DisplayMetrics displayMetrics = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getRealMetrics(displayMetrics);
        screenWidth = displayMetrics.widthPixels;
        screenHeight = displayMetrics.heightPixels;

        Log.d(TAG, "screenWidth: " + screenWidth + " screenHeight: " + screenHeight);

        if (isPortraitMode) {
            // Portrait mode: PiP window with chip's aspect ratio
            // Use the chip's actual resolution to determine aspect ratio
            int chipWidth = activity.getPreviewWidth();
            int chipHeight = activity.getPreviewHeight();

            // If chip resolution is not available yet, use default 16:9
            if (chipWidth <= 0 || chipHeight <= 0) {
                chipWidth = 16;
                chipHeight = 9;
            }

            // Calculate PiP dimensions based on chip's aspect ratio
            // Use screen width as reference, calculate height to match chip's aspect ratio
            int pipWidth = screenWidth / 3;
            int pipHeight = (pipWidth * chipHeight) / chipWidth;

            // Resize thumbnail_container to match cameraViewSecond's dimensions
            // This ensures the indicator position maps correctly to the PiP area
            ViewGroup.LayoutParams containerParams = thumbnailContainer.getLayoutParams();
            containerParams.width = pipWidth;
            containerParams.height = pipHeight;
            thumbnailContainer.setLayoutParams(containerParams);

            cameraViewSecond.getLayoutParams().width = pipWidth;
            cameraViewSecond.getLayoutParams().height = pipHeight;
            cameraViewSecond.requestLayout();

            // Set cameraViewSecond's aspect ratio to match the chip
            cameraViewSecond.setAspectRatio(chipWidth, chipHeight);

            if (indicatorView != null) {
                indicatorView.getLayoutParams().width = pipWidth / 2;
                indicatorView.getLayoutParams().height = (indicatorView.getLayoutParams().width * chipHeight) / chipWidth;
                indicatorView.requestLayout();
            }
        } else {
            // Landscape mode: original sizing
            cameraViewSecond.getLayoutParams().width = screenWidth / 4;
            cameraViewSecond.getLayoutParams().height = screenHeight / 4;
            cameraViewSecond.requestLayout();

            if (indicatorView != null) {
                indicatorView.getLayoutParams().width = screenWidth / 8;
                indicatorView.getLayoutParams().height = screenHeight / 8;
                indicatorView.requestLayout();
            }
        }

        setupDragButton();

        if (keyBoardZoomInOutButton != null && keyBoardView != null) {
            setupKeyboardZoomButton(); // New setup method
        }
        if (dragHandle != null) {
            setupDragHandle();
        }
    }
    
    private void setupKeyboardZoomButton() {
        keyBoardZoomInOutButton.setOnClickListener(v -> {
            Log.d(TAG, "setupKeyboardZoomButton: " + originalWidth + " " + originalHeight);
            FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) keyBoardView.getLayoutParams();
            if (isKeyboardScaled){
                params.width = ViewGroup.LayoutParams.MATCH_PARENT;
                params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                isKeyboardScaled = false;
                keyBoardZoomInOutButton.setImageResource(R.drawable.arrows_angle_contract);
                keyBoardView.setX(0);
            }else {
                params.width = originalWidth / 2;
                params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                keyBoardZoomInOutButton.setImageResource(R.drawable.arrows_angle_expand);
                isKeyboardScaled = true;
            }
            Log.d(TAG, "setupKeyboardZoomButton: " + params.width + " " + params.height);
            keyBoardView.setLayoutParams(params);
        });
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupDragHandle() {
        dragHandle.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    lastTouchX = event.getRawX() - keyBoardView.getX();
                    lastTouchY = event.getRawY() - keyBoardView.getY();
                    return true;
                case MotionEvent.ACTION_MOVE:
                    float newX = event.getRawX() - lastTouchX;
                    float newY = event.getRawY() - lastTouchY;

                    // Constrain within screen boundaries
                    newX = Math.max(0, Math.min(newX, screenWidth - keyBoardView.getWidth()));
                    newY = Math.max(0, Math.min(newY, screenHeight - keyBoardView.getHeight()));

                    keyBoardView.setX(newX);
                    keyBoardView.setY(newY);
                    return true;
                case MotionEvent.ACTION_UP:
                    return true;
            }
            return false;
        });
    }

    public static void enlargeView(){
        // Guard against missing views
        if (thumbnailContainer == null || dragButton == null || activity == null) {
            Log.d(TAG, "enlargeView: views not initialized, skipping");
            return;
        }

        // First, make the views visible so Surface can be created
        if (thumbnailContainer.getVisibility() != View.VISIBLE) {
            thumbnailContainer.setVisibility(View.VISIBLE);
            dragButton.setVisibility(View.VISIBLE);

            // Position drag button
            thumbnailContainer.post(() -> {
                float thumbnailX = thumbnailContainer.getX();
                float thumbnailY = thumbnailContainer.getY();
                float thumbnailWidth = thumbnailContainer.getWidth();
                float dragButtonWidth = dragButton.getWidth();

                float thumbnailCenterX = thumbnailX + (thumbnailWidth / 2f);
                float dragButtonX = thumbnailCenterX - (dragButtonWidth / 2f);
                float dragButtonY = thumbnailY - dragButton.getHeight() - (4 * activity.getResources().getDisplayMetrics().density);

                dragButton.setX(dragButtonX);
                dragButton.setY(dragButtonY);
            });
        }

        // Now try to add surface if available. Not having the binding view is no reason
        // to skip the PiP setup below: the indicator must still be sized and draggable.
        if (activity.mBinding == null || activity.mBinding.cameraViewSecond == null) {
            Log.d(TAG, "enlargeView: cameraViewSecond not available");
        } else if (activity.mBinding.cameraViewSecond.getHolder() != null &&
            activity.mBinding.cameraViewSecond.getHolder().getSurface() != null) {
            activity.mCameraHelper.addSurface(activity.mBinding.cameraViewSecond.getHolder().getSurface(), false);
        }

        showThumbnailWindow();
        resetIndicatorToCenter();
    }


    public static void zoomOut(){
        // Guard against missing views
        if (thumbnailContainer == null || dragButton == null || activity == null) {
            Log.d(TAG, "zoomOut: views not initialized, skipping");
            return;
        }
        // Reset drawer layout if it exists (landscape mode)
        if (drawerLayout != null) {
            drawerLayout.setScaleX(1f);
            drawerLayout.setScaleY(1f);
            drawerLayout.scrollTo(0, 0);
        }
        // Reset portrait zoom if in portrait mode (check if viewMainPreview exists)
        if (activity.mBinding != null && activity.mBinding.viewMainPreview != null) {
            activity.mBinding.viewMainPreview.setTransform(null);
        }
        if (activity.cameraViewSecond != null && activity.cameraViewSecond.getHolder() != null
                && activity.cameraViewSecond.getHolder().getSurface() != null && mCameraHelper != null) {
            mCameraHelper.removeSurface(activity.cameraViewSecond.getHolder().getSurface());
        }
        thumbnailContainer.setVisibility(View.GONE);
        dragButton.setVisibility(View.GONE);
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupDragButton() {
        dragButton.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    lastTouchX = event.getRawX();
                    lastTouchY = event.getRawY();
                    return true;

                case MotionEvent.ACTION_MOVE:
                    float deltaX = event.getRawX() - lastTouchX;
                    float deltaY = event.getRawY() - lastTouchY;

                    float newLeft = thumbnailContainer.getX() + deltaX;
                    float newTop = thumbnailContainer.getY() + deltaY;

                    // Keep within screen bounds
                    newLeft = Math.max(0, Math.min(newLeft, screenWidth - thumbnailContainer.getWidth()));
                    newTop = Math.max(0, Math.min(newTop, screenHeight - thumbnailContainer.getHeight()));

                    thumbnailContainer.setX(newLeft);
                    thumbnailContainer.setY(newTop);

                    // Update dragButton to stay at top-right with offset
                    float thumbnailX = thumbnailContainer.getX();
                    float thumbnailY = thumbnailContainer.getY();
                    float thumbnailWidth = thumbnailContainer.getWidth();
                    float dragButtonWidth = dragButton.getWidth();

                    // Calculate the center X position relative to thumbnailContainer
                    float thumbnailCenterX = thumbnailX + (thumbnailWidth / 2f);

                    // Position dragButton at the top center of thumbnailContainer
                    float dragButtonX = thumbnailCenterX - (dragButtonWidth / 2f); // Center horizontally relative to thumbnailContainer
                    float dragButtonY = thumbnailY - dragButton.getHeight() - (4 * activity.getResources().getDisplayMetrics().density); // Slightly above thumbnailContainer

                    dragButton.setX(dragButtonX);
                    dragButton.setY(dragButtonY);

                    lastTouchX = event.getRawX();
                    lastTouchY = event.getRawY();

                    return true;

                case MotionEvent.ACTION_UP:
                    return true;
            }
            return false;
        });
    }

    private static void resetIndicatorToCenter() {
        // Guard against null views
        if (activity == null || activity.cameraViewSecond == null || indicatorView == null) {
            Log.d(TAG, "resetIndicatorToCenter: views not available, skipping");
            return;
        }

        // Use the requested sizes rather than the measured ones: this runs right after
        // the indicator was resized, before the new size has been laid out.
        float pipWidth = pipWidth();
        float pipHeight = pipHeight();
        float indicatorWidth = indicatorWidth();
        float indicatorHeight = indicatorHeight();
        if (pipWidth <= 0 || pipHeight <= 0) {
            return;
        }

        float centerX = pipWidth / 2f - indicatorWidth / 2f;
        float centerY = pipHeight / 2f - indicatorHeight / 2f;

        // Ensure the position stays within bounds
        indicatorView.setX(Math.max(0, Math.min(centerX, pipWidth - indicatorWidth)));
        indicatorView.setY(Math.max(0, Math.min(centerY, pipHeight - indicatorHeight)));

        // Sync main view to match the centered indicator
        syncMainViewPosition(centerX + indicatorWidth / 2f, centerY + indicatorHeight / 2f);
    }

    private static void showThumbnailWindow() {
        // Guard against null views before calling sub-methods
        if (activity == null || activity.mBinding == null || activity.mBinding.viewMainPreview == null) {
            Log.d(TAG, "showThumbnailWindow: views not available, skipping");
            return;
        }

        // Generate thumbnail
        setupThumbnailDrag();

        // Initialize indicator position based on current main view state
        initializeIndicatorPosition();

        // On the frame the container is made visible it has no measured size yet, so the
        // indicator was sized from the requested layout values. Size it again from the real
        // PiP size as soon as the first layout pass lands.
        if (thumbnailContainer != null) {
            thumbnailContainer.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
                @Override
                public void onLayoutChange(View v, int left, int top, int right, int bottom,
                                           int oldLeft, int oldTop, int oldRight, int oldBottom) {
                    if (right - left <= 0 || bottom - top <= 0) {
                        return;
                    }
                    v.removeOnLayoutChangeListener(this);
                    applyIndicatorSize(currentZoomScale());
                    resetIndicatorToCenter();
                }
            });
        }
    }

    private static void initializeIndicatorPosition() {
        if (indicatorView == null || activity == null || activity.cameraViewSecond == null ||
            activity.mBinding == null || activity.mBinding.viewMainPreview == null) {
            return;
        }

        applyIndicatorSize(currentZoomScale());

        // Position indicator at center initially
        resetIndicatorToCenter();
    }

    /**
     * Size of the PiP. The container is what the user sees, so it is the reference; the
     * surface view can measure differently. Falls back to the requested layout size while
     * the container has not been laid out yet.
     */
    private static float pipWidth() {
        if (thumbnailContainer != null && thumbnailContainer.getWidth() > 0) {
            return thumbnailContainer.getWidth();
        }
        if (activity != null && activity.cameraViewSecond != null && activity.cameraViewSecond.getWidth() > 0) {
            return activity.cameraViewSecond.getWidth();
        }
        if (activity != null && activity.cameraViewSecond != null) {
            ViewGroup.LayoutParams params = activity.cameraViewSecond.getLayoutParams();
            if (params != null && params.width > 0) return params.width;
        }
        if (thumbnailContainer != null && thumbnailContainer.getLayoutParams() != null) {
            return Math.max(0, thumbnailContainer.getLayoutParams().width);
        }
        return 0f;
    }

    private static float pipHeight() {
        if (thumbnailContainer != null && thumbnailContainer.getHeight() > 0) {
            return thumbnailContainer.getHeight();
        }
        if (activity != null && activity.cameraViewSecond != null && activity.cameraViewSecond.getHeight() > 0) {
            return activity.cameraViewSecond.getHeight();
        }
        if (activity != null && activity.cameraViewSecond != null) {
            ViewGroup.LayoutParams params = activity.cameraViewSecond.getLayoutParams();
            if (params != null && params.height > 0) return params.height;
        }
        if (thumbnailContainer != null && thumbnailContainer.getLayoutParams() != null) {
            return Math.max(0, thumbnailContainer.getLayoutParams().height);
        }
        return 0f;
    }

    /**
     * Indicator size as currently requested. The measured size is stale right after a
     * resize, so the layout parameters win when they are set.
     */
    private static float indicatorWidth() {
        if (indicatorView == null) return 0f;
        ViewGroup.LayoutParams params = indicatorView.getLayoutParams();
        return params != null && params.width > 0 ? params.width : indicatorView.getWidth();
    }

    private static float indicatorHeight() {
        if (indicatorView == null) return 0f;
        ViewGroup.LayoutParams params = indicatorView.getLayoutParams();
        return params != null && params.height > 0 ? params.height : indicatorView.getHeight();
    }

    /**
     * The zoom that is actually applied to what the user sees: landscape scales the
     * drawer, portrait scales the image matrix. Reading the portrait scale in landscape
     * always returned 1.0 and the indicator never matched the real viewport.
     */
    private static float currentZoomScale() {
        if (drawerLayout != null) {
            return Math.max(1f, drawerLayout.getScaleX());
        }
        return Math.max(1f, getCurrentPortraitZoomScale());
    }

    /**
     * The PiP shows the whole frame and the indicator the part of it that is on screen,
     * so the indicator is the PiP size divided by the zoom.
     *
     * @return the applied indicator size as {width, height}
     */
    private static float[] applyIndicatorSize(float zoomScale) {
        float pipWidth = pipWidth();
        float pipHeight = pipHeight();
        if (indicatorView == null || pipWidth <= 0 || pipHeight <= 0) {
            return new float[]{0f, 0f};
        }

        float scale = Math.max(1f, zoomScale);
        float width = Math.max(20f, Math.min(pipWidth / scale, pipWidth));
        float height = Math.max(20f, Math.min(pipHeight / scale, pipHeight));

        ViewGroup.LayoutParams params = indicatorView.getLayoutParams();
        if (params != null
                && (Math.abs(params.width - (int) width) > INDICATOR_SIZE_THRESHOLD
                || Math.abs(params.height - (int) height) > INDICATOR_SIZE_THRESHOLD)) {
            params.width = (int) width;
            params.height = (int) height;
            indicatorView.setLayoutParams(params);
            // Fires only when the size really changes, so a pinch produces a handful
            // of lines. Useful to confirm the indicator tracks the real zoom.
            Log.d(TAG, "indicator size " + params.width + "x" + params.height
                    + " scale=" + scale + " pip=" + pipWidth + "x" + pipHeight);
        }

        return new float[]{width, height};
    }

    /**
     * Called while the landscape zoom changes so the indicator tracks the viewport.
     */
    public static void updateIndicatorSize(float zoomScale) {
        applyIndicatorSize(zoomScale);
    }

    /**
     * Get current portrait zoom scale from CustomTouchListener
     */
    private static float getCurrentPortraitZoomScale() {
        return com.openterface.AOS.serial.CustomTouchListener.getPortraitZoomScale();
    }

    @SuppressLint("ClickableViewAccessibility")
    private static void setupThumbnailDrag() {
        // Guard against null views
        if (activity == null || activity.cameraViewSecond == null) {
            Log.d(TAG, "setupThumbnailDrag: cameraViewSecond not available, skipping");
            return;
        }

        activity.cameraViewSecond.setOnTouchListener((v, event) -> {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    if (indicatorView == null) {
                        return true;
                    }
                    // Record the offset between touch point and indicator position
                    // to prevent jumping when starting to drag
                    dragOffsetX = event.getX() - indicatorView.getX();
                    dragOffsetY = event.getY() - indicatorView.getY();
                    updateIndicatorPosition(event.getX(), event.getY());
                    syncMainViewPosition(
                        indicatorView.getX() + indicatorView.getWidth() / 2f,
                        indicatorView.getY() + indicatorView.getHeight() / 2f
                    );
                    return true;

                case MotionEvent.ACTION_MOVE:
                    if (indicatorView == null) {
                        return true;
                    }
                    // Dragging - update position with offset preserved
                    updateIndicatorPosition(event.getX(), event.getY());
                    syncMainViewPosition(
                        indicatorView.getX() + indicatorView.getWidth() / 2f,
                        indicatorView.getY() + indicatorView.getHeight() / 2f
                    );
                    return true;

                case MotionEvent.ACTION_UP:
                    setMouseLocation();
                    return true;
            }
            return false;
        });
    }

    private static void setMouseLocation() {
        // Don't move mouse to viewport center when BT mouse is connected
        // User is using the mouse, not touch gestures
        if (com.openterface.AOS.serial.CustomTouchListener.isBtMouseConnected()) {
            Log.d(TAG, "setMouseLocation: skipped - BT mouse connected");
            return;
        }
        // Delegate to CustomTouchListener to calculate viewport center based on current zoom/pan state
        com.openterface.AOS.serial.CustomTouchListener.moveMouseToViewportCenter();
        Log.d(TAG, "setMouseLocation: moved to viewport center");
    }

    private static void updateIndicatorPosition(float x, float y) {
        // Guard against null views
        if (indicatorView == null || activity == null || activity.cameraViewSecond == null) {
            return;
        }

        // Get the dimensions
        float pipWidth = pipWidth();
        float pipHeight = pipHeight();
        float indicatorWidth = indicatorWidth();
        float indicatorHeight = indicatorHeight();

        // Calculate the position using the drag offset to prevent jumping
        float indicatorX = x - dragOffsetX;
        float indicatorY = y - dragOffsetY;

        // Clamp the position to keep the indicator within bounds
        float maxX = pipWidth - indicatorWidth;
        float maxY = pipHeight - indicatorHeight;

        indicatorX = Math.max(0, Math.min(indicatorX, maxX));
        indicatorY = Math.max(0, Math.min(indicatorY, maxY));

        // Update position directly (no animation for immediate response)
        indicatorView.setX(indicatorX);
        indicatorView.setY(indicatorY);
    }

    /**
     * Update the indicator position and size based on the main view's zoom and pan state
     * This should be called whenever the main view is zoomed or panned
     *
     * The math:
     * - Main view is scaled around its center by zoomScale
     * - translateX shifts the scaled view (positive = shift right)
     * - When translateX = 0, the center of the content is visible
     * - When translateX = -maxTranslate, the left edge is visible (indicator at left of PiP)
     * - When translateX = +maxTranslate, the right edge is visible (indicator at right of PiP)
     *
     * @param zoomScale Current zoom scale of the main view (1.0 = no zoom)
     * @param translateX Horizontal translation of the main view
     * @param translateY Vertical translation of the main view
     */
    public static void updateIndicatorFromMainView(float zoomScale, float translateX, float translateY) {
        if (indicatorView == null || activity == null || activity.cameraViewSecond == null ||
            activity.mBinding == null || activity.mBinding.viewMainPreview == null) {
            return;
        }

        float pipWidth = activity.cameraViewSecond.getWidth();
        float pipHeight = activity.cameraViewSecond.getHeight();

        if (pipWidth <= 0 || pipHeight <= 0) return;

        // Calculate indicator size based on zoom scale
        float[] size = applyIndicatorSize(zoomScale);
        float indicatorWidth = size[0];
        float indicatorHeight = size[1];

        if (zoomScale <= 1.0f) {
            // Not zoomed, center the indicator
            indicatorView.setX((pipWidth - indicatorWidth) / 2f);
            indicatorView.setY((pipHeight - indicatorHeight) / 2f);
            return;
        }

        // Use the main view's display dimensions to calculate max translation
        // This matches the transform applied in applyPortraitZoomTransform()
        float viewWidth = activity.mBinding.viewMainPreview.getWidth();
        float viewHeight = activity.mBinding.viewMainPreview.getHeight();
        float maxTranslateX = (viewWidth * (zoomScale - 1f)) / 2f;
        float maxTranslateY = (viewHeight * (zoomScale - 1f)) / 2f;

        float normalizedX = 0.5f;
        float normalizedY = 0.5f;

        if (maxTranslateX > 0) {
            normalizedX = 0.5f - (translateX / (2f * maxTranslateX));
            normalizedX = Math.max(0, Math.min(normalizedX, 1));
        }

        if (maxTranslateY > 0) {
            normalizedY = 0.5f - (translateY / (2f * maxTranslateY));
            normalizedY = Math.max(0, Math.min(normalizedY, 1));
        }

        // Map normalized position to indicator position within PiP bounds
        // normalizedX=0 → indicator at left edge (0), normalizedX=1 → indicator at right edge
        // Account for indicator's own size so it stays fully within bounds
        float maxIndicatorX = pipWidth - indicatorWidth;
        float maxIndicatorY = pipHeight - indicatorHeight;
        float indicatorX = maxIndicatorX > 0 ? normalizedX * maxIndicatorX : 0;
        float indicatorY = maxIndicatorY > 0 ? normalizedY * maxIndicatorY : 0;

        indicatorX = Math.max(0, Math.min(indicatorX, maxIndicatorX));
        indicatorY = Math.max(0, Math.min(indicatorY, maxIndicatorY));

        indicatorView.setX(indicatorX);
        indicatorView.setY(indicatorY);
    }


    private static void syncMainViewPosition(float thumbX, float thumbY) {
        // Guard against null views - especially drawerLayout which is null in portrait mode
        if (indicatorView == null || thumbnailContainer == null || activity == null ||
            activity.mBinding == null || activity.mBinding.viewMainPreview == null ||
            activity.cameraViewSecond == null) {
            if (DEBUG_LAYOUT_LOG) {
                Log.d(TAG, "syncMainViewPosition: views not available, skipping");
            }
            return;
        }

        // This runs for every MOVE event while the thumbnail is dragged, so it
        // stays free of logging and of work that is not needed for the mapping.
        ratioX = (float) (activity.mBinding.viewMainPreview.getWidth()) / activity.cameraViewSecond.getWidth();
        ratioY = (float) (activity.mBinding.viewMainPreview.getHeight()) / activity.cameraViewSecond.getHeight();

        float mainX = thumbX * ratioX;
        float mainY = thumbY * ratioY;

        // In landscape mode, use drawerLayout to scroll the main view
        if (drawerLayout != null) {
            int scrollX;
            if((mainX - drawerLayout.getWidth()/2f) < -(drawerLayout.getWidth()/ ratioX)){
                scrollX = (int)(-(drawerLayout.getWidth()/ ratioX));
            } else if ((mainX - drawerLayout.getWidth()/2f) > (drawerLayout.getWidth()/ ratioX)) {
                scrollX = (int)((drawerLayout.getWidth()/ ratioX));
            }else {
                scrollX = (int)(mainX - drawerLayout.getWidth()/2f);
            }

            int scrollY;
            if((mainY - drawerLayout.getHeight()/2f) < -(drawerLayout.getHeight()/ ratioY)){
                scrollY = (int)(-(drawerLayout.getHeight()/ ratioY));
            } else if ((mainY - drawerLayout.getHeight()/2f) > (drawerLayout.getHeight()/ ratioY)) {
                scrollY = (int)((drawerLayout.getHeight()/ ratioY));
            }else {
                scrollY = (int)(mainY - drawerLayout.getHeight()/2f);
            }
            if (DEBUG_LAYOUT_LOG) {
                Log.d(TAG, "syncMainViewPosition: scrollX=" + scrollX
                        + " scrollY=" + scrollY);
            }
            drawerLayout.scrollTo(scrollX, scrollY);
        } else {
            // In portrait mode, use CustomTouchListener's zoom state
            float currentZoomScale = com.openterface.AOS.serial.CustomTouchListener.getPortraitZoomScale();

            if (currentZoomScale <= 1.0f) {
                return;
            }

            // Use the main view's display dimensions to calculate max translation
            // This matches the transform applied in applyPortraitZoomTransform()
            float viewWidth = activity.mBinding.viewMainPreview.getWidth();
            float viewHeight = activity.mBinding.viewMainPreview.getHeight();

            float pipWidth = activity.cameraViewSecond.getWidth();
            float pipHeight = activity.cameraViewSecond.getHeight();

            // Get indicator dimensions for correct edge-to-edge mapping
            float indicatorWidth = indicatorView.getWidth();
            float indicatorHeight = indicatorView.getHeight();

            // Map indicator position within valid range to normalized [0..1]
            // indicatorX = 0 (left edge) → normalizedX = 0 (showing left edge of zoomed content)
            // indicatorX = pipWidth - indicatorWidth (right edge) → normalizedX = 1 (showing right edge)
            float maxIndicatorX = pipWidth - indicatorWidth;
            float maxIndicatorY = pipHeight - indicatorHeight;
            float indicatorX = indicatorView.getX();
            float indicatorY = indicatorView.getY();

            float normalizedX = maxIndicatorX > 0 ? indicatorX / maxIndicatorX : 0.5f;
            float normalizedY = maxIndicatorY > 0 ? indicatorY / maxIndicatorY : 0.5f;

            // Clamp to valid range
            normalizedX = Math.max(0, Math.min(normalizedX, 1));
            normalizedY = Math.max(0, Math.min(normalizedY, 1));

            // Calculate actual content dimensions after stretch correction.
            // Stretch correction fills viewWidth, maintains buffer aspect ratio,
            // so contentHeight = viewWidth / bufferAspect (may be < viewHeight).
            float bufferWidth = activity.getPreviewWidth();
            float bufferHeight = activity.getPreviewHeight();
            float contentWidth = viewWidth;
            float contentHeight = viewWidth * bufferHeight / bufferWidth;
            // Clamp: if content is larger than view, cap at view size
            if (contentHeight > viewHeight) contentHeight = viewHeight;
            if (contentWidth > viewWidth) contentWidth = viewWidth;

            // Convert normalized position to main view translation
            // Use corrected content dimensions for accurate edge-to-edge mapping
            float maxTranslateX = (contentWidth * (currentZoomScale - 1f)) / 2f;
            float maxTranslateY = (contentHeight * (currentZoomScale - 1f)) / 2f;

            // normalizedX=0 → translateX=+max (left edge visible)
            // normalizedX=1 → translateX=-max (right edge visible)
            float newTranslateX = (0.5f - normalizedX) * 2f * maxTranslateX;
            float newTranslateY = (0.5f - normalizedY) * 2f * maxTranslateY;

            // Clamp to valid range
            newTranslateX = Math.max(-maxTranslateX, Math.min(maxTranslateX, newTranslateX));
            newTranslateY = Math.max(-maxTranslateY, Math.min(maxTranslateY, newTranslateY));

            // Update CustomTouchListener's state and apply transform
            com.openterface.AOS.serial.CustomTouchListener.setPortraitPan(newTranslateX, newTranslateY);
            com.openterface.AOS.serial.CustomTouchListener.applyCurrentPortraitTransform();

            if (DEBUG_LAYOUT_LOG) {
                Log.d(TAG, "Portrait mode: normalizedX=" + normalizedX + " normalizedY=" + normalizedY
                    + " translateX=" + newTranslateX + " translateY=" + newTranslateY
                    + " viewWidth=" + viewWidth + " viewHeight=" + viewHeight);
            }
        }
    }

}
