package com.buglife.emailsender;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import com.buglife.emailsender.entities.Creds;
import com.buglife.sdk.reporting.ISubmitReportTask;
import com.buglife.sdk.reporting.ReportSubmissionCallback;
import com.buglife.sdk.reporting.SubmitReportResult;

import org.json.JSONObject;

import javax.mail.MessagingException;

import com.buglife.emailsender.emailer.GMailSender;

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
            GMailSender sender = new GMailSender(credentials.getUsername(), credentials.getPassword());
            sender.addAttachment(report);
            String mailDetailsSubject = "Test mail";
            String mailDetailsBody = "This mail has been sent from android app along with attachment";
            String mailDetailsSender = credentials.getUsername();
            String mailDetailsRecipient = credentials.getUsername();
            sender.sendMail(
                    mailDetailsSubject,
                    mailDetailsBody,
                    mailDetailsSender,
                    mailDetailsRecipient
            );
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
                    GMailSender sender = new GMailSender(
                            credentials.getUsername(),
                            credentials.getPassword()
                    );
                    sender.addAttachment(report);
                    String mailDetailsSubject = "Test mail";
                    String mailDetailsBody = "This mail has been sent from android app along with attachment";
                    String mailDetailsSender = credentials.getUsername();
                    String mailDetailsRecipient = credentials.getPassword();
                    sender.sendMail(
                            mailDetailsSubject,
                            mailDetailsBody,
                            mailDetailsSender,
                            mailDetailsRecipient
                    );
                } catch (MessagingException e) {
//                    Toast.makeText(context, "Error", Toast.LENGTH_LONG).show();
                    Log.e(Consts.TAG, "Error", e);
                }
            }
        }).start();
    }

}
