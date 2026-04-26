# Recipes

These recipes use only the canonical session-first authoring API. Public application code should not import `com.demcha.compose.engine.*`.

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

## Filled Card With Rounded Corners

```java
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.style.DocumentTextStyle;

document.pageFlow(page -> page
        .spacing(12)
        .addSection("InfoCard", card -> card
                .fillColor(DocumentColor.rgb(245, 248, 255))
                .stroke(DocumentStroke.of(DocumentColor.ROYAL_BLUE, 0.8))
                .cornerRadius(10)
                .padding(DocumentInsets.of(12))
                .margin(DocumentInsets.bottom(10))
                .addParagraph(paragraph -> paragraph
                        .text("Block text inside a filled rounded card.")
                        .textStyle(DocumentTextStyle.DEFAULT)
                        .lineSpacing(2))));
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

## Spacer, Line, And Circle

```java
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentStroke;

document.pageFlow(page -> page
        .name("VisualPrimitives")
        .spacing(8)
        .addSpacer(spacer -> spacer.name("Gap").height(12))
        .addLine(line -> line
                .name("Rule")
                .horizontal(180)
                .thickness(2)
                .color(DocumentColor.ROYAL_BLUE))
        .addEllipse(ellipse -> ellipse
                .name("Badge")
                .circle(24)
                .fillColor(DocumentColor.ORANGE)
                .stroke(DocumentStroke.of(DocumentColor.BLACK, 0.5))));
```

## Image Fit

```java
import com.demcha.compose.document.image.DocumentImageFitMode;

import java.nio.file.Path;

document.pageFlow(page -> page
        .name("ImageFit")
        .addImage(image -> image
                .name("Logo")
                .source(Path.of("assets/logo.png"))
                .fitToBounds(96, 48)
                .fitMode(DocumentImageFitMode.CONTAIN))
        .addImage(image -> image
                .name("Avatar")
                .source(Path.of("assets/avatar.png"))
                .fitToBounds(48, 48)
                .fitMode(DocumentImageFitMode.COVER)));
```

## Snapshot Regression

```java
try (DocumentSession document = GraphCompose.document().create()) {
    document.pageFlow(page -> page
            .module("Snapshot Example", module -> module.paragraph("Hello GraphCompose")));

    LayoutSnapshot snapshot = document.layoutSnapshot();
}
```
