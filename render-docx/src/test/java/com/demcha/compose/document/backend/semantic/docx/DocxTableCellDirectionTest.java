package com.demcha.compose.document.backend.semantic.docx;

import static org.assertj.core.api.Assertions.assertThat;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.node.TextDirection;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.table.DocumentTableColumn;
import com.demcha.compose.document.table.DocumentTableStyle;
import com.demcha.compose.font.FontName;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTRPr;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.function.Consumer;

/**
 * Holds that a table cell tells Word which way it runs.
 *
 * <p>Word does the reordering and the Arabic joining itself, so the text goes over exactly
 * as it was typed and nothing here asserts about its contents. What Word cannot work out
 * is the base direction: without {@code w:bidi} it assumes left to right, opens the cell
 * on the wrong edge, and puts a trailing full stop at the wrong end of the line. That one
 * property is the whole contract, and its absence is invisible in any Latin document —
 * which is why it needs a test of its own rather than an eye.</p>
 */
class DocxTableCellDirectionTest {

    private static final String HEBREW = "שלום עולם";
    private static final String LATIN = "Hello world";
    /** Arabic closing on a bracket: the case that exposed the missing run flag. */
    private static final String ARABIC = "صدرت في (2026)";

    @Test
    void aRightToLeftCellDeclaresItsDirection() throws Exception {
        assertThat(bidiOfFirstCell(HEBREW, TextDirection.RTL))
                .describedAs("Word is told the base direction, which is the only way it can "
                        + "lay out a cell that opens on a neutral character")
                .isTrue();
    }

    @Test
    void aCellWithNoDeclaredDirectionSaysNothing() throws Exception {
        assertThat(bidiOfFirstCell(LATIN, null))
                .describedAs("every table exported before direction existed must keep "
                        + "producing the same file")
                .isFalse();
    }

    @Test
    void autoIsAnsweredByTheCellItLandedIn() throws Exception {
        // The same declared direction over two cells, which answer it differently. AUTO is
        // a question about text, and in a table the text is per cell.
        byte[] docx = build(t -> t
                .columns(DocumentTableColumn.fixed(200), DocumentTableColumn.fixed(200))
                .defaultCellStyle(cellStyle(TextDirection.AUTO))
                .row(HEBREW, LATIN));

        withTable(docx, table -> {
            assertThat(isBidi(firstParagraph(table, 0)))
                    .describedAs("the Hebrew cell reads right to left")
                    .isTrue();
            assertThat(isBidi(firstParagraph(table, 1)))
                    .describedAs("and the Latin cell beside it does not")
                    .isFalse();
        });
    }

    @Test
    void theRunItselfDeclaresTheDirectionToo() throws Exception {
        // w:bidi settles which edge the line starts from and nothing else. Word resolves the
        // characters inside a run from w:rtl, and a run without it is handled as Latin —
        // measured in Word, an Arabic cell ending in "(2026)" drew it as ")2026(" while the
        // same document as a PDF was correct. Hebrew happened to come out right, which is
        // why the paragraph path carried this defect unnoticed since the feature shipped.
        byte[] docx = build(t -> t
                .columns(DocumentTableColumn.fixed(300))
                .defaultCellStyle(cellStyle(TextDirection.RTL))
                .row(ARABIC));

        withTable(docx, table -> assertThat(runProperties(firstParagraph(table, 0)))
                .describedAs("the run says it is right-to-left text, not only the paragraph")
                .isNotNull()
                .matches(properties -> properties.sizeOfRtlArray() > 0));
    }

    @Test
    void aCellWithNoDirectionLeavesItsRunsAlone() throws Exception {
        byte[] docx = build(t -> t
                .columns(DocumentTableColumn.fixed(300))
                .defaultCellStyle(cellStyle(null))
                .row(LATIN));

        withTable(docx, table -> {
            CTRPr properties = runProperties(firstParagraph(table, 0));
            assertThat(properties == null || properties.sizeOfRtlArray() == 0)
                    .describedAs("a left-to-right cell writes the same runs it always did")
                    .isTrue();
        });
    }

    @Test
    void theTextItselfIsHandedOverUntouched() throws Exception {
        byte[] docx = build(t -> t
                .columns(DocumentTableColumn.fixed(300))
                .defaultCellStyle(cellStyle(TextDirection.RTL))
                .row(HEBREW));

        withTable(docx, table -> assertThat(table.getRow(0).getCell(0).getText())
                .describedAs("Word has a bidirectional engine of its own; rewriting the "
                        + "text before it gets there would reorder it twice")
                .isEqualTo(HEBREW));
    }

    private static boolean bidiOfFirstCell(String text, TextDirection direction) throws Exception {
        byte[] docx = build(t -> t
                .columns(DocumentTableColumn.fixed(300))
                .defaultCellStyle(cellStyle(direction))
                .row(text));
        boolean[] bidi = new boolean[1];
        withTable(docx, table -> bidi[0] = isBidi(firstParagraph(table, 0)));
        return bidi[0];
    }

    private static boolean isBidi(XWPFParagraph paragraph) {
        return paragraph.getCTP().isSetPPr() && paragraph.getCTP().getPPr().isSetBidi();
    }

    private static CTRPr runProperties(XWPFParagraph paragraph) {
        return paragraph.getRuns().get(0).getCTR().getRPr();
    }

    private static XWPFParagraph firstParagraph(XWPFTable table, int column) {
        return table.getRow(0).getCell(column).getParagraphs().get(0);
    }

    private static void withTable(byte[] docx, Consumer<XWPFTable> assertions) throws Exception {
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(docx))) {
            assertions.accept(document.getTables().get(0));
        }
    }

    private static DocumentTableStyle cellStyle(TextDirection direction) {
        DocumentTableStyle.Builder builder = DocumentTableStyle.builder()
                .textStyle(DocumentTextStyle.builder()
                        .fontName(FontName.DAVID_LIBRE)
                        .size(13)
                        .build());
        if (direction != null) {
            builder.direction(direction);
        }
        return builder.build();
    }

    private static byte[] build(Consumer<com.demcha.compose.document.dsl.TableBuilder> spec)
            throws Exception {
        try (DocumentSession document = GraphCompose.document()
                .pageSize(595, 842)
                .margin(DocumentInsets.of(36))
                .create()) {

            document.pageFlow(page -> page.addTable(spec));
            return document.export(new DocxSemanticBackend());
        }
    }
}
