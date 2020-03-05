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

import java.util.HashMap;
import java.util.Map;

final class Attribute implements Parcelable {
    private String mValue;
    public final static int FLAG_CUSTOM     = 1 << 0;
    public final static int FLAG_SYSTEM     = 1 << 1;
    public final static int FLAG_PUBLIC     = 1 << 2;
    public final static int FLAG_INTERNAL   = 1 << 3;
    enum ValueType {
        STRING(0),
        INT(1),
        FLOAT(2),
        BOOL(3);
        private int mValue;
        private ValueType(int value) {
            mValue = value;
        }
        private static Map map = new HashMap<>();
        static {
            for (ValueType valueType : ValueType.values()) {
                map.put(valueType.mValue, valueType);
            }
        }
        public static ValueType valueOf(int typeValue) {
            return (ValueType) map.get(typeValue);
        }

        public int getValue() {
            return mValue;
        }

    }
    private ValueType mValueType;
    private int mFlags;

    Attribute(String value, ValueType valueType, int flags) {
        mValue = new String(value);
        mValueType = valueType;
        mFlags = flags;
    }

    // TODO: consider whether valueType should be removed on both platforms
    // or other constructors added here.

    Attribute(Attribute other) {
        mValue = new String(other.mValue);
        mValueType = other.mValueType;
        mFlags = other.mFlags;
    }
    Attribute(Parcel source) {
        mValue = source.readString();
        mValueType = ValueType.valueOf(source.readInt());
        mFlags = source.readInt();
    }
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mValue);
        dest.writeInt(mValueType.getValue());
        dest.writeInt(mFlags);
    }
    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Attribute> CREATOR = new Creator<Attribute>() {
        @Override
        public Attribute createFromParcel(Parcel in) {
            return new Attribute(in);
        }

        @Override
        public Attribute[] newArray(int size) {
            return new Attribute[size];
        }
    };
    public String getValue() {
        return mValue;
    }
    public void setValue(String value) {
        mValue = new String(value);
    }
    public ValueType getValueType() {
        return mValueType;
    }
    public int getFlags() {
        return mFlags;
    }



}
