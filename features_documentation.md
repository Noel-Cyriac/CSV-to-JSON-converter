# CSV to JSON Converter - Features Documentation

This document describes all the core features implemented in this project, outlining their code ranges, configuration limits, error responses, selected inbuilt functions with rationale, and the unit tests validating them.

---

## 1. Directory & Configuration Initialization
*   **File**: [`src/main/java/com/example/csvjson/config/AppConfig.java`](file:///home/noelcyriac/Documents/June_Projects/CSV-to-JSON-converter/src/main/java/com/example/csvjson/config/AppConfig.java)
*   **Lines**: `1` to `137`
*   **Description**: Loads configuration properties from `config/application.properties` (falling back to program defaults if missing or invalid) and initializes all workspace directories.

### Limits (Max / Min)
*   **Config File Path**: Fixed to `config/application.properties` (defined in `Constants.java:16`).
*   **Thread Count (`processing.threads`)**:
    *   *Min*: `1` (implicitly, if configured). Falls back to standard default of `4` if parsing fails.
    *   *Max*: No upper bound enforced by code; bounded only by JVM resources.
*   **CSV Delimiter**: Expected to be a single character. Defaults to `,`.
*   **CSV Encoding**: Defaults to `UTF-8`. Must be a valid charset name.

### Error Messages Generated
*   `Warning: Could not read config/application.properties. Using defaults. Error: [message]` (Line 44)
*   `Config file not found at config/application.properties. Using default directories.` (Line 47)
*   `Fatal error: Could not initialize directories. [message]` (Line 88)

### Inbuilt Functions Used & Selection Rationale
*   `Properties.load(InputStream)`: Standard Java API to parse `.properties` key-value pairs without manual file scanning.
*   `Boolean.parseBoolean(String)`: Safe conversion of configuration strings into primitive boolean flags (`inferTypes`, `jsonPretty`).
*   `Integer.parseInt(String)`: Converts string properties to numeric type for pool sizes; wrapped in try-catch to enable fallback defaults if format is incorrect.
*   `String.charAt(0)`: Extracts the first character from the delimiter property string to form the `char` required by the CSV library.

### Tests Written
*   [`src/test/java/com/example/csvjson/config/AppConfigTest.java`](file:///home/noelcyriac/Documents/June_Projects/CSV-to-JSON-converter/src/test/java/com/example/csvjson/config/AppConfigTest.java) (`1` to `22`):
    *   `testDefaultConfigLoading`: Asserts directories are non-null and correctly initialized, and that the default CSV delimiter is configured.

---

## 2. File System Operations & Directory Validation
*   **File**: [`src/main/java/com/example/csvjson/util/FileUtils.java`](file:///home/noelcyriac/Documents/June_Projects/CSV-to-JSON-converter/src/main/java/com/example/csvjson/util/FileUtils.java)
*   **Lines**: `1` to `69`
*   **Description**: Provides atomic file moves, case-insensitive extension matching, and strict directory read/write capability assertions.

### Limits (Max / Min)
*   **Directory Permissions**: Checks for read (`.canRead()`) and write (`.canWrite()`) permissions.
*   **File Move Mode**: Uses `StandardCopyOption.REPLACE_EXISTING` (overwrites existing files in target destination folder).

### Error Messages Generated
*   `Failed to create directory: [absolutePath]` (Line 28)
*   `Path exists but is not a directory: [absolutePath]` (Line 32)
*   `Insufficient permissions for directory: [absolutePath]` (Line 35)
*   `Source file does not exist.` (Line 48)

### Inbuilt Functions Used & Selection Rationale
*   `File.mkdirs()`: Automatically creates nested/parent directories in a single operation if they do not exist.
*   `File.isDirectory()`, `File.canRead()`, `File.canWrite()`: Robust OS-level checks to verify folder status and read/write security credentials prior to processing.
*   `Files.move(Path, Path, CopyOption...)`: Standard modern Java NIO framework for thread-safe/atomic file system relocation.
*   `String.toLowerCase()`, `String.endsWith()`: Normalizes and checks extensions case-insensitively.

### Tests Written
No isolated test class was created for `FileUtils`. The utility is transitively covered by all other integration test suites (e.g., `MetadataServiceTest`, `CsvParserServiceTest`) that handle actual folders and files on disk.

---

## 3. Directory File Scanner
*   **File**: [`src/main/java/com/example/csvjson/service/FileScannerService.java`](file:///home/noelcyriac/Documents/June_Projects/CSV-to-JSON-converter/src/main/java/com/example/csvjson/service/FileScannerService.java)
*   **Lines**: `1` to `62`
*   **Description**: Scans configured directories (e.g., input and failed folders) to collect files matching the `.csv` extension.

### Limits (Max / Min)
*   *Min*: `0` files (returns empty collection).
*   *Max*: Boundless (limited only by memory and the file system).
*   **File Filtering**: Strictly matches files with the `.csv` extension (case-insensitive).

### Error Messages Generated
No error exceptions are thrown. If directories do not exist or are inaccessible, it handles them gracefully by returning `Collections.emptyList()` (Lines 44-46, 49-51).

### Inbuilt Functions Used & Selection Rationale
*   `File.listFiles()`: Native filesystem query to fetch files inside a target folder.
*   `Collections.emptyList()`: Avoids heap allocations when scanning empty folders.
*   `ArrayList.add(File)`: Collects matching file references dynamically.

### Tests Written
Transitively covered via the test cases in `CsvParserServiceTest` and `MetadataServiceTest`.

---

## 4. CSV Parsing & Type Inference
*   **File**: [`src/main/java/com/example/csvjson/service/CsvParserService.java`](file:///home/noelcyriac/Documents/June_Projects/CSV-to-JSON-converter/src/main/java/com/example/csvjson/service/CsvParserService.java)
*   **Lines**: `1` to `126`
*   **Description**: Parses CSV files into structured maps using `org.apache.commons.csv.CSVParser`. Infers primitive data types (Boolean, Long, Double) while preserving leading zero string formats (e.g. ZIP codes).

### Limits (Max / Min)
*   **File Size**:
    *   *Min*: Must be greater than 0 bytes (`file.length() == 0` throws an exception).
    *   *Max*: Bound only by heap memory allocated to the JVM.
*   **Delimiter**: Must be a single character (e.g., `,`, `;`, or `\t`).
*   **Column Inconsistency**: Columns in all records must align with the header count (`record.isConsistent()`).

### Error Messages Generated
*   `File reference is null.` (Line 34)
*   `File does not exist: [absolutePath]` (Line 37)
*   `CSV file is empty.` (Line 40)
*   `CSV file is missing headers.` (Line 58)
*   `CSV file contains empty or blank header names.` (Line 64)
*   `Malformed row at record [number]: Column count ([size]) does not match header count ([headers]).` (Line 70)
*   `IO error reading CSV file: [message]` (Line 83)
*   `Invalid CSV structure or duplicate/malformed headers: [message]` (Line 85)

### Inbuilt Functions Used & Selection Rationale
*   `FileReader(File, Charset)`: Standard class to read text files using specified encodings.
*   `Charset.forName(String)`: Safely loads the configured character encoding.
*   `String.trim()`, `String.isEmpty()`: Sanitizes header and cell strings by stripping whitespace.
*   `String.matches(String)`: Regular expression checks for classifying cell values as Longs or Doubles.
*   `Long.parseLong(String)`: Conversions to numeric 64-bit integer values.
*   `Double.parseDouble(String)`: Conversions to double-precision floating-point numbers.
*   `LinkedHashMap`: Maintains insertion order of headers so fields map correctly in the final JSON objects.

### Tests Written
*   [`src/test/java/com/example/csvjson/service/CsvParserServiceTest.java`](file:///home/noelcyriac/Documents/June_Projects/CSV-to-JSON-converter/src/test/java/com/example/csvjson/service/CsvParserServiceTest.java) (`1` to `262`):
    *   `testParseValidCsvNoInference`: Checks parsing without type conversion.
    *   `testParseTypeInference`: Checks parsing with type conversion for longs, doubles, booleans, and string preservation of leading zeroes.
    *   `testParseEmptyFile`: Asserts exception on 0-byte file.
    *   `testParseMissingHeaders`: Asserts exception when headers are missing.
    *   `testParseBlankHeaderName`: Asserts exception when a header name is blank.
    *   `testParseMalformedRow`: Asserts exception when a row is missing values.
    *   `testParseSemicolonDelimiter`: Verifies configuration of custom semicolon separator.
    *   `testParseTabDelimiter`: Verifies configuration of custom tab separator.
    *   `testParseUtf8Encoding`: Verifies character parsing under `UTF-8`.
    *   `testParseIso88591Encoding`: Verifies character parsing under `ISO-8859-1`.

---

## 5. JSON Writing / Serialization
*   **File**: [`src/main/java/com/example/csvjson/service/JsonWriterService.java`](file:///home/noelcyriac/Documents/June_Projects/CSV-to-JSON-converter/src/main/java/com/example/csvjson/service/JsonWriterService.java)
*   **Lines**: `1` to `51`
*   **Description**: Converts structured data records into JSON files, supporting either pretty-printed formatted blocks or compact single-line outputs.

### Limits (Max / Min)
*   **Filename**: Must not be null or empty.
*   **Format**: Automatically handles matching extensions. Replaces `.csv` with `.json` if present; otherwise, appends `.json`.

### Error Messages Generated
*   `Filename cannot be null or empty.` (Line 37)

### Inbuilt Functions Used & Selection Rationale
*   `String.toLowerCase()`, `String.endsWith()`, `String.substring()`: String manipulation utilities to convert extensions.
*   `ObjectMapper.writeValue(File, Object)`: (Jackson Library) Industry standard, efficient JSON serializer that writes object structures directly to a file stream.

### Tests Written
*   [`src/test/java/com/example/csvjson/service/JsonWriterServiceTest.java`](file:///home/noelcyriac/Documents/June_Projects/CSV-to-JSON-converter/src/test/java/com/example/csvjson/service/JsonWriterServiceTest.java) (`1` to `80`):
    *   `testPrettyPrintJson`: Validates JSON formatting contains indentation and newlines.
    *   `testCompactJson`: Validates compact JSON formatting output does not contain any formatting whitespaces or newlines.

---

## 6. Metadata Ledger Management
*   **File**: [`src/main/java/com/example/csvjson/service/MetadataService.java`](file:///home/noelcyriac/Documents/June_Projects/CSV-to-JSON-converter/src/main/java/com/example/csvjson/service/MetadataService.java)
*   **Lines**: `1` to `148`
*   **Description**: Manages `processed-files.csv` as a synchronized database of conversion statuses, timestamps, and retry counts, which prevents duplicate conversions.

### Limits (Max / Min)
*   **Thread Safety**: Methods are explicitly `synchronized` to allow safe writes and reads from multiple threads concurrently.
*   **Ledger File Path**: Programmatically fixed to `processed-files.csv` inside the metadata directory.

### Error Messages Generated
*   `Warning: Failed to load metadata from [path]. Error: [message]` (Lines 66-67)
*   `Error: Failed to save metadata to [path]. Error: [message]` (Lines 143-144)

### Inbuilt Functions Used & Selection Rationale
*   `synchronized`: Ensures mutual exclusion so that concurrent worker threads do not write dirty data or corrupt the ledger file.
*   `HashMap`: In-memory cache tracking metadata for quick O(1) lookups during execution.
*   `LocalDateTime.parse(CharSequence)`, `LocalDateTime.format(DateTimeFormatter)`: Encodes and decodes local time values to ISO standards inside the ledger.
*   `Integer.parseInt(String)`: Translates persisted string numbers back to local integers.

### Tests Written
*   [`src/test/java/com/example/csvjson/service/MetadataServiceTest.java`](file:///home/noelcyriac/Documents/June_Projects/CSV-to-JSON-converter/src/test/java/com/example/csvjson/service/MetadataServiceTest.java) (`1` to `166`):
    *   `testEmptyMetadataLoad`: Verifies initial cache loading.
    *   `testMarkSuccessAndCheck`: Asserts success state is marked correctly.
    *   `testMarkFailureAndCheck`: Asserts failure state is written properly.
    *   `testIncrementRetry`: Confirms fail retry counts increment correctly.
    *   `testLoadFromExistingFile`: Validates parsing of an pre-existing CSV metadata database.
    *   `testConcurrentWrites`: Launches 10 threads to write success records concurrently to verify synchronization safety and file alignment.

---

## 7. Execution Logging & Results Ledger
*   **File**: [`src/main/java/com/example/csvjson/service/LogService.java`](file:///home/noelcyriac/Documents/June_Projects/CSV-to-JSON-converter/src/main/java/com/example/csvjson/service/LogService.java)
*   **Lines**: `1` to `125`
*   **Description**: Manages standard error/info console output, `logs/execution.log`, and appends formatted conversion statuses into `logs/results.csv`.

### Limits (Max / Min)
*   **Thread Safety**: Writing to `results.csv` is thread-safe (`synchronized`).
*   **Console Logging level**: Programmatically locked to `Level.INFO`.
*   **File Logging level**: Logs all messages (`Level.ALL`).

### Error Messages Generated
*   `Failed to initialize file logging: [message]` (Line 68)
*   `Failed to write to results log: [path]` (Line 108)

### Inbuilt Functions Used & Selection Rationale
*   `java.util.logging.Logger`: Native lightweight logging engine that does not require additional external dependencies.
*   `ConsoleHandler` / `FileHandler`: Stream outputs directly to Console stdout and appends to files.
*   `FileWriter(File, boolean append)`: Opens the CSV results file with the `true` append flag to prevent overwriting past execution results.
*   `String.join(CharSequence, Iterable)`: Concatenates header arrays cleanly.
*   `String.contains(CharSequence)`, `String.replace(CharSequence, CharSequence)`: Automatically wraps fields in double quotes and escapes existing quotes to generate compliant CSV fields.

### Tests Written
Transitively covered via the test cases in `MetadataServiceTest` and `CsvParserServiceTest` that perform conversion cycles, writing logs and outcome records to temporary folders.

---

## 8. Parallel Batch Conversion Execution Orchestration
*   **File**: [`src/main/java/com/example/csvjson/processor/BatchProcessor.java`](file:///home/noelcyriac/Documents/June_Projects/CSV-to-JSON-converter/src/main/java/com/example/csvjson/processor/BatchProcessor.java)
*   **Lines**: `1` to `130`
*   **Description**: Coordinates normal execution, scheduling conversion tasks across a thread pool while moving processed files to successful or failed folders and updating records.

### Limits (Max / Min)
*   **Thread Pool Size**: Configured via properties (`processing.threads`, defaults to `4`).
*   **Processing Timeout**: Hardcoded to wait up to `30` minutes (`30, TimeUnit.MINUTES`) before terminating processing threads.

### Error Messages Generated
*   `ERROR invalid.csv failed` (or `[fileName] failed`) (Line 93)
*   `Failed to move failed file: [fileName] to failed directory.` (Line 103)
*   `Timeout waiting for file conversion tasks to complete.` (Line 120)
*   `Execution interrupted` (Line 123)

### Inbuilt Functions Used & Selection Rationale
*   `Executors.newFixedThreadPool(int)`: Standard Java thread-management API to run multiple files conversions concurrently without CPU thrashing.
*   `ExecutorService.submit(Runnable)`: Submits conversion tasks asynchronously for parallel processing.
*   `ExecutorService.shutdown()` / `awaitTermination(long, TimeUnit)`: Safely locks the thread pool and waits for tasks to finish, protecting against infinite thread hangs.
*   `Thread.currentThread().interrupt()`: Re-interrupts the thread to preserve the execution state if interrupted during termination waiting.

### Tests Written
Transitively covered through application conversion flow executions.

---

## 9. Failed File Retry Mechanism
*   **File**: [`src/main/java/com/example/csvjson/service/RetryService.java`](file:///home/noelcyriac/Documents/June_Projects/CSV-to-JSON-converter/src/main/java/com/example/csvjson/service/RetryService.java)
*   **Lines**: `1` to `115`
*   **Description**: Scans the `failed/` directory, checks the metadata ledger for FAILED status, increments their attempt counts, and retries the conversion process.

### Limits (Max / Min)
*   **Retry Trigger**: Retries only files in the `failed/` folder where status is specifically `FAILED` in the ledger.
*   **Retry Count Limit**: No maximum retry limit is enforced (retry limits have been removed, count increments on each attempt).

### Error Messages Generated
*   `ERROR invalid.csv failed` (or `[fileName] failed on retry attempt [count]`) (Line 102)

### Inbuilt Functions Used & Selection Rationale
*   Standard collections and loop iterations for filtering and execution.

### Tests Written
Transitively covered through retry mode execution cycles.
