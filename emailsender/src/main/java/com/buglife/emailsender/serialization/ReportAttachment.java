package com.buglife.emailsender.serialization;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

public class ReportAttachment {
    @NonNull String filename;
    @NonNull String base64AttachmentData;
    @NonNull String mimeType;
    @Nullable String logVersion;

    @NonNull
    public String getFilename() {
        return filename;
    }

    @NonNull
    public String getBase64AttachmentData() {
        return base64AttachmentData;
    }

    @NonNull
    public String getMimeType() {
        return mimeType;
    }

    @Nullable
    public String getLogVersion() {
        return logVersion;
    }

    @Override
    public String toString() {
        return "ReportAttachment{" +
                "filename='" + filename + '\'' +
                ", mimeType='" + mimeType + '\'' +
                ", logVersion='" + logVersion + '\'' +
                '}';
    }
}
