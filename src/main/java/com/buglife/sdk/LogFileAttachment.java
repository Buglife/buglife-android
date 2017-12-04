package com.buglife.sdk;

import android.support.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;

class LogFileAttachment extends FileAttachment {
    private static final String LOG_VERSION = "2.1";

    public LogFileAttachment(@NonNull File file) {
        super(file, "application/json");
    }

    @Override public JSONObject toJSON() throws JSONException {
        JSONObject json = super.toJSON();
        json.put("log_version", LOG_VERSION);
        return json;
    }
}
