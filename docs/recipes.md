# Recipes

These recipes use only the canonical v1.2 session-first authoring API. Public application code should not import `com.demcha.compose.engine.*`.

## Paragraph Module

```java
document.pageFlow(page -> page
        .module("Professional Summary", module -> module.paragraph(
                "Backend engineer focused on secure Java systems and reliable document generation.")));
```

## Bullet List

```java
document.pageFlow(page -> page
        .module("Technical Skills", module -> module.bullets(
                "Java 21",
                "Spring Boot",
                "PostgreSQL",
                "Docker")));
```

## Markerless Rows

```java
document.pageFlow(page -> page
        .module("Projects", module -> module.rows(
                "GraphCompose - Declarative PDF/document layout engine.",
                "CVRewriter - Profile-aware CV tailoring platform.")));
```

## Styled Table

```java
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.table.DocumentTableColumn;
import com.demcha.compose.document.table.DocumentTableStyle;

document.pageFlow(page -> page
        .module("Status", module -> module.table(table -> table
                .columns(
                        DocumentTableColumn.fixed(90),
                        DocumentTableColumn.auto(),
                        DocumentTableColumn.auto())
                .defaultCellStyle(DocumentTableStyle.builder()
                        .padding(DocumentInsets.of(6))
                        .build())
                .headerStyle(DocumentTableStyle.builder()
                        .fillColor(DocumentColor.LIGHT_GRAY)
                        .padding(DocumentInsets.of(6))
                        .build())
                .header("Area", "Owner", "Status")
                .rows(
                        new String[]{"Engine", "GraphCompose", "Stable"},
                        new String[]{"Templates", "Canonical", "Active"}))));
```

## Divider And Accent Shape

```java
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;

document.pageFlow(page -> page
        .module("Visual Blocks", module -> module
                .divider(divider -> divider
                        .width(220)
                        .thickness(3)
                        .color(DocumentColor.ROYAL_BLUE)
                        .padding(DocumentInsets.of(6)))
                .addShape(shape -> shape
                        .name("Accent")
                        .size(3, 90)
                        .fillColor(DocumentColor.ORANGE)
                        .padding(DocumentInsets.of(6)))));
```

## Snapshot Regression

```java
try (DocumentSession document = GraphCompose.document().create()) {
    document.pageFlow(page -> page
            .module("Snapshot Example", module -> module.paragraph("Hello GraphCompose")));

    LayoutSnapshot snapshot = document.layoutSnapshot();
}
```
