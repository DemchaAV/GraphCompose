# Lifecycle Logging

GraphCompose ships SLF4J lifecycle logs but does not ship a production `logback.xml`. Consumers decide whether to enable these categories.

## Logger Categories

| Category | Emits |
| --- | --- |
| `com.demcha.compose.document.lifecycle` | `DocumentSession` create, compose, layout, snapshot, PDF bytes, build, render, and close events. |
| `com.demcha.compose.templates.lifecycle` | Built-in template compose and template-module compose start/end/failure events. |
| `com.demcha.compose.engine.layout` | Semantic layout compiler and low-level layout-system start/end events. |
| `com.demcha.compose.engine.pagination` | Semantic pagination and low-level page-breaker/fallback events. |
| `com.demcha.compose.engine.render` | Canonical PDF backend and low-level render-pass events. |

## Policy

- Normal lifecycle events use `DEBUG`.
- Recoverable graph inconsistencies or cleanup failures use `WARN`.
- Failed render/build paths use `ERROR`.
- Logs must not include full document text, contact details, addresses, or output paths.
- Logs may include safe diagnostics such as session ids, template ids, spec types, counts, booleans, backend names, byte counts, and timings.

## Example Logback Snippet

```xml
<logger name="com.demcha.compose.document.lifecycle" level="DEBUG"/>
<logger name="com.demcha.compose.templates.lifecycle" level="DEBUG"/>
<logger name="com.demcha.compose.engine.layout" level="DEBUG"/>
<logger name="com.demcha.compose.engine.pagination" level="DEBUG"/>
<logger name="com.demcha.compose.engine.render" level="DEBUG"/>
```

Keep this configuration in the consuming application or test resources. The library itself should remain quiet by default.
