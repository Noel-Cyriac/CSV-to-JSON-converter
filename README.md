# CSV to JSON Converter

A production-style standalone Java application built on Java 21 and Maven. It scans a directory for CSV files, converts them into pretty-printed JSON files, prevents duplicate processing using a metadata ledger, maintains execution and results logs, and supports manual or scheduled retries without any arbitrary retry limit.

---

## Features
* **Dynamic Schema-free Parsing**: Automatically maps CSV headers to JSON keys, adapting to any structure and column count dynamically.
* **Strict Validation & Error Detection**:
  * Detects and warns about empty CSV files.
  * Disallows files with missing or empty/blank headers.
  * Performs row consistency checks to ensure the column count in every record matches the header count.
* **Duplicate Processing Prevention**: Consults a persistent metadata ledger (`metadata/processed-files.csv`) to skip previously successfully processed files.
* **Indefinite Retry Mode**: Failed files are safely quarantined in a `failed/` directory, allowing manual or scheduled retries that increment retry counts indefinitely, without a maximum limit.
* **Audit Ledgers**:
  * `logs/execution.log` preserves high-level steps and trace events.
  * `logs/results.csv` captures a structured tabular outcome log (`filename`, `status`, `message`, `timestamp`).
* **Pretty-printed JSON**: Generates structured, readable JSON files formatted with double-space indentation.

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
│   └── application.properties       # Directory configurations
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
You can configure folder directories relative to the execution root:
```properties
dir.input=input
dir.output=output
dir.processed=processed
dir.failed=failed
dir.logs=logs
dir.metadata=metadata
```

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

### 2. Manual Retry Mode
To process files that are currently in the `failed/` directory:
```bash
java -jar target/csv-json-converter-1.0.0.jar --retryFailed
```

### 3. Scheduled Mode (Cron)
Both modes can be scheduled to run automatically (e.g. via cron jobs) at designated intervals.

* **Scheduled Normal Processing** (e.g. scans for new files every hour):
  ```text
  0 * * * * java -jar /path/to/csv-json-converter-1.0.0.jar
  ```

* **Scheduled Retry Processing** (e.g. retries failed files every 2 hours):
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
   * For each failed file:
     * Retries parsing and writing.
     * **Success**: Moved to `processed/`. Metadata: `SUCCESS`, `retryCount: 0`.
     * **Failure**: Stays in `failed/`. Metadata: `FAILED`, `retryCount: incremented`. There is no maximum retry limit.
