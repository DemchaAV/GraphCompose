package com.demcha.compose.document.templates.cv.presets;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.backend.semantic.docx.DocxSemanticBackend;
import com.demcha.compose.document.layout.LayoutGraph;
import com.demcha.compose.document.layout.PlacedNode;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.cv.data.CvDocument;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.junit.jupiter.api.Test;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSpacing;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STLineSpacingRule;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * The banded {@code MintEditorial} masthead takes in Word the room it takes on the page.
 *
 * <p>The band's masthead is a canvas pulled up to the page's top edge by its margin, its name and
 * title placed down it. Written one block under another from the canvas's corner, the gap between
 * the name and the title was lost and the room owed under them was measured from that corner,
 * 48pt above where Word sets the name: the row under the masthead did not fit the first page, and
 * the two-page CV took three in Word 16 and LibreOffice. Word's distance from the page's margin to
 * that row is read from the file — each paragraph's space above and below and its exact line.</p>
 */
class MintEditorialBandedDocxTest {

    private static final float MARGIN = (float) MintEditorial.RECOMMENDED_MARGIN;

    @Test
    void theRowUnderTheBandedMastheadStartsWhereThePageStartsIt() throws Exception {
        DocumentTemplate<CvDocument> banded = MintEditorial.create(MintEditorial.Options.builder()
                .headerBandColor(DocumentColor.rgb(228, 217, 198)).build());
        try (DocumentSession session = GraphCompose.document()
                .pageSize(DocumentPageSize.A4)
                .margin(MARGIN, MARGIN, MARGIN, MARGIN)
                .create()) {
            banded.compose(session, CvPresetFixtures.canonicalDocument());
            LayoutGraph layout = session.layoutGraph();
            PlacedNode row = layout.nodes().stream()
                    .filter(node -> "CvV2MintEditorialPageOne".equals(node.semanticName()))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("no placed first-page row"));
            double rowTop = session.canvas().height() - (row.placementY() + row.placementHeight());

            try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(
                    session.export(new DocxSemanticBackend())))) {
                assertThat(heightAboveTheFirstTable(document))
                        .as("from the page's margin to the row under the masthead")
                        .isCloseTo(rowTop - MARGIN, within(0.1));
            }
        }
    }

    /** How far down Word sets the body's first table: the paragraphs above it, each an exact line. */
    private static double heightAboveTheFirstTable(XWPFDocument document) {
        double height = 0;
        for (IBodyElement element : document.getBodyElements()) {
            if (element instanceof XWPFTable) {
                return height;
            }
            assertThat(element).as("what stands above the first table").isInstanceOf(XWPFParagraph.class);
            XWPFParagraph paragraph = (XWPFParagraph) element;
            CTSpacing spacing = paragraph.getCTP().isSetPPr() && paragraph.getCTP().getPPr().isSetSpacing()
                    ? paragraph.getCTP().getPPr().getSpacing() : null;
            assertThat(spacing != null && spacing.getLineRule() == STLineSpacingRule.EXACT)
                    .as("%s written in an exact line", paragraph.getText()).isTrue();
            height += points(spacing.isSetBefore() ? spacing.getBefore() : null)
                      + points(spacing.getLine())
                      + points(spacing.isSetAfter() ? spacing.getAfter() : null);
        }
        throw new AssertionError("no table in the body");
    }

    private static double points(Object twips) {
        return twips == null ? 0 : new BigDecimal(twips.toString()).doubleValue() / 20.0;
    }
}
