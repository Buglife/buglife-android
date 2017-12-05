package com.buglife.sdk;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class LogDumper {

    // Note that max log messages != the number of lines read from the log buffer, since some log
    // messages (i.e. stack traces) may be multiple lines.
    private static final int MAX_LOG_MESSAGES = 200;
    private static final String processId = Integer.toString(android.os.Process
            .myPid());

    static void dumpToFile(File file) throws IOException {
        JSONArray jsonArray = new JSONArray();
        for (LogMessage logMessage : getLogMessages()) {
            JSONObject json = logMessage.toJSON();
            jsonArray.put(json);
        }
        IOUtils.writeStringToFile(jsonArray.toString(), file);
    }

    private static List<LogMessage> getLogMessages() {

        // A typical log line looks like:
        // 11-22 10:54:01.114  2897  2897 I zygote  : Not late-enabling -Xcheck:jni (already on)
        Pattern pattern = Pattern.compile("([\\S]+)[\\s]+([\\S]+)[\\s]+[\\d]+[\\s]+[\\d]+[\\s]+([A-Z])[\\s]+(.+): (.+)");
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MM-dd HH:mm:ss.SSS");
        ArrayList<LogMessage> logMessages = new ArrayList<>();

        try {
            String[] command = new String[] { "logcat", "--pid=" + processId, "-t", Integer.toString(MAX_LOG_MESSAGES), "-v", "threadtime" };

            Process process = Runtime.getRuntime().exec(command);

            BufferedReader bufferedReader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()));

            String line;
            while ((line = bufferedReader.readLine()) != null) {
                Matcher matcher = pattern.matcher(line);

                if (matcher.matches() && matcher.groupCount() > 4) {
                    String dateString = matcher.group(1);
                    String timeString = matcher.group(2);
                    String level = matcher.group(3);
                    String tag = matcher.group(4);
                    String message = matcher.group(5);
                    Date timestamp;

                    try {
                        timestamp = simpleDateFormat.parse(dateString + " " + timeString);
                    } catch (ParseException e) {
                        // We probably don't want to log anything here, since we're actively reading the log buffer
                        timestamp = null;
                    }

                    LogMessage logMessage = new LogMessage(timestamp, level, tag, message);
                    logMessages.add(logMessage);
                }
            }
        } catch (IOException ex) {
            Log.e("Error dumping logs", ex);
        }

        return logMessages;
    }
}
