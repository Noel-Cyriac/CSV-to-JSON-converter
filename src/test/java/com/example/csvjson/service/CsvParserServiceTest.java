package com.example.csvjson.service;

import com.example.csvjson.config.AppConfig;
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
    public void testParseValidCsvNoInference(@TempDir Path tempDir) throws IOException, CsvParseException {
        File csvFile = tempDir.resolve("valid.csv").toFile();
        try (FileWriter writer = new FileWriter(csvFile)) {
            writer.write("id,name,age\n");
            writer.write("1,John,25\n");
            writer.write("2,Mary,30\n");
        }

        // Passing null disables type inference
        List<Map<String, Object>> results = parserService.parse(csvFile, null);

        assertEquals(2, results.size());
        
        Map<String, Object> row1 = results.get(0);
        assertEquals("1", row1.get("id"));
        assertEquals("John", row1.get("name"));
        assertEquals("25", row1.get("age"));

        Map<String, Object> row2 = results.get(1);
        assertEquals("2", row2.get("id"));
        assertEquals("Mary", row2.get("name"));
        assertEquals("30", row2.get("age"));
    }

    @Test
    public void testParseTypeInference(@TempDir Path tempDir) throws IOException, CsvParseException {
        File csvFile = tempDir.resolve("inference.csv").toFile();
        try (FileWriter writer = new FileWriter(csvFile)) {
            writer.write("id,name,age,active,price,zip\n");
            writer.write("101,John,25,true,89.99,02138\n");
            writer.write("102,Mary,30,false,120.0,00912\n");
        }

        // Create a stub AppConfig with inferTypes = true
        AppConfig stubConfig = new AppConfig() {
            @Override
            public boolean isInferTypes() {
                return true;
            }
            @Override
            public char getCsvDelimiter() {
                return ',';
            }
        };

        List<Map<String, Object>> results = parserService.parse(csvFile, stubConfig);

        assertEquals(2, results.size());
        
        Map<String, Object> row1 = results.get(0);
        assertEquals(101L, row1.get("id")); // Parsed as Long
        assertEquals("John", row1.get("name")); // Remains String
        assertEquals(25L, row1.get("age")); // Parsed as Long
        assertEquals(Boolean.TRUE, row1.get("active")); // Parsed as Boolean
        assertEquals(89.99, row1.get("price")); // Parsed as Double
        assertEquals("02138", row1.get("zip")); // Retains leading zero as String

        Map<String, Object> row2 = results.get(1);
        assertEquals(102L, row2.get("id"));
        assertEquals("Mary", row2.get("name"));
        assertEquals(30L, row2.get("age"));
        assertEquals(Boolean.FALSE, row2.get("active"));
        assertEquals(120.0, row2.get("price"));
        assertEquals("00912", row2.get("zip"));
    }

    @Test
    public void testParseEmptyFile(@TempDir Path tempDir) throws IOException {
        File csvFile = tempDir.resolve("empty.csv").toFile();
        csvFile.createNewFile(); // 0 bytes

        CsvParseException exception = assertThrows(CsvParseException.class, () -> {
            parserService.parse(csvFile, null);
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
            parserService.parse(csvFile, null);
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
            parserService.parse(csvFile, null);
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
            parserService.parse(csvFile, null);
        });
        assertTrue(exception.getMessage().contains("Malformed row") || exception.getMessage().contains("Column count"), 
                "Exception should complain about column count mismatch");
    }

    @Test
    public void testParseSemicolonDelimiter(@TempDir Path tempDir) throws IOException, CsvParseException {
        File csvFile = tempDir.resolve("semicolon.csv").toFile();
        try (FileWriter writer = new FileWriter(csvFile)) {
            writer.write("id;name;age\n");
            writer.write("1;John;25\n");
            writer.write("2;Mary;30\n");
        }

        AppConfig stubConfig = new AppConfig() {
            @Override
            public char getCsvDelimiter() {
                return ';';
            }
            @Override
            public boolean isInferTypes() {
                return false;
            }
        };

        List<Map<String, Object>> results = parserService.parse(csvFile, stubConfig);

        assertEquals(2, results.size());
        
        Map<String, Object> row1 = results.get(0);
        assertEquals("1", row1.get("id"));
        assertEquals("John", row1.get("name"));
        assertEquals("25", row1.get("age"));
    }

    @Test
    public void testParseTabDelimiter(@TempDir Path tempDir) throws IOException, CsvParseException {
        File csvFile = tempDir.resolve("tab.csv").toFile();
        try (FileWriter writer = new FileWriter(csvFile)) {
            writer.write("id\tname\tage\n");
            writer.write("1\tJohn\t25\n");
            writer.write("2\tMary\t30\n");
        }

        AppConfig stubConfig = new AppConfig() {
            @Override
            public char getCsvDelimiter() {
                return '\t';
            }
            @Override
            public boolean isInferTypes() {
                return false;
            }
        };

        List<Map<String, Object>> results = parserService.parse(csvFile, stubConfig);

        assertEquals(2, results.size());
        
        Map<String, Object> row1 = results.get(0);
        assertEquals("1", row1.get("id"));
        assertEquals("John", row1.get("name"));
        assertEquals("25", row1.get("age"));
    }

    @Test
    public void testParseUtf8Encoding(@TempDir Path tempDir) throws IOException, CsvParseException {
        File csvFile = tempDir.resolve("utf8.csv").toFile();
        try (java.io.OutputStreamWriter writer = new java.io.OutputStreamWriter(
                new java.io.FileOutputStream(csvFile), java.nio.charset.StandardCharsets.UTF_8)) {
            writer.write("id,name,symbol\n");
            writer.write("1,Café,€\n");
        }

        AppConfig stubConfig = new AppConfig() {
            @Override
            public String getCsvEncoding() {
                return "UTF-8";
            }
            @Override
            public char getCsvDelimiter() {
                return ',';
            }
        };

        List<Map<String, Object>> results = parserService.parse(csvFile, stubConfig);
        assertEquals(1, results.size());
        Map<String, Object> row = results.get(0);
        assertEquals("Café", row.get("name"));
        assertEquals("€", row.get("symbol"));
    }

    @Test
    public void testParseIso88591Encoding(@TempDir Path tempDir) throws IOException, CsvParseException {
        File csvFile = tempDir.resolve("iso.csv").toFile();
        try (java.io.OutputStreamWriter writer = new java.io.OutputStreamWriter(
                new java.io.FileOutputStream(csvFile), java.nio.charset.StandardCharsets.ISO_8859_1)) {
            writer.write("id,name,symbol\n");
            writer.write("1,Café,£\n");
        }

        AppConfig stubConfig = new AppConfig() {
            @Override
            public String getCsvEncoding() {
                return "ISO-8859-1";
            }
            @Override
            public char getCsvDelimiter() {
                return ',';
            }
        };

        List<Map<String, Object>> results = parserService.parse(csvFile, stubConfig);
        assertEquals(1, results.size());
        Map<String, Object> row = results.get(0);
        assertEquals("Café", row.get("name"));
        assertEquals("£", row.get("symbol"));
    }
}
