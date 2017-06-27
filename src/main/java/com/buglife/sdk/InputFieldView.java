package com.buglife.sdk;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.widget.LinearLayout;

abstract class InputFieldView extends LinearLayout {

    InputFieldView(Context context) {
        super(context);
    }

    abstract void configureWithInputField(@NonNull InputField inputField, @NonNull ValueCoordinator valueCoordinator);

    abstract void setValue(@Nullable String value);

    interface ValueCoordinator {
        void onValueChanged(@NonNull InputField inputField, @Nullable String newValue);
    }
}
