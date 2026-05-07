package com.demcha.examples;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.node.ListMarker;
import com.demcha.compose.document.node.ListNode;
import com.demcha.compose.document.node.ParagraphNode;
import com.demcha.compose.document.node.TableNode;
import com.demcha.compose.document.node.TextAlign;
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

import java.util.List;
import java.util.Map;
import java.nio.file.Path;

/**
 * Runnable showcase for the v1.6 Phase B composed table cell content:
 * {@code DocumentTableCell.node(DocumentNode)} accepts any composable
 * canonical node and the table layout pipeline prepares the child
 * sub-tree against the cell's resolved inner width, sizes the row from
 * the prepared height, and dispatches the child's fragments through
 * the registered {@code NodeDefinition} pipeline.
 *
 * <p>The generated PDF puts paragraphs (with markdown rich text) and a
 * nested list inside table cells, alongside plain-text cells, so the
 * difference between the v1.5 line-only shape and the v1.6 composed
 * shape is visible at a glance.</p>
 *
 * @author Artem Demchyshyn
 */
public final class ComposedTableCellExample {

    private static final DocumentColor INK = DocumentColor.rgb(34, 38, 50);
    private static final DocumentColor MUTED = DocumentColor.rgb(102, 106, 118);
    private static final DocumentColor RULE = DocumentColor.rgb(180, 188, 200);
    private static final DocumentColor HEADER_FILL = DocumentColor.rgb(20, 60, 75);
    private static final DocumentColor ROW_TINT = DocumentColor.rgb(244, 247, 252);

    private ComposedTableCellExample() {
    }

    public static Path generate() throws Exception {
        Path outputFile = ExampleOutputPaths.prepare("composed-table-cell-showcase.pdf");

        DocumentTextStyle title = DocumentTextStyle.builder()
                .fontName(FontName.HELVETICA_BOLD)
                .size(20)
                .color(INK)
                .decoration(DocumentTextDecoration.BOLD)
                .build();
        DocumentTextStyle sectionHeading = DocumentTextStyle.builder()
                .fontName(FontName.HELVETICA_BOLD)
                .size(13)
                .color(INK)
                .decoration(DocumentTextDecoration.BOLD)
                .build();
        DocumentTextStyle caption = DocumentTextStyle.builder()
                .fontName(FontName.HELVETICA_OBLIQUE)
                .size(10)
                .color(MUTED)
                .build();
        DocumentTextStyle body = DocumentTextStyle.builder()
                .fontName(FontName.HELVETICA)
                .size(10.5)
                .color(INK)
                .build();
        DocumentTextStyle headerText = DocumentTextStyle.builder()
                .fontName(FontName.HELVETICA_BOLD)
                .size(11)
                .color(DocumentColor.WHITE)
                .decoration(DocumentTextDecoration.BOLD)
                .build();

        DocumentTableStyle headerStyle = DocumentTableStyle.builder()
                .fillColor(HEADER_FILL)
                .stroke(DocumentStroke.of(RULE, 0.6))
                .padding(DocumentInsets.of(8))
                .textStyle(headerText)
                .build();
        DocumentTableStyle bodyCellStyle = DocumentTableStyle.builder()
                .stroke(DocumentStroke.of(RULE, 0.6))
                .padding(DocumentInsets.of(8))
                .textStyle(body)
                .build();
        DocumentTableStyle tintedCellStyle = DocumentTableStyle.builder()
                .fillColor(ROW_TINT)
                .stroke(DocumentStroke.of(RULE, 0.6))
                .padding(DocumentInsets.of(8))
                .textStyle(body)
                .build();

        try (DocumentSession document = GraphCompose.document(outputFile)
                .pageSize(DocumentPageSize.A4)
                .margin(36, 36, 36, 36)
                .markdown(true)
                .create()) {

            document.pageFlow()
                    .name("ComposedCellShowcase")
                    .spacing(8)
                    .addParagraph("v1.6 Phase B — Composed table cell content", title)
                    .addParagraph(
                            "DocumentTableCell.node(DocumentNode) accepts any registered "
                                    + "canonical node — paragraphs (with markdown), nested lists, "
                                    + "even sub-tables — and the table layout pipeline prepares "
                                    + "each child against the cell's resolved inner width, sizes "
                                    + "the row from the prepared height, and dispatches the "
                                    + "child's fragments through the standard NodeDefinition path.", caption)
                    .build();

            // 1) Paragraph-in-cell with markdown rich text.
            document.pageFlow()
                    .name("RichTextCellSection")
                    .spacing(6)
                    .addParagraph("1. Paragraph in cell with markdown rich text", sectionHeading)
                    .build();

            DocumentTableCell paraCellLeft = DocumentTableCell.node(
                    new ParagraphNode(
                            "RichSummary",
                            "**Q3 results** were **strong** — *revenue* grew 18% year-over-year, "
                                    + "driven by **enterprise renewals** and a *new* mid-market motion. "
                                    + "Margin expanded by 4 points.",
                            body, TextAlign.LEFT, 1.0,
                            DocumentInsets.zero(), DocumentInsets.zero()));
            DocumentTableCell paraCellRight = DocumentTableCell.node(
                    new ParagraphNode(
                            "RichRisk",
                            "*Note:* the **EU rollout** is at risk if the *compliance review* "
                                    + "slips past the *Nov 28* deadline. Owner: **Legal**.",
                            body, TextAlign.LEFT, 1.0,
                            DocumentInsets.zero(), DocumentInsets.zero()));

            document.add(new TableNode(
                    "RichSummaryTable",
                    List.of(DocumentTableColumn.fixed(244), DocumentTableColumn.fixed(244)),
                    List.of(
                            List.of(
                                    DocumentTableCell.text("Headline").withStyle(headerStyle),
                                    DocumentTableCell.text("Risk note").withStyle(headerStyle)),
                            List.of(
                                    paraCellLeft.withStyle(bodyCellStyle),
                                    paraCellRight.withStyle(bodyCellStyle))),
                    bodyCellStyle,
                    Map.of(),
                    Map.of(),
                    488.0,
                    null,
                    null,
                    DocumentInsets.zero(),
                    new DocumentInsets(0, 0, 12, 0),
                    1));

            // 2) Nested list in cell.
            document.pageFlow()
                    .name("ListCellSection")
                    .spacing(6)
                    .addParagraph("2. Nested list in cell", sectionHeading)
                    .addParagraph(
                            "A ListNode placed via DocumentTableCell.node(...) renders inside the "
                                    + "cell's content area with the same depth cascade as a top-level "
                                    + "nested list. The row sizes itself to the list's measured height.", caption)
                    .build();

            ListNode actionList = new com.demcha.compose.document.dsl.ListBuilder()
                    .name("CellActionList")
                    .textStyle(body)
                    .itemSpacing(2)
                    .markerFor(0, ListMarker.dash())
                    .markerFor(1, ListMarker.custom("*"))
                    .addItem("Approve EU rollout plan", body0 -> body0
                            .addItem("Review compliance checklist")
                            .addItem("Confirm vendor contracts"))
                    .addItem("Schedule Q4 board update")
                    .build();

            ListNode ownerList = new com.demcha.compose.document.dsl.ListBuilder()
                    .name("CellOwnerList")
                    .textStyle(body)
                    .itemSpacing(2)
                    .markerFor(1, ListMarker.dash())
                    .addItem("**Legal** — EU compliance")
                    .addItem("**Finance** — Q4 forecast", body1 -> body1
                            .addItem("Reconcile renewal pipeline")
                            .addItem("Ship board deck draft"))
                    .build();

            document.add(new TableNode(
                    "ActionsTable",
                    List.of(DocumentTableColumn.fixed(244), DocumentTableColumn.fixed(244)),
                    List.of(
                            List.of(
                                    DocumentTableCell.text("Actions").withStyle(headerStyle),
                                    DocumentTableCell.text("Owners").withStyle(headerStyle)),
                            List.of(
                                    DocumentTableCell.node(actionList).withStyle(bodyCellStyle),
                                    DocumentTableCell.node(ownerList).withStyle(tintedCellStyle))),
                    bodyCellStyle,
                    Map.of(),
                    Map.of(),
                    488.0,
                    null,
                    null,
                    DocumentInsets.zero(),
                    new DocumentInsets(0, 0, 12, 0),
                    1));

            // 3) Mixed paragraph + plain text in the same row.
            document.pageFlow()
                    .name("MixedCellSection")
                    .spacing(6)
                    .addParagraph("3. Mixed composed and plain-text cells in the same row", sectionHeading)
                    .addParagraph(
                            "Plain-text cells continue to use the existing DocumentTableCell.text(...) "
                                    + "factory and render through the v1.5 line-iteration code path. "
                                    + "Composed cells render via NodeDefinition recursion alongside.", caption)
                    .build();

            document.add(new TableNode(
                    "MixedRowTable",
                    List.of(
                            DocumentTableColumn.fixed(140),
                            DocumentTableColumn.fixed(244),
                            DocumentTableColumn.fixed(104)),
                    List.of(
                            List.of(
                                    DocumentTableCell.text("Status").withStyle(headerStyle),
                                    DocumentTableCell.text("Notes").withStyle(headerStyle),
                                    DocumentTableCell.text("Owner").withStyle(headerStyle)),
                            List.of(
                                    DocumentTableCell.text("On track").withStyle(bodyCellStyle),
                                    DocumentTableCell.node(new ParagraphNode(
                                            "OnTrackNote",
                                            "All P0 stories merged. *Buffer* for QA *holds* through "
                                                    + "**release branch cut**.",
                                            body, TextAlign.LEFT, 1.0,
                                            DocumentInsets.zero(), DocumentInsets.zero()))
                                            .withStyle(bodyCellStyle),
                                    DocumentTableCell.text("Eng").withStyle(bodyCellStyle)),
                            List.of(
                                    DocumentTableCell.text("At risk").withStyle(tintedCellStyle),
                                    DocumentTableCell.node(new ParagraphNode(
                                            "AtRiskNote",
                                            "Compliance review tracking *one week* late. "
                                                    + "**Mitigation**: parallel scope reduction.",
                                            body, TextAlign.LEFT, 1.0,
                                            DocumentInsets.zero(), DocumentInsets.zero()))
                                            .withStyle(tintedCellStyle),
                                    DocumentTableCell.text("Legal").withStyle(tintedCellStyle))),
                    bodyCellStyle,
                    Map.of(),
                    Map.of(),
                    488.0,
                    null,
                    null,
                    DocumentInsets.zero(),
                    DocumentInsets.zero(),
                    1));

            document.buildPdf();
        }
        return outputFile;
    }

    public static void main(String[] args) throws Exception {
        Path output = generate();
        System.out.println("Generated: " + output);
    }
}
