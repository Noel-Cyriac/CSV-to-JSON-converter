# Implementation Plan: 5. Multi-threaded Processing & Append-Only Ledger

This feature enables concurrent execution of CSV parsing tasks using a thread pool. To achieve safe, high-performance execution without file locking issues, we refactor the metadata serialization ledger to be append-only.

## User Review Required
> [!IMPORTANT]
> **Append-Only Metadata Ledger**:
> The `MetadataService` will shift from overwriting `processed-files.csv` (which had high locking overhead) to an append-only model. On startup, the ledger is parsed sequentially to load the initial status of each file in-memory. During processing, new records are simply appended to the end of the file.

---

## Proposed Changes

### 1. Configuration Setup
In `config/application.properties`:
```properties
processing.threads=4
```

In `AppConfig.java`:
```java
private int processingThreads = 4;

// In loadProperties():
try {
    this.processingThreads = Integer.parseInt(properties.getProperty("processing.threads", "4"));
} catch (NumberFormatException e) {
    this.processingThreads = 4;
}

// Getter:
public int getProcessingThreads() {
    return processingThreads;
}
```

### 2. Thread-Safe Cache & Append-Only Ledger
In `MetadataService.java`:
* Update cache map:
```java
private final Map<String, FileMetadata> metadataCache = new ConcurrentHashMap<>();
```
* Add append-only write method and remove full-file overwrite:
```java
private synchronized void appendMetadataRecord(FileMetadata metadata) {
    boolean writeHeader = !metadataFile.exists() || metadataFile.length() == 0;
    CSVFormat csvFormat = CSVFormat.Builder.create(CSVFormat.DEFAULT)
            .setHeader(Constants.METADATA_HEADERS)
            .setSkipHeaderRecord(!writeHeader)
            .build();

    try (FileWriter writer = new FileWriter(metadataFile, true);
         CSVPrinter printer = new CSVPrinter(writer, csvFormat)) {
        
        printer.printRecord(
                metadata.getFilename(),
                metadata.getStatus(),
                metadata.getRetryCount(),
                metadata.getLastAttempt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        );
        printer.flush();
    } catch (IOException e) {
        System.err.println("Error: Failed to append metadata to ledger: " + e.getMessage());
    }
}
```
* Update mutators to utilize the append method:
```java
public void markSuccess(String fileName, int retryCount, LocalDateTime lastAttempt) {
    FileMetadata metadata = new FileMetadata(fileName, Constants.STATUS_SUCCESS, retryCount, lastAttempt);
    metadataCache.put(fileName, metadata);
    appendMetadataRecord(metadata);
}

public void markFailure(String fileName, int retryCount, LocalDateTime lastAttempt) {
    FileMetadata metadata = new FileMetadata(fileName, Constants.STATUS_FAILED, retryCount, lastAttempt);
    metadataCache.put(fileName, metadata);
    appendMetadataRecord(metadata);
}

public void markPermanentFailure(String fileName, int retryCount, LocalDateTime lastAttempt) {
    FileMetadata metadata = new FileMetadata(fileName, Constants.STATUS_PERMANENT_FAILURE, retryCount, lastAttempt);
    metadataCache.put(fileName, metadata);
    appendMetadataRecord(metadata);
}
```

### 3. Multi-threaded Processing
In `BatchProcessor.java`, execute jobs concurrently:
```java
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public void process() {
    logService.logInfo("Application Started");
    List<File> files = scannerService.getNewFiles();
    logService.logInfo("Found " + files.size() + " CSV files");

    ExecutorService executor = Executors.newFixedThreadPool(config.getProcessingThreads());

    for (File file : files) {
        String filename = file.getName();
        if (metadataService.alreadyProcessed(filename)) {
            logService.logInfo("Skipping " + filename + " (already successfully processed or permanently failed)");
            continue;
        }

        executor.submit(() -> {
            logService.logInfo("Processing " + filename);
            try {
                List<Map<String, Object>> parsedData = parserService.parse(file, config);
                jsonWriterService.writeJson(filename, parsedData);
                FileUtils.moveFile(file, config.getProcessedDir());
                metadataService.markSuccess(filename, 0, LocalDateTime.now());

                logService.logInfo(filename + " converted successfully");
                logService.saveResult(new ProcessingResult(
                        filename,
                        Constants.STATUS_SUCCESS,
                        "Converted successfully",
                        LocalDateTime.now()
                ));
            } catch (Exception e) {
                logService.logError(filename + " failed", e);
                try {
                    FileUtils.moveFile(file, config.getFailedDir());
                    metadataService.markFailure(filename, 1, LocalDateTime.now());
                } catch (Exception moveEx) {
                    logService.logError("Failed to move failed file: " + filename, moveEx);
                }
                logService.saveResult(new ProcessingResult(
                        filename,
                        Constants.STATUS_FAILED,
                        e.getMessage(),
                        LocalDateTime.now()
                ));
            }
        });
    }

    executor.shutdown();
    try {
        if (!executor.awaitTermination(30, TimeUnit.MINUTES)) {
            logService.logError("Timeout waiting for file conversion tasks to complete.");
        }
    } catch (InterruptedException e) {
        logService.logError("Execution interrupted", e);
        Thread.currentThread().interrupt();
    }
    logService.logInfo("Application Finished");
}
```

---

## Verification Plan

### Automated Tests
* Run concurrent write tests in `MetadataServiceTest.java` invoking `markSuccess` from 10 distinct threads simultaneously, verifying that the cache stays accurate and the ledger file contains exactly 10 lines.

### Manual Verification
* Place 20 CSV files in the input directory. Run the utility and confirm in the execution log that files are processed in an interleaved (non-sequential) order.
