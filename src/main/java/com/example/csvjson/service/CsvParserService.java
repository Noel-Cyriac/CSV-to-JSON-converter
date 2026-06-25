package com.example.csvjson.service;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.File;
import java.io.FileReader;
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
     * @param file The CSV file to parse.
     * @return List of rows, where each row is a map from header to cell value.
     * @throws CsvParseException If the file is empty, headers are missing, rows are malformed, or CSV is invalid.
     */
    public List<Map<String, String>> parse(File file) throws CsvParseException {
        if (file == null) {
            throw new CsvParseException("File reference is null.");
        }
        if (!file.exists()) {
            throw new CsvParseException("File does not exist: " + file.getAbsolutePath());
        }
        if (file.length() == 0) {
            throw new CsvParseException("CSV file is empty.");
        }

        List<Map<String, String>> results = new ArrayList<>();
        CSVFormat csvFormat = CSVFormat.Builder.create(CSVFormat.DEFAULT)
                .setHeader()
                .setSkipHeaderRecord(true)
                .setTrim(true)
                .build();

        try (Reader reader = new FileReader(file);
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

                Map<String, String> row = new LinkedHashMap<>();
                for (String header : headerMap.keySet()) {
                    row.put(header, record.get(header));
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
}
