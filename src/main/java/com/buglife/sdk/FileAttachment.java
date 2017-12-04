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

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Base64;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class FileAttachment implements Attachment {
    private static final String MIME_TYPE_TEXT = "text/plain";
    private static final String MIME_TYPE_JSON = "application/json";
    private static final String MIME_TYPE_SQLITE = "application/x-sqlite3";
    private static final String MIME_TYPE_PNG = "image/png";
    private static final String MIME_TYPE_JPEG = "image/jpeg";
    private static final String MIME_TYPE_MP4 = "video/mp4";

    @NonNull private final File mFile;
    @NonNull private final String mMimeType;

    public static FileAttachment newJSONFileAttachment(@NonNull File file) {
        return new FileAttachment(file, MIME_TYPE_JSON);
    }

    public static FileAttachment newPNGFileAttachment(@NonNull File file) {
        return new FileAttachment(file, MIME_TYPE_PNG);
    }

    public static FileAttachment newJPGFileAttachment(@NonNull File file) {
        return new FileAttachment(file, MIME_TYPE_JPEG);
    }

    public static FileAttachment newMP4FileAttachment(@NonNull File file) {
        return new FileAttachment(file, MIME_TYPE_MP4);
    }

    public static FileAttachment newPlainTextFileAttachment(@NonNull File file) {
        return new FileAttachment(file, MIME_TYPE_TEXT);
    }

    public static FileAttachment newSQLiteFileAttachment(@NonNull File file) {
        return new FileAttachment(file, MIME_TYPE_SQLITE);
    }

    public FileAttachment(@NonNull File file, @NonNull String mimeType) {
        this.mFile = file;
        this.mMimeType = mimeType;
    }

    @Override public JSONObject toJSON() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("filename", mFile.getName());

        byte[] data = toByteArray();
        if (data != null) {
            json.put("base64_attachment_data", Base64.encodeToString(data, Base64.NO_WRAP));
        }

        json.put("mime_type", mMimeType);
        return json;
    }

    @NonNull public File getFile() {
        return mFile;
    }

    @Override public boolean isImageAttachment() {
        return mMimeType.equals(MIME_TYPE_JPEG) || mMimeType.equals(MIME_TYPE_PNG);
    }

    @Override public boolean isVideoAttachment() {
        return mMimeType.equals(MIME_TYPE_MP4);
    }

    @Nullable
    private byte[] toByteArray() {
        FileInputStream inputStream = null;
        ByteArrayOutputStream outputStream = null;
        try {
            inputStream = new FileInputStream(mFile);
            outputStream = new ByteArrayOutputStream((int) mFile.length());
            IOUtils.write(inputStream, outputStream);
            return outputStream.toByteArray();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (inputStream != null) { IOUtils.closeQuietly(inputStream); }
            if (outputStream != null) { IOUtils.closeQuietly(outputStream); }
        }
        return null;
    }
}
