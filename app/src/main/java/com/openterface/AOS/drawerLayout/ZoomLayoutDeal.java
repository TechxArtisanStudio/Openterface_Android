package com.openterface.AOS.drawerLayout;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.drawerlayout.widget.DrawerLayout;

import com.openterface.AOS.ICameraHelper;
import com.openterface.AOS.R;
import com.openterface.AOS.activity.MainActivity;
import com.openterface.AOS.databinding.ActivityMainBinding;
import com.openterface.AOS.target.MouseManager;
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

    private static Button dragButton;
    private float lastTouchX, lastTouchY;

    public ZoomLayoutDeal(MainActivity activity, ICameraHelper mCameraHelper, ActivityMainBinding mBinding) {
        this.activity = activity;
        this.mCameraHelper = mCameraHelper;
        this.mBinding = mBinding;
        cameraViewSecond = activity.findViewById(R.id.cameraViewSecond);
        drawerLayout = activity.findViewById(R.id.drawer_layout);
        thumbnailContainer = activity.findViewById(R.id.thumbnail_container);
        indicatorView = activity.findViewById(R.id.view_indicator);
        dragButton = activity.findViewById(R.id.drag_button);

        DisplayMetrics displayMetrics = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getRealMetrics(displayMetrics);
        screenWidth = displayMetrics.widthPixels;
        screenHeight = displayMetrics.heightPixels;

        Log.d("initViews", "screenWidth: " + screenWidth + " screenHeight: " + screenHeight);


        cameraViewSecond.getLayoutParams().width = screenWidth / 4;
        cameraViewSecond.getLayoutParams().height = screenHeight / 4;
        cameraViewSecond.requestLayout();

        indicatorView.getLayoutParams().width = screenWidth / 8;
        indicatorView.getLayoutParams().height = screenHeight / 8;
        indicatorView.requestLayout();

        setupDragButton();
    }

    public static void enlargeView(){
        activity.mCameraHelper.addSurface(activity.mBinding.cameraViewSecond.getHolder().getSurface(), false);
        thumbnailContainer.setVisibility(View.VISIBLE);
        dragButton.setVisibility(View.VISIBLE);
        showThumbnailWindow();
        resetIndicatorToCenter();

        float thumbnailRight = thumbnailContainer.getX() + thumbnailContainer.getWidth();
        float thumbnailTop = thumbnailContainer.getY();
        float offset = 10f; // Small offset to avoid overlapping indicatorView
        dragButton.setX(thumbnailRight - offset); // Slightly left of right edge
        dragButton.setY(thumbnailTop - dragButton.getHeight() + offset);
    }


    public static void zoomOut(){
        drawerLayout.setScaleX(1f);
        drawerLayout.setScaleY(1f);
        mCameraHelper.removeSurface(activity.cameraViewSecond.getHolder().getSurface());
        thumbnailContainer.setVisibility(View.GONE);
        dragButton.setVisibility(View.GONE);
        drawerLayout.scrollTo(
                (0),
                (0)
        );
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
                    float offset = 10f;
                    dragButton.setX(newLeft + thumbnailContainer.getWidth() - offset);
                    dragButton.setY(newTop - dragButton.getHeight() + offset);

                    lastTouchX = event.getRawX();
                    lastTouchY = event.getRawY();

                    Log.d("ZoomLayoutDeal", "Dragging: x=" + newLeft + ", y=" + newTop);
                    return true;

                case MotionEvent.ACTION_UP:
                    return true;
            }
            return false;
        });
    }

    private static void resetIndicatorToCenter() {
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

        // Generate thumbnail
        generateThumbnail();

        // Set drag and drop listening
        setupThumbnailDrag();
    }

    private static void generateThumbnail() {
        if (!activity.mBinding.viewMainPreview.isAvailable()) {
            Log.e("generateThumbnail", "TextureView is not available");
            return;
        }

        Bitmap mainBitmap = activity.mBinding.viewMainPreview.getBitmap();
        if (mainBitmap == null) {
            Log.e("generateThumbnail", "Failed to get bitmap from TextureView");
            return;
        }

    }

    @SuppressLint("ClickableViewAccessibility")
    private static void setupThumbnailDrag() {
        activity.cameraViewSecond.setOnTouchListener((v, event) -> {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    updateIndicatorPosition(event.getX(), event.getY());
                    syncMainViewPosition(event.getX(), event.getY());
//                    indicatorView.setVisibility(View.VISIBLE);
                    return true;

                case MotionEvent.ACTION_MOVE:
                    updateIndicatorPosition(event.getX(), event.getY());
                    syncMainViewPosition(event.getX(), event.getY());
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
        MouseManager.sendHexAbsData(mouseLocationX, mouseLocationY);
        Log.d("setMouseLocation", "mouseLocationX: " + mouseLocationX + " mouseLocationY: " + mouseLocationY);
    }

    private static void updateIndicatorPosition(float x, float y) {
        // Center display indicator
        float indicatorX = x - indicatorView.getWidth()/2f;
        float indicatorY = y - indicatorView.getHeight()/2f;
        indicatorView.setX(Math.max(0, Math.min(
                indicatorX,
                activity.cameraViewSecond.getWidth() - indicatorView.getWidth()
        )));

        indicatorView.setY(Math.max(0, Math.min(
                indicatorY,
                activity.cameraViewSecond.getHeight() - indicatorView.getHeight()
        )));
    }

    private static void syncMainViewPosition(float thumbX, float thumbY) {
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
        float currentScale = drawerLayout.getScaleX();
        ratioX = (float) (activity.mBinding.viewMainPreview.getWidth()) / activity.cameraViewSecond.getWidth();
        ratioY = (float) (activity.mBinding.viewMainPreview.getHeight()) / activity.cameraViewSecond.getHeight();
        Log.d("syncMainViewPosition", "currentScale: " + currentScale);
        Log.d("Center Coordinates", "ratioX: " + ratioX + ", ratioY: " + ratioY);

        float mainX = thumbX * ratioX;
        float mainY = thumbY * ratioY;

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
    }

}
