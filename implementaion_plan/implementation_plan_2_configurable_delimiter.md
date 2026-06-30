# Implementation Plan: 2. Configurable CSV Delimiter

This feature allows loading the CSV delimiter symbol from the configurations, enabling support for semicolon (`;`), tab (`\t`), or colon (`:`) separated files.

---

## Proposed Changes

### 1. Configuration Setup
In `config/application.properties`:
```properties
csv.delimiter=,
```

In `AppConfig.java`:
* Declare variable `private char csvDelimiter = ',';`
* Parse the value:
```java
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

### 2. Configure CsvParserService Format Builder
In `CsvParserService.java`, modify how the Apache Commons `CSVFormat` is initialized to use the custom delimiter:
```java
CSVFormat csvFormat = CSVFormat.Builder.create(CSVFormat.DEFAULT)
        .setDelimiter(config.getCsvDelimiter())
        .setHeader()
        .setSkipHeaderRecord(true)
        .setTrim(true)
        .build();
```

---

## Verification Plan

### Automated Tests
* In `CsvParserServiceTest.java`, add tests:
  * Parse a semicolon-delimited string (e.g. `id;name;age`) using a configuration stub with `csvDelimiter = ';'`.
  * Parse a tab-delimited string (e.g. `id\tname\tage`) using a configuration stub with `csvDelimiter = '\t'`.

### Manual Verification
* Change `csv.delimiter=;` in `config/application.properties`.
* Place a semicolon-delimited CSV file in `input/` and run the tool to verify it parses correctly without malformed row errors.
