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

import android.content.Context;
import androidx.annotation.AttrRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.util.AttributeSet;
import android.widget.FrameLayout;

public final class AspectFitFrameLayout extends FrameLayout {

    private float mAspectRatio = 1;

    public AspectFitFrameLayout(@NonNull Context context) {
        super(context);
    }

    public AspectFitFrameLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public AspectFitFrameLayout(@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    float getAspectRatio() {
        return mAspectRatio;
    }

    void setAspectRatio(float aspectRatio) {
        mAspectRatio = aspectRatio;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        float measuredAspectRatio = ((float)widthSize) / ((float)heightSize);
        float newWidth = widthSize;
        float newHeight = heightSize;

        if (measuredAspectRatio > getAspectRatio()) {
            newWidth = (float)heightSize * getAspectRatio();
        } else {
            newHeight = (float)widthSize / getAspectRatio();
        }

        super.onMeasure(MeasureSpec.makeMeasureSpec((int)newWidth, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec((int)newHeight, MeasureSpec.EXACTLY));
    }
}
