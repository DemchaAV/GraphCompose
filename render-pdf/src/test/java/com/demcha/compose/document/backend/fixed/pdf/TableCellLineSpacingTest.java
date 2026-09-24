package com.demcha.compose.document.backend.fixed.pdf;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.table.DocumentTableCell;
import com.demcha.compose.document.table.DocumentTableColumn;
import com.demcha.compose.document.table.DocumentTableStyle;
import com.demcha.compose.document.table.DocumentTableTextAnchor;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * A table cell's {@code lineSpacing} is space between its lines.
 *
 * <p>The layout sizes a row with it — {@code lines × lineHeight + (lines − 1) × lineSpacing}
 * — and the renderer set the lines a line height apart regardless, so the row came out
 * taller than its text by the spacing it never showed, and the text sat in the leftover as
 * the anchor placed it.</p>
 */
class TableCellLineSpacingTest {

    @Test
    void aCellsLinesStandItsLineSpacingFurtherApart() throws Exception {
        List<Double> tight = baselines(render(DocumentTableTextAnchor.TOP_LEFT, 0));
        List<Double> spaced = baselines(render(DocumentTableTextAnchor.TOP_LEFT, 6));

        assertThat(spaced).hasSize(3);
        for (int line = 1; line < 3; line++) {
            double tightPitch = tight.get(line) - tight.get(line - 1);
            double spacedPitch = spaced.get(line) - spaced.get(line - 1);
            assertThat(spacedPitch - tightPitch).as("line %d", line).isCloseTo(6.0, within(0.01));
        }
    }

    @Test
    void theLinesFillTheRowTheLayoutSizedSoEveryAnchorPutsThemInOnePlace() throws Exception {
        // The one cell is the row's tallest, so the row is exactly its text and padding:
        // nothing is left over for an anchor to move the lines within.
        List<Double> top = baselines(render(DocumentTableTextAnchor.TOP_LEFT, 6));
        List<Double> middle = baselines(render(DocumentTableTextAnchor.CENTER_LEFT, 6));
        List<Double> bottom = baselines(render(DocumentTableTextAnchor.BOTTOM_LEFT, 6));

        for (int line = 0; line < 3; line++) {
            assertThat(middle.get(line)).as("middle, line %d", line).isCloseTo(top.get(line), within(0.01));
            assertThat(bottom.get(line)).as("bottom, line %d", line).isCloseTo(top.get(line), within(0.01));
        }
    }

    private static List<Double> baselines(byte[] pdf) throws Exception {
        return DrawnGlyphs.byLine(pdf).stream()
                .filter(line -> !line.isEmpty())
                .map(line -> line.get(0).baselineY())
                .sorted()
                .toList();
    }

    private static byte[] render(DocumentTableTextAnchor anchor, double lineSpacing) {
        try (DocumentSession document = GraphCompose.document()
                .pageSize(595, 842)
                .margin(DocumentInsets.of(36))
                .create()) {
            document.pageFlow(page -> page.addTable(table -> table
                    .columns(DocumentTableColumn.fixed(200))
                    .defaultCellStyle(DocumentTableStyle.builder()
                            .padding(DocumentInsets.of(4))
                            .textAnchor(anchor)
                            .lineSpacing(lineSpacing)
                            .build())
                    .rowCells(DocumentTableCell.lines("Alpha", "Bravo", "Charlie"))));
            return document.toPdfBytes();
        } catch (Exception failure) {
            throw new IllegalStateException(failure);
        }
    }
}
