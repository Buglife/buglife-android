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
    private static final float MINIMUM_ANNOTATION_SIZE_PERCENT = 0.05f;

    // The screenshot attachment being annotated.
    private @NonNull Attachment mAttachment;
    // The BugContext, which is required if & only if this is the initial activity in the
    // reporter flow.
    private @Nullable BugContext mBugContext;
    private ImageView mImageView;
    private View mGestureView;
    private BlurAnnotationView mBlurAnnotationView;
    private LoupeAnnotationView mLoupeAnnotationView;
    private ArrowAnnotationView mArrowAnnotationView;
    private Annotation mMutatingAnnotation;
    private PointF mMovingTouchPoint = null;
    private PointF mMovingStartPoint = null;
    private PointF mMovingEndPoint = null;
    private PointF mMultiTouch0 = null;
    private PointF mMultiTouch1 = null;
    // Used for two-finger gestures, i.e. rotating annotations
    private boolean mTouchesFlipped = false;
    private Annotation.Type mSelectedTool;
    private View mAnnotationToolbar;
    private ImageButton mArrowTool;
    private ImageButton mLoupeTool;
    private ImageButton mBlurTool;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_screenshot_annotator);

        mImageView = (ImageView) findViewById(R.id.image_view);
        mBlurAnnotationView = (BlurAnnotationView) findViewById(R.id.blur_annotation_view);
        mLoupeAnnotationView = (LoupeAnnotationView) findViewById(R.id.loupe_annotation_view);
        mArrowAnnotationView = (ArrowAnnotationView) findViewById(R.id.arrow_annotation_view);
        mGestureView = findViewById(R.id.gesture_view);

        Intent intent = getIntent();
        mAttachment = intent.getParcelableExtra(INTENT_KEY_ATTACHMENT);
        mBugContext = intent.getParcelableExtra(INTENT_KEY_BUG_CONTEXT);

        Bitmap bitmap = mAttachment.getBitmap();
        final float bitmapWidth = bitmap.getWidth();
        final float bitmapHeight = bitmap.getHeight();
        mImageView.setImageBitmap(bitmap);
        mBlurAnnotationView.setSourceBitmap(bitmap);
        mLoupeAnnotationView.setSourceBitmap(bitmap);

        // Annotation tools
        mAnnotationToolbar = (View) findViewById(R.id.annotation_toolbar);
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

        mGestureView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                switch (event.getPointerCount()) {
                    case 2:
                        return onMultitouchEvent(view, event);
                    default:
                        return onSingleTouchEvent(view, event);
                }
            }
        });

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

        float aspectRatio = bitmapWidth / bitmapHeight;
        AspectFitFrameLayout canvasView = (AspectFitFrameLayout) findViewById(R.id.canvas_view);
        canvasView.setAspectRatio(aspectRatio);
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

    private boolean onMultitouchEvent(View view, MotionEvent event) {
        final float canvasWidth = view.getWidth();
        final float canvasHeight = view.getHeight();
        final PointF touch0 = new PointF(event.getX(0), event.getY(0));
        final PointF touch1 = new PointF(event.getX(1), event.getY(1));

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                mMutatingAnnotation = getAnnotationAtPoint(touch0);

                if (mMutatingAnnotation == null) {
                    break;
                }

                mMovingStartPoint = AnnotationView.getPointFromPercentPoint(mMutatingAnnotation.getStartPercentPoint(), canvasWidth, canvasHeight);
                mMovingEndPoint = AnnotationView.getPointFromPercentPoint(mMutatingAnnotation.getEndPercentPoint(), canvasWidth, canvasHeight);

                mMultiTouch0 = touch0;
                mMultiTouch1 = touch1;

                if (getDistance(touch0, mMovingEndPoint) < getDistance(touch1, mMovingEndPoint)) {
                    mTouchesFlipped = true;
                } else {
                    mTouchesFlipped = false;
                }

                break;
            case MotionEvent.ACTION_MOVE:
                if (mMutatingAnnotation == null) {
                    break;
                }

                float deltaX0 = touch0.x - mMultiTouch0.x;
                float deltaY0 = touch0.y - mMultiTouch0.y;
                float deltaX1 = touch1.x - mMultiTouch1.x;
                float deltaY1 = touch1.y -  mMultiTouch1.y;
                final float newStartX;
                final float newStartY;
                final float newEndX;
                final float newEndY;

                if (mTouchesFlipped) {
                    newStartX = mMovingStartPoint.x + deltaX1;
                    newStartY = mMovingStartPoint.y + deltaY1;
                    newEndX = mMovingEndPoint.x + deltaX0;
                    newEndY = mMovingEndPoint.y + deltaY0;
                } else {
                    newStartX = mMovingStartPoint.x + deltaX0;
                    newStartY = mMovingStartPoint.y + deltaY0;
                    newEndX = mMovingEndPoint.x + deltaX1;
                    newEndY = mMovingEndPoint.y + deltaY1;
                }

                mMutatingAnnotation.setStartPercentPoint(newStartX / canvasWidth, newStartY / canvasHeight);
                mMutatingAnnotation.setEndPercentPoint(newEndX / canvasWidth, newEndY / canvasHeight);

                Annotation.Type annotationType = mMutatingAnnotation.getAnnotationType();
                AnnotationView annotationView = getAnnotationView(annotationType);
                annotationView.invalidate();
                annotationsWithTypeDidChange(annotationType);
                break;
            case MotionEvent.ACTION_POINTER_UP:
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mMutatingAnnotation = null;
                mMultiTouch0 = null;
                mMultiTouch1 = null;
                break;
        }

        return true;
    }

    private boolean onSingleTouchEvent(View view, MotionEvent event) {
        final int action = (event.getAction() & MotionEvent.ACTION_MASK);
        float canvasWidth = view.getWidth();
        float canvasHeight = view.getHeight();
        float pointX = event.getX();
        float pointY = event.getY();
        PointF point = new PointF(pointX, pointY);
        float percentX = pointX / canvasWidth;
        float percentY = pointY / canvasHeight;

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN: {
                setToolbarsHidden(true);
                Annotation.Type annotationType;
                Annotation existingAnnotation = getAnnotationAtPoint(point);

                if (existingAnnotation != null) {
                    // If we tapped on an existing annotation, start moving that
                    mMutatingAnnotation = existingAnnotation;
                    mMovingTouchPoint = point;
                    mMovingStartPoint = AnnotationView.getPointFromPercentPoint(existingAnnotation.getStartPercentPoint(), canvasWidth, canvasHeight);
                    mMovingEndPoint = AnnotationView.getPointFromPercentPoint(existingAnnotation.getEndPercentPoint(), canvasWidth, canvasHeight);
                } else {
                    // If we're drawing a new annotation, use whatever type is currently selected
                    annotationType = mSelectedTool;
                    mMutatingAnnotation = new Annotation(annotationType);
                    mMutatingAnnotation.setStartPercentPoint(percentX, percentY);
                }

                break;
            }
            case MotionEvent.ACTION_MOVE: {
                if (mMutatingAnnotation == null) {
                    break;
                }

                if (mMultiTouch0 != null && mMultiTouch1 != null) {
                    // If the user is using two fingers, ignore this gesture
                    break;
                }

                Annotation.Type annotationType = mMutatingAnnotation.getAnnotationType();
                AnnotationView annotationView = getAnnotationView(annotationType);

                // If the annotation is being moved, set both the start & end point
                if (mMovingStartPoint != null) {
                    float deltaX = pointX - mMovingTouchPoint.x;
                    float deltaY = pointY - mMovingTouchPoint.y;
                    float newStartX = mMovingStartPoint.x + deltaX;
                    float newStartY = mMovingStartPoint.y + deltaY;
                    float newEndX = mMovingEndPoint.x + deltaX;
                    float newEndY = mMovingEndPoint.y + deltaY;

                    mMutatingAnnotation.setStartPercentPoint(newStartX / canvasWidth, newStartY / canvasHeight);
                    mMutatingAnnotation.setEndPercentPoint(newEndX / canvasWidth, newEndY / canvasHeight);
                } else {
                    // If we're creating a new annotation, then just set the end point
                    mMutatingAnnotation.setEndPercentPoint(percentX, percentY);

                    // Only add the annotation once it passes the minimum size threshold
                    boolean annotationAlreadyAdded = annotationView.getAnnotations().contains(mMutatingAnnotation);

                    if (!annotationAlreadyAdded && (getDistance(mMutatingAnnotation.getEndPercentPoint(), mMutatingAnnotation.getStartPercentPoint()) > MINIMUM_ANNOTATION_SIZE_PERCENT)) {
                        annotationView.addAnnotation(mMutatingAnnotation);
                    }
                }

                annotationView.invalidate();
                annotationsWithTypeDidChange(annotationType);

                break;
            }
            case MotionEvent.ACTION_UP:
                setToolbarsHidden(false);
                mMutatingAnnotation = null;
                mMovingTouchPoint = null;
                mMovingStartPoint = null;
                mMovingEndPoint = null;
                break;
            default:
                return false;
        }

        return true;
    }

    private void annotationsWithTypeDidChange(Annotation.Type annotationType) {
        // When blur annotations are drawn or moved, update the loupes which are on top of them
        if (annotationType == Annotation.Type.BLUR) {
            Bitmap screenshotWithBlurs = createBitmapWithBlurAnnotations();
            mLoupeAnnotationView.setSourceBitmap(screenshotWithBlurs);
            mLoupeAnnotationView.invalidate();;
        }
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

    private @Nullable Annotation getAnnotationAtPoint(PointF point) {
        // Arrow annotations have the highest z-index, then loupe annotations, then blur annotations
        AnnotationView[] annotationViews = {mArrowAnnotationView, mLoupeAnnotationView, mBlurAnnotationView};

        for (AnnotationView annotationView : annotationViews) {
            for (Annotation annotation : annotationView.getAnnotations()) {
                RectF rect = annotation.getRectF(annotationView.getWidth(), mArrowAnnotationView.getHeight());

                if (annotation.getAnnotationType() == Annotation.Type.LOUPE) {
                    float radius = annotationView.getLength(annotation);
                    PointF center = annotationView.getPointFromPercentPoint(annotation.getStartPercentPoint());
                    float left = center.x - radius;
                    float top = center.y - radius;
                    float right = center.x + radius;
                    float bottom = center.y + radius;
                    rect = new RectF(left, top, right, bottom);
                }

                if (rect.contains(point.x, point.y)) {
                    return annotation;
                }
            }
        }

        return null;
    }

    private View.OnClickListener mToolClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (view == mArrowTool) {
                setSelectedTool(Annotation.Type.ARROW);
            } else if (view == mLoupeTool) {
                setSelectedTool(Annotation.Type.LOUPE);
            } else if (view == mBlurTool) {
                setSelectedTool(Annotation.Type.BLUR);
            }
        }
    };

    private void setSelectedTool(Annotation.Type annotationType) {
        mSelectedTool = annotationType;
        int tintColor = getToolColorFilter();

        mArrowTool.setSelected(annotationType == Annotation.Type.ARROW);
        mLoupeTool.setSelected(annotationType == Annotation.Type.LOUPE);
        mBlurTool.setSelected(annotationType == Annotation.Type.BLUR);

        mArrowTool.setColorFilter(tintColor);
        mLoupeTool.setColorFilter(tintColor);
        mBlurTool.setColorFilter(tintColor);
    }

    private AnnotationView getAnnotationView(Annotation.Type annotationType) {
        switch (annotationType) {
            case ARROW:
                return mArrowAnnotationView;
            case LOUPE:
                return mLoupeAnnotationView;
            case BLUR:
                return mBlurAnnotationView;
        }

        return null;
    }

    private @NonNull Attachment getAttachmentCopyWithAnnotations() {
        final Context context = this;
        final Bitmap destinationBitmap = createBitmapWithBlurAnnotations();
        final Canvas canvas = new Canvas(destinationBitmap);
        float strokeWidth = LoupeRenderer.getStrokeWidth(context);
        final LoupeRenderer loupeRenderer = new LoupeRenderer(destinationBitmap, strokeWidth);

        for (Annotation annotation : mLoupeAnnotationView.getAnnotations()) {
            loupeRenderer.drawAnnotation(annotation, canvas);
        }

        int fillColor = getResources().getColor(R.color.arrow_annotation_fill_color);
        int strokeColor = getResources().getColor(R.color.arrow_annotation_stroke_color);
        ArrowRenderer arrowRenderer = new ArrowRenderer(fillColor, strokeColor);

        for (Annotation annotation : mArrowAnnotationView.getAnnotations()) {
            arrowRenderer.drawAnnotation(annotation, canvas);
        }

        return mAttachment.getCopy(destinationBitmap);
    }

    private @NonNull Bitmap createBitmapWithBlurAnnotations() {
        final Bitmap sourceBitmap = mAttachment.getBitmap();
        return BitmapAnnotator.createBitmapWithBlurAnnotations(sourceBitmap, mBlurAnnotationView.getAnnotations());
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

    private static float getDistance(PointF p0, PointF p1) {
        return (float) Math.sqrt(Math.pow(p1.x - p0.x, 2) + Math.pow(p1.y - p0.y, 2));
    }
}
