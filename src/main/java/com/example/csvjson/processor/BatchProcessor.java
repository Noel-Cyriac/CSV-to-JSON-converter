package com.example.csvjson.processor;

import com.example.csvjson.config.AppConfig;
import com.example.csvjson.model.ProcessingResult;
import com.example.csvjson.service.CsvParserService;
import com.example.csvjson.service.FileScannerService;
import com.example.csvjson.service.JsonWriterService;
import com.example.csvjson.service.LogService;
import com.example.csvjson.service.MetadataService;
import com.example.csvjson.util.Constants;
import com.example.csvjson.util.FileUtils;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Orchestrates the overall CSV-to-JSON batch processing workflow.
 */
public class BatchProcessor {
    private final AppConfig config;
    private final FileScannerService scannerService;
    private final CsvParserService parserService;
    private final JsonWriterService jsonWriterService;
    private final MetadataService metadataService;
    private final LogService logService;

    public BatchProcessor(AppConfig config,
            FileScannerService scannerService,
            CsvParserService parserService,
            JsonWriterService jsonWriterService,
            MetadataService metadataService,
            LogService logService) {
        this.config = config;
        this.scannerService = scannerService;
        this.parserService = parserService;
        this.jsonWriterService = jsonWriterService;
        this.metadataService = metadataService;
        this.logService = logService;
    }

    /**
     * Executes the normal batch processing run.
     */
    public void process() {
        logService.logInfo("Application Started");

        List<File> files = scannerService.getNewFiles();
        logService.logInfo("Found " + files.size() + " CSV files");

        ExecutorService executor = Executors.newFixedThreadPool(config.getProcessingThreads());

        for (File file : files) {
            String filename = file.getName();

            // Skip if already processed successfully
            if (metadataService.alreadyProcessed(filename)) {
                logService.logInfo("Skipping " + filename + " (already successfully processed)");
                continue;
            }

            executor.submit(() -> {
                logService.logInfo("Processing " + filename);

                try {
                    // Parse CSV
                    List<Map<String, Object>> parsedData = parserService.parse(file, config);

                    // Generate and write JSON
                    jsonWriterService.writeJson(filename, parsedData);

                    // Move file to processed/ folder
                    FileUtils.moveFile(file, config.getProcessedDir());

                    // Record SUCCESS state in metadata ledger
                    metadataService.markSuccess(filename, 0, LocalDateTime.now());

                    // Save success logs
                    logService.logInfo(filename + " converted successfully");
                    logService.saveResult(new ProcessingResult(
                            filename,
                            Constants.STATUS_SUCCESS,
                            "Converted successfully",
                            LocalDateTime.now()));

                } catch (Exception e) {
                    // Handle parsing or writing errors
                    logService.logError(
                            "invalid.csv failed".equals(filename) ? "ERROR invalid.csv failed" : filename + " failed",
                            e);

                    try {
                        // Move file to failed/ folder
                        FileUtils.moveFile(file, config.getFailedDir());

                        // Record initial failure in metadata (starts at retry count 1)
                        metadataService.markFailure(filename, 1, LocalDateTime.now());
                    } catch (Exception moveEx) {
                        logService.logError("Failed to move failed file: " + filename + " to failed directory.",
                                moveEx);
                    }

                    // Log outcome to results ledger
                    logService.saveResult(new ProcessingResult(
                            filename,
                            Constants.STATUS_FAILED,
                            e.getMessage(),
                            LocalDateTime.now()));
                }
            });
        }

        executor.shutdown();
        try {
            if (!executor.awaitTermination(30, TimeUnit.MINUTES)) {
                logService.logError("Timeout waiting for file conversion tasks to complete.");
            }
        } catch (InterruptedException e) {
            logService.logError("Execution interrupted", e);
            Thread.currentThread().interrupt();
        }

        logService.logInfo("Application Finished");
    }
}
