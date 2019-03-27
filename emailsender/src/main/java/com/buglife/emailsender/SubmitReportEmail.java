package com.buglife.emailsender;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import com.buglife.emailsender.emailer.GMailSender;
import com.buglife.emailsender.entities.Creds;
import com.buglife.emailsender.serialization.ReportAttachment;
import com.buglife.emailsender.serialization.ReportResponse;
import com.buglife.sdk.reporting.ISubmitReportTask;
import com.buglife.sdk.reporting.ReportSubmissionCallback;
import com.buglife.sdk.reporting.SubmitReportResult;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import javax.mail.MessagingException;

public final class SubmitReportEmail implements ISubmitReportTask {

    @NonNull private final Context context;
    @NonNull private final Creds credentials;

    public SubmitReportEmail(@NonNull Context context, @NonNull Creds credentials) {
        this.context = context;
        this.credentials = credentials;
    }

    /**
     * Synchronously executes a POST request
     *
     * @param report a JSON payload with the report to submit
     * @return The result of the network request
     */
    @Override
    public SubmitReportResult execute(JSONObject report) {
        try {
            sendEmail(report);
            return new SubmitReportResult(report);
        } catch (MessagingException e) {
            Log.e(Consts.TAG, "Error", e);
            return new SubmitReportResult(e);
        }
    }

    /**
     * Asynchronously executes a POST request
     *
     * @param report   a JSON payload with the report to submit
     * @param callback Calls back with the result of the request; this is called on the main thread
     */
    @Override
    public void execute(final JSONObject report, final ReportSubmissionCallback callback) {
        new Thread(new Runnable() {
            public void run() {
                try {
                    sendEmail(report);
                } catch (MessagingException e) {
//                    Toast.makeText(context, "Error", Toast.LENGTH_LONG).show();
                    Log.e(Consts.TAG, "Error", e);
                }
            }
        }).start();
    }

    private void sendEmail(@NonNull JSONObject report) throws MessagingException {
        GMailSender sender = new GMailSender(
                credentials.getUsername(),
                credentials.getPassword()
        );
        final ReportResponse reportResponse = ReportResponse.fromJson(report);
        final StringBuilder sb = new StringBuilder("Report details:\n\n");
        sb.append(reportResponse.getReport());
        sb.append(reportResponse.getApp());
        sb.append("\n\nEmail: ");
        sb.append(reportResponse.getEmail());
        for (ReportAttachment attachment : reportResponse.getReport().getAttachments()) {
            sender.addAttachment(attachment);
        }

        final String mailDetailsSubject = "Buglife report";
        final String mailDetailsBody = sb.toString();
        final String mailDetailsSender = credentials.getUsername();
        final String mailDetailsRecipient = credentials.getUsername();
        sender.sendMail(
                mailDetailsSubject,
                mailDetailsBody,
                mailDetailsSender,
                mailDetailsRecipient
        );
    }

}
