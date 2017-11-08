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

import com.buglife.sdk.reporting.DeviceSnapshot;
import com.buglife.sdk.reporting.SessionSnapshot;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.List;

final class BugContext implements Parcelable {
    private final ArrayList<Attachment> mAttachments;
    private final AttributeMap mAttributes;
    private final EnvironmentSnapshot mEnvironmentSnapshot;
    private final DeviceSnapshot mDeviceSnapshot;
    private final SessionSnapshot mSessionSnapshot;

    private BugContext(@NonNull List<Attachment> attachments, @NonNull AttributeMap attributes, SessionSnapshot sessionSnapshot, DeviceSnapshot deviceSnapshot, @NonNull EnvironmentSnapshot environment) {
        mAttachments = new ArrayList<>(attachments);
        mAttributes = attributes;
        mSessionSnapshot = sessionSnapshot;
        mDeviceSnapshot = deviceSnapshot;
        mEnvironmentSnapshot = environment;
    }

    void addAttachment(@NonNull Attachment attachment) {
        mAttachments.add(attachment);
    }

    List<Attachment> getAttachments() {
        return mAttachments;
    }

    List<Attachment> getMediaAttachments() {
        ArrayList<Attachment> mediaAttachments = new ArrayList<>();

        for (Attachment attachment : getAttachments()) {
            if (attachment.isImageAttachment() || attachment.isVideoAttachment()) {
                mediaAttachments.add(attachment);
            }
        }

        return mediaAttachments;
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

    DeviceSnapshot getDeviceSnapshot() {
        return mDeviceSnapshot;
    }

    SessionSnapshot getSessionSnapshot() {
        return mSessionSnapshot;
    }

    /* Parcelable*/

    BugContext(Parcel in) {
        mAttachments = in.createTypedArrayList(Attachment.CREATOR);
        mAttributes = in.readParcelable(AttributeMap.class.getClassLoader());
        mEnvironmentSnapshot = in.readParcelable(EnvironmentSnapshot.class.getClassLoader());
        mDeviceSnapshot = in.readParcelable(DeviceSnapshot.class.getClassLoader());
        mSessionSnapshot = in.readParcelable(SessionSnapshot.class.getClassLoader());
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeTypedList(mAttachments);
        dest.writeParcelable(mAttributes, flags);
        dest.writeParcelable(mEnvironmentSnapshot, flags);
        dest.writeParcelable(mDeviceSnapshot, flags);
        dest.writeParcelable(mSessionSnapshot, flags);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<BugContext> CREATOR = new Creator<BugContext>() {
        @Override
        public BugContext createFromParcel(Parcel in) {
            return new BugContext(in);
        }

        @Override
        public BugContext[] newArray(int size) {
            return new BugContext[size];
        }
    };

    static class Builder {
        private final @NonNull Context mContext;
        private @NonNull ArrayList<Attachment> mAttachments = new ArrayList();
        private @NonNull AttributeMap mAttributeMap = new AttributeMap();
        private @NonNull String mUserEmail;
        private @NonNull String mUserIdentifier;
        private @Nullable String mApiKey;
        private @Nullable String mApiEmail;

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

        Builder setUserEmail(String userEmail) {
            userEmail = userEmail;
            return this;
        }

        Builder setUserIdentifier(String userIdentifier) {
            userIdentifier = userIdentifier;
            return this;
        }

        Builder setApiKey(String apiKey) {
            mApiKey = apiKey;
            return this;
        }

        Builder setApiEmail(String apiEmail) {
            mApiEmail = apiEmail;
            return this;
        }

        public BugContext build() {
            SessionSnapshot sessionSnapshot = new SessionSnapshot(mContext, mUserEmail, mUserIdentifier, mApiKey, mApiEmail);
            EnvironmentSnapshot environment = new EnvironmentSnapshot.Builder(mContext).build();
            DeviceSnapshot deviceSnapshot = new DeviceSnapshot();
            return new BugContext(mAttachments, mAttributeMap, sessionSnapshot, deviceSnapshot, environment);
        }
    }
}
