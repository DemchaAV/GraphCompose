package com.demcha.compose.document.backend.semantic.docx;

import static org.assertj.core.api.Assertions.assertThat;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.node.TextDirection;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.font.FontName;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.function.Consumer;

/**
 * Holds what Word will draw for a right-to-left paragraph, not what the file was told.
 *
 * <p>Word reads three of a run's properties differently from the way they are written. Its
 * {@code w:jc} takes {@code left} and {@code right} as the <em>start</em> and <em>end</em>
 * of the text flow rather than as edges of the page, so in a right-to-left paragraph the
 * two swap. And Hebrew and Arabic are complex scripts, which take their size and weight
 * from {@code w:szCs}, {@code w:bCs} and {@code w:iCs} — the Latin twins do not reach them.
 * All three were written the plain way, and the result was a document that opened flush on
 * the wrong margin with its Hebrew at Word's default size instead of the asked one.</p>
 *
 * <p>None of it shows in a left-to-right document, which is why it went unnoticed: the
 * swap is identity there, and Latin obeys the properties that were being written.</p>
 */
class DocxRightToLeftTest {

    private static final String HEBREW = "שלום עולם";
    private static final String LATIN = "Hello world";

    @Test
    void aRightToLeftParagraphIsAlignedToTheEdgeItStartsFrom() throws Exception {
        // The page lays a right-to-left paragraph flush right. Word has to be told "start
        // of the flow" to draw that, and the flow starts on the right — so the file says
        // left, which is the one value that puts the text on the right.
        XWPFParagraph paragraph = onlyParagraph(HEBREW, TextDirection.RTL, null);

        assertThat(paragraph.getCTP().getPPr().isSetBidi())
                .describedAs("the paragraph declares its direction")
                .isTrue();
        assertThat(paragraph.getCTP().getPPr().getJc().getVal().toString())
                .describedAs("Word reads this as the start of the flow, which is the right "
                        + "edge for a right-to-left paragraph")
                .isEqualTo("left");
    }

    @Test
    void anExplicitAlignmentOnARightToLeftParagraphIsSwappedToo() throws Exception {
        // An author who asks for left on a right-to-left paragraph means the physical left,
        // which Word calls the end of the flow.
        XWPFParagraph paragraph = onlyParagraph(HEBREW, TextDirection.RTL, TextAlign.LEFT);

        assertThat(paragraph.getCTP().getPPr().getJc().getVal().toString()).isEqualTo("right");
    }

    @Test
    void aLeftToRightParagraphIsUntouched() throws Exception {
        // The control, and the reason this went unseen: for a left-to-right paragraph the
        // mapping is the identity and nothing about the old behaviour looked wrong.
        XWPFParagraph paragraph = onlyParagraph(LATIN, TextDirection.LTR, TextAlign.RIGHT);

        assertThat(paragraph.getCTP().getPPr().isSetBidi()).isFalse();
        assertThat(paragraph.getCTP().getPPr().getJc().getVal().toString()).isEqualTo("right");
    }

    @Test
    void aCentredRightToLeftParagraphStaysCentred() throws Exception {
        // The third arm of the swap. Only left and right are flow-relative in ST_Jc;
        // written as a two-way "right becomes left, everything else becomes right", a
        // centred Hebrew heading would export as `right` and Word would draw it against
        // a margin, with every other case in this class still green.
        XWPFParagraph paragraph = onlyParagraph(HEBREW, TextDirection.RTL, TextAlign.CENTER);

        assertThat(paragraph.getCTP().getPPr().getJc().getVal().toString())
                .describedAs("centre is the same edge in either direction")
                .isEqualTo("center");
    }

    @Test
    void aLeftToRightCellParagraphKeepsTheAlignmentItAskedFor() throws Exception {
        // The one change here that moves a document containing no Hebrew: before, a cell
        // paragraph was written with its runs and no alignment at all, so an explicit
        // right-aligned amount drew flush left. Gate the new call on `rightToLeft` and it
        // silently goes back to doing that.
        List<XWPFParagraph> cells = cellParagraphs(row -> row
                .weights(1.0)
                .addParagraph(p -> p.text("1,200.00").align(TextAlign.RIGHT)
                        .textStyle(DocumentTextStyle.builder()
                                .fontName(FontName.HELVETICA).size(12).build())));

        assertThat(cells).hasSize(1);
        assertThat(cells.get(0).getCTP().getPPr().isSetBidi())
                .describedAs("nothing declares a direction it does not have")
                .isFalse();
        assertThat(cells.get(0).getCTP().getPPr().getJc().getVal().toString())
                .describedAs("the alignment the author asked for reaches the cell")
                .isEqualTo("right");
    }

    @Test
    void theRequestedSizeReachesTheComplexScriptCharacters() throws Exception {
        XWPFParagraph paragraph = onlyParagraph(HEBREW, TextDirection.RTL, null);
        var properties = paragraph.getRuns().get(0).getCTR().getRPr();

        assertThat(halfPoints(properties.getSzArray(0).getVal()))
                .describedAs("half-points, as Word counts them")
                .isEqualTo(30);
        assertThat(halfPoints(properties.getSzCsArray(0).getVal()))
                .describedAs("Hebrew takes its size from here and nowhere else, so a missing "
                        + "value draws it at Word's default rather than the asked 15pt")
                .isEqualTo(30);
    }

    @Test
    void theRequestedWeightReachesTheComplexScriptCharacters() throws Exception {
        XWPFParagraph paragraph = onlyParagraph(HEBREW, TextDirection.RTL,
                null, DocumentTextDecoration.BOLD_ITALIC);
        var properties = paragraph.getRuns().get(0).getCTR().getRPr();

        assertThat(properties.sizeOfBArray()).describedAs("bold, the Latin way").isEqualTo(1);
        assertThat(properties.sizeOfBCsArray())
                .describedAs("and the way that reaches Hebrew")
                .isEqualTo(1);
        assertThat(properties.sizeOfIArray()).isEqualTo(1);
        assertThat(properties.sizeOfICsArray()).isEqualTo(1);
    }

    /** The schema types a measure as an open value; every writer here puts a number in it. */
    private static int halfPoints(Object value) {
        return ((Number) value).intValue();
    }

    @Test
    void aRightToLeftParagraphInsideATableCellIsDeclaredToo() throws Exception {
        // Where an invoice keeps its line items. A row becomes a one-row table, and the
        // cell walk wrote the runs without the alignment and direction the same paragraph
        // gets outside a table — so every right-to-left cell was left undeclared.
        List<XWPFParagraph> cells = cellParagraphs(row -> row
                .weights(1.0, 1.0)
                .addParagraph(p -> p.text(HEBREW).direction(TextDirection.RTL)
                        .textStyle(DocumentTextStyle.builder()
                                .fontName(FontName.DAVID_LIBRE).size(12).build()))
                .addParagraph(p -> p.text("1,200.00").direction(TextDirection.RTL)
                        .textStyle(DocumentTextStyle.builder()
                                .fontName(FontName.DAVID_LIBRE).size(12).build())));

        assertThat(cells).describedAs("the row became a table with its cells").hasSize(2);
        assertThat(cells).allSatisfy(paragraph -> {
            assertThat(paragraph.getCTP().getPPr().isSetBidi())
                    .describedAs("cell paragraph %s declares its direction",
                            paragraph.getText())
                    .isTrue();
            assertThat(paragraph.getCTP().getPPr().getJc().getVal().toString())
                    .describedAs("and starts from the edge a right-to-left line starts from")
                    .isEqualTo("left");
        });
    }

    /** Exports a one-row table and returns the paragraphs its cells carry. */
    private static List<XWPFParagraph> cellParagraphs(
            Consumer<com.demcha.compose.document.dsl.RowBuilder> row) throws Exception {
        byte[] docx;
        try (DocumentSession document = GraphCompose.document()
                .pageSize(400, 200)
                .margin(DocumentInsets.of(20))
                .create()) {
            document.pageFlow(page -> page.addRow(row::accept));
            docx = document.export(new DocxSemanticBackend());
        }

        try (XWPFDocument word = new XWPFDocument(new ByteArrayInputStream(docx))) {
            return word.getTables().stream()
                    .flatMap(table -> table.getRows().stream())
                    .flatMap(tableRow -> tableRow.getTableCells().stream())
                    .flatMap(cell -> cell.getParagraphs().stream())
                    .filter(paragraph -> !paragraph.getText().isBlank())
                    .toList();
        }
    }

    private static XWPFParagraph onlyParagraph(String text, TextDirection direction,
                                               TextAlign align) throws Exception {
        return onlyParagraph(text, direction, align, null);
    }

    private static XWPFParagraph onlyParagraph(String text, TextDirection direction,
                                               TextAlign align,
                                               DocumentTextDecoration decoration) throws Exception {
        DocumentTextStyle.Builder style = DocumentTextStyle.builder()
                .fontName(FontName.DAVID_LIBRE).size(15);
        if (decoration != null) {
            style.decoration(decoration);
        }
        Consumer<com.demcha.compose.document.dsl.ParagraphBuilder> body = p -> {
            p.text(text).direction(direction).textStyle(style.build());
            if (align != null) {
                p.align(align);
            }
        };

        byte[] docx;
        try (DocumentSession document = GraphCompose.document()
                .pageSize(400, 200)
                .margin(DocumentInsets.of(20))
                .create()) {
            document.pageFlow(page -> page.addParagraph(body));
            docx = document.export(new DocxSemanticBackend());
        }

        try (XWPFDocument word = new XWPFDocument(new ByteArrayInputStream(docx))) {
            return word.getParagraphs().stream()
                    .filter(paragraph -> !paragraph.getRuns().isEmpty())
                    .findFirst()
                    .orElseThrow();
        }
    }
}
