package com.example.csvjson.service;

import com.example.csvjson.config.AppConfig;
import com.example.csvjson.model.FileMetadata;
import com.example.csvjson.util.Constants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class MetadataServiceTest {

    private AppConfig stubConfig;
    private MetadataService metadataService;

    @BeforeEach
    public void setUp(@TempDir Path tempDir) {
        // Stub AppConfig to redirect metadataDir to the temp directory
        stubConfig = new AppConfig() {
            @Override
            public File getMetadataDir() {
                return tempDir.toFile();
            }
        };
        metadataService = new MetadataService(stubConfig);
    }

    @Test
    public void testEmptyMetadataLoad() {
        Map<String, FileMetadata> metadata = metadataService.loadMetadata();
        assertTrue(metadata.isEmpty(), "Metadata should be empty initially");
    }

    @Test
    public void testMarkSuccessAndCheck() {
        String filename = "test.csv";
        assertFalse(metadataService.alreadyProcessed(filename));

        LocalDateTime now = LocalDateTime.now();
        metadataService.markSuccess(filename, 0, now);

        assertTrue(metadataService.alreadyProcessed(filename));
        FileMetadata meta = metadataService.getMetadata(filename);
        assertNotNull(meta);
        assertEquals(Constants.STATUS_SUCCESS, meta.getStatus());
        assertEquals(0, meta.getRetryCount());
    }

    @Test
    public void testMarkFailureAndCheck() {
        String filename = "fail.csv";
        assertFalse(metadataService.alreadyProcessed(filename));

        LocalDateTime now = LocalDateTime.now();
        metadataService.markFailure(filename, 1, now);

        // A failed file is NOT "already processed" since it needs retry
        assertFalse(metadataService.alreadyProcessed(filename));

        FileMetadata meta = metadataService.getMetadata(filename);
        assertNotNull(meta);
        assertEquals(Constants.STATUS_FAILED, meta.getStatus());
        assertEquals(1, meta.getRetryCount());
    }

    @Test
    public void testIncrementRetry() {
        String filename = "retry.csv";
        metadataService.incrementRetry(filename); // First fail -> count 1

        FileMetadata meta = metadataService.getMetadata(filename);
        assertNotNull(meta);
        assertEquals(1, meta.getRetryCount());
        assertEquals(Constants.STATUS_FAILED, meta.getStatus());

        metadataService.incrementRetry(filename); // Second fail -> count 2
        meta = metadataService.getMetadata(filename);
        assertEquals(2, meta.getRetryCount());
    }

    @Test
    public void testLoadFromExistingFile(@TempDir Path tempDir) throws IOException {
        File metaFile = new File(tempDir.toFile(), "processed-files.csv");
        try (FileWriter writer = new FileWriter(metaFile)) {
            writer.write("filename,status,retryCount,lastAttempt\n");
            writer.write("file1.csv,SUCCESS,0,2026-06-24T10:00:00\n");
            writer.write("file2.csv,FAILED,2,2026-06-24T11:00:00\n");
        }

        // Setup service with existing file
        AppConfig customConfig = new AppConfig() {
            @Override
            public File getMetadataDir() {
                return tempDir.toFile();
            }
        };
        MetadataService service = new MetadataService(customConfig);
        Map<String, FileMetadata> metadataMap = service.loadMetadata();

        assertEquals(2, metadataMap.size());
        
        FileMetadata f1 = metadataMap.get("file1.csv");
        assertNotNull(f1);
        assertEquals(Constants.STATUS_SUCCESS, f1.getStatus());
        assertEquals(0, f1.getRetryCount());

        FileMetadata f2 = metadataMap.get("file2.csv");
        assertNotNull(f2);
        assertEquals(Constants.STATUS_FAILED, f2.getStatus());
        assertEquals(2, f2.getRetryCount());
    }
}
