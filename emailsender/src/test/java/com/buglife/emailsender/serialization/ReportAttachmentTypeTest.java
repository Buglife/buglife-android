package com.buglife.emailsender.serialization;

import org.junit.Test;

import static org.junit.Assert.*;

public class ReportAttachmentTypeTest {

    @Test
    public void malformedMime() {
        final String fileName = "log_";
        final String mimeType = "invalid_mime";

        final ReportAttachment.Type resultType = ReportAttachment.Type.from(fileName, mimeType);

        assertEquals(ReportAttachment.Type.UNKNOWN, resultType);
    }

    @Test
    public void image() {
        final String fileName = "screenshot_";
        final String mimeType = "image/png";

        final ReportAttachment.Type resultType = ReportAttachment.Type.from(fileName, mimeType);

        assertEquals(ReportAttachment.Type.IMAGE, resultType);
    }

    @Test
    public void log() {
        final String fileName = "log_";
        final String mimeType = "application/json";

        final ReportAttachment.Type resultType = ReportAttachment.Type.from(fileName, mimeType);

        assertEquals(ReportAttachment.Type.LOG, resultType);
    }

    @Test
    public void logNotSpecifiedInName() {
        final String fileName = "1234567.json"; // filename does not have log in it, we're not sure if it is a log for sure
        final String mimeType = "application/json";

        final ReportAttachment.Type resultType = ReportAttachment.Type.from(fileName, mimeType);

        assertEquals(ReportAttachment.Type.UNKNOWN, resultType);
    }
}