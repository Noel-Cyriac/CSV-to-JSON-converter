package com.example.csvjson.service;

import com.example.csvjson.config.AppConfig;
import com.example.csvjson.model.FileMetadata;
import com.example.csvjson.model.ProcessingResult;
import com.example.csvjson.util.Constants;
import com.example.csvjson.util.FileUtils;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Service responsible for retrying failed CSV file conversions.
 */
public class RetryService {
    private final AppConfig config;
    private final FileScannerService scannerService;
    private final CsvParserService parserService;
    private final JsonWriterService jsonWriterService;
    private final MetadataService metadataService;
    private final LogService logService;

    public RetryService(AppConfig config,
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
     * Finds and retries processing on all failed CSV files.
     */
    public void retryFailedFiles() {
        logService.logInfo("Retry execution started");
        List<File> failedFiles = scannerService.getFailedFiles();
        
        if (failedFiles.isEmpty()) {
            logService.logInfo("No failed files found to retry");
            logService.logInfo("Application Finished");
            return;
        }

        logService.logInfo("Found " + failedFiles.size() + " failed CSV files for retry");

        for (File file : failedFiles) {
            String filename = file.getName();
            FileMetadata metadata = metadataService.getMetadata(filename);

            int currentRetryCount = 0;
            if (metadata != null) {
                // Only retry if status is FAILED
                if (!Constants.STATUS_FAILED.equals(metadata.getStatus())) {
                    logService.logInfo("Skipping " + filename + " because status is " + metadata.getStatus());
                    continue;
                }
                currentRetryCount = metadata.getRetryCount();
            } else {
                logService.logInfo("Failed file " + filename + " has no metadata. Initializing retry count to 0.");
            }

            int maxRetries = config.getMaxRetries();
            if (currentRetryCount >= maxRetries) {
                logService.logInfo("Skipping " + filename + " because retry count (" + currentRetryCount + ") reached max retries (" + maxRetries + ").");
                // Ensure metadata reflects permanent failure if not already set
                metadataService.markPermanentFailure(filename, currentRetryCount, LocalDateTime.now());
                continue;
            }

            int attempt = currentRetryCount + 1;
            logService.logInfo("Retrying " + filename + " (attempt " + attempt + " of " + maxRetries + ")");

            try {
                // Parse failed file from failed/ directory
                List<Map<String, Object>> data = parserService.parse(file, config);

                // Write JSON output
                jsonWriterService.writeJson(filename, data);

                // Move from failed/ to processed/
                FileUtils.moveFile(file, config.getProcessedDir());

                // Update metadata to SUCCESS
                metadataService.markSuccess(filename, 0, LocalDateTime.now());

                // Save log entries
                logService.logInfo(filename + " converted successfully on retry");
                logService.saveResult(new ProcessingResult(
                        filename,
                        Constants.STATUS_SUCCESS,
                        "Converted successfully on retry (attempt " + attempt + ")",
                        LocalDateTime.now()
                ));

            } catch (Exception e) {
                // Increment retry count
                int newRetryCount = currentRetryCount + 1;
                LocalDateTime now = LocalDateTime.now();

                if (newRetryCount >= maxRetries) {
                    // Mark as permanent failure
                    metadataService.markPermanentFailure(filename, newRetryCount, now);
                    logService.logError("invalid.csv failed".equals(filename) ? "ERROR invalid.csv failed" : filename + " failed permanently after " + newRetryCount + " attempts");
                    logService.logError(filename + " marked as PERMANENT_FAILURE");
                    logService.saveResult(new ProcessingResult(
                            filename,
                            Constants.STATUS_PERMANENT_FAILURE,
                            "Failed permanently: " + e.getMessage(),
                            now
                    ));
                } else {
                    // Mark as failed and keep in failed/
                    metadataService.markFailure(filename, newRetryCount, now);
                    logService.logError("invalid.csv failed".equals(filename) ? "ERROR invalid.csv failed" : filename + " failed on retry attempt " + newRetryCount);
                    logService.saveResult(new ProcessingResult(
                            filename,
                            Constants.STATUS_FAILED,
                            "Failed on retry: " + e.getMessage(),
                            now
                    ));
                }
            }
        }

        logService.logInfo("Application Finished");
    }
}
