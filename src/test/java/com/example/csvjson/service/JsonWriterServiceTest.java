package com.example.csvjson.service;

import com.example.csvjson.config.AppConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class JsonWriterServiceTest {

    @Test
    public void testPrettyPrintJson(@TempDir Path tempDir) throws IOException {
        AppConfig stubConfig = new AppConfig() {
            @Override
            public File getOutputDir() {
                return tempDir.toFile();
            }
            @Override
            public boolean isJsonPretty() {
                return true;
            }
        };

        JsonWriterService writerService = new JsonWriterService(stubConfig);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("name", "Alice");
        data.put("age", 30);

        writerService.writeJson("test_pretty.json", data);

        File outputFile = tempDir.resolve("test_pretty.json").toFile();
        assertTrue(outputFile.exists());

        String content = Files.readString(outputFile.toPath());
        // A pretty printed JSON should span multiple lines (have newlines)
        assertTrue(content.contains("\n") || content.contains("\r\n"), "Pretty printed JSON should contain newlines");
        assertTrue(content.contains("  \"name\""), "Pretty printed JSON should contain indentation spaces");
    }

    @Test
    public void testCompactJson(@TempDir Path tempDir) throws IOException {
        AppConfig stubConfig = new AppConfig() {
            @Override
            public File getOutputDir() {
                return tempDir.toFile();
            }
            @Override
            public boolean isJsonPretty() {
                return false;
            }
        };

        JsonWriterService writerService = new JsonWriterService(stubConfig);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("name", "Bob");
        data.put("age", 25);

        writerService.writeJson("test_compact.json", data);

        File outputFile = tempDir.resolve("test_compact.json").toFile();
        assertTrue(outputFile.exists());

        String content = Files.readString(outputFile.toPath()).trim();
        // A compact JSON should be on a single line (no newlines inside the JSON structure)
        assertFalse(content.contains("\n"), "Compact JSON should not contain newlines");
        assertFalse(content.contains("\r"), "Compact JSON should not contain carriage returns");
        assertEquals("{\"name\":\"Bob\",\"age\":25}", content);
    }
}
