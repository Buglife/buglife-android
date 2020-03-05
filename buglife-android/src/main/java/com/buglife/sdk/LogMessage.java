package com.buglife.sdk;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

final class LogMessage {
    private static final int LOG_CONTEXT_SDK_INTERNAL = 100;

    final private Date mTimestamp;
    final private String mLevel;
    final private String mTag;
    final private String mMessage;

    LogMessage(Date timestamp, String level, String tag, String message) {
        mTimestamp = timestamp;
        mLevel = level;
        mTag = tag;
        mMessage = message;
    }

    Date getTimestamp() {
        return mTimestamp;
    }

    String getTag() {
        return mTag;
    }

    // When parsing lines from logcat, we only get the string representation of the
    // log level. Later on, this must be converted to the log level representation
    // that the Buglife API expects.
    String getLevel() {
        return mLevel;
    }

    // Returns the log level formatted for the Buglife API
    int getFormattedLevel() {
        String level = getLevel();

        if (level.equals("E")) {
            return 1;
        } else if (level.equals("W")) {
            return 2;
        } else if (level.equals("I")) {
            return 4;
        } else if (level.equals("D")) {
            return 8;
        } else if (level.equals("V")) {
            return 16;
        } else {
            return 0;
        }
    }

    String getMessage() {
        return mMessage;
    }

    int getContext() {
        if (getTag().equals(Log.TAG)) {
            return LOG_CONTEXT_SDK_INTERNAL;
        } else {
            return 0;
        }
    }

    JSONObject toJSON() {
        JSONObject json = new JSONObject();
        Date timestamp = getTimestamp();

        try {
            json.put("type", getFormattedLevel());
            json.put("message", getMessage());
            json.put("context", getTag());
            if (timestamp != null) {
                json.put("timestamp", timestamp.getTime());
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return json;
    }
}
