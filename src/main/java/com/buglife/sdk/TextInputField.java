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

import androidx.annotation.NonNull;

/**
 * Represents a single- or multi-line text input field in the Buglife bug reporter interface.
 */
public final class TextInputField extends InputField {
    static final String SUMMARY_ATTRIBUTE_NAME = "com.buglife.summary";

    private boolean mMultiline = false;

    /**
     * Default constructor
     * @param attributeName The attribute name. If the given attributeName is equal to
     *                      a custom attribute value set via `Buglife.putAttribute()`, then
     *                      that value will be the default value for this field in the bug
     *                      reporter UI.
     */
    public TextInputField(@NonNull String attributeName) {
        super(attributeName);
    }

    TextInputField(@NonNull String attributeName, boolean systemField) {
        super(attributeName, systemField);
    }

    /**
     * Returns `true` if this is a multi-line text input field.
     * Default value is `false`.
     */
    public boolean isMultine() {
        return mMultiline;
    }

    /**
     * Sets whether this should be a multi-line text input field.
     */
    public void setMultiline(boolean multiline) {
        mMultiline = multiline;
    }

    /**
     * Returns the system-provided summary field (i.e. "What happened?").
     * When there are no custom input fields configured,
     * the bug reporter UI shows the summary field by default.
     */
    public static TextInputField summaryInputField() {
        TextInputField summaryInputField = new TextInputField(SUMMARY_ATTRIBUTE_NAME, true);
        summaryInputField.setMultiline(true);
        String title = Buglife.getContext().getString(R.string.summary_field_title);
        summaryInputField.setTitle(title);
        return summaryInputField;
    }
}
