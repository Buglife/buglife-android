package com.buglife.sdk;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.widget.LinearLayout;

abstract class InputFieldView extends LinearLayout {

    InputFieldView(Context context) {
        super(context);
    }

    public static InputFieldView newInstance(Context context, InputField inputField) {
        if (inputField instanceof TextInputField) {
            return new TextInputFieldView(context);
        } else if (inputField instanceof PickerInputField) {
            return new PickerInputFieldView(context);
        } else {
            throw new Buglife.BuglifeException("Unexpected input field type: " + inputField);
        }
    }

    abstract void configureWithInputField(@NonNull InputField inputField, @NonNull ValueCoordinator valueCoordinator);

    abstract void setValue(@Nullable String value);

    interface ValueCoordinator {
        void onValueChanged(@NonNull InputField inputField, @Nullable String newValue);
    }
}
