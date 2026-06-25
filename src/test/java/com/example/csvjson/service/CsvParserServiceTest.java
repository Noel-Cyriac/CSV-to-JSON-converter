package com.example.csvjson.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class CsvParserServiceTest {

    private CsvParserService parserService;

    @BeforeEach
    public void setUp() {
        parserService = new CsvParserService();
    }

    @Test
    public void testParseValidCsv(@TempDir Path tempDir) throws IOException, CsvParseException {
        File csvFile = tempDir.resolve("valid.csv").toFile();
        try (FileWriter writer = new FileWriter(csvFile)) {
            writer.write("id,name,age\n");
            writer.write("1,John,25\n");
            writer.write("2,Mary,30\n");
        }

        List<Map<String, String>> results = parserService.parse(csvFile);

        assertEquals(2, results.size());
        
        Map<String, String> row1 = results.get(0);
        assertEquals("1", row1.get("id"));
        assertEquals("John", row1.get("name"));
        assertEquals("25", row1.get("age"));

        Map<String, String> row2 = results.get(1);
        assertEquals("2", row2.get("id"));
        assertEquals("Mary", row2.get("name"));
        assertEquals("30", row2.get("age"));
    }

    @Test
    public void testParseEmptyFile(@TempDir Path tempDir) throws IOException {
        File csvFile = tempDir.resolve("empty.csv").toFile();
        csvFile.createNewFile(); // 0 bytes

        CsvParseException exception = assertThrows(CsvParseException.class, () -> {
            parserService.parse(csvFile);
        });
        assertTrue(exception.getMessage().contains("empty"), "Exception message should mention file is empty");
    }

    @Test
    public void testParseMissingHeaders(@TempDir Path tempDir) throws IOException {
        File csvFile = tempDir.resolve("no_headers.csv").toFile();
        try (FileWriter writer = new FileWriter(csvFile)) {
            writer.write("\n\n");
        }

        assertThrows(CsvParseException.class, () -> {
            parserService.parse(csvFile);
        });
    }

    @Test
    public void testParseBlankHeaderName(@TempDir Path tempDir) throws IOException {
        File csvFile = tempDir.resolve("blank_header.csv").toFile();
        try (FileWriter writer = new FileWriter(csvFile)) {
            writer.write("id,,age\n");
            writer.write("1,John,25\n");
        }

        CsvParseException exception = assertThrows(CsvParseException.class, () -> {
            parserService.parse(csvFile);
        });
        String msg = exception.getMessage().toLowerCase();
        assertTrue(msg.contains("blank") || msg.contains("empty") || msg.contains("missing"),
                "Exception should complain about blank or missing header name. Got: " + exception.getMessage());
    }

    @Test
    public void testParseMalformedRow(@TempDir Path tempDir) throws IOException {
        File csvFile = tempDir.resolve("malformed.csv").toFile();
        try (FileWriter writer = new FileWriter(csvFile)) {
            writer.write("id,name,age\n");
            writer.write("1,John,25\n");
            writer.write("2,Mary\n"); // Missing age column
        }

        CsvParseException exception = assertThrows(CsvParseException.class, () -> {
            parserService.parse(csvFile);
        });
        assertTrue(exception.getMessage().contains("Malformed row") || exception.getMessage().contains("Column count"), 
                "Exception should complain about column count mismatch");
    }
}
