package com.buglife.sdk.screenrecorder;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.FrameLayout;

import com.buglife.sdk.R;

import static android.graphics.PixelFormat.TRANSLUCENT;
import static android.view.WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR;
import static android.view.WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
import static android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;
import static android.view.WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
import static android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;

@TargetApi(Build.VERSION_CODES.KITKAT_WATCH)
final class OverlayView extends FrameLayout {

    private static final int OVERLAY_WIDTH = 500;
    private static final int ANIMATION_DURATION = 300;

    private final @NonNull OverlayViewClickListener mListener;

    public OverlayView(@NonNull Context context, @NonNull OverlayViewClickListener listener) {
        super(context);
        mListener = listener;
        inflate(context, R.layout.overlay_view, this);

        Button stopButton = (Button) findViewById(R.id.stop_button);

        stopButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.onStopButtonClicked();
            }
        });
    }

    @Override
    public WindowInsets onApplyWindowInsets(WindowInsets insets) {
        ViewGroup.LayoutParams layoutParams = getLayoutParams();
        layoutParams.height = insets.getSystemWindowInsetTop();

        mListener.onResize();

        return insets.consumeSystemWindowInsets();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        setTranslationX(OVERLAY_WIDTH);
        animate().translationX(0).setDuration(ANIMATION_DURATION).setInterpolator(new DecelerateInterpolator());
    }

    interface OverlayViewClickListener {
        void onResize();
        void onStopButtonClicked();
    }

    static WindowManager.LayoutParams getLayoutParams(Context context) {
        int flags = FLAG_NOT_FOCUSABLE | FLAG_NOT_TOUCH_MODAL | FLAG_LAYOUT_NO_LIMITS | FLAG_LAYOUT_INSET_DECOR | FLAG_LAYOUT_IN_SCREEN;
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams(OVERLAY_WIDTH, ViewGroup.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.TYPE_SYSTEM_ERROR, flags, TRANSLUCENT);
        layoutParams.gravity = Gravity.TOP;
        return layoutParams;
    }
}
