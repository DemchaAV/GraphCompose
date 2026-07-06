package com.demcha.examples.features.tables;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.node.TableNode;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.table.DocumentTableCell;
import com.demcha.compose.document.table.DocumentTableColumn;
import com.demcha.compose.document.table.DocumentTableStyle;
import com.demcha.compose.font.FontName;
import com.demcha.examples.support.ExampleOutputPaths;

import java.nio.file.Path;
import java.util.List;

/**
 * Runnable showcase for long inline-code coordinates inside table columns.
 *
 * <p>A composed cell holding a single long {@code inlineCode(...)} token — a
 * Maven coordinate, fully-qualified class name or URL — no longer overflows its
 * column. In a narrow <b>fixed</b> column the chip breaks <em>inside</em> the
 * cell, preferring its {@code . : / -} seams and char-splitting only as a last
 * resort, with the rounded fill intact on every fragment. In an <b>auto</b>
 * column the column grows to fit the coordinate on one line instead of
 * collapsing.</p>
 *
 * @author Artem Demchyshyn
 */
public final class InlineCodeColumnWrapExample {

    private static final DocumentColor INK = DocumentColor.rgb(34, 38, 50);
    private static final DocumentColor MUTED = DocumentColor.rgb(102, 106, 118);
    private static final DocumentColor RULE = DocumentColor.rgb(180, 188, 200);
    private static final DocumentColor HEADER_FILL = DocumentColor.rgb(20, 60, 75);
    private static final DocumentColor ROW_TINT = DocumentColor.rgb(244, 247, 252);

    private static final List<String> COORDINATES = List.of(
            "org.junit.jupiter:junit-jupiter:5.10.2",
            "io.github.demchaav:graph-compose:1.9.0",
            "https://repo1.maven.org/maven2/");

    private InlineCodeColumnWrapExample() {
    }

    public static Path generate() throws Exception {
        Path outputFile = ExampleOutputPaths.prepare("features/tables", "inline-code-column-wrap.pdf");

        DocumentTextStyle title = DocumentTextStyle.builder()
                .fontName(FontName.HELVETICA_BOLD).size(20).color(INK)
                .decoration(DocumentTextDecoration.BOLD).build();
        DocumentTextStyle caption = DocumentTextStyle.builder()
                .fontName(FontName.HELVETICA_OBLIQUE).size(10).color(MUTED).build();
        DocumentTextStyle body = DocumentTextStyle.builder()
                .fontName(FontName.HELVETICA).size(10.5).color(INK).build();
        DocumentTextStyle headerText = DocumentTextStyle.builder()
                .fontName(FontName.HELVETICA_BOLD).size(11).color(DocumentColor.WHITE)
                .decoration(DocumentTextDecoration.BOLD).build();

        DocumentTableStyle headerStyle = DocumentTableStyle.builder()
                .fillColor(HEADER_FILL).stroke(DocumentStroke.of(RULE, 0.6))
                .padding(DocumentInsets.of(8)).textStyle(headerText).build();
        DocumentTableStyle bodyCellStyle = DocumentTableStyle.builder()
                .stroke(DocumentStroke.of(RULE, 0.6)).padding(DocumentInsets.of(8)).textStyle(body).build();
        DocumentTableStyle tintedCellStyle = DocumentTableStyle.builder()
                .fillColor(ROW_TINT).stroke(DocumentStroke.of(RULE, 0.6))
                .padding(DocumentInsets.of(8)).textStyle(body).build();

        try (DocumentSession document = GraphCompose.document(outputFile)
                .pageSize(DocumentPageSize.A4)
                .margin(36, 36, 36, 36)
                .create()) {

            document.pageFlow()
                    .name("CodeWrapShowcase")
                    .spacing(8)
                    .addParagraph("Long inline-code coordinates stay inside the column", title)
                    .addParagraph(
                            "A composed cell with one long inlineCode(...) token used to spill over the "
                                    + "next column. It now breaks at its . : / - seams inside a narrow FIXED "
                                    + "column (char-splitting only when a segment is still too wide), and an "
                                    + "AUTO column grows to fit it on one line.", caption)
                    .build();

            List<List<DocumentTableCell>> rows = new java.util.ArrayList<>();
            rows.add(List.of(
                    DocumentTableCell.text("Fixed 104pt").withStyle(headerStyle),
                    DocumentTableCell.text("Auto").withStyle(headerStyle)));
            for (int i = 0; i < COORDINATES.size(); i++) {
                String coordinate = COORDINATES.get(i);
                DocumentTableStyle cellStyle = i % 2 == 0 ? bodyCellStyle : tintedCellStyle;
                rows.add(List.of(
                        DocumentTableCell.node(document.dsl().paragraph().inlineCode(coordinate).build())
                                .withStyle(cellStyle),
                        DocumentTableCell.node(document.dsl().paragraph().inlineCode(coordinate).build())
                                .withStyle(cellStyle)));
            }

            document.add(new TableNode(
                    "CodeWrapTable",
                    List.of(DocumentTableColumn.fixed(104),
                            DocumentTableColumn.auto()),
                    rows,
                    bodyCellStyle,
                    null,
                    DocumentInsets.zero(),
                    DocumentInsets.zero()));

            document.buildPdf();
        }
        return outputFile;
    }

    public static void main(String[] args) throws Exception {
        System.out.println("Generated: " + generate());
    }
}
