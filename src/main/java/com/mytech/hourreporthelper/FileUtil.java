package com.mytech.hourreporthelper;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class FileUtil {
    public static void zipDirectory(Path sourceDir, Path zipFile) throws IOException {
        if (!Files.exists(sourceDir) || !Files.isDirectory(sourceDir)) {
            throw new IOException("Source directory does not exist or is not a directory: " + sourceDir);
        }

        // Kiểm tra nếu tệp zip đang được tạo ra đã tồn tại
        if (Files.exists(zipFile)) {
            Files.delete(zipFile); // Xóa tệp zip cũ nếu có
        }

        try (ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(zipFile.toFile()))) {
            Files.walk(sourceDir)
                    .filter(path -> !Files.isDirectory(path) && !path.endsWith(zipFile.getFileName())) // Loại bỏ tệp zip
                    .forEach(path -> {
                        ZipEntry zipEntry = new ZipEntry(sourceDir.relativize(path).toString());
                        try {
                            zipOut.putNextEntry(zipEntry);
                            byte[] buffer = new byte[1024];
                            int len;
                            try (var inputStream = Files.newInputStream(path)) {
                                while ((len = inputStream.read(buffer)) > 0) {
                                    zipOut.write(buffer, 0, len);
                                }
                            }
                            zipOut.closeEntry();
                        } catch (IOException e) {
                            System.err.println("Failed to zip entry: " + path + " - " + e.getMessage());
                        }
                    });
        }
    }
}