package com.example.csvjson.service;

import com.example.csvjson.config.AppConfig;
import com.example.csvjson.util.Constants;
import com.example.csvjson.util.FileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Service responsible for scanning the file system directories for CSV files.
 */
public class FileScannerService {
    private final AppConfig config;

    public FileScannerService(AppConfig config) {
        this.config = config;
    }

    /**
     * Scans the input folder for CSV files.
     *
     * @return List of CSV files in the input folder.
     */
    public List<File> getNewFiles() {
        return scanDir(config.getInputDir());
    }

    /**
     * Scans the failed folder for CSV files.
     *
     * @return List of CSV files in the failed folder.
     */
    public List<File> getFailedFiles() {
        return scanDir(config.getFailedDir());
    }

    /**
     * Helper to list all files with the CSV extension in a directory.
     */
    private List<File> scanDir(File dir) {
        if (dir == null || !dir.exists() || !dir.isDirectory()) {
            return Collections.emptyList();
        }

        File[] files = dir.listFiles();
        if (files == null) {
            return Collections.emptyList();
        }

        List<File> csvFiles = new ArrayList<>();
        for (File file : files) {
            if (FileUtils.hasExtension(file, Constants.CSV_EXTENSION)) {
                csvFiles.add(file);
            }
        }
        return csvFiles;
    }
}
