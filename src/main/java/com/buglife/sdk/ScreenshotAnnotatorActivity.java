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
import android.graphics.Canvas;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.Spanned;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;

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
    private @NonNull Attachment mAttachment;
    // The BugContext, which is required if & only if this is the initial activity in the
    // reporter flow.
    private @Nullable BugContext mBugContext;
    private View mAnnotationToolbar;
    private ImageButton mArrowTool;
    private ImageButton mLoupeTool;
    private ImageButton mBlurTool;
    private AnnotationView2 mAnnotationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_screenshot_annotator);

        mAnnotationView = (AnnotationView2) findViewById(R.id.annotation_view);

        Intent intent = getIntent();
        mAttachment = intent.getParcelableExtra(INTENT_KEY_ATTACHMENT);
        mBugContext = intent.getParcelableExtra(INTENT_KEY_BUG_CONTEXT);

        Bitmap bitmap = mAttachment.getBitmap();
        mAnnotationView.setImage(bitmap);

        // Annotation tools
        mAnnotationToolbar = findViewById(R.id.annotation_toolbar);
        mAnnotationToolbar.setBackgroundColor(Buglife.getColorPalette().getColorPrimary());

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
        mAnnotationView.setCurrentAnnotation(Annotation.newArrowInstance());

//        mGestureView.setOnTouchListener(new View.OnTouchListener() {
//            @Override
//            public boolean onTouch(View view, MotionEvent event) {
//                switch (event.getPointerCount()) {
//                    case 2:
//                        return onMultitouchEvent(view, event);
//                    default:
//                        return onSingleTouchEvent(view, event);
//                }
//            }
//        });

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();

        if (actionBar != null) {
            int colorPrimary = Buglife.getColorPalette().getColorPrimary();
            int titleTextColor = Buglife.getColorPalette().getTextColorPrimary();
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

            Drawable homeAsUpIndicator = ActivityUtils.getTintedDrawable(this, homeAsUpIndicatorDrawableId);
            CharSequence title = ActivityUtils.getTextWithColor(this, titleTextColor, titleStringId);
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeAsUpIndicator(homeAsUpIndicator);
            actionBar.setBackgroundDrawable(new ColorDrawable(colorPrimary));
            actionBar.setTitle(title);
        }

        ActivityUtils.setStatusBarColor(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (isInitialScreenshotAnnotationActivity()) {
            MenuItem sendItem = menu.add(0, NEXT_MENU_ITEM, Menu.NONE, R.string.next);
            sendItem.setShowAsAction(SHOW_AS_ACTION_ALWAYS);
            Drawable drawable = ActivityUtils.getTintedDrawable(this, R.drawable.ic_arrow_right);
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

    private void continueToReportActivity() {
        Attachment attachment = getAttachmentCopyWithAnnotations();
        mBugContext.addAttachment(attachment);

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
                mAnnotationView.setCurrentAnnotation(annotation);
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

    private @NonNull Attachment getAttachmentCopyWithAnnotations() {
        Bitmap output = mAnnotationView.captureDecoratedImage();
        return mAttachment.getCopy(output);
    }

    private void setAttachmentResult() {
        Attachment attachment = getAttachmentCopyWithAnnotations();
        Intent intent = new Intent();
        intent.putExtra(INTENT_KEY_ATTACHMENT, attachment);
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
        return Buglife.getColorPalette().getColorAccent();
    }
}
