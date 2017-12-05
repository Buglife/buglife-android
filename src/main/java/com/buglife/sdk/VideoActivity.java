package com.buglife.sdk;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.KeyEvent;
import android.widget.MediaController;
import android.widget.VideoView;

import java.io.File;

import static com.buglife.sdk.ActivityUtils.INTENT_KEY_ATTACHMENT;

public final class VideoActivity extends Activity {

    private VideoView mVideoView;
    private MediaController mMediaController;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video);

        mVideoView = (VideoView) findViewById(R.id.video_view);

        Intent intent = getIntent();
        FileAttachment videoAttachment = intent.getParcelableExtra(INTENT_KEY_ATTACHMENT);
        File file = videoAttachment.getFile();
        mVideoView.setVideoURI(Uri.fromFile(file));

        mMediaController = new BuglifeMediaController(this);
        mMediaController.setAnchorView(mVideoView);
        mVideoView.setMediaController(mMediaController);

        mVideoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                mVideoView.start();
                mMediaController.show(0);
            }
        });
    }

    /**
     * The "normal" behavior of MediaController is to simply hide the anchor view
     * when the back button is tapped. This subclass fixes this behavior by
     * finishing the activity when back is tapped
     */
    class BuglifeMediaController extends MediaController {

        public BuglifeMediaController(Context context) {
            super(context);
        }

        @Override
        public boolean dispatchKeyEvent(KeyEvent event) {
            if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    // VideoView consumes the ACTION_DOWN event; we need to ignore this
                    // in order to actually get an ACTION_UP event
                    return false;
                } else if (event.getAction() == KeyEvent.ACTION_UP) {
                    VideoActivity.this.finish();
                }
            }

            return super.dispatchKeyEvent(event);
        }
    }
}
