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

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

class SpecUtils {
    private SpecUtils() {/* No instances */}

    static String readContentsOfResourceFile(String filename) {
        FileInputStream inputStream = null;
        ByteArrayOutputStream outputStream = null;
        try {
            File file = getResourceFile(filename);
            inputStream = new FileInputStream(file);
            outputStream = new ByteArrayOutputStream((int) file.length());
            IOUtils.write(inputStream, outputStream);
            return outputStream.toString("utf-8");
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

    static File getResourceFile(String filename) {
        return new File("src/test/resources", filename);
    }
}
