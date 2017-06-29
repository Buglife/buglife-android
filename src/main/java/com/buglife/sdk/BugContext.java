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
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import junit.framework.Assert;

import org.w3c.dom.Attr;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class BugContext implements Parcelable {
    private final ArrayList<Attachment> mAttachments;
    private final AttributeMap mAttributes;
    private final EnvironmentSnapshot mEnvironmentSnapshot;

    private BugContext(@NonNull List<Attachment> attachments, @NonNull AttributeMap attributes, @NonNull EnvironmentSnapshot environment) {
        mAttachments = new ArrayList<Attachment>(attachments);
        mAttributes = attributes;
        mEnvironmentSnapshot = environment;
    }

    public BugContext(Parcel source) {
        mAttachments = new ArrayList();
        source.readTypedList(mAttachments, Attachment.CREATOR);
        mAttributes = source.readParcelable(AttributeMap.class.getClassLoader());
        mEnvironmentSnapshot = source.readParcelable(EnvironmentSnapshot.class.getClassLoader());
    }

    void addAttachment(@NonNull Attachment attachment) {
        mAttachments.add(attachment);
    }

    List<Attachment> getAttachments() {
        return mAttachments;
    }

    EnvironmentSnapshot getEnvironmentSnapshot() {
        return mEnvironmentSnapshot;
    }

    void updateAttachment(Attachment updatedAttachment) {
        for (Attachment attachment : mAttachments) {
            if (attachment.isAnnotatedCopy(updatedAttachment)) {
                int index = mAttachments.indexOf(attachment);
                mAttachments.set(index, updatedAttachment);
                return;
            }
        }

        Assert.fail("Unable to find existing copy of attachment");
    }

    void putAttribute(@NonNull String key, @Nullable String value) {
        mAttributes.put(key, value);
    }

    @Nullable String getAttribute(@NonNull String key) {
        return mAttributes.get(key);
    }

    @NonNull AttributeMap getAttributes() {
        return mAttributes;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeTypedList(mAttachments);
        dest.writeParcelable(mAttributes, flags);
        dest.writeParcelable(mEnvironmentSnapshot, flags);
    }

    public static final Parcelable.Creator CREATOR =
            new Parcelable.Creator() {

                @Override
                public BugContext createFromParcel(Parcel source) {
                    return new BugContext(source);
                }

                @Override
                public BugContext[] newArray(int size) {
                    return new BugContext[size];
                }
            };

    static class Builder {
        private @NonNull ArrayList<Attachment> mAttachments = new ArrayList();
        private @NonNull AttributeMap mAttributeMap = new AttributeMap();
        private final @NonNull Context mContext;

        Builder(@NonNull Context context) {
            mContext = context;
        }

        Builder setAttachments(@NonNull List<Attachment> attachments) {
            mAttachments = new ArrayList(attachments);
            return this;
        }

        Builder setAttributes(@NonNull AttributeMap attributes) {
            mAttributeMap = new AttributeMap(attributes);
            return this;
        }

        public BugContext build() {
            EnvironmentSnapshot environment = new EnvironmentSnapshot.Builder(mContext).build();
            return new BugContext(mAttachments, mAttributeMap, environment);
        }
    }
}
