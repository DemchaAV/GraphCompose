package com.demcha.compose.document.backend.fixed.pptx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.table.DocumentTableCell;
import com.demcha.compose.document.table.DocumentTableColumn;
import com.demcha.compose.document.table.DocumentTableStyle;
import com.demcha.compose.document.table.DocumentTableTextAnchor;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * A table cell's {@code lineSpacing} is space between its lines on a slide, as on the page:
 * the layout sizes the row with it, and each line's frame stands that much further from the
 * last.
 */
class PptxTableCellLineSpacingTest {

    @Test
    void aCellsLinesStandItsLineSpacingFurtherApart() throws Exception {
        List<Double> tight = lineTops(export(DocumentTableTextAnchor.TOP_LEFT, 0));
        List<Double> spaced = lineTops(export(DocumentTableTextAnchor.TOP_LEFT, 6));

        assertThat(spaced).hasSize(3);
        for (int line = 1; line < 3; line++) {
            double tightPitch = tight.get(line) - tight.get(line - 1);
            double spacedPitch = spaced.get(line) - spaced.get(line - 1);
            assertThat(spacedPitch - tightPitch).as("line %d", line).isCloseTo(6.0, within(0.01));
        }
    }

    @Test
    void theLinesFillTheRowTheLayoutSizedSoEveryAnchorPutsThemInOnePlace() throws Exception {
        List<Double> top = lineTops(export(DocumentTableTextAnchor.TOP_LEFT, 6));
        List<Double> bottom = lineTops(export(DocumentTableTextAnchor.BOTTOM_LEFT, 6));

        for (int line = 0; line < 3; line++) {
            assertThat(bottom.get(line)).as("line %d", line).isCloseTo(top.get(line), within(0.01));
        }
    }

    /** The top of each line's text frame, in slide points from the top, in reading order. */
    private static List<Double> lineTops(byte[] pptx) throws Exception {
        List<Double> tops = new ArrayList<>();
        try (XMLSlideShow show = new XMLSlideShow(new ByteArrayInputStream(pptx))) {
            for (XSLFShape shape : show.getSlides().get(0).getShapes()) {
                if ("GraphCompose Table Cell Text".equals(shape.getShapeName())) {
                    tops.add(shape.getAnchor().getY());
                }
            }
        }
        tops.sort(null);
        return tops;
    }

    private static byte[] export(DocumentTableTextAnchor anchor, double lineSpacing) throws Exception {
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
            return document.render(new PptxFixedLayoutBackend());
        }
    }
}
