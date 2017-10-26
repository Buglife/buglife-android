package com.buglife.sdk.screenrecorder;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.support.annotation.NonNull;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;

import com.buglife.sdk.R;

import static android.graphics.PixelFormat.TRANSLUCENT;
import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;

@TargetApi(Build.VERSION_CODES.KITKAT_WATCH)
final class OverlayView extends FrameLayout {
    private static final int ANIMATION_DURATION = 300;

    private final @NonNull OverlayViewClickListener mListener;

    public OverlayView(@NonNull Context context, @NonNull OverlayViewClickListener listener) {
        super(context);
        mListener = listener;
        inflate(context, R.layout.overlay_view, this);

        ImageButton stopButton = (ImageButton) findViewById(R.id.stop_button);

        stopButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.onStopButtonClicked();
            }
        });
    }

    public ImageButton getStopButton() {
        return (ImageButton) findViewById(R.id.stop_button);
    }

    interface OverlayViewClickListener {
        void onResize();
        void onStopButtonClicked();
    }
}
