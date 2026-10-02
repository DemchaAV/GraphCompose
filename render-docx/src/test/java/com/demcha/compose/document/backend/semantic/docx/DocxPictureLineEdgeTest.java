package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.font.FontName;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.junit.jupiter.api.Test;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSpacing;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STLineSpacingRule;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A line of a drawn shape and no text keeps the page's height in Word, its picture's frame
 * taken from the space around it.
 *
 * <p>A shape is written as a picture larger than its ink by a transparent frame on every side
 * ({@link DocxShapePictures#EDGE}). Word makes a line of pictures and no text, written at least
 * their reach, as tall as the pictures themselves, frame included: {@code MonogramSidebar}'s
 * contact icons stood in lines half a point taller than the page's, and each contact half a
 * point lower than the one above.</p>
 *
 * @author Artem Demchyshyn
 */
class DocxPictureLineEdgeTest {

    private static final long EDGE_TWIPS = Math.round(DocxShapePictures.EDGE * 20);

    @Test
    void theFrameAboveComesOffTheSpaceAboveAndTheFrameBelowOffTheSpaceUnder() throws Exception {
        DocumentTextStyle small = DocumentTextStyle.builder().fontName(FontName.LATO).size(6).build();
        try (XWPFDocument document = DocxExports.withLayout(300, 400, 20, page -> page
                .spacing(0)
                .addParagraph("Above")
                .addParagraph(p -> p.textStyle(small).align(TextAlign.CENTER)
                        .margin(DocumentInsets.top(4))
                        .dot(22, DocumentColor.rgb(176, 141, 87)))
                .addParagraph(p -> p.text("Under").textStyle(small).align(TextAlign.CENTER)
                        .margin(DocumentInsets.top(8))))) {
            XWPFParagraph icon = document.getParagraphs().get(1);
            XWPFParagraph under = document.getParagraphs().get(2);
            assertThat(icon.getText()).isEmpty();
            assertThat(spacing(icon).getLineRule()).as("grown to the picture").isEqualTo(STLineSpacingRule.AT_LEAST);

            assertThat(before(icon)).as("its 4pt, less the frame above the ink").isEqualTo(4 * 20 - EDGE_TWIPS);
            assertThat(before(under)).as("its 8pt, less the frame below the ink").isEqualTo(8 * 20 - EDGE_TWIPS);
        }
    }

    @Test
    void aShapeAmongTextTakesNothing() throws Exception {
        // Its line is the text's, exact, and Word does not grow it to the picture.
        try (XWPFDocument document = DocxExports.withLayout(300, 400, 20, page -> page
                .spacing(0)
                .addParagraph("Above")
                .addParagraph(p -> p.margin(DocumentInsets.top(4))
                        .dot(4, DocumentColor.rgb(176, 141, 87)).inlineText("  Contact", null)))) {
            XWPFParagraph line = document.getParagraphs().get(1);

            assertThat(before(line)).isEqualTo(4 * 20L);
        }
    }

    private static CTSpacing spacing(XWPFParagraph paragraph) {
        return paragraph.getCTP().getPPr().getSpacing();
    }

    private static long before(XWPFParagraph paragraph) {
        CTSpacing spacing = paragraph.getCTP().getPPr() == null ? null : paragraph.getCTP().getPPr().getSpacing();
        return spacing == null || !spacing.isSetBefore() ? 0 : DocxTwips.of(spacing.getBefore());
    }
}
