package com.example.csvjson;

import com.example.csvjson.config.AppConfig;
import com.example.csvjson.processor.BatchProcessor;
import com.example.csvjson.service.*;
import com.example.csvjson.util.Constants;

/**
 * Main application class and entry point.
 */
public class CsvJsonApplication {

    public static void main(String[] args) {
        boolean retryMode = false;

        // Parse command line arguments
        if (args != null) {
            for (String arg : args) {
                if (Constants.ARG_RETRY_FAILED.equalsIgnoreCase(arg)) {
                    retryMode = true;
                    break;
                }
            }
        }

        try {
            // Initialize configurations and services
            AppConfig config = new AppConfig();
            LogService logService = new LogService(config);
            FileScannerService scannerService = new FileScannerService(config);
            CsvParserService parserService = new CsvParserService();
            JsonWriterService jsonWriterService = new JsonWriterService(config);
            MetadataService metadataService = new MetadataService(config);

            if (retryMode) {
                RetryService retryService = new RetryService(
                        config, scannerService, parserService, jsonWriterService, metadataService, logService);
                retryService.retryFailedFiles();
            } else {
                BatchProcessor batchProcessor = new BatchProcessor(
                        config, scannerService, parserService, jsonWriterService, metadataService, logService);
                batchProcessor.process();
            }
        } catch (Exception e) {
            System.err.println("Fatal Application Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
