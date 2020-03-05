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

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * Represents an input field in the Buglife bug reporter interface.
 */
public abstract class InputField {
    private final @NonNull String mAttributeName;
    private @Nullable String mTitle;
    private final boolean mSystemField;

    /**
     * Default constructor
     * @param attributeName The attribute name. If the given attributeName is equal to
     *                      a custom attribute value set via `Buglife.putAttribute()`, then
     *                      that value will be the default value for this field in the bug
     *                      reporter UI.
     */
    protected InputField(@NonNull String attributeName) {
        this(attributeName, false);
    }

    /**
     * Default constructor.
     * @param attributeName The attribute name
     * @param systemField `true` if this input field is used for internal purposes
     */
    InputField(@NonNull String attributeName, boolean systemField) {
        mAttributeName = attributeName;
        mSystemField = systemField;
    }

    /**
     * Returns the attribute name for this input field.
     * This attribute name is the `key` for custom attribute key/value pairs, and is the key
     * used in submitted Buglife bug reports.
     */
    @NonNull String getAttributeName() {
        return mAttributeName;
    }

    /**
     * The user-facing label for the input field.
     * The default value returned is the attribute name.
     */
    public @NonNull String getTitle() {
        if (mTitle != null && mTitle.length() > 0) {
            return mTitle;
        } else {
            return mAttributeName;
        }
    }

    /**
     * Sets the user-facing label for the input field.
     * @param title The user-facing label
     */
    public void setTitle(@NonNull String title) {
        mTitle = title;
    }

    boolean isSystemField() {
        return mSystemField;
    }
}
