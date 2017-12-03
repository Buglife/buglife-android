package com.buglife.sdk;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

class SpecUtils {
    private SpecUtils() {/* No instances */}

    static File createFile(String filename, String data) {
        File file = getResourceFile(filename);
        if (!file.getParentFile().exists()) {
            file.mkdirs();
        }
        try {
            InputStream inputStream = new ByteArrayInputStream(data.getBytes());
            OutputStream outputStream = new FileOutputStream(file);
            byte[] buffer = new byte[1024];
            for (int read = 0; read != -1; read = inputStream.read(buffer)) {
                outputStream.write(buffer, 0, read);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        file.deleteOnExit();
        return file;
    }

    static File getResourceFile(String filename) {
        return new File("src/test/resources", filename);
    }
}
