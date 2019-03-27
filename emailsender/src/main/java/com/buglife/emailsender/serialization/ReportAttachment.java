package com.buglife.emailsender.serialization;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.fasterxml.jackson.annotation.JsonInclude;


public class ReportAttachment {
    @NonNull private String filename;
    @NonNull private String base64AttachmentData;
    @NonNull private String mimeType;
    @NonNull private String logVersion; // might be 'null'

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

    @NonNull
    public String getLogVersion() {
        return logVersion;
    }

    @NonNull
    public Type getType() {
        return Type.from(filename, mimeType);
    }

    @Override
    public String toString() {
        return "\nReportAttachment{" +
                "\n\tfilename='" + filename + '\'' +
                "\n\t, mimeType='" + mimeType + '\'' +
                "\n\t, logVersion='" + logVersion + '\'' +
                "\n}";
    }

    public enum Type {
        UNKNOWN,
        IMAGE,      // screenshot image
        LOG,        // Output log
        ;

        private static final String MIME_JSON = "application/json";
        private static final String MIME_PNG = "image/png";

        public static Type from(
                @NonNull final String fileName,
                @NonNull final String mimeType
        ) {
            if (fileName.contains("log_") && MIME_JSON.equals(mimeType)) {
                return LOG;
            }
            if (MIME_PNG.equals(mimeType)) {
                return IMAGE;
            }
            return UNKNOWN;
        }
    }

}
