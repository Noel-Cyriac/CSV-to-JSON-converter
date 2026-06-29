# Implementation Plan: CSV to JSON Converter Enhancements

This document outlines the detailed design and implementation steps for adding CSV delimiter configuration, character encoding support, JSON formatting toggles, data type inference (auto-typing), and multi-threaded processing to the CSV-to-JSON batch converter.

---

## User Review Required

> [!IMPORTANT]
> **Metadata Ledger Behavior Change**:
> The `MetadataService` will shift from overwriting `processed-files.csv` (which had $O(N)$ write cost and high locking overhead) to an **append-only ledger** model. On startup, the ledger is parsed sequentially to load the initial status of each file in-memory. During processing, new records are simply appended to the end of the file. This is crucial for multi-threaded performance.

---

## Open Questions

> [!NOTE]
> * **Data Type Inference Scope**: Should numeric values with leading zeros (e.g. phone numbers or postal codes like `02138`) be kept as Strings or parsed as numbers? *Recommendation: If a numeric string has a length > 1 and starts with `0`, keep it as a String to prevent data loss (e.g., converting `02138` to `2138`).*
> * **Tab Characters in Delimiters**: Properties files do not support raw tabs easily. We will support the special string value `\t` in the properties file and map it to the character `\t` in Java.

---

## Feature-by-Feature Detailed Code Changes

### 1. Data Type Inference (Auto-Typing)
**Goal**: Convert CSV cell values (which are strings) to appropriate JSON native data types (Numbers, Booleans, Nulls) instead of encoding everything as a String.

#### API Changes
We will update the return signature of `CsvParserService.parse` from `List<Map<String, String>>` to `List<Map<String, Object>>` to allow heterogeneous object mapping.

#### Code Changes in `CsvParserService.java`
```java
// Under: package com.example.csvjson.service;

// 1. Change method signature:
public List<Map<String, Object>> parse(File file, AppConfig config) throws CsvParseException {
    // ... validation checks ...

    List<Map<String, Object>> results = new ArrayList<>();
    
    // ... parser initialization ...
    
    for (CSVRecord record : parser) {
        if (!record.isConsistent()) {
            throw new CsvParseException("Malformed row at record " + record.getRecordNumber());
        }

        Map<String, Object> row = new LinkedHashMap<>();
        for (String header : headerMap.keySet()) {
            String rawValue = record.get(header);
            Object parsedValue = config.isInferTypes() ? inferType(rawValue) : rawValue;
            row.put(header, parsedValue);
        }
        results.add(row);
    }
    return results;
}

// 2. Add helper method:
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
```

---

### 2. Configurable CSV Delimiter
**Goal**: Allow the CSV delimiter to be loaded from `application.properties` so the utility can process Tab-separated, Semicolon-separated, or Comma-separated files.

#### Code Changes in `AppConfig.java`
```java
private char csvDelimiter = ',';

// In loadProperties():
String delimProp = properties.getProperty("csv.delimiter", ",");
if ("\\t".equals(delimProp) || "\t".equals(delimProp)) {
    this.csvDelimiter = '\t';
} else if (!delimProp.isEmpty()) {
    this.csvDelimiter = delimProp.charAt(0);
}

// Getter:
public char getCsvDelimiter() {
    return csvDelimiter;
}
```

#### Code Changes in `CsvParserService.java`
Configure the `CSVFormat` builder with the dynamic delimiter:
```java
CSVFormat csvFormat = CSVFormat.Builder.create(CSVFormat.DEFAULT)
        .setDelimiter(config.getCsvDelimiter())
        .setHeader()
        .setSkipHeaderRecord(true)
        .setTrim(true)
        .build();
```

---

### 3. Configurable CSV File Encoding
**Goal**: Explicitly define the Charset used to read CSV files to prevent platform-dependent decoding issues.

#### Code Changes in `AppConfig.java`
```java
private String csvEncoding = "UTF-8";

// In loadProperties():
this.csvEncoding = properties.getProperty("csv.encoding", "UTF-8");

// Getter:
public String getCsvEncoding() {
    return csvEncoding;
}
```

#### Code Changes in `CsvParserService.java`
```java
// Open FileReader using Java 21 charset configuration:
try (Reader reader = new FileReader(file, Charset.forName(config.getCsvEncoding()));
     CSVParser parser = new CSVParser(reader, csvFormat)) {
     // ... parsing logic ...
}
```

---

### 4. Toggling Compact vs. Pretty-Printed JSON
**Goal**: Enable minifying the JSON output to reduce file sizes for server-to-server data exchange.

#### Code Changes in `AppConfig.java`
```java
private boolean jsonPretty = true;

// In loadProperties():
this.jsonPretty = Boolean.parseBoolean(properties.getProperty("json.pretty", "true"));

// Getter:
public boolean isJsonPretty() {
    return jsonPretty;
}
```

#### Code Changes in `JsonWriterService.java`
```java
public JsonWriterService(AppConfig config) {
    this.config = config;
    this.objectMapper = new ObjectMapper();
    if (config.isJsonPretty()) {
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    } else {
        this.objectMapper.disable(SerializationFeature.INDENT_OUTPUT);
    }
}
```

---

### 5. Multi-threaded Processing
**Goal**: Convert sequential iteration to concurrent task execution. Introduce an append-only metadata ledger to prevent file-locking conflicts.

#### Code Changes in `MetadataService.java`
```java
// 1. Change cache type
private final Map<String, FileMetadata> metadataCache = new ConcurrentHashMap<>();

// 2. Replace saveAllMetadata() with appendMetadataRecord()
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

// 3. Update mutator methods to use append
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

#### Code Changes in `BatchProcessor.java`
```java
public void process() {
    logService.logInfo("Application Started");
    List<File> files = scannerService.getNewFiles();
    logService.logInfo("Found " + files.size() + " CSV files");

    // Initialize thread pool based on config
    ExecutorService executor = Executors.newFixedThreadPool(config.getProcessingThreads());

    for (File file : files) {
        String filename = file.getName();

        if (metadataService.alreadyProcessed(filename)) {
            logService.logInfo("Skipping " + filename + " (already successfully processed or permanently failed)");
            continue;
        }

        // Submit task to thread pool
        executor.submit(() -> {
            logService.logInfo("Processing " + filename);
            try {
                // Parse CSV
                List<Map<String, Object>> parsedData = parserService.parse(file, config);

                // Write JSON
                jsonWriterService.writeJson(filename, parsedData);

                // Move file
                FileUtils.moveFile(file, config.getProcessedDir());

                // Metadata success
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

    // Await thread completion
    executor.shutdown();
    try {
        if (!executor.awaitTermination(30, TimeUnit.MINUTES)) {
            logService.logError("Timeout: Batch processing did not finish within 30 minutes.");
        }
    } catch (InterruptedException e) {
        logService.logError("Batch processing execution was interrupted.", e);
        Thread.currentThread().interrupt();
    }

    logService.logInfo("Application Finished");
}
```

---

## Verification Plan

### Automated Tests
Run Maven unit test suite to verify no regressions in basic loading, parsing, and writing logic:
```bash
mvn clean test
```
* **New Tests to Add**:
  * `CsvParserServiceTest`: Test delimiter override (semicolon, tab).
  * `CsvParserServiceTest`: Test parsing integers, decimals, booleans, and strings with leading zeros.
  * `MetadataServiceTest`: Verify that metadata works seamlessly with concurrent threads updating status.

### Manual Verification
1. Place 3 input CSV files in `input/` (a semicolon separated file, a standard comma file, and a malformed file).
2. Configure `processing.threads=4`, `csv.delimiter=;`, and `json.infer.types=true`.
3. Execute the converter and verify the outputs in `output/` contain correct JSON representations (non-string formats for numbers, correct delimiting).
4. Verify the `processed-files.csv` ledger contains correct append records.
