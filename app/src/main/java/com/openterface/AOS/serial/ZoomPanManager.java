package com.openterface.AOS.serial;

import android.graphics.Matrix;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import androidx.drawerlayout.widget.DrawerLayout;

import com.openterface.AOS.activity.MainActivity;
import com.openterface.AOS.drawerLayout.ZoomLayoutDeal;
import com.serenegiant.widget.AspectRatioTextureView;

/**
 * Manages zoom and pan state for both portrait and landscape modes.
 *
 * Portrait mode: Uses TextureView.setTransform(Matrix) for GPU-quality scaling.
 * Landscape mode: Uses DrawerLayout.setScaleX/Y for simpler scaling.
 *
 * Coordinates the PiP (Picture-in-Picture) indicator via ZoomLayoutDeal.
 */
public class ZoomPanManager {
    private static final String TAG = "OP-ZOOM";

    // Portrait mode state
    private static float mPortraitZoomScale = 1.0f;
    private static float mPortraitTranslateX = 0f;
    private static float mPortraitTranslateY = 0f;

    // Scale limits
    private static final float MIN_SCALE = 1.0f;
    private static final float MAX_SCALE_LANDSCAPE = 2.0f;
    private static final float MAX_SCALE_PORTRAIT = 5.0f;
    private static final float DEFAULT_MAX_SCALE_PORTRAIT = 3.0f;
    private static final float CONTAINER_HEIGHT_RATIO = 0.95f;

    // PiP state: enlargeView() grabs a bitmap and re-adds camera surfaces, so it
    // must only run on the unzoomed -> zoomed transition, never per pinch frame.
    private static boolean mPipVisible = false;

    // References
    private static MainActivity activity;
    private static DrawerLayout drawerLayout;
    private static boolean isPortraitMode;

    // Callback for UI updates
    private static Callbacks callbacks;

    public interface Callbacks {
        void onBlackBorderOverlayUpdate();
    }

    // ===== Initialization =====

    public static void init(MainActivity mainActivity, DrawerLayout drawer, boolean portrait, Callbacks cb) {
        activity = mainActivity;
        drawerLayout = drawer;
        isPortraitMode = portrait;
        callbacks = cb;
        // A new listener means a fresh layout (startup or rotation): start unzoomed so
        // the stored state always matches what is actually drawn.
        reset();
        Log.d(TAG, "init: portrait=" + portrait + " drawerLayout=" + (drawer != null));
    }

    public static void setPortraitMode(boolean portrait) {
        isPortraitMode = portrait;
    }

    // ===== Getters =====

    public static float getPortraitZoomScale() {
        return mPortraitZoomScale;
    }

    public static float getPortraitTranslateX() {
        return mPortraitTranslateX;
    }

    public static float getPortraitTranslateY() {
        return mPortraitTranslateY;
    }

    public static boolean isPortraitMode() {
        return isPortraitMode;
    }

    /**
     * Get effective zoom scale (minimum 1.0).
     */
    public static float getEffectiveZoomScale() {
        return Math.max(1.0f, mPortraitZoomScale);
    }

    // ===== Setters =====

    /**
     * Set portrait pan translation from external source (e.g., PiP indicator drag).
     */
    public static void setPortraitPan(float translateX, float translateY) {
        mPortraitTranslateX = translateX;
        mPortraitTranslateY = translateY;
    }

    /**
     * Set portrait zoom scale.
     */
    public static void setPortraitZoomScale(float scale) {
        mPortraitZoomScale = Math.max(MIN_SCALE, scale);
    }

    // ===== Reset =====

    /**
     * Reset all zoom/pan state to default.
     */
    public static void reset() {
        mPortraitZoomScale = 1.0f;
        mPortraitTranslateX = 0f;
        mPortraitTranslateY = 0f;
        mPipVisible = false;
    }

    // ===== Zoom operations =====

    /**
     * Adjust zoom by a factor (e.g., 1.1 for zoom in, 0.9 for zoom out).
     */
    public static void adjustZoom(float zoomFactor) {
        if (isPortraitMode) {
            adjustPortraitZoom(zoomFactor);
        } else {
            adjustLandscapeZoom(zoomFactor);
        }
    }

    private static void adjustPortraitZoom(float zoomFactor) {
        if (activity == null || activity.mBinding == null || activity.mBinding.viewMainPreview == null) {
            return;
        }

        // Calculate max scale based on container size
        float maxScale = calculateMaxPortraitScale();

        // Calculate new scale
        float currentScale = mPortraitZoomScale;
        float newScale = currentScale * zoomFactor;

        // Clamp within bounds
        newScale = Math.max(MIN_SCALE, Math.min(maxScale, newScale));
        mPortraitZoomScale = newScale;

        // If zooming out to minimum, hide PiP and reset translation
        if (newScale <= MIN_SCALE) {
            mPortraitTranslateX = 0f;
            mPortraitTranslateY = 0f;
            if (mPipVisible) {
                ZoomLayoutDeal.zoomOut();
                mPipVisible = false;
            }
            resetTextureViewHeight();
        } else if (!mPipVisible) {
            // Only on the transition into zoom: enlargeView() captures a bitmap and
            // re-adds surfaces, which is far too heavy to run on every pinch frame
            // (it caused the pinch to stutter/freeze after the first step).
            ZoomLayoutDeal.enlargeView();
            mPipVisible = true;
        }

        // Apply transform
        applyPortraitZoomTransform();

        // Update black border overlay
        if (callbacks != null) {
            callbacks.onBlackBorderOverlayUpdate();
        }

        Log.d(TAG, "Portrait zoom adjusted: newScale=" + newScale +
              " translateX=" + mPortraitTranslateX + " translateY=" + mPortraitTranslateY);
    }

    private static void adjustLandscapeZoom(float zoomFactor) {
        if (drawerLayout == null) {
            Log.d(TAG, "adjustZoom: drawerLayout not available, skipping zoom");
            return;
        }

        // Get current scale
        float currentScale = drawerLayout.getScaleX();
        float newScale = currentScale * zoomFactor;

        // Clamp within bounds
        if (newScale < MIN_SCALE) {
            newScale = MIN_SCALE;
            if (mPipVisible) {
                ZoomLayoutDeal.zoomOut();
                mPipVisible = false;
            }
        } else if (newScale > MAX_SCALE_LANDSCAPE) {
            newScale = MAX_SCALE_LANDSCAPE;
        }

        // Show PiP once when zoomed in (not on every pinch frame)
        if (newScale > MIN_SCALE && !mPipVisible) {
            ZoomLayoutDeal.enlargeView();
            mPipVisible = true;
        }

        // Apply the new scale
        drawerLayout.setScaleX(newScale);
        drawerLayout.setScaleY(newScale);

        // Center pivot
        drawerLayout.setPivotX(drawerLayout.getWidth() / 2f);
        drawerLayout.setPivotY(drawerLayout.getHeight() / 2f);

        // Enforce clipping
        drawerLayout.setClipChildren(true);
        drawerLayout.setClipToPadding(true);

        Log.d(TAG, "Landscape zoom adjusted: newScale=" + newScale);
    }

    /**
     * Maximum portrait zoom scale.
     *
     * The portrait preview displays the (landscape) camera buffer at full view width, so
     * the natural display height is viewWidth * bufferHeight / bufferWidth. Zoom is capped
     * so the enlarged video can fill the available container height, and never above
     * MAX_SCALE_PORTRAIT.
     */
    private static float calculateMaxPortraitScale() {
        if (activity == null || activity.getPreviewWidth() <= 0 || activity.getPreviewHeight() <= 0) {
            return DEFAULT_MAX_SCALE_PORTRAIT;
        }

        float viewWidth = CoordinateConverter.getTouchViewWidth();
        if (viewWidth <= 0) {
            return DEFAULT_MAX_SCALE_PORTRAIT;
        }

        float bufferWidth = activity.getPreviewWidth();
        float bufferHeight = activity.getPreviewHeight();
        // Height of the video as displayed at scale 1.0 (aspect preserved, full view width)
        float naturalHeight = viewWidth * bufferHeight / bufferWidth;

        // Available height: the preview's container, or the screen as a fallback
        float containerHeight = 0f;
        if (activity.mBinding != null && activity.mBinding.viewMainPreview != null
                && activity.mBinding.viewMainPreview.getParent() instanceof View) {
            containerHeight = ((View) activity.mBinding.viewMainPreview.getParent()).getHeight();
        }
        if (containerHeight <= 0) {
            DisplayMetrics metrics = new DisplayMetrics();
            activity.getWindowManager().getDefaultDisplay().getRealMetrics(metrics);
            containerHeight = metrics.heightPixels * CONTAINER_HEIGHT_RATIO;
        }

        float maxScale = containerHeight / Math.max(1f, naturalHeight);
        return Math.max(MIN_SCALE, Math.min(maxScale, MAX_SCALE_PORTRAIT));
    }

    // ===== Transform application =====

    /**
     * Apply the current portrait zoom transform to the TextureView.
     *
     * The camera buffer is landscape (e.g. 1920x1080). In portrait mode, the TextureView
     * fills the screen but the video should display with correct aspect ratio.
     *
     * Approach: Set the TextureView's aspect ratio to match the camera buffer,
     * so the view measures itself correctly (landscape aspect in portrait screen = centered with black bars).
     * Then apply zoom/pan Matrix only when zoomed in (scale > 1.0).
     */
    public static void applyPortraitZoomTransform() {
        if (activity == null || activity.mBinding == null
                || activity.mBinding.viewMainPreview == null) {
            return;
        }

        AspectRatioTextureView textureView = activity.mBinding.viewMainPreview;
        float viewWidth = textureView.getWidth();
        float viewHeight = textureView.getHeight();
        if (viewWidth <= 0 || viewHeight <= 0) return;

        float bufferWidth = activity.getPreviewWidth();
        float bufferHeight = activity.getPreviewHeight();
        if (bufferWidth <= 0 || bufferHeight <= 0) return;

        // Set the TextureView's aspect ratio to match the camera buffer.
        // This makes the view measure itself with the correct proportions:
        // - In portrait screen, a landscape aspect ratio view will be centered with black bars top/bottom
        // - The camera frames fill the view uniformly (no stretching)
        textureView.setAspectRatio((int) bufferWidth, (int) bufferHeight);

        // Apply zoom and pan Matrix only when zoomed in
        if (mPortraitZoomScale > 1.0f) {
            Matrix matrix = new Matrix();
            float pivotX = viewWidth / 2f;
            float pivotY = viewHeight / 2f;
            matrix.postScale(mPortraitZoomScale, mPortraitZoomScale, pivotX, pivotY);
            matrix.postTranslate(mPortraitTranslateX, mPortraitTranslateY);
            textureView.setTransform(matrix);
        } else {
            // No zoom — clear any previous transform
            textureView.setTransform(null);
        }
        textureView.invalidate();

        // Sync PiP indicator
        ZoomLayoutDeal.updateIndicatorFromMainView(
                mPortraitZoomScale, mPortraitTranslateX, mPortraitTranslateY);

        Log.d(TAG, "Portrait transform: buffer=" + bufferWidth + "x" + bufferHeight +
              " view=" + viewWidth + "x" + viewHeight + " zoom=" + mPortraitZoomScale);
    }

    /**
     * Apply current portrait transform (called by ZoomLayoutDeal after updating pan).
     */
    public static void applyCurrentPortraitTransform() {
        applyPortraitZoomTransform();
        if (callbacks != null) {
            callbacks.onBlackBorderOverlayUpdate();
        }
    }

    // ===== TextureView reset =====

    private static void resetTextureViewHeight() {
        if (activity == null || activity.mBinding == null || activity.mBinding.viewMainPreview == null) {
            return;
        }

        AspectRatioTextureView textureView = activity.mBinding.viewMainPreview;

        // Portrait keeps the aspect-ratio driven height (WRAP_CONTENT) declared in the
        // portrait layout, so the preview never gets resized during a pinch gesture.
        // Only make sure a previous landscape resize left MATCH_PARENT behind.
        ViewGroup.LayoutParams params = textureView.getLayoutParams();
        if (isPortraitMode && params != null
                && params.height != ViewGroup.LayoutParams.WRAP_CONTENT) {
            params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            textureView.setLayoutParams(params);
        }

        // Apply the current transform (identity at scale 1.0)
        applyPortraitZoomTransform();
    }

    // ===== Pan adjustment (for pinch-to-zoom auto-pan) =====

    /**
     * Adjust pan to keep a point centered after zoom.
     */
    public static void adjustPanForZoom(float oldScale, float newScale, float focusX, float focusY) {
        // Calculate the point that was at focus before zoom
        float pivotX = CoordinateConverter.getTouchViewWidth() / 2f;
        float pivotY = CoordinateConverter.getTouchViewHeight() / 2f;

        // Adjust translation to keep focus point stationary
        float scaleRatio = newScale / oldScale;
        mPortraitTranslateX = focusX - scaleRatio * (focusX - mPortraitTranslateX - pivotX) - pivotX;
        mPortraitTranslateY = focusY - scaleRatio * (focusY - mPortraitTranslateY - pivotY) - pivotY;

        // Clamp pan to valid range
        clampPortraitPan();
    }

    /**
     * Clamp portrait pan to valid range based on current zoom.
     */
    public static void clampPortraitPan() {
        if (mPortraitZoomScale <= 1.0f) {
            mPortraitTranslateX = 0f;
            mPortraitTranslateY = 0f;
            return;
        }

        float viewW = CoordinateConverter.getTouchViewWidth();
        float viewH = CoordinateConverter.getTouchViewHeight();

        // Maximum translation: (zoom - 1) * viewSize / 2
        float maxTranslateX = (mPortraitZoomScale - 1f) * viewW / 2f;
        float maxTranslateY = (mPortraitZoomScale - 1f) * viewH / 2f;

        mPortraitTranslateX = Math.max(-maxTranslateX, Math.min(maxTranslateX, mPortraitTranslateX));
        mPortraitTranslateY = Math.max(-maxTranslateY, Math.min(maxTranslateY, mPortraitTranslateY));
    }
}
