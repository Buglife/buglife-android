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
import android.content.DialogInterface;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

final class PickerInputFieldView extends InputFieldView {
    @Nullable private String mCurrentValue;
    private TextView mTitleTextView;
    private TextView mSubtitleTextView;

    PickerInputFieldView(Context context) {
        super(context);
        init();
    }

    private void init() {
        inflate(getContext(), R.layout.picker_input_field_view, this);
        mTitleTextView = (TextView) findViewById(R.id.title_text_view);
        mSubtitleTextView = (TextView) findViewById(R.id.subtitle_text_view);
    }

    @Override
    public void configureWithInputField(@NonNull InputField inputField, @NonNull final ValueCoordinator valueCoordinator) {
        final PickerInputField pickerInputField = (PickerInputField)inputField;
        mTitleTextView.setText(pickerInputField.getTitle());

        this.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                showPickerDialog(pickerInputField, valueCoordinator);
            }
        });
    }

    @Override
    void setValue(@Nullable String value) {
        mCurrentValue = value;
        mSubtitleTextView.setText(value);
    }

    private void showPickerDialog(@NonNull final PickerInputField pickerInputField, @NonNull final ValueCoordinator valueCoordinator) {
        Context context = getContext();
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(pickerInputField.getTitle());

        final List<String> optionTitles = pickerInputField.getOptionTitles();
        final String[] optionTitlesArray = optionTitles.toArray(new String[optionTitles.size()]);
        final ArrayAdapter<String> adapter = new ArrayAdapter<String>(context, android.R.layout.simple_expandable_list_item_1, optionTitles);
        final String currentValue = mCurrentValue;
        int checkedItem = -1;

        if (currentValue != null) {
            // indexOf() will return -1 if the currentValue isn't found
            checkedItem = pickerInputField.indexOfOptionValue(currentValue);
        }

        builder.setSingleChoiceItems(optionTitlesArray, checkedItem, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which != -1) {
                    final String newValue = pickerInputField.getOptionValue(which);
                    PickerInputFieldView.this.setValue(newValue);
                    valueCoordinator.onValueChanged(pickerInputField, newValue);
                }
            }
        });

        builder.show();
    }
}
