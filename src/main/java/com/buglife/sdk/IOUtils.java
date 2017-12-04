package com.buglife.sdk;

import android.support.annotation.Nullable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class IOUtils {
    private IOUtils() {/* No instances */}

    static void writeStringToFile(String data, File file) {
        OutputStream output = null;
        InputStream input = null;
        try {
            input = new ByteArrayInputStream(data.getBytes());
            output = new FileOutputStream(file);
            write(input, output);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (output != null) { closeQuietly(output); }
            if (input != null) { closeQuietly(input); }
        }
    }

    @Nullable
    public static String readStringFromFile(File file) {
        ByteArrayOutputStream output = null;
        FileInputStream input = null;
        try {
            input = new FileInputStream(file);
            output = new ByteArrayOutputStream((int) file.length());
            write(input, output);
            return output.toString("utf-8");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (output != null) { closeQuietly(output); }
            if (input != null) { closeQuietly(input); }
        }
        return null;
    }

    static void write(InputStream input, OutputStream output) throws IOException {
        byte[] buffer = new byte[1024];
        for (int read = 0; read != -1; read = input.read(buffer)) {
            output.write(buffer, 0, read);
        }
    }

    public static void closeQuietly(Closeable closeable) {
        try {
            closeable.close();
        } catch (IOException ignored) {
            // Ignore
        }
    }
}
