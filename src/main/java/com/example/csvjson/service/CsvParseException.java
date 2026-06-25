package com.example.csvjson.service;

/**
 * Exception thrown when CSV parsing or validation fails.
 */
public class CsvParseException extends Exception {
    public CsvParseException(String message) {
        super(message);
    }

    public CsvParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
