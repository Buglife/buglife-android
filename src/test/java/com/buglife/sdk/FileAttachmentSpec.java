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

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static org.assertj.core.api.Java6Assertions.assertThat;

public final class FileAttachmentSpec {
    @Test
    public void createsJSONObjectWithJSONFile() throws JSONException {
        File file = createFile("data.json", "{\"id\": 1}");

        JSONObject json = new FileAttachment(file).toJSON();

        assertThat(json.getString("filename")).isEqualTo("data.json");
        assertThat(json.getString("base64_attachment_data")).isEqualTo("eyJpZCI6IDF9");
    }

    @Test
    public void createsJSONObjectWithImageFile() throws JSONException {
        File file = getResourceFile("test_image.png");

        JSONObject json = new FileAttachment(file).toJSON();

        assertThat(json.getString("filename")).isEqualTo("test_image.png");
        assertThat(json.getString("base64_attachment_data")).isEqualTo("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAIAAACQd1PeAAABG2lUWHRYTUw6Y29tLmFkb2JlLnhtcAAAAAAAPD94cGFja2V0IGJlZ2luPSLvu78iIGlkPSJXNU0wTXBDZWhpSHpyZVN6TlRjemtjOWQiPz4KPHg6eG1wbWV0YSB4bWxuczp4PSJhZG9iZTpuczptZXRhLyIgeDp4bXB0az0iWE1QIENvcmUgNS41LjAiPgogPHJkZjpSREYgeG1sbnM6cmRmPSJodHRwOi8vd3d3LnczLm9yZy8xOTk5LzAyLzIyLXJkZi1zeW50YXgtbnMjIj4KICA8cmRmOkRlc2NyaXB0aW9uIHJkZjphYm91dD0iIi8+CiA8L3JkZjpSREY+CjwveDp4bXBtZXRhPgo8P3hwYWNrZXQgZW5kPSJyIj8+Gkqr6gAAAYFpQ0NQc1JHQiBJRUM2MTk2Ni0yLjEAACiRdZHLS0JBFIe/tFDKMMhFixYS1sqiDKQ2QUpUIBFmkNVGbz4CH5d7lZC2QdugIGrTa1F/QW2D1kFQFEEE7VoXtam4nauCEnmGM+eb38w5zJwBSySjZPXmQcjmClp4MuBeiC66bS84sOLEhT2m6Or47GyIhvZ5T5MZb/vNWo3P/WttKwldgSa78JiiagXhKeHQWkE1eUfYpaRjK8Jnwl5NLih8Z+rxCr+anKrwt8laJBwES4ewO1XH8TpW0lpWWF6OJ5spKtX7mC9xJHLzcxJ7xLvRCTNJADfTTBDEzxCjMvvpx8eArGiQP1jOnyEvuYrMKiU0VkmRpoBX1KJUT0hMip6QkaFk9v9vX/XksK9S3RGAlmfDeO8F2zb8bBnG15Fh/ByD9Qkuc7X8/CGMfIi+VdM8B+DcgPOrmhbfhYtN6HpUY1qsLFnFLckkvJ1CexQ6b6B1qdKz6j4nDxBZl6+6hr196JPzzuVf7GBnrsnkr0EAAAAJcEhZcwAACxMAAAsTAQCanBgAAAAMSURBVAiZY/jIwAAAAtcA8u02OtYAAAAASUVORK5CYII=");
    }

    private File createFile(String filename, String data) {
        File file = getResourceFile(filename);
        if (!file.getParentFile().exists()) {
            file.mkdirs();
        }
        try {
            InputStream inputStream = new ByteArrayInputStream(data.getBytes());
            OutputStream outputStream = new FileOutputStream(file);
            byte[] buffer = new byte[1024];
            for (int read = 0; read != -1; read = inputStream.read(buffer)) {
                outputStream.write(buffer, 0, read);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        file.deleteOnExit();
        return file;
    }

    private File getResourceFile(String filename) {
        return new File("src/test/resources", filename);
    }
}
