# Implementation Plan: 1. Data Type Inference (Auto-Typing)

This feature introduces automatic data type parsing for CSV values, converting standard String representation of records into matching native JSON types (Numbers, Booleans, Nulls).

## User Review Required
> [!IMPORTANT]
> **API Signature Change**:
> The parsing API return signature changes from `List<Map<String, String>>` to `List<Map<String, Object>>`. This will require updates to:
> - `CsvParserService`
> - `BatchProcessor`
> - `RetryService`

---

## Proposed Changes

### 1. Configuration Check
In `AppConfig.java`, we will load a toggle key:
```properties
json.infer.types=true
```
Code addition:
```java
private boolean inferTypes = true;

// inside loadProperties():
this.inferTypes = Boolean.parseBoolean(properties.getProperty("json.infer.types", "true"));

// Getter:
public boolean isInferTypes() {
    return inferTypes;
}
```

### 2. Type Detection Implementation
In `CsvParserService.java`, add the `inferType(String value)` helper method:
```java
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
        // Retain leading zeroes (e.g. postal codes, IDs) as String
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

### 3. Update CsvParserService Signature
Change return type of `parse` and map keys:
```java
public List<Map<String, Object>> parse(File file, AppConfig config) throws CsvParseException {
    // ...
    List<Map<String, Object>> results = new ArrayList<>();
    
    // ...
    for (CSVRecord record : parser) {
        // ...
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
```

---

## Verification Plan

### Automated Tests
* Create unit tests in `CsvParserServiceTest.java` verifying that:
  * `"123"` becomes `123L`
  * `"45.67"` becomes `45.67d`
  * `"true"` / `"false"` becomes `Boolean` objects
  * `"01234"` stays as `"01234"` (String)
  * Empty cells become `null`

### Manual Verification
* Feed a CSV with mixed types (IDs, names, prices, boolean flags) to the utility and check the output JSON to ensure numbers and booleans do not have enclosing double quotes.
