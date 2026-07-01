package com.example.csvjson.service;

import com.example.csvjson.config.AppConfig;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.File;
import java.io.FileReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Service to parse CSV files into a dynamic List of Maps representing rows.
 */
public class CsvParserService {

    /**
     * Parses the given CSV file into a List of Maps.
     *
     * @param file   The CSV file to parse.
     * @param config The application configuration.
     * @return List of rows, where each row is a map from header to cell value.
     * @throws CsvParseException If the file is empty, headers are missing, rows are malformed, or CSV is invalid.
     */
    public List<Map<String, Object>> parse(File file, AppConfig config) throws CsvParseException {
        if (file == null) {
            throw new CsvParseException("File reference is null.");
        }
        if (!file.exists()) {
            throw new CsvParseException("File does not exist: " + file.getAbsolutePath());
        }
        if (file.length() == 0) {
            throw new CsvParseException("CSV file is empty.");
        }

        List<Map<String, Object>> results = new ArrayList<>();
        char delimiter = (config != null) ? config.getCsvDelimiter() : ',';
        CSVFormat csvFormat = CSVFormat.Builder.create(CSVFormat.DEFAULT)
                .setDelimiter(delimiter)
                .setHeader()
                .setSkipHeaderRecord(true)
                .setTrim(true)
                .build();

        Charset encoding = (config != null) ? Charset.forName(config.getCsvEncoding()) : StandardCharsets.UTF_8;
        try (Reader reader = new FileReader(file, encoding);
             CSVParser parser = new CSVParser(reader, csvFormat)) {

            Map<String, Integer> headerMap = parser.getHeaderMap();
            if (headerMap == null || headerMap.isEmpty()) {
                throw new CsvParseException("CSV file is missing headers.");
            }

            // Check for empty or blank headers
            for (String header : headerMap.keySet()) {
                if (header == null || header.trim().isEmpty()) {
                    throw new CsvParseException("CSV file contains empty or blank header names.");
                }
            }

            for (CSVRecord record : parser) {
                if (!record.isConsistent()) {
                    throw new CsvParseException("Malformed row at record " + record.getRecordNumber()
                            + ": Column count (" + record.size() + ") does not match header count (" + headerMap.size() + ").");
                }

                Map<String, Object> row = new LinkedHashMap<>();
                for (String header : headerMap.keySet()) {
                    String rawValue = record.get(header);
                    Object parsedValue = (config != null && config.isInferTypes()) ? inferType(rawValue) : rawValue;
                    row.put(header, parsedValue);
                }
                results.add(row);
            }
        } catch (IOException e) {
            throw new CsvParseException("IO error reading CSV file: " + e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            throw new CsvParseException("Invalid CSV structure or duplicate/malformed headers: " + e.getMessage(), e);
        }

        return results;
    }

    private Object inferType(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        String trimmed = value.trim();

        // Boolean Inference
        if ("true".equalsIgnoreCase(trimmed)) return Boolean.TRUE;
        if ("false".equalsIgnoreCase(trimmed)) return Boolean.FALSE;

        // Integer/Long Inference
        if (trimmed.matches("-?\\d+")) {
            // Prevent stripping leading zeros in codes/ZIPs (e.g. "02138" shouldn't become 2138)
            if (trimmed.length() > 1 && trimmed.startsWith("0") && !trimmed.startsWith("-")) {
                return trimmed;
            }
            try {
                return Long.parseLong(trimmed);
            } catch (NumberFormatException e) {
                return trimmed; // fallback to string if number exceeds Long capacity
            }
        }

        // Decimal/Double Inference
        if (trimmed.matches("-?\\d*\\.\\d+")) {
            try {
                return Double.parseDouble(trimmed);
            } catch (NumberFormatException e) {
                return trimmed;
            }
        }

        return value;
    }
}
