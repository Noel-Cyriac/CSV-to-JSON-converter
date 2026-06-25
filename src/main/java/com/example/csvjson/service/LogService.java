package com.example.csvjson.service;

import com.example.csvjson.config.AppConfig;
import com.example.csvjson.model.ProcessingResult;
import com.example.csvjson.util.Constants;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * Manages the application execution log (execution.log) and processing results (results.csv).
 */
public class LogService {
    private static final Logger logger = Logger.getLogger(LogService.class.getName());
    private final AppConfig config;
    private final File resultsFile;

    public LogService(AppConfig config) {
        this.config = config;
        this.resultsFile = new File(config.getLogsDir(), "results.csv");
        setupLogger();
    }

    /**
     * Programmatically configures java.util.logging for console and file output.
     */
    private void setupLogger() {
        // Remove default handlers
        Logger rootLogger = Logger.getLogger("");
        for (var handler : rootLogger.getHandlers()) {
            rootLogger.removeHandler(handler);
        }

        // Custom single-line formatter
        Formatter customFormatter = new Formatter() {
            @Override
            public String format(LogRecord record) {
                // Formatting format: LEVEL Message
                // To keep log files professional, we can include the level and message.
                // We can also prepend a timestamp if desired, but we will keep it simple and clean.
                return String.format("%s %s%n", record.getLevel().getLocalizedName(), record.getMessage());
            }
        };

        // Console handler
        ConsoleHandler consoleHandler = new ConsoleHandler();
        consoleHandler.setLevel(Level.INFO);
        consoleHandler.setFormatter(customFormatter);
        rootLogger.addHandler(consoleHandler);

        // File handler
        try {
            File logFile = new File(config.getLogsDir(), "execution.log");
            FileHandler fileHandler = new FileHandler(logFile.getAbsolutePath(), true);
            fileHandler.setLevel(Level.ALL);
            fileHandler.setFormatter(customFormatter);
            rootLogger.addHandler(fileHandler);
            rootLogger.setLevel(Level.ALL);
        } catch (IOException e) {
            System.err.println("Failed to initialize file logging: " + e.getMessage());
        }
    }

    public void logInfo(String message) {
        logger.info(message);
    }

    public void logError(String message) {
        logger.severe(message);
    }

    public void logError(String message, Throwable t) {
        logger.log(Level.SEVERE, message + (t != null ? " - " + t.getMessage() : ""));
    }

    /**
     * Appends a processing result to the results.csv ledger.
     *
     * @param result The result record to append.
     */
    public synchronized void saveResult(ProcessingResult result) {
        boolean writeHeader = !resultsFile.exists() || resultsFile.length() == 0;

        try (FileWriter writer = new FileWriter(resultsFile, true)) {
            if (writeHeader) {
                writer.write(String.join(",", Constants.RESULT_HEADERS) + System.lineSeparator());
            }

            String timestampStr = result.getTimestamp().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            String escapedMessage = escapeCsvField(result.getMessage());

            String line = String.format("%s,%s,%s,%s%n",
                    result.getFilename(),
                    result.getStatus(),
                    escapedMessage,
                    timestampStr
            );
            writer.write(line);
        } catch (IOException e) {
            logError("Failed to write to results log: " + resultsFile.getAbsolutePath(), e);
        }
    }

    /**
     * Escapes CSV fields to handle quotes and commas.
     */
    private String escapeCsvField(String field) {
        if (field == null) {
            return "";
        }
        if (field.contains(",") || field.contains("\"") || field.contains("\n") || field.contains("\r")) {
            return "\"" + field.replace("\"", "\"\"") + "\"";
        }
        return field;
    }
}
