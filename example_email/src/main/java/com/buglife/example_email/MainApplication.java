package com.buglife.example_email;

import android.app.Application;

import com.buglife.emailsender.SubmitReportProviderLocalEmail;
import com.buglife.emailsender.entities.Creds;
import com.buglife.sdk.Buglife;

public class MainApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // TODO: Replace `name@example.com` with your email to receive bug reports :)
        Creds credentials = new Creds("name@example.com", "");
        Buglife.builder(this)
                .setReportSubmitProvider(new SubmitReportProviderLocalEmail(this, credentials))
                .buildWithEmail("name@example.com");
    }
}
