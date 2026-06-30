# Implementation Plan: 3. Configurable File Encoding

This feature enforces explicit charset encoding (e.g. UTF-8, ISO-8859-1) when reading CSV files to prevent platform-dependent file corruption.

---

## Proposed Changes

### 1. Configuration Setup
In `config/application.properties`:
```properties
csv.encoding=UTF-8
```

In `AppConfig.java`:
```java
private String csvEncoding = "UTF-8";

// In loadProperties():
this.csvEncoding = properties.getProperty("csv.encoding", "UTF-8");

// Getter:
public String getCsvEncoding() {
    return csvEncoding;
}
```

### 2. Update File Readers in Service layer
In `CsvParserService.java`, modify how `FileReader` is instantiated. Java 21 supports specifying `Charset` directly in the `FileReader` constructor:
```java
import java.nio.charset.Charset;

// In parse():
try (Reader reader = new FileReader(file, Charset.forName(config.getCsvEncoding()));
     CSVParser parser = new CSVParser(reader, csvFormat)) {
     // ...
}
```

---

## Verification Plan

### Automated Tests
* In `CsvParserServiceTest.java`, add tests:
  * Parse a CSV saved in UTF-8 containing special symbols (e.g., currency symbols, accents).
  * Parse a CSV saved in `ISO-8859-1` using the configured encoding.

### Manual Verification
* Run on a system where standard default encoding is not UTF-8 (e.g., Windows cmd) and process a file with special characters (like `ü`, `ñ`, `€`). Verify that characters are rendered correctly in the output JSON.
