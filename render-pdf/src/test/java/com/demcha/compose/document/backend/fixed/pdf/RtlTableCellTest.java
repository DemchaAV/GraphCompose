package com.demcha.compose.document.backend.fixed.pdf;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.node.TextDirection;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.table.DocumentTableColumn;
import com.demcha.compose.document.table.DocumentTableStyle;
import com.demcha.compose.font.FontName;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves a cell written as a plain string is drawn the way its direction says.
 *
 * <p>A table cell used to reach the page through the table's own layout, which measured
 * and drew the characters in the order they were typed. For Latin that is the same order
 * either way, so nothing looked wrong until Hebrew went into a cell and came out
 * backwards — in a document whose paragraphs, by then, were correct. The evidence here is
 * the same evidence the paragraph suite takes: where each glyph actually landed.</p>
 */
class RtlTableCellTest {

    private static final String HEBREW = "שלום";
    private static final String LATIN = "Hello";

    @Test
    void hebrewInACellIsDrawnRightToLeft() throws Exception {
        String drawn = DrawnGlyphs.readLeftToRight(render(HEBREW, TextDirection.RTL));

        assertThat(drawn)
                .describedAs("walking the cell left to right yields the word reversed, "
                        + "because its first letter is the rightmost one drawn")
                .isEqualTo(new StringBuilder(HEBREW).reverse().toString());
    }

    @Test
    void aCellWithNoDeclaredDirectionIsUntouched() throws Exception {
        assertThat(DrawnGlyphs.readLeftToRight(render(LATIN, null)))
                .describedAs("the path every existing table takes must be unchanged")
                .isEqualTo(LATIN);
    }

    @Test
    void autoReadsTheDirectionOffTheCellItLandedIn() throws Exception {
        // One table, one declared direction, two cells that answer it differently. The
        // question AUTO asks is about text, and in a table the text is per cell — resolved
        // once for the table instead, one of these two comes out wrong.
        byte[] pdf = renderPair(HEBREW, LATIN);
        String drawn = DrawnGlyphs.readLeftToRight(pdf);

        assertThat(drawn)
                .describedAs("the Hebrew cell reversed, the Latin cell did not")
                .contains(new StringBuilder(HEBREW).reverse().toString())
                .contains(LATIN);
    }

    @Test
    void autoFixesOneDirectionForTheWholeCell() throws Exception {
        // Two lines in one cell; the first strong character is Hebrew, so both run right to
        // left — including the second, which opens on Latin. Resolved per line instead,
        // that line takes a left-to-right base and arranges itself backwards relative to
        // the one above it. The cell is what the algorithm calls a paragraph.
        byte[] pdf = render(table -> table
                .columns(DocumentTableColumn.fixed(300))
                .defaultCellStyle(cellStyle(TextDirection.AUTO, null))
                .rowCells(com.demcha.compose.document.table.DocumentTableCell.lines(
                        HEBREW, LATIN + " " + HEBREW)));

        java.util.List<DrawnGlyphs.Glyph> secondLine = DrawnGlyphs.byLine(pdf).stream()
                .filter(line -> line.stream().anyMatch(g -> "H".equals(g.character())))
                .findFirst()
                .orElseThrow();
        double hebrewX = secondLine.stream()
                .filter(g -> g.is(codePoint -> codePoint >= 'א' && codePoint <= 'ת'))
                .mapToDouble(DrawnGlyphs.Glyph::left)
                .min()
                .orElseThrow();
        double latinX = secondLine.stream()
                .filter(g -> "H".equals(g.character()))
                .mapToDouble(DrawnGlyphs.Glyph::left)
                .min()
                .orElseThrow();

        assertThat(hebrewX)
                .describedAs("under the cell's base direction the Hebrew sits left of the "
                        + "Latin it follows in reading order")
                .isLessThan(latinX);
    }

    @Test
    void aRightToLeftCellSitsAtItsRightEdge() throws Exception {
        double rightAligned = leftmostGlyph(render(HEBREW, TextDirection.RTL));
        double leftAligned = leftmostGlyph(render(HEBREW, null));

        assertThat(rightAligned)
                .describedAs("direction decides the edge when nobody asked for one, the "
                        + "same rule a paragraph follows")
                .isGreaterThan(leftAligned);
    }

    @Test
    void anExplicitAnchorWinsOverTheDirection() throws Exception {
        double declaredLeft = leftmostGlyph(renderAnchored(HEBREW));
        double directionChosen = leftmostGlyph(render(HEBREW, TextDirection.RTL));

        assertThat(declaredLeft)
                .describedAs("alignment says where the line sits and direction says which "
                        + "way it runs; they meet only in the default")
                .isLessThan(directionChosen);
    }

    private static double leftmostGlyph(byte[] pdf) throws Exception {
        return DrawnGlyphs.leftToRight(pdf).stream()
                .mapToDouble(DrawnGlyphs.Glyph::left)
                .min()
                .orElseThrow();
    }

    private static byte[] render(String text, TextDirection direction) {
        return render(table -> table
                .columns(DocumentTableColumn.fixed(300))
                .defaultCellStyle(cellStyle(direction, null))
                .row(text));
    }

    private static byte[] renderAnchored(String text) {
        return render(table -> table
                .columns(DocumentTableColumn.fixed(300))
                .defaultCellStyle(cellStyle(TextDirection.RTL,
                        com.demcha.compose.document.table.DocumentTableTextAnchor.CENTER_LEFT))
                .row(text));
    }

    private static byte[] renderPair(String first, String second) {
        return render(table -> table
                .columns(DocumentTableColumn.fixed(200), DocumentTableColumn.fixed(200))
                .defaultCellStyle(cellStyle(TextDirection.AUTO, null))
                .row(first, second));
    }

    private static DocumentTableStyle cellStyle(
            TextDirection direction,
            com.demcha.compose.document.table.DocumentTableTextAnchor anchor) {
        DocumentTableStyle.Builder builder = DocumentTableStyle.builder()
                .textStyle(DocumentTextStyle.builder()
                        .fontName(FontName.DAVID_LIBRE)
                        .size(24)
                        .build());
        if (direction != null) {
            builder.direction(direction);
        }
        if (anchor != null) {
            builder.textAnchor(anchor);
        }
        return builder.build();
    }

    private static byte[] render(
            java.util.function.Consumer<com.demcha.compose.document.dsl.TableBuilder> spec) {
        try (DocumentSession document = GraphCompose.document()
                .pageSize(595, 842)
                .margin(DocumentInsets.of(36))
                .create()) {

            document.pageFlow(page -> page.addTable(spec));
            return document.toPdfBytes();
        }
    }
}
