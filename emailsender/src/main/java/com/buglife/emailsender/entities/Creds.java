package com.buglife.emailsender.entities;

import android.support.annotation.NonNull;

/**
 * Represents credentials for the app
 * Where to export personal account with account credentials: https://support.google.com/accounts/answer/185833
 */
public class Creds {

    @NonNull private final String username;
    @NonNull private final String password;

    public Creds(@NonNull String username, @NonNull String password) {
        this.username = username;
        this.password = password;
    }

    @NonNull
    public String getUsername() {
        return username;
    }

    @NonNull
    public String getPassword() {
        return password;
    }
}
