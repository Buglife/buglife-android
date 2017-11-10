package com.buglife.sdk;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

final public class LogDumper {

    private static final String TAG = LogDumper.class.getCanonicalName();
    private static final String processId = Integer.toString(android.os.Process
            .myPid());

    public static StringBuilder getLog() {

        StringBuilder builder = new StringBuilder();

        try {
            String[] command = new String[] { "logcat", "-d", "-v", "threadtime" };

            Process process = Runtime.getRuntime().exec(command);

            BufferedReader bufferedReader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()));

            String line;
            while ((line = bufferedReader.readLine()) != null) {
                if (line.contains(processId)) {
                    builder.append(line);
                    //Code here
                }
            }
        } catch (IOException ex) {
            Log.e("Error dumping logs", ex);
        }

        return builder;
    }

}
