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

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a method of invoking the bug reporter UI.
 */
public enum InvocationMethod {
    NONE(0),
    SHAKE(1),
    SCREENSHOT(2),
    BUG_BUTTON(4),
    SCREEN_RECORDING(8);
    private int mValue;
    private InvocationMethod(int value) {
        mValue = value;
    }
    private static Map map = new HashMap<>();
    static {
        for (InvocationMethod invocationMethod : InvocationMethod.values()) {
            map.put(invocationMethod.mValue, invocationMethod);
        }
    }
    public static InvocationMethod valueOf(int methodValue) {
        return (InvocationMethod) map.get(methodValue);
    }

    public int getValue() {
        return mValue;
    }


}
