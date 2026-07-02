# CSV to JSON Converter

A production-style standalone Java application built on Java 21 and Maven. It scans a directory for CSV files, converts them into pretty-printed JSON files (with automatic data type inference), prevents duplicate processing using a metadata ledger, maintains execution and results logs, and supports manual or scheduled retries.

---

## Technical Stack & Libraries
* **Java**: JDK 21
* **Build System**: Maven 3.x
* **JSON Serializer**: Jackson Databind 2.17.1
* **CSV Parser**: Apache Commons CSV 1.11.0
* **Testing**: JUnit Jupiter 5.10.2

---

## Directory Structure

```text
CSV-to-JSON-converter/
├── config/
│   └── application.properties       # Directory configuration
├── input/                           # Drop new CSV files here
├── output/                          # Generated pretty JSON outputs
├── processed/                       # CSV files processed successfully
├── failed/                          # CSV files that failed processing
├── logs/
│   ├── execution.log                # Custom runtime/cron log file
│   └── results.csv                  # Tabular outcomes (filename,status,message,timestamp)
├── metadata/
│   └── processed-files.csv          # Persistence ledger (filename,status,retryCount,lastAttempt)
├── pom.xml                          # Maven build file (generates shaded runner JAR)
└── src/                             # Source code
```

---

## Configurations (`config/application.properties`)
You can configure folder directories relative to the execution root and processing behavior:
```properties
dir.input=input
dir.output=output
dir.processed=processed
dir.failed=failed
dir.logs=logs
dir.metadata=metadata
json.infer.types=true
csv.delimiter=,
csv.encoding=UTF-8
json.pretty=true
processing.threads=4
```

* `json.infer.types`: Automatically infers and converts CSV cell data types (numbers, booleans). If set to `false`, all values are treated and serialized as plain strings. Defaults to `true`.
* `csv.delimiter`: The CSV field separator. Supports any single character (e.g. `,`, `;`, `:`) or tab (`\t` or `\\t`). Defaults to `,`.
* `csv.encoding`: The character encoding used to read CSV files (e.g. `UTF-8`, `ISO-8859-1`). Defaults to `UTF-8`.
* `json.pretty`: Controls JSON formatting. If set to `true`, the output JSON is pretty-printed (indented). If `false`, the output is compact (minified). Defaults to `true`.
* `processing.threads`: The size of the fixed thread pool used for concurrent CSV file processing. Defaults to `4`.


---

## Build & Test Instructions

### Compile and Run Unit Tests
To compile the source code and run the JUnit test suite:
```bash
mvn clean test
```

### Build Runnable Shaded JAR
To build the executable fat-jar:
```bash
mvn clean package
```
This produces `target/csv-json-converter-1.0.0.jar`.

---

## How to Run

### 1. Normal Processing Mode
To scan the `input/` folder, parse new CSVs, write JSONs to `output/`, and move files:
```bash
java -jar target/csv-json-converter-1.0.0.jar
```

### 2. Manual/Scheduled Retry Mode
To process files that are currently in the `failed/` directory:
```bash
java -jar target/csv-json-converter-1.0.0.jar --retryFailed
```
This is suitable for running via a cron job at designated intervals, e.g., every 2 hours:
```text
0 */2 * * * java -jar /path/to/csv-json-converter-1.0.0.jar --retryFailed
```

---

## State Transition Rules
1. **New File (Input)**:
   * **Success**: Moved to `processed/`. Metadata: `SUCCESS`, `retryCount: 0`.
   * **Failure**: Moved to `failed/`. Metadata: `FAILED`, `retryCount: 1`.
2. **Retry Run**:
   * Scans `failed/` directory.
   * Retries parsing and writing.
   * **Success**: Moved to `processed/`. Metadata: `SUCCESS`, `retryCount: 0`.
   * **Failure**: Stays in `failed/`. Metadata: `FAILED`, `retryCount: incremented`.
