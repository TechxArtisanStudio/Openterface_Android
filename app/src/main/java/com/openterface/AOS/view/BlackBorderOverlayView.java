package com.openterface.AOS.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

/**
 * Fully transparent overlay covering the whole video area.
 *
 * It used to draw a semi-transparent mask over the black border plus a
 * "please operate inside the picture" hint. Both are gone:
 * - the black border is already pure black, so a 50% black mask was invisible and
 *   carried no information;
 * - the mask never influenced mouse/touch handling, the black border is excluded
 *   from HID mapping by coordinate clamping, not by this view;
 * - the hint text was unwanted.
 *
 * What the view is still needed for: it is the only surface above the black bars,
 * so it carries the transparent 1x1 PointerIcon and keeps the system mouse cursor
 * from reappearing whenever it leaves the preview.
 */
public class BlackBorderOverlayView extends View {

    public BlackBorderOverlayView(Context context) {
        this(context, null);
    }

    public BlackBorderOverlayView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BlackBorderOverlayView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        // Initially hidden
        setVisibility(GONE);
        setAlpha(0f);
    }

    /**
     * Show the overlay with animation.
     */
    public void showOverlay() {
        setVisibility(VISIBLE);
        animate().alpha(1f).setDuration(200).start();
    }

    /**
     * Hide the overlay with animation.
     */
    public void hideOverlay() {
        animate().alpha(0f).setDuration(200).withEndAction(() -> setVisibility(GONE)).start();
    }
}
