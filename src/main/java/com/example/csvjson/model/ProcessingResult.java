package com.example.csvjson.model;

import java.time.LocalDateTime;

/**
 * Represents the processing outcome of an individual file.
 * This maps to a row in the results log (results.csv).
 */
public class ProcessingResult {
    private String filename;
    private String status;
    private String message;
    private LocalDateTime timestamp;

    public ProcessingResult() {
    }

    public ProcessingResult(String filename, String status, String message, LocalDateTime timestamp) {
        this.filename = filename;
        this.status = status;
        this.message = message;
        this.timestamp = timestamp;
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

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "ProcessingResult{" +
                "filename='" + filename + '\'' +
                ", status='" + status + '\'' +
                ", message='" + message + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
