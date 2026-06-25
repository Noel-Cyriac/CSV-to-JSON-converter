package com.example.csvjson.util;

/**
 * Shared constants across the application.
 */
public final class Constants {

    private Constants() {
        // Prevent instantiation
    }

    // Command-line arguments
    public static final String ARG_RETRY_FAILED = "--retryFailed";

    // Configuration file path
    public static final String CONFIG_FILE_PATH = "config/application.properties";

    // Execution states
    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_FAILED = "FAILED";
    public static final String STATUS_PERMANENT_FAILURE = "PERMANENT_FAILURE";

    // File extensions
    public static final String CSV_EXTENSION = ".csv";
    public static final String JSON_EXTENSION = ".json";

    // CSV Headers
    public static final String[] METADATA_HEADERS = {"filename", "status", "retryCount", "lastAttempt"};
    public static final String[] RESULT_HEADERS = {"filename", "status", "message", "timestamp"};
}
