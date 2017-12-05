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

import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

/**
 * Represents a file/resource that can be attached to a bug report.
 * This class is now deprecated. Prefer {@link FileAttachment} over {@link Attachment}.
 */
@Deprecated
public class Attachment implements Parcelable {
    public static final String TYPE_TEXT = MimeTypes.PLAIN_TEXT;
    public static final String TYPE_JSON = MimeTypes.JSON;
    public static final String TYPE_SQLITE = MimeTypes.SQLITE;
    public static final String TYPE_PNG = MimeTypes.PNG;
    public static final String TYPE_JPEG = MimeTypes.JPG;
    public static final String TYPE_MP4 = MimeTypes.MP4;

    private FileAttachment mFileAttachment;

    private Attachment(@NonNull File file, @NonNull String type) {
        mFileAttachment = new FileAttachment(file, type);
    }

    JSONObject getJSONObject() throws JSONException {
        return mFileAttachment.toJSON();
    }

    boolean isImageAttachment() {
        return mFileAttachment.isImage();
    }

    boolean isVideoAttachment() {
        return mFileAttachment.isVideo();
    }

    FileAttachment getFileAttachment() {
        return mFileAttachment;
    }

    /* Parcelable */

    @Override public int describeContents() {
        return 0;
    }

    @Override public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(this.mFileAttachment, flags);
    }

    protected Attachment(Parcel in) {
        this.mFileAttachment = in.readParcelable(FileAttachment.class.getClassLoader());
    }

    public static final Creator<Attachment> CREATOR = new Creator<Attachment>() {
        @Override public Attachment createFromParcel(Parcel source) {
            return new Attachment(source);
        }

        @Override public Attachment[] newArray(int size) {
            return new Attachment[size];
        }
    };

    /**
     * Builder for Attachment objects.
     */
    @Deprecated
    public static class Builder {
        @NonNull final private String mFilename;
        @NonNull final private String mType;

        /**
         * Default constructor.
         *
         * @param filename The attachment's filename. This should include the attachment extension
         * @param type The attachment type
         */
        public Builder(@NonNull String filename, @NonNull String type) {
            mFilename = filename;
            mType = type;
        }

        /**
         * Builds an attachment using a File reference.
         * @param file The file
         * @return The attachment
         */
        public @NonNull Attachment build(@NonNull File file) {
            return new Attachment(file, mType);
        }

        /**
         * Builds an attachment using a File reference.
         * @param file The file
         * @param temporaryFile The file will be deleted when no references to it remain. Calls build(file) if false.
         * @return The attachment
         */
        public @NonNull Attachment build(@NonNull File file, boolean temporaryFile) {
            return build(file);
        }

        /**
         * Builds an image attachment using a bitmap
         * @param bitmap
         * @return The attachment
         */
        public @NonNull Attachment build(@NonNull Bitmap bitmap) {
            File file = new File(Buglife.getContext().getCacheDir(), mFilename);
            try {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, new FileOutputStream(file));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            return build(file);
        }
    }
}
