package com.example.csvjson.config;

import com.example.csvjson.util.Constants;
import com.example.csvjson.util.FileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Handles application configuration loading and folder initialization.
 */
public class AppConfig {
    private final Properties properties = new Properties();

    private File inputDir;
    private File outputDir;
    private File processedDir;
    private File failedDir;
    private File logsDir;
    private File metadataDir;
    private int maxRetries = 3;
    private boolean inferTypes = true;
    private char csvDelimiter = ',';

    public AppConfig() {
        loadProperties();
        initializeDirs();
    }

    /**
     * Loads properties from the config file, falling back to defaults if not found.
     */
    private void loadProperties() {
        File configFile = new File(Constants.CONFIG_FILE_PATH);
        if (configFile.exists() && configFile.isFile()) {
            try (InputStream input = new FileInputStream(configFile)) {
                properties.load(input);
            } catch (IOException e) {
                System.err.println("Warning: Could not read " + Constants.CONFIG_FILE_PATH + ". Using defaults. Error: " + e.getMessage());
            }
        } else {
            System.out.println("Config file not found at " + Constants.CONFIG_FILE_PATH + ". Using default directories.");
        }

        // Set up directory fields (using defaults if properties are empty/missing)
        inputDir = new File(properties.getProperty("dir.input", "input"));
        outputDir = new File(properties.getProperty("dir.output", "output"));
        processedDir = new File(properties.getProperty("dir.processed", "processed"));
        failedDir = new File(properties.getProperty("dir.failed", "failed"));
        logsDir = new File(properties.getProperty("dir.logs", "logs"));
        metadataDir = new File(properties.getProperty("dir.metadata", "metadata"));

        try {
            maxRetries = Integer.parseInt(properties.getProperty("max.retries", "3"));
        } catch (NumberFormatException e) {
            System.err.println("Warning: Invalid max.retries value. Defaulting to 3.");
            maxRetries = 3;
        }

        inferTypes = Boolean.parseBoolean(properties.getProperty("json.infer.types", "true"));

        String delimProp = properties.getProperty("csv.delimiter", ",");
        if ("\\t".equals(delimProp) || "\t".equals(delimProp)) {
            this.csvDelimiter = '\t';
        } else if (!delimProp.isEmpty()) {
            this.csvDelimiter = delimProp.charAt(0);
        }
    }

    /**
     * Creates all necessary directories if they do not exist.
     */
    private void initializeDirs() {
        try {
            FileUtils.ensureDirectoryExists(inputDir);
            FileUtils.ensureDirectoryExists(outputDir);
            FileUtils.ensureDirectoryExists(processedDir);
            FileUtils.ensureDirectoryExists(failedDir);
            FileUtils.ensureDirectoryExists(logsDir);
            FileUtils.ensureDirectoryExists(metadataDir);
        } catch (IOException e) {
            throw new RuntimeException("Fatal error: Could not initialize directories. " + e.getMessage(), e);
        }
    }

    // Getters
    public File getInputDir() {
        return inputDir;
    }

    public File getOutputDir() {
        return outputDir;
    }

    public File getProcessedDir() {
        return processedDir;
    }

    public File getFailedDir() {
        return failedDir;
    }

    public File getLogsDir() {
        return logsDir;
    }

    public File getMetadataDir() {
        return metadataDir;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public boolean isInferTypes() {
        return inferTypes;
    }

    public char getCsvDelimiter() {
        return csvDelimiter;
    }
}
