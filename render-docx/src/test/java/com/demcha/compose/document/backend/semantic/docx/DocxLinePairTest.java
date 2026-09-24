package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.LayerAlign;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.ClipPolicy;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextStyle;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.junit.jupiter.api.Test;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPPr;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Text an overlay sets at the left and the right of one line is one Word line, split by a tab.
 *
 * <p>A CV entry's head band puts its title at the left and its dates at the right, as two
 * layers of one container. Written one after the other, the dates took a line of their own
 * under every title.</p>
 */
class DocxLinePairTest {

    private static final double PAGE_WIDTH = 400;
    private static final double MARGIN = 20;
    private static final double CONTENT = PAGE_WIDTH - 2 * MARGIN;

    @Test
    void aTitleAtTheLeftAndDatesAtTheRightAreOneLineSplitByARightTab() throws Exception {
        try (XWPFDocument document = export(band(20, 0))) {
            List<XWPFParagraph> paragraphs = written(document);

            assertThat(paragraphs).hasSize(1);
            XWPFParagraph line = paragraphs.get(0);
            assertThat(line.getText()).isEqualTo("MARKETING MANAGER\tJan 2022 - Present");
            CTPPr properties = line.getCTP().getPPr();
            assertThat(properties.getTabs().sizeOfTabArray()).isEqualTo(1);
            assertThat(properties.getTabs().getTabArray(0).getVal().toString()).isEqualTo("right");
            assertThat(DocxTwips.of(properties.getTabs().getTabArray(0).getPos()))
                    .as("at the band's right edge, where the dates end")
                    .isCloseTo(Math.round(CONTENT * 20), org.assertj.core.data.Offset.offset(2L));
        }
    }

    @Test
    void aTitleStandingAboveItsBandTakesThatSpaceFromTheGapAbove() throws Exception {
        // The band is shorter than the title's line and the title is pulled up to centre on it:
        // on the page the title's top stands above the band, into the gap over it.
        try (XWPFDocument roomy = export(band(20, 0));
             XWPFDocument pulledUp = export(band(6, -4))) {
            XWPFParagraph plain = written(roomy).get(0);
            XWPFParagraph raised = written(pulledUp).get(0);

            assertThat(before(plain)).as("the gap above, and the band's room above its text")
                    .isGreaterThanOrEqualTo(12L * 20);
            assertThat(before(raised)).as("the gap above, less what the raised title stands into")
                    .isLessThan(12L * 20);
        }
    }

    @Test
    void textHangingBelowItsBandTakesThatSpaceFromTheGapBelow() throws Exception {
        // The next paragraph's margin is measured from the band's bottom, and the text on the
        // page already reaches past it.
        try (XWPFDocument document = DocxExports.withLayout(PAGE_WIDTH, 600, MARGIN, page -> page
                .add(band(6, 4))
                .addParagraph(p -> p.text("After").margin(DocumentInsets.top(10))))) {
            XWPFParagraph after = document.getParagraphs().stream()
                    .filter(paragraph -> "After".equals(paragraph.getText())).findFirst().orElseThrow();

            assertThat(before(after)).as("its 10pt, less what the text hangs below the band")
                    .isLessThan(10L * 20);
        }
    }

    @Test
    void threeParagraphsInABandStayOneAfterTheOther() throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(PAGE_WIDTH, 600, MARGIN, page -> page
                .addParagraph(p -> p.text("Before").margin(DocumentInsets.bottom(12)))
                .addContainer(head -> head
                        .name("Band")
                        .rectangle(CONTENT, 20)
                        .clipPolicy(ClipPolicy.OVERFLOW_VISIBLE)
                        .position(paragraph("Left", TextAlign.LEFT), 0, 0, LayerAlign.CENTER_LEFT)
                        .position(paragraph("Middle", TextAlign.CENTER), 0, 0, LayerAlign.CENTER)
                        .position(paragraph("Right", TextAlign.RIGHT), 0, 0, LayerAlign.CENTER_RIGHT)))) {
            assertThat(written(document)).extracting(XWPFParagraph::getText)
                    .contains("Left", "Middle", "Right");
        }
    }

    private static XWPFDocument export(DocumentNode band) throws Exception {
        return DocxExports.withLayout(PAGE_WIDTH, 600, MARGIN, page -> page
                .addParagraph(p -> p.text("Before").margin(DocumentInsets.bottom(12)))
                .add(band));
    }

    /** A head band: the title at the left, the dates at the right, both pulled by {@code pull}. */
    private static DocumentNode band(double height, double pull) {
        return new com.demcha.compose.document.dsl.ShapeContainerBuilder()
                .name("EntryHead")
                .rectangle(CONTENT, height)
                .clipPolicy(ClipPolicy.OVERFLOW_VISIBLE)
                .position(new ParagraphBuilder().name("Title").text("MARKETING MANAGER")
                        .textStyle(DocumentTextStyle.DEFAULT.withSize(12)).build(), 0, pull, LayerAlign.CENTER_LEFT)
                .position(paragraph("Jan 2022 - Present", TextAlign.RIGHT), 0, pull, LayerAlign.CENTER_RIGHT)
                .build();
    }

    private static DocumentNode paragraph(String text, TextAlign align) {
        return new ParagraphBuilder().text(text).align(align)
                .textStyle(DocumentTextStyle.DEFAULT.withSize(9)).build();
    }

    private static List<XWPFParagraph> written(XWPFDocument document) {
        return document.getParagraphs().stream()
                .filter(paragraph -> !paragraph.getText().isBlank() && !"Before".equals(paragraph.getText()))
                .toList();
    }

    private static long before(XWPFParagraph paragraph) {
        CTPPr properties = paragraph.getCTP().getPPr();
        if (properties == null || !properties.isSetSpacing() || !properties.getSpacing().isSetBefore()) {
            return 0;
        }
        return DocxTwips.of(properties.getSpacing().getBefore());
    }
}
