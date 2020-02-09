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
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Represents a `picker` style input field in the Buglife bug reporter interface,
 * that allows the user to select a single value from an array of options.
 */
public final class PickerInputField extends InputField {
    private final LinkedHashMap<String, String> mOptions = new LinkedHashMap();

    /**
     * Default constructor
     * @param attributeName The attribute name. If the given attributeName is equal to
     *                      a custom attribute value set via `Buglife.putAttribute()`, then
     *                      that value will be the default value for this field in the bug
     *                      reporter UI.
     */
    public PickerInputField(@NonNull String attributeName) {
        super(attributeName);
    }

    /**
     * Sets the options for the picker.
     */
    public void setOptions(@NonNull String... options) {
        List<String> optionsList = Arrays.asList(options);

        mOptions.clear();

        for (String option : optionsList) {
            addOption(option);
        }
    }

    /**
     * Adds an option to the picker.
     * @param option The option
     */
    public void addOption(@NonNull String option) {
        addOption(option, option);
    }

    /**
     * Adds an option to the picker.
     * @param optionTitle The user-facing option title
     * @param optionValue The option value, which is submitted along with bug reports
     */
    private void addOption(@NonNull String optionTitle, @NonNull String optionValue) {
        mOptions.put(optionTitle, optionValue);
    }

    List<String> getOptionTitles() {
        return new ArrayList(mOptions.keySet());
    }

    List<String> getOptionValues() {
        return new ArrayList(mOptions.values());
    }

    @Nullable String getOptionValue(@NonNull String optionTitle) {
        return mOptions.get(optionTitle);
    }

    /**
     * Returns -1 if the optionValue is not found
     */
    int indexOfOptionValue(@NonNull String optionValue) {
        return getOptionValues().indexOf(optionValue);
    }

    @NonNull String getOptionValue(int index) {
        return getOptionValues().get(index);
    }
}
