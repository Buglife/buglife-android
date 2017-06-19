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

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.List;

final class BugContext implements Parcelable {
    private final ArrayList<Attachment> mAttachments;
    private final EnvironmentSnapshot mEnvironmentSnapshot;

    private BugContext(List<Attachment> attachments, EnvironmentSnapshot environment) {
        mAttachments = new ArrayList<Attachment>(attachments);
        mEnvironmentSnapshot = environment;
    }

    public BugContext(Parcel source) {
        mAttachments = new ArrayList<Attachment>();
        source.readTypedList(mAttachments, Attachment.CREATOR);
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

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeTypedList(mAttachments);
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
        private ArrayList<Attachment> mAttachments;
        private final Context mContext;

        public Builder(Context context) {
            mContext = context;
            mAttachments = new ArrayList<>();
        }

        public Builder addAttachment(Attachment attachment) {
            mAttachments.add(attachment);
            return this;
        }

        public BugContext build() {
            EnvironmentSnapshot environment = new EnvironmentSnapshot.Builder(mContext).build();
            return new BugContext(mAttachments, environment);
        }
    }
}
