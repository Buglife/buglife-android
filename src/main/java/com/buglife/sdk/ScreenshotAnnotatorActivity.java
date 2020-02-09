/*
 * Copyright (C) 2017 Buglife, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.buglife.sdk;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

import static android.view.MenuItem.SHOW_AS_ACTION_ALWAYS;
import static com.buglife.sdk.ActivityUtils.INTENT_KEY_ATTACHMENT;
import static com.buglife.sdk.ActivityUtils.INTENT_KEY_BUG_CONTEXT;

/**
 * Activity for annotating screenshots.
 *
 * This activity can be presented either as the initial activity of the bug reporter flow,
 * or as a "child" of ReportActivity. For the former, the ScreenshotAnnotatorActivity
 * should be given both an Attachment & a BugContext on initialization; both objects are
 * then passed on to the subsequent ReportActivity.
 *
 * When presented as a "child" of ReportActivity, it should be initialized via
 * startActivityForResult(), along with an Attachment object (no BugContext).
 */
public class ScreenshotAnnotatorActivity extends AppCompatActivity {

    private static final int NEXT_MENU_ITEM = 1;
    static final int REQUEST_CODE = 100;

    // The screenshot attachment being annotated.
    private @NonNull FileAttachment mAttachment;
    // The BugContext, which is required if & only if this is the initial activity in the
    // reporter flow.
    private @Nullable BugContext mBugContext;
    private View mAnnotationToolbar;
    private ImageButton mArrowTool;
    private ImageButton mLoupeTool;
    private ImageButton mBlurTool;
    private AnnotationView mAnnotationView;
    private ColorPalette mColorPalette;

    public static Intent newStartIntent(Context context, FileAttachment screenshotAttachment, BugContext bugContext) {
        Intent intent = newStartIntent(context, screenshotAttachment);
        intent.putExtra(INTENT_KEY_BUG_CONTEXT, bugContext);
        return intent;
    }

    public static Intent newStartIntent(Context context, FileAttachment screenshotAttachment) {
        Intent intent = new Intent(context, ScreenshotAnnotatorActivity.class);
        intent.putExtra(INTENT_KEY_ATTACHMENT, screenshotAttachment);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_screenshot_annotator);

        mAnnotationView = (AnnotationView) findViewById(R.id.annotation_view);

        Intent intent = getIntent();
        intent.setExtrasClassLoader(FileAttachment.class.getClassLoader());
        mAttachment = intent.getParcelableExtra(INTENT_KEY_ATTACHMENT);
        intent.setExtrasClassLoader(BugContext.class.getClassLoader());
        mBugContext = intent.getParcelableExtra(INTENT_KEY_BUG_CONTEXT);

        File file = mAttachment.getFile();
        Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
        mAnnotationView.setImage(bitmap);

        mColorPalette = new ColorPalette.Builder(this).build();

        // Annotation tools
        mAnnotationToolbar = findViewById(R.id.annotation_toolbar);
        mAnnotationToolbar.setBackgroundColor(mColorPalette.getColorPrimary());

        mArrowTool = (ImageButton) findViewById(R.id.arrow_tool);
        mLoupeTool = (ImageButton) findViewById(R.id.loupe_tool);
        mBlurTool = (ImageButton) findViewById(R.id.blur_tool);

        mArrowTool.setColorFilter(getToolColorFilter());
        mLoupeTool.setColorFilter(getToolColorFilter());
        mBlurTool.setColorFilter(getToolColorFilter());

        mArrowTool.setOnClickListener(mToolClickListener);
        mLoupeTool.setOnClickListener(mToolClickListener);
        mBlurTool.setOnClickListener(mToolClickListener);

        setSelectedTool(Annotation.Type.ARROW);
        mAnnotationView.setAnnotation(Annotation.newArrowInstance());

        mAnnotationView.setOnTouchListener(new View.OnTouchListener() {
            @Override public boolean onTouch(View v, MotionEvent event) {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        setToolbarsHidden(true);
                        break;
                    case MotionEvent.ACTION_UP:
                        setToolbarsHidden(false);
                        break;
                }
                return false; // Must return false in order for event to propagate down
            }
        });

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();

        if (actionBar != null) {
            int colorPrimary = mColorPalette.getColorPrimary();
            int titleTextColor = mColorPalette.getTextColorPrimary();
            final @DrawableRes int homeAsUpIndicatorDrawableId;
            final @StringRes int titleStringId;

            if (isInitialScreenshotAnnotationActivity()) {
                // If this is the initial screenshot activity, show a different title
                // along with close + next buttons
                homeAsUpIndicatorDrawableId = android.R.drawable.ic_menu_close_clear_cancel;
                titleStringId = R.string.report_a_bug;
            } else {
                // Otherwise, this is a child activity of ReportActivity
                homeAsUpIndicatorDrawableId = R.drawable.buglife_abc_ic_ab_back_mtrl_am_alpha;
                titleStringId = R.string.screenshot_annotator_activity_label;
            }

            Drawable homeAsUpIndicator = ActivityUtils.getTintedDrawable(this, homeAsUpIndicatorDrawableId, mColorPalette.getTextColorPrimary());
            CharSequence title = ActivityUtils.getTextWithColor(this, titleTextColor, titleStringId);
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeAsUpIndicator(homeAsUpIndicator);
            actionBar.setBackgroundDrawable(new ColorDrawable(colorPrimary));
            actionBar.setTitle(title);
        }

        ActivityUtils.setStatusBarColor(this, mColorPalette.getColorPrimaryDark());
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                //ClientEventReporter.getInstance(Buglife.getContext()).reportClientEvent("presented_reporter", mBugContext.getApiIdentity());
            }
        }, 2000);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (isInitialScreenshotAnnotationActivity()) {
            MenuItem sendItem = menu.add(0, NEXT_MENU_ITEM, Menu.NONE, R.string.next);
            sendItem.setShowAsAction(SHOW_AS_ACTION_ALWAYS);
            Drawable drawable = ActivityUtils.getTintedDrawable(this, R.drawable.ic_arrow_right, mColorPalette.getTextColorPrimary());
            sendItem.setIcon(drawable);
            return true;
        } else {
            return super.onCreateOptionsMenu(menu);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                willGoBackOrDismiss();
                finish();
                return true;
            case NEXT_MENU_ITEM:
                continueToReportActivity();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        willGoBackOrDismiss();
        super.onBackPressed();
    }

    private void willGoBackOrDismiss() {
        if (isInitialScreenshotAnnotationActivity()) {
            cancelReporterFlow();
        } else {
            setAttachmentResult();
        }
    }

    @SuppressWarnings("ConstantConditions")
    private void continueToReportActivity() {
        saveAnnotatedBitmap();
        mBugContext.addAttachment(mAttachment);

        Context context = this;
        Intent intent = new Intent(context, ReportActivity.class);
        intent.putExtra(INTENT_KEY_BUG_CONTEXT, mBugContext);
        context.startActivity(intent);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    private void setToolbarsHidden(boolean hidden) {
        ActionBar actionBar = getSupportActionBar();

        if (actionBar != null) {
            if (hidden) {
                actionBar.hide();
                mAnnotationToolbar.animate().alpha(0);
            } else {
                actionBar.show();
                mAnnotationToolbar.animate().alpha(1);
            }
        }
    }

    private View.OnClickListener mToolClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            Annotation annotation = null;
            if (view == mArrowTool) {
                annotation = Annotation.newArrowInstance();
            } else if (view == mLoupeTool) {
                annotation = Annotation.newLoupeInstance(ScreenshotAnnotatorActivity.this);
            } else if (view == mBlurTool) {
                annotation = Annotation.newBlurInstance();
            }

            if (annotation != null) {
                mAnnotationView.setAnnotation(annotation);
                setSelectedTool(annotation.getAnnotationType());
            }
        }
    };

    private void setSelectedTool(Annotation.Type annotationType) {
        int tintColor = getToolColorFilter();

        mArrowTool.setSelected(annotationType == Annotation.Type.ARROW);
        mLoupeTool.setSelected(annotationType == Annotation.Type.LOUPE);
        mBlurTool.setSelected(annotationType == Annotation.Type.BLUR);

        mArrowTool.setColorFilter(tintColor);
        mLoupeTool.setColorFilter(tintColor);
        mBlurTool.setColorFilter(tintColor);
    }

    private void saveAnnotatedBitmap() {
        try {
            Bitmap output = mAnnotationView.captureDecoratedImage();
            File file = mAttachment.getFile();
            output.compress(Bitmap.CompressFormat.PNG, 100, new FileOutputStream(file));
        } catch (FileNotFoundException e) {
            Log.e("Error saving screenshot!", e);
            Toast.makeText(getApplicationContext(), R.string.error_save_screenshot, Toast.LENGTH_LONG).show();
        }
    }

    private void setAttachmentResult() {
        saveAnnotatedBitmap();
        Intent intent = new Intent();
        intent.putExtra(INTENT_KEY_ATTACHMENT, mAttachment);
        setResult(Activity.RESULT_OK, intent);
    }

    private void cancelReporterFlow() {
        Buglife.onFinishReportFlow();
        finish();
    }

    private boolean isInitialScreenshotAnnotationActivity() {
        return mBugContext != null;
    }

    private int getToolColorFilter() {
        return mColorPalette.getColorAccent();
    }
}
