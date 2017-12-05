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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class IOUtils {
    private IOUtils() {/* No instances */}

    static void writeStringToFile(String data, File file) throws IOException {
        OutputStream output = new FileOutputStream(file);
        InputStream input = new ByteArrayInputStream(data.getBytes());
        try {
            write(input, output);
        } finally {
            closeQuietly(output);
            closeQuietly(input);
        }
    }

    public static String readStringFromFile(File file) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream((int) file.length());
        FileInputStream input = new FileInputStream(file);
        try {
            write(input, output);
            return output.toString("utf-8");
        } finally {
            closeQuietly(output);
            closeQuietly(input);
        }
    }

    static void write(InputStream input, OutputStream output) throws IOException {
        byte[] buffer = new byte[1024];
        for (int read = 0; read != -1; read = input.read(buffer)) {
            output.write(buffer, 0, read);
        }
    }

    public static void closeQuietly(Closeable closeable) {
        try {
            closeable.close();
        } catch (IOException ignored) {
            // Ignore
        }
    }
}
