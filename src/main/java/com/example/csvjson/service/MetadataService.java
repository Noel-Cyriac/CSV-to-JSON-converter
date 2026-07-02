package com.example.csvjson.service;

import com.example.csvjson.config.AppConfig;
import com.example.csvjson.model.FileMetadata;
import com.example.csvjson.util.Constants;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Manages the processed-files.csv ledger to check file status and update
 * execution states.
 */
public class MetadataService {
    private final AppConfig config;
    private final File metadataFile;
    private final Map<String, FileMetadata> metadataCache = new HashMap<>();

    public MetadataService(AppConfig config) {
        this.config = config;
        this.metadataFile = new File(config.getMetadataDir(), "processed-files.csv");
        loadMetadata();
    }

    /**
     * Reads all metadata records from the CSV file into memory.
     */
    public synchronized Map<String, FileMetadata> loadMetadata() {
        metadataCache.clear();
        if (!metadataFile.exists() || metadataFile.length() == 0) {
            return metadataCache;
        }

        CSVFormat csvFormat = CSVFormat.Builder.create(CSVFormat.DEFAULT)
                .setHeader(Constants.METADATA_HEADERS)
                .setSkipHeaderRecord(true)
                .build();

        try (Reader reader = new FileReader(metadataFile);
                CSVParser parser = new CSVParser(reader, csvFormat)) {

            for (CSVRecord record : parser) {
                String filename = record.get("filename");
                String status = record.get("status");
                int retryCount = Integer.parseInt(record.get("retryCount"));
                LocalDateTime lastAttempt = LocalDateTime.parse(record.get("lastAttempt"));

                FileMetadata metadata = new FileMetadata(filename, status, retryCount, lastAttempt);
                metadataCache.put(filename, metadata);
            }
        } catch (Exception e) {
            // Log warning, but allow application to continue with empty cache or partial
            // load
            System.err.println("Warning: Failed to load metadata from " + metadataFile.getAbsolutePath() + ". Error: "
                    + e.getMessage());
        }
        return metadataCache;
    }

    /**
     * Checks if a file has already been successfully processed.
     *
     * @param fileName Name of the file.
     * @return true if status is SUCCESS, false otherwise.
     */
    public synchronized boolean alreadyProcessed(String fileName) {
        FileMetadata metadata = metadataCache.get(fileName);
        if (metadata == null) {
            return false;
        }
        String status = metadata.getStatus();
        return Constants.STATUS_SUCCESS.equals(status);
    }

    /**
     * Gets the metadata of a specific file.
     */
    public synchronized FileMetadata getMetadata(String fileName) {
        return metadataCache.get(fileName);
    }

    /**
     * Marks a file as SUCCESS.
     */
    public synchronized void markSuccess(String fileName, int retryCount, LocalDateTime lastAttempt) {
        FileMetadata metadata = new FileMetadata(fileName, Constants.STATUS_SUCCESS, retryCount, lastAttempt);
        metadataCache.put(fileName, metadata);
        saveAllMetadata();
    }

    /**
     * Marks a file as FAILED.
     */
    public synchronized void markFailure(String fileName, int retryCount, LocalDateTime lastAttempt) {
        FileMetadata metadata = new FileMetadata(fileName, Constants.STATUS_FAILED, retryCount, lastAttempt);
        metadataCache.put(fileName, metadata);
        saveAllMetadata();
    }

    /**
     * Increments the retry count for a file in the metadata.
     */
    public synchronized void incrementRetry(String fileName) {
        FileMetadata metadata = metadataCache.get(fileName);
        int newRetryCount = (metadata != null) ? metadata.getRetryCount() + 1 : 1;
        FileMetadata updated = new FileMetadata(fileName, Constants.STATUS_FAILED, newRetryCount, LocalDateTime.now());
        metadataCache.put(fileName, updated);
        saveAllMetadata();
    }

    /**
     * Saves the entire metadata cache back to the CSV ledger.
     */
    private synchronized void saveAllMetadata() {
        CSVFormat csvFormat = CSVFormat.Builder.create(CSVFormat.DEFAULT)
                .setHeader(Constants.METADATA_HEADERS)
                .build();

        try (FileWriter writer = new FileWriter(metadataFile);
                CSVPrinter printer = new CSVPrinter(writer, csvFormat)) {

            for (FileMetadata metadata : metadataCache.values()) {
                printer.printRecord(
                        metadata.getFilename(),
                        metadata.getStatus(),
                        metadata.getRetryCount(),
                        metadata.getLastAttempt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            }
            printer.flush();
        } catch (IOException e) {
            System.err.println("Error: Failed to save metadata to " + metadataFile.getAbsolutePath() + ". Error: "
                    + e.getMessage());
        }
    }
}
