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

import static org.assertj.core.api.Java6Assertions.assertThat;

public final class VideoFileAttachmentSpec {

    private FileAttachment mFileAttachment;

    @Before
    public void beforeEach() {
        File file = SpecUtils.getResourceFile("test_video.mp4");
        mFileAttachment = new FileAttachment(file, MimeTypes.MP4);
    }

    @Test
    public void serializeWithCorrectFilename() throws Exception {
        JSONObject json = mFileAttachment.toJSON();
        assertThat(json.getString("filename")).isEqualTo("test_video.mp4");
    }

    @Test
    public void serializeWithCorrectData() throws Exception {
        String testVideoBase64 = SpecUtils.readContentsOfResourceFile("test_video_base64.txt");
        JSONObject json = mFileAttachment.toJSON();
        assertThat(json.getString("base64_attachment_data")).isEqualTo(testVideoBase64);
    }

    @Test
    public void serializeWithCorrectMimeType() throws Exception {
        JSONObject json = mFileAttachment.toJSON();
        assertThat(json.getString("mime_type")).isEqualTo("video/mp4");
    }
}
