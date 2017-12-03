package com.buglife.sdk;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

class SpecUtils {
    private SpecUtils() {/* No instances */}

    static String readContentsOfResourceFile(String filename) {
        FileInputStream inputStream = null;
        ByteArrayOutputStream outputStream = null;
        try {
            File file = getResourceFile(filename);
            inputStream = new FileInputStream(file);
            outputStream = new ByteArrayOutputStream((int) file.length());
            byte[] buffer = new byte[1024];
            for (int read = 0; read != -1; read = inputStream.read(buffer)) {
                outputStream.write(buffer, 0, read);
            }
            return outputStream.toString("utf-8");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (inputStream != null) { closeQuietly(inputStream); }
            if (outputStream != null) { closeQuietly(outputStream); }
        }
        return null;
    }

    static File getResourceFile(String filename) {
        return new File("src/test/resources", filename);
    }

    private static void closeQuietly(Closeable closeable) {
        try {
            closeable.close();
        } catch (IOException ignored) {
            // Ignored
        }
    }
}
