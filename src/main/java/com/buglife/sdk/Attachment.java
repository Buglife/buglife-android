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
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * Represents a file/resource that can be attached to a bug report.
 */
public class Attachment implements Parcelable {
    public static final String TYPE_TEXT = "text/plain";
    public static final String TYPE_JSON = "application/json";
    public static final String TYPE_SQLITE = "application/x-sqlite3";
    public static final String TYPE_PNG = "image/png";
    public static final String TYPE_JPEG = "image/jpeg";
    public static final String TYPE_MP4 = "video/mp4";

    private static final Bitmap.CompressFormat DEFAULT_SCREENSHOT_FORMAT = Bitmap.CompressFormat.PNG;
    private static final int DEFAULT_SCREENSHOT_COMPRESSION_QUALITY = 100;

    private final int mIdentifier; // Used for replacing attachments
    @NonNull private final String mFilename;
    @NonNull private final String mType;

    /**
     * For internal use (copying & replacing attachments).
     */
    private Attachment(int identifier, @NonNull String filename, @NonNull String type) {
        mIdentifier = identifier;
        mFilename = filename;
        mType = type;
    }

    protected Attachment(Parcel source) {
        mIdentifier = source.readInt();
        mFilename = source.readString();
        mType = source.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mIdentifier);
        dest.writeString(mFilename);
        dest.writeString(mType);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Attachment> CREATOR = new Creator<Attachment>() {
        @Override
        public Attachment createFromParcel(Parcel in) {
            return new Attachment(in);
        }

        @Override
        public Attachment[] newArray(int size) {
            return new Attachment[size];
        }
    };

    private int getIdentifier() {
        return mIdentifier;
    }

    /**
     * Used two compare two Attachment objects where one is a "fork" of the other,
     * i.e. a screenshot Attachment that was annotated
     * @param attachment The attachment being compared
     */
    boolean isAnnotatedCopy(Attachment attachment) {
        return getIdentifier() == attachment.getIdentifier();
    }

    AttachmentData getData() {
        return AttachmentDataCache.getInstance().getData(mIdentifier);
    }

    String getFilename() {
        return mFilename;
    }

    JSONObject getJSONObject() throws JSONException {
        JSONObject jsonObject = new JSONObject();
        String base64EncodedData = getBase64EncodedData();

        jsonObject.put("base64_attachment_data", base64EncodedData);
        jsonObject.put("filename", mFilename);
        jsonObject.put("mime_type", mType);

        return jsonObject;
    }

    String getBase64EncodedData() {
        return getData().getBase64EncodedData();
    }

    Bitmap getBitmap(Context context) {
        if (isImageAttachmentType(mType)) {
            BitmapData bitmapData = (BitmapData) getData();
            return bitmapData.getBitmap();
        } else if (mType.equals(TYPE_MP4)) {
            FileData fileData = (FileData) getData();
            Uri fileUri = fileData.getUri();
            MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
            mediaMetadataRetriever.setDataSource(context, fileUri);
            Bitmap bitmap = mediaMetadataRetriever.getFrameAtTime(0);
            mediaMetadataRetriever.release();
            return bitmap;
        }

        throw new Buglife.BuglifeException("No bitmap available for attachment of type " + mType);
    }

    Attachment getCopy(Bitmap newBitmap) {
        return new Builder(mFilename, mType).setIdentifier(mIdentifier).build(newBitmap);
    }

    static boolean isImageAttachmentType(@NonNull String attachmentType) {
        return attachmentType.equals(TYPE_PNG) || attachmentType.equals(TYPE_JPEG);
    }

    /**
     * Builder for Attachment objects.
     */
    public static class Builder {
        private int mIdentifier;
        @NonNull final private String mFilename;
        @NonNull final private String mType;

        /**
         * Default constructor.
         *
         * @param filename The attachment's filename. This should include the attachment extension
         * @param type The attachment type
         */
        public Builder(@NonNull String filename, @NonNull String type) {
            mIdentifier = getNewIdentifier();
            mFilename = filename;
            mType = type;
        }

        Builder setIdentifier(int identifier) {
            mIdentifier = identifier;
            return this;
        }

        /**
         * Builds an attachment using a resource URI.
         *
         * @param uri The resource URI
         * @return The attachment
         */
        public @NonNull Attachment build(@NonNull Uri uri) {
            FileData fileData = new FileData(uri);
            AttachmentDataCache.getInstance().putData(mIdentifier, fileData);
            return new Attachment(mIdentifier, mFilename, mType);
        }

        /**
         * Builds an attachment using a File reference.
         * @param file The file
         * @return The attachment
         */
        public @NonNull Attachment build(@NonNull File file) {
            if (isImageAttachmentType(mType)) {
                BitmapFactory.Options options = new BitmapFactory.Options();
                Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath(), options);
                return build(bitmap);
            } else {
                Uri uri = Uri.fromFile(file);
                return build(uri);
            }
        }

        /**
         * Builds an image attachment using a bitmap
         * @param bitmap
         * @return The attachment
         */
        public @NonNull Attachment build(@NonNull Bitmap bitmap) {
            BitmapData bitmapData = new BitmapData(bitmap);
            AttachmentDataCache.getInstance().putData(mIdentifier, bitmapData);
            return new Attachment(mIdentifier, mFilename, mType);
        }

        private static int gIdentifierCounter = 0;

        private static int getNewIdentifier() {
            gIdentifierCounter += 1;
            return gIdentifierCounter;
        }
    }
}
