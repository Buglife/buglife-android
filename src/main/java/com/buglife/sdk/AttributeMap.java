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

import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Represents a collection of custom attributes set by a user.
 */
final class AttributeMap implements Parcelable {
    private final HashMap<String, Attribute> mAttributes;

    AttributeMap() {
        mAttributes = new HashMap();
    }

    AttributeMap(AttributeMap attributeMap) {
        mAttributes = new HashMap(attributeMap.mAttributes);
    }

    AttributeMap(Parcel source) {
        mAttributes = new HashMap();

        final int size = source.readInt();

        for (int i = 0; i < size; i++) {
            String key = source.readString();
            Attribute attr = source.readParcelable(Attribute.class.getClassLoader());
            mAttributes.put(key, attr);
        }
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        final int size = mAttributes.size();
        dest.writeInt(size);

        if (size > 0) {
            for (Map.Entry<String, Attribute> entry : mAttributes.entrySet()) {
                dest.writeString(entry.getKey());
                dest.writeParcelable(entry.getValue(), 0);
            }
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<AttributeMap> CREATOR = new Creator<AttributeMap>() {
        @Override
        public AttributeMap createFromParcel(Parcel in) {
            return new AttributeMap(in);
        }

        @Override
        public AttributeMap[] newArray(int size) {
            return new AttributeMap[size];
        }
    };

    void put(@NonNull String key, @Nullable Attribute value) {
        mAttributes.put(key, value);
    }

    @Nullable Attribute get(@NonNull String key) {
        return mAttributes.get(key);
    }

    void clear() {
        mAttributes.clear();
    }

    Set<Map.Entry<String, Attribute>> entrySet() {
        return mAttributes.entrySet();
    }
}
