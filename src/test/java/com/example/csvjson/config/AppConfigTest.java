package com.example.csvjson.config;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.io.File;

public class AppConfigTest {

    @Test
    public void testDefaultConfigLoading() {
        AppConfig config = new AppConfig();
        
        assertNotNull(config.getInputDir(), "Input directory should not be null");
        assertNotNull(config.getOutputDir(), "Output directory should not be null");
        assertNotNull(config.getProcessedDir(), "Processed directory should not be null");
        assertNotNull(config.getFailedDir(), "Failed directory should not be null");
        assertNotNull(config.getLogsDir(), "Logs directory should not be null");
        assertNotNull(config.getMetadataDir(), "Metadata directory should not be null");
        
    }
}
