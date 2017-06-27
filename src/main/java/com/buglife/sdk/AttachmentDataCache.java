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

import java.util.HashMap;
import java.util.Map;

final class AttachmentDataCache {
    private static AttachmentDataCache mInstance;
    private final Map<Integer, AttachmentData> mDataCache = new HashMap<>();

    static synchronized AttachmentDataCache getInstance() {
        if (mInstance == null) {
            mInstance = new AttachmentDataCache();
        }

        return mInstance;
    }

    void putData(int attachmentIdentifier, AttachmentData data) {
        mDataCache.put(attachmentIdentifier, data);
    }

    AttachmentData getData(int attachmentIdentifier) {
        return mDataCache.get(attachmentIdentifier);
    }

    void clear() {
        mDataCache.clear();
        // TODO: Collect all the garbage and eat it
    }
}
