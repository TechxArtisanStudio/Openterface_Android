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
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.drawerlayout.widget.DrawerLayout;

import com.openterface.AOS.ICameraHelper;
import com.openterface.AOS.R;
import com.openterface.AOS.activity.MainActivity;
import com.openterface.AOS.databinding.ActivityMainBinding;
import com.openterface.AOS.target.HidManager;
// import com.openterface.AOS.target.MouseManager;
import com.serenegiant.widget.AspectRatioSurfaceView;

public class ZoomLayoutDeal {
    private static MainActivity activity;
    private static DrawerLayout drawerLayout;
    private static View thumbnailContainer;
    private static View indicatorView;
    private static float ratioX;
    private static float ratioY;
    private final AspectRatioSurfaceView cameraViewSecond;
    private static ICameraHelper mCameraHelper;
    private ActivityMainBinding mBinding;
    private static int setMaxViewX;
    private static int setMaxViewY;
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
            Log.d("ZoomLayoutDeal", "Essential views not found, skipping zoom setup");
            return;
        }

        DisplayMetrics displayMetrics = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getRealMetrics(displayMetrics);
        screenWidth = displayMetrics.widthPixels;
        screenHeight = displayMetrics.heightPixels;

        Log.d("initViews", "screenWidth: " + screenWidth + " screenHeight: " + screenHeight);

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
            cameraViewSecond.getLayoutParams().width = pipWidth;
            cameraViewSecond.getLayoutParams().height = pipHeight;
            cameraViewSecond.requestLayout();

            // Set cameraViewSecond's aspect ratio to match the chip
            cameraViewSecond.setAspectRatio(chipWidth, chipHeight);

            if (indicatorView != null) {
                indicatorView.getLayoutParams().width = screenWidth / 6;
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
            Log.d("keyBoard", "setupKeyboardZoomButton: " + originalWidth + " " + originalHeight);
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
            Log.d("keyBoard", "setupKeyboardZoomButton: " + params.width + " " + params.height);
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
            Log.d("ZoomLayoutDeal", "enlargeView: views not initialized, skipping");
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

        // Now try to add surface if available
        if (activity.mBinding == null || activity.mBinding.cameraViewSecond == null) {
            Log.d("ZoomLayoutDeal", "enlargeView: cameraViewSecond not available");
            return;
        }

        if (activity.mBinding.cameraViewSecond.getHolder() != null &&
            activity.mBinding.cameraViewSecond.getHolder().getSurface() != null) {
            activity.mCameraHelper.addSurface(activity.mBinding.cameraViewSecond.getHolder().getSurface(), false);
        }

        showThumbnailWindow();
        resetIndicatorToCenter();
    }


    public static void zoomOut(){
        // Guard against missing views
        if (thumbnailContainer == null || dragButton == null || activity == null) {
            Log.d("ZoomLayoutDeal", "zoomOut: views not initialized, skipping");
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
            Log.d("ZoomLayoutDeal", "resetIndicatorToCenter: views not available, skipping");
            return;
        }

        // Calculate center position of cameraViewSecond
        float centerX = activity.cameraViewSecond.getWidth() / 2f - indicatorView.getWidth() / 2f;
        float centerY = activity.cameraViewSecond.getHeight() / 2f - indicatorView.getHeight() / 2f;

        // Ensure the position stays within bounds
        indicatorView.setX(Math.max(0, Math.min(centerX, activity.cameraViewSecond.getWidth() - indicatorView.getWidth())));
        indicatorView.setY(Math.max(0, Math.min(centerY, activity.cameraViewSecond.getHeight() - indicatorView.getHeight())));

        // Sync main view to match the centered indicator
        syncMainViewPosition(centerX + indicatorView.getWidth() / 2f, centerY + indicatorView.getHeight() / 2f);

        Log.d("ZoomLayoutDeal", "Indicator reset to center: x=" + indicatorView.getX() + ", y=" + indicatorView.getY());
    }

    private static void showThumbnailWindow() {
        // Guard against null views before calling sub-methods
        if (activity == null || activity.mBinding == null || activity.mBinding.viewMainPreview == null) {
            Log.d("ZoomLayoutDeal", "showThumbnailWindow: views not available, skipping");
            return;
        }

        // Generate thumbnail
        generateThumbnail();

        // Set drag and drop listening
        setupThumbnailDrag();

        // Initialize indicator position based on current main view state
        initializeIndicatorPosition();
    }

    private static void generateThumbnail() {
        if (activity == null || activity.mBinding == null || activity.mBinding.viewMainPreview == null) {
            Log.e("generateThumbnail", "TextureView is not available");
            return;
        }

        if (!activity.mBinding.viewMainPreview.isAvailable()) {
            Log.e("generateThumbnail", "TextureView is not available");
            return;
        }

        try {
            Bitmap mainBitmap = activity.mBinding.viewMainPreview.getBitmap();
            if (mainBitmap == null) {
                Log.e("generateThumbnail", "Failed to get bitmap from TextureView");
                return;
            }
        } catch (Exception e) {
            Log.e("generateThumbnail", "Exception getting bitmap: " + e.getMessage());
        }
    }

    /**
     * Initialize indicator position based on current main view zoom/pan state
     * This ensures the green box shows the correct area of the main view
     */
    private static void initializeIndicatorPosition() {
        if (indicatorView == null || activity == null || activity.cameraViewSecond == null ||
            activity.mBinding == null || activity.mBinding.viewMainPreview == null) {
            return;
        }

        // Get current zoom scale from CustomTouchListener
        float currentScale = getCurrentPortraitZoomScale();

        // Calculate indicator size based on zoom scale
        // When zoomed in, the visible area is smaller, so indicator should be smaller
        float indicatorWidth = activity.cameraViewSecond.getWidth() / currentScale;
        float indicatorHeight = activity.cameraViewSecond.getHeight() / currentScale;

        // Constrain indicator size
        indicatorWidth = Math.max(20f, Math.min(indicatorWidth, activity.cameraViewSecond.getWidth()));
        indicatorHeight = Math.max(20f, Math.min(indicatorHeight, activity.cameraViewSecond.getHeight()));

        indicatorView.getLayoutParams().width = (int) indicatorWidth;
        indicatorView.getLayoutParams().height = (int) indicatorHeight;
        indicatorView.requestLayout();

        // Position indicator at center initially
        resetIndicatorToCenter();
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
            Log.d("ZoomLayoutDeal", "setupThumbnailDrag: cameraViewSecond not available, skipping");
            return;
        }

        activity.cameraViewSecond.setOnTouchListener((v, event) -> {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    // Record the offset between touch point and indicator position
                    // to prevent jumping when starting to drag
                    if (indicatorView != null) {
                        dragOffsetX = event.getX() - indicatorView.getX();
                        dragOffsetY = event.getY() - indicatorView.getY();
                    }
                    // Update indicator position with offset preserved
                    updateIndicatorPosition(event.getX(), event.getY());
                    syncMainViewPosition(
                        indicatorView.getX() + indicatorView.getWidth() / 2f,
                        indicatorView.getY() + indicatorView.getHeight() / 2f
                    );
                    return true;

                case MotionEvent.ACTION_MOVE:
                    // Dragging - update position with offset preserved
                    updateIndicatorPosition(event.getX(), event.getY());
                    syncMainViewPosition(
                        indicatorView.getX() + indicatorView.getWidth() / 2f,
                        indicatorView.getY() + indicatorView.getHeight() / 2f
                    );
                    return true;

                case MotionEvent.ACTION_UP:
                    setMouseLocation(setMaxViewX, setMaxViewY);
                    return true;
            }
            return false;
        });
    }

    private static void setMouseLocation(int setMaxViewX, int setMaxViewY) {
        float mouseLocationX = (float)setMaxViewX + screenWidth/2.0f;
        float mouseLocationY = (float)setMaxViewY + screenHeight/2.0f;
        HidManager.sendHexAbsData(mouseLocationX, mouseLocationY);
        Log.d("setMouseLocation", "mouseLocationX: " + mouseLocationX + " mouseLocationY: " + mouseLocationY);
    }

    private static void updateIndicatorPosition(float x, float y) {
        // Guard against null views
        if (indicatorView == null || activity == null || activity.cameraViewSecond == null) {
            return;
        }

        // Get the dimensions
        float pipWidth = activity.cameraViewSecond.getWidth();
        float pipHeight = activity.cameraViewSecond.getHeight();
        float indicatorWidth = indicatorView.getWidth();
        float indicatorHeight = indicatorView.getHeight();

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

        // Use the chip's actual resolution (mPreviewWidth x mPreviewHeight)
        int chipWidth = activity.getPreviewWidth();
        int chipHeight = activity.getPreviewHeight();

        // If chip resolution is not available yet, use default 1920x1080
        if (chipWidth <= 0 || chipHeight <= 0) {
            chipWidth = 1920;
            chipHeight = 1080;
        }

        float pipWidth = activity.cameraViewSecond.getWidth();
        float pipHeight = activity.cameraViewSecond.getHeight();

        if (pipWidth <= 0 || pipHeight <= 0) return;

        // Calculate indicator size based on zoom scale
        float indicatorWidth = pipWidth / zoomScale;
        float indicatorHeight = pipHeight / zoomScale;
        indicatorWidth = Math.max(20f, Math.min(indicatorWidth, pipWidth));
        indicatorHeight = Math.max(20f, Math.min(indicatorHeight, pipHeight));

        // Update indicator size
        ViewGroup.LayoutParams params = indicatorView.getLayoutParams();
        params.width = (int) indicatorWidth;
        params.height = (int) indicatorHeight;
        indicatorView.setLayoutParams(params);

        if (zoomScale <= 1.0f) {
            // Not zoomed, center the indicator
            indicatorView.setX((pipWidth - indicatorWidth) / 2f);
            indicatorView.setY((pipHeight - indicatorHeight) / 2f);
            return;
        }

        // Use the chip's actual resolution to calculate max translation
        // The chip resolution is the actual video content dimensions
        float maxTranslateX = (chipWidth * (zoomScale - 1f)) / 2f;
        float maxTranslateY = (chipHeight * (zoomScale - 1f)) / 2f;

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

        // The indicator center should be at normalizedX * pipWidth
        float indicatorX = normalizedX * pipWidth - indicatorWidth / 2f;
        float indicatorY = normalizedY * pipHeight - indicatorHeight / 2f;

        // Clamp to bounds
        indicatorX = Math.max(0, Math.min(indicatorX, pipWidth - indicatorWidth));
        indicatorY = Math.max(0, Math.min(indicatorY, pipHeight - indicatorHeight));

        indicatorView.setX(indicatorX);
        indicatorView.setY(indicatorY);
    }


    private static void syncMainViewPosition(float thumbX, float thumbY) {
        // Guard against null views - especially drawerLayout which is null in portrait mode
        if (indicatorView == null || thumbnailContainer == null || activity == null ||
            activity.mBinding == null || activity.mBinding.viewMainPreview == null ||
            activity.cameraViewSecond == null) {
            Log.d("ZoomLayoutDeal", "syncMainViewPosition: views not available, skipping");
            return;
        }

        int[] location = new int[2];
        indicatorView.getLocationInWindow(location);

        int[] containerLocation = new int[2];
        thumbnailContainer.getLocationInWindow(containerLocation);

        // Computation center coordinate
        float centerX = location[0] - containerLocation[0] + indicatorView.getWidth() / 2f;
        float centerY = location[1] - containerLocation[1] + indicatorView.getHeight() / 2f;

        Log.d("Center Coordinates", "X: " + centerX + ", Y: " + centerY);

        Log.d("syncMainViewPosition", "sizeThumbX: " + thumbX + " sizeThumbY: " + thumbY);
        // Calculate where the main view should scroll to
        ratioX = (float) (activity.mBinding.viewMainPreview.getWidth()) / activity.cameraViewSecond.getWidth();
        ratioY = (float) (activity.mBinding.viewMainPreview.getHeight()) / activity.cameraViewSecond.getHeight();
        Log.d("syncMainViewPosition", "ratioX: " + ratioX + ", ratioY: " + ratioY);

        float mainX = thumbX * ratioX;
        float mainY = thumbY * ratioY;

        // In landscape mode, use drawerLayout to scroll the main view
        if (drawerLayout != null) {
            float currentScale = drawerLayout.getScaleX();
            Log.d("syncMainViewPosition", "currentScale: " + currentScale);
            Log.d("Center Coordinates", "ratioX: " + ratioX + ", ratioY: " + ratioY);

            Log.d("syncMainViewPosition", "mainX: " + mainX + " mainY: " + mainY);
            // Update the main view location
            Log.d("syncMainViewPosition", "mainX - drawerLayout.getWidth()/2f: " + (mainX - drawerLayout.getWidth()/2f));
            Log.d("syncMainViewPosition", "mainY - drawerLayout.getHeight()/2f: " + (mainY - drawerLayout.getHeight()/2f));
            if((mainX - drawerLayout.getWidth()/2f) < -(drawerLayout.getWidth()/ ratioX)){
                setMaxViewX = (int)(-(drawerLayout.getWidth()/ ratioX));
            } else if ((mainX - drawerLayout.getWidth()/2f) > (drawerLayout.getWidth()/ ratioX)) {
                setMaxViewX = (int)((drawerLayout.getWidth()/ ratioX));
            }else {
                setMaxViewX = (int)(mainX - drawerLayout.getWidth()/2f);
            }

            if((mainY - drawerLayout.getHeight()/2f) < -(drawerLayout.getHeight()/ ratioY)){
                setMaxViewY = (int)(-(drawerLayout.getHeight()/ ratioY));
            } else if ((mainY - drawerLayout.getHeight()/2f) > (drawerLayout.getHeight()/ ratioY)) {
                setMaxViewY = (int)((drawerLayout.getHeight()/ ratioY));
            }else {
                setMaxViewY = (int)(mainY - drawerLayout.getHeight()/2f);
            }
            Log.d("syncMainViewPosition", "setMaxViewX: " + setMaxViewX + " setMaxViewY: " + setMaxViewY);
            drawerLayout.scrollTo(
                    (setMaxViewX),
                    (setMaxViewY)
            );
        } else {
            // In portrait mode, use CustomTouchListener's zoom state
            float currentZoomScale = com.openterface.AOS.serial.CustomTouchListener.getPortraitZoomScale();

            if (currentZoomScale <= 1.0f) {
                return;
            }

            // Use the chip's actual resolution (mPreviewWidth x mPreviewHeight)
            int chipWidth = activity.getPreviewWidth();
            int chipHeight = activity.getPreviewHeight();

            // If chip resolution is not available yet, use default 1920x1080
            if (chipWidth <= 0 || chipHeight <= 0) {
                chipWidth = 1920;
                chipHeight = 1080;
            }

            float pipWidth = activity.cameraViewSecond.getWidth();
            float pipHeight = activity.cameraViewSecond.getHeight();

            // Calculate indicator center in PiP coordinates
            float indicatorCenterX = indicatorView.getX() + indicatorView.getWidth() / 2f;
            float indicatorCenterY = indicatorView.getY() + indicatorView.getHeight() / 2f;

            // Normalize: 0 = left/top edge, 0.5 = center, 1 = right/bottom edge
            float normalizedX = pipWidth > 0 ? indicatorCenterX / pipWidth : 0.5f;
            float normalizedY = pipHeight > 0 ? indicatorCenterY / pipHeight : 0.5f;

            // Clamp to valid range
            normalizedX = Math.max(0, Math.min(normalizedX, 1));
            normalizedY = Math.max(0, Math.min(normalizedY, 1));

            // Convert normalized position to main view translation
            // Use chip's actual resolution for max translation calculation
            float maxTranslateX = (chipWidth * (currentZoomScale - 1f)) / 2f;
            float maxTranslateY = (chipHeight * (currentZoomScale - 1f)) / 2f;

            float newTranslateX = (0.5f - normalizedX) * 2f * maxTranslateX;
            float newTranslateY = (0.5f - normalizedY) * 2f * maxTranslateY;

            // Clamp to valid range
            newTranslateX = Math.max(-maxTranslateX, Math.min(maxTranslateX, newTranslateX));
            newTranslateY = Math.max(-maxTranslateY, Math.min(maxTranslateY, newTranslateY));

            // Update CustomTouchListener's state and apply transform
            com.openterface.AOS.serial.CustomTouchListener.setPortraitPan(newTranslateX, newTranslateY);
            com.openterface.AOS.serial.CustomTouchListener.applyCurrentPortraitTransform();

            // Store for setMouseLocation calculation
            setMaxViewX = (int) newTranslateX;
            setMaxViewY = (int) newTranslateY;

            Log.d("syncMainViewPosition", "Portrait mode: normalizedX=" + normalizedX + " normalizedY=" + normalizedY
                + " translateX=" + newTranslateX + " translateY=" + newTranslateY
                + " chipWidth=" + chipWidth + " chipHeight=" + chipHeight);
        }
    }

}
