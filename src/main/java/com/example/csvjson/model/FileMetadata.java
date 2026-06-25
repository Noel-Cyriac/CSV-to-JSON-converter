package com.example.csvjson.model;

import java.time.LocalDateTime;

/**
 * Represents the metadata associated with a file processed by the application.
 * Map to entries in the metadata ledger (processed-files.csv).
 */
public class FileMetadata {
    private String filename;
    private String status;
    private int retryCount;
    private LocalDateTime lastAttempt;

    public FileMetadata() {
    }

    public FileMetadata(String filename, String status, int retryCount, LocalDateTime lastAttempt) {
        this.filename = filename;
        this.status = status;
        this.retryCount = retryCount;
        this.lastAttempt = lastAttempt;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

    public LocalDateTime getLastAttempt() {
        return lastAttempt;
    }

    public void setLastAttempt(LocalDateTime lastAttempt) {
        this.lastAttempt = lastAttempt;
    }

    @Override
    public String toString() {
        return "FileMetadata{" +
                "filename='" + filename + '\'' +
                ", status='" + status + '\'' +
                ", retryCount=" + retryCount +
                ", lastAttempt=" + lastAttempt +
                '}';
    }
}
