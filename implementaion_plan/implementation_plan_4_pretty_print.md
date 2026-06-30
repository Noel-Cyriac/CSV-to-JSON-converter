# Implementation Plan: 4. Compact vs. Pretty-Printed JSON

This feature provides control over JSON formatting, allowing users to toggle between pretty-printed (indented, easy to read) and compact (one-line, smaller file footprint) JSON output.

---

## Proposed Changes

### 1. Configuration Setup
In `config/application.properties`:
```properties
json.pretty=true
```

In `AppConfig.java`:
```java
private boolean jsonPretty = true;

// In loadProperties():
this.jsonPretty = Boolean.parseBoolean(properties.getProperty("json.pretty", "true"));

// Getter:
public boolean isJsonPretty() {
    return jsonPretty;
}
```

### 2. Update JsonWriterService Configuration
In `JsonWriterService.java`, modify how the Jackson `ObjectMapper` is initialized:
```java
import com.fasterxml.jackson.databind.SerializationFeature;

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

## Verification Plan

### Automated Tests
* In `JsonWriterServiceTest.java` (create if needed, or check output directly):
  * When `json.pretty` is true, output starts with array delimiters and contains newlines/indent spaces.
  * When `json.pretty` is false, output contains no newlines (entire JSON is on one line).

### Manual Verification
* Set `json.pretty=false` in configuration.
* Run the application and check output files to ensure they are minified.
