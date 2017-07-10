package com.buglife.sdk;

import android.app.Activity;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.ColorInt;
import android.support.annotation.Nullable;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.VideoView;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

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
        Attachment videoAttachment = intent.getParcelableExtra(INTENT_KEY_ATTACHMENT);
        FileData fileData = (FileData) videoAttachment.getData();
        Uri uri = fileData.getUri();
        mVideoView.setVideoURI(uri);
        mVideoView.start();

        mMediaController = new MediaController(this);
        mMediaController.setAnchorView(mVideoView);
        mVideoView.setMediaController(mMediaController);
    }

    @Override
    public void onResume() {
        super.onResume();
        mMediaController.show();
    }
}
