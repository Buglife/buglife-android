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
import android.content.res.ColorStateList;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputLayout;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.widget.EditText;
import android.widget.LinearLayout;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

final class TextInputFieldView extends InputFieldView {
    private TextInputLayout mTextInputLayout;
    private EditText mEditText;
    private ColorPalette mColorPalette;

    TextInputFieldView(Context context) {
        super(context);
        init();
    }

    private void init() {
        inflate(getContext(), R.layout.text_input_field_view, this);
        mTextInputLayout = (TextInputLayout) findViewById(R.id.input_layout);
        mEditText = (EditText) findViewById(R.id.edit_text);
        mColorPalette = new ColorPalette.Builder(getContext()).build();
        setHintColor();
    }

    @Override
    public void configureWithInputField(@NonNull InputField inputField, @NonNull final ValueCoordinator valueCoordinator) {
        final TextInputField textInputField = (TextInputField)inputField;

        mTextInputLayout.setHint(textInputField.getTitle());

        int inputType = InputType.TYPE_CLASS_TEXT;

        if (textInputField.isMultine()) {
            inputType = inputType | InputType.TYPE_TEXT_FLAG_MULTI_LINE;
        }

        mEditText.setInputType(inputType);

        mEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                String newValue = s.toString();
                valueCoordinator.onValueChanged(textInputField, newValue);
            }
        });
    }

    @Override
    public void setValue(@Nullable String value) {
        mEditText.setText(value);
    }

    private void setHintColor() {
        @ColorInt int colorAccent = mColorPalette.getColorAccent();

        try {
            Field field = mTextInputLayout.getClass().getDeclaredField("mFocusedTextColor");
            field.setAccessible(true);
            int[][] states = new int[][] {
                    new int[]{}
            };
            int[] colors = new int[] {
                    colorAccent
            };
            ColorStateList colorStateList = new ColorStateList(states, colors);
            field.set(mTextInputLayout, colorStateList);

            Method method = mTextInputLayout.getClass().getDeclaredMethod("updateLabelState", boolean.class);
            method.setAccessible(true);
            method.invoke(mTextInputLayout, true);
        } catch (Exception e) {
            // Ignore this exception, since failing to set the hint color isn't a big deal
        }
    }
}
