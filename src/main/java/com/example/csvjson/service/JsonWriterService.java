package com.example.csvjson.service;

import com.example.csvjson.config.AppConfig;
import com.example.csvjson.util.Constants;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.File;
import java.io.IOException;

/**
 * Service to write dynamic data structures (List of Maps) as formatted JSON files.
 */
public class JsonWriterService {
    private final AppConfig config;
    private final ObjectMapper objectMapper;

    public JsonWriterService(AppConfig config) {
        this.config = config;
        this.objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    }

    /**
     * Serializes an object to JSON and writes it to the output folder.
     *
     * @param fileName The output filename (CSV extension will be replaced by JSON).
     * @param data     The object data to serialize (typically List of Maps).
     * @throws IOException If writing fails.
     */
    public void writeJson(String fileName, Object data) throws IOException {
        if (fileName == null || fileName.trim().isEmpty()) {
            throw new IllegalArgumentException("Filename cannot be null or empty.");
        }

        String jsonFileName = fileName;
        if (fileName.toLowerCase().endsWith(Constants.CSV_EXTENSION)) {
            jsonFileName = fileName.substring(0, fileName.length() - Constants.CSV_EXTENSION.length()) + Constants.JSON_EXTENSION;
        } else if (!fileName.toLowerCase().endsWith(Constants.JSON_EXTENSION)) {
            jsonFileName = fileName + Constants.JSON_EXTENSION;
        }

        File targetFile = new File(config.getOutputDir(), jsonFileName);
        objectMapper.writeValue(targetFile, data);
    }
}
