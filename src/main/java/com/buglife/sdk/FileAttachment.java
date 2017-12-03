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
import android.util.Base64;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

class FileAttachment implements IAttachment {
    @NonNull private final File mFile;

    FileAttachment(@NonNull File file) {
        this.mFile = file;
    }

    @Override public JSONObject toJSON() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("filename", mFile.getName());
        json.put("base64_attachment_data", Base64.encodeToString(toByteArray(), Base64.NO_WRAP));
        return json;
    }

    private byte[] toByteArray() {
        FileInputStream inputStream = null;
        ByteArrayOutputStream outputStream = null;
        try {
            inputStream = new FileInputStream(mFile);
            outputStream = new ByteArrayOutputStream((int) mFile.length());
            byte[] buffer = new byte[1024];
            for (int read = 0; read != -1; read = inputStream.read(buffer)) {
                outputStream.write(buffer, 0, read);
            }
            return outputStream.toByteArray();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (inputStream != null) { closeQuietly(inputStream); }
            if (outputStream != null) { closeQuietly(outputStream); }
        }
        return null;
    }

    private void closeQuietly(Closeable closeable) {
        try {
            closeable.close();
        } catch (IOException ignored) {
            // Ignore
        }
    }
}
