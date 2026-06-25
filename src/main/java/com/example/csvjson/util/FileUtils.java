package com.example.csvjson.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * Utility functions for file system operations.
 */
public final class FileUtils {

    private FileUtils() {
        // Prevent instantiation
    }

    /**
     * Ensures a directory exists. If it does not exist, it creates it.
     * Verifies that the path is actually a directory and is writeable.
     *
     * @param dir The directory to check/create.
     * @throws IOException If the directory could not be created or lacks read/write permissions.
     */
    public static void ensureDirectoryExists(File dir) throws IOException {
        if (!dir.exists()) {
            boolean created = dir.mkdirs();
            if (!created) {
                throw new IOException("Failed to create directory: " + dir.getAbsolutePath());
            }
        }
        if (!dir.isDirectory()) {
            throw new IOException("Path exists but is not a directory: " + dir.getAbsolutePath());
        }
        if (!dir.canRead() || !dir.canWrite()) {
            throw new IOException("Insufficient permissions for directory: " + dir.getAbsolutePath());
        }
    }

    /**
     * Moves a file to a destination directory, replacing any existing file with the same name.
     *
     * @param sourceFile The file to move.
     * @param destDir    The target directory.
     * @throws IOException If the move fails or paths are invalid.
     */
    public static void moveFile(File sourceFile, File destDir) throws IOException {
        if (sourceFile == null || !sourceFile.exists()) {
            throw new IOException("Source file does not exist.");
        }
        ensureDirectoryExists(destDir);
        File targetFile = new File(destDir, sourceFile.getName());
        Files.move(sourceFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    /**
     * Checks if a file ends with a specific extension (case-insensitive).
     *
     * @param file      The file to check.
     * @param extension The extension, including the dot (e.g. ".csv").
     * @return true if the file ends with the extension, false otherwise.
     */
    public static boolean hasExtension(File file, String extension) {
        if (file == null || !file.isFile()) {
            return false;
        }
        return file.getName().toLowerCase().endsWith(extension.toLowerCase());
    }
}
