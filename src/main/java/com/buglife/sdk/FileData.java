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
import android.net.Uri;
import android.support.annotation.NonNull;
import android.util.Base64;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

class FileData extends AttachmentData {

    final protected File mFile;

    FileData(File file) {
        mFile = file;
    }

    File getFile() {
        return mFile;
    }

    @NonNull
    @Override
    String getBase64EncodedData() {
        int size = (int) mFile.length();
        byte buffer[] = new byte[size];
        try {
            BufferedInputStream bufferedInputStream = new BufferedInputStream(new FileInputStream(mFile));
            bufferedInputStream.read(buffer, 0, size);
            bufferedInputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return Base64.encodeToString(buffer, Base64.DEFAULT);
    }
}