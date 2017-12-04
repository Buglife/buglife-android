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
            IOUtils.write(inputStream, outputStream);
            return outputStream.toString("utf-8");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (inputStream != null) { IOUtils.closeQuietly(inputStream); }
            if (outputStream != null) { IOUtils.closeQuietly(outputStream); }
        }
        return null;
    }

    static File getResourceFile(String filename) {
        return new File("src/test/resources", filename);
    }
}
