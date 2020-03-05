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
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.assertj.core.api.Java6Assertions.assertThat;

public final class JSONFileAttachmentSpec {

    private FileAttachment mFileAttachment;

    @Before
    public void beforeEach() {
        File file = SpecUtils.getResourceFile("test_json.json");
        mFileAttachment = new FileAttachment(file, MimeTypes.JSON);
    }

    @Test
    public void serializeWithCorrectFilename() throws JSONException, IOException {
        JSONObject json = mFileAttachment.toJSON();
        assertThat(json.getString("filename")).isEqualTo("test_json.json");
    }

    @Test
    public void serializeWithCorrectData() throws JSONException, IOException {
        String testJsonBase64 = SpecUtils.readContentsOfResourceFile("test_json_base64.txt");
        JSONObject json = mFileAttachment.toJSON();
        assertThat(json.getString("base64_attachment_data")).isEqualTo(testJsonBase64);
    }

    @Test
    public void serializeWithCorrectMimeType() throws JSONException, IOException {
        JSONObject json = mFileAttachment.toJSON();
        assertThat(json.getString("mime_type")).isEqualTo("application/json");
    }
}
