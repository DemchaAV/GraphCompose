package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.junit.jupiter.api.Test;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPPr;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The space a container puts between its children reaches Word.
 *
 * <p>The layout puts a section's {@code spacing} between every two neighbouring children, and
 * the export wrote none of it: a CV sidebar laid out with 9pt between its blocks came out with
 * its blocks touching. It is written as the space below one child and above the next. A spacer
 * is space of the same kind, and is written as its height alone.</p>
 */
class DocxContainerSpacingTest {

    private static final long NINE_POINTS = 9 * 20L;

    @Test
    void aSectionsSpacingStandsBetweenEachTwoOfItsChildren() throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> page
                .addSection("Sidebar", sidebar -> sidebar
                        .spacing(9)
                        .addParagraph("Contact")
                        .addParagraph("Skills")
                        .addParagraph("Languages")))) {
            List<XWPFParagraph> paragraphs = written(document.getParagraphs());

            assertThat(paragraphs).extracting(XWPFParagraph::getText)
                    .containsExactly("Contact", "Skills", "Languages");
            assertThat(before(paragraphs.get(0))).as("nothing above the first child").isZero();
            assertThat(before(paragraphs.get(1))).isEqualTo(NINE_POINTS);
            assertThat(before(paragraphs.get(2))).isEqualTo(NINE_POINTS);
        }
    }

    @Test
    void theSpacingAboveATableIsHeldBelowTheParagraphBeforeIt() throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> page
                .addSection("Entry", entry -> entry
                        .spacing(9)
                        .addParagraph("Summary")
                        .addTable(t -> t.autoColumns(2).row("Title", "2024"))))) {
            List<XWPFParagraph> paragraphs = written(document.getParagraphs());

            assertThat(after(paragraphs.get(0))).isEqualTo(NINE_POINTS);
        }
    }

    @Test
    void aPanelsSpacingStandsBetweenTheChildrenInItsCell() throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> page
                .addSection("Card", card -> card
                        .spacing(9)
                        .fillColor(DocumentColor.rgb(240, 240, 240))
                        .padding(DocumentInsets.of(6))
                        .addParagraph("First")
                        .addParagraph("Second")))) {
            XWPFTableCell cell = document.getTables().get(0).getRow(0).getCell(0);
            List<XWPFParagraph> paragraphs = written(cell.getParagraphs());

            assertThat(paragraphs).extracting(XWPFParagraph::getText).containsExactly("First", "Second");
            assertThat(before(paragraphs.get(1))).isEqualTo(NINE_POINTS);
        }
    }

    @Test
    void aPageBreakTakesNoSpacingBeforeItAndTheNextPageStartsItsSpacingDown() throws Exception {
        // The layout ends the page at the break, and places the next child the section's
        // spacing below the top of the next page.
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> page
                .addSection("Pages", pages -> pages
                        .spacing(9)
                        .addParagraph("Last on the first page")
                        .addPageBreak(b -> b.name("Break"))
                        .addParagraph("First on the second page")))) {
            List<XWPFParagraph> paragraphs = written(document.getParagraphs());

            assertThat(after(paragraphs.get(0))).isZero();
            assertThat(before(paragraphs.get(1))).isEqualTo(NINE_POINTS);
        }
    }

    @Test
    void aSpacerIsItsHeightAndNotALineOfTextAsWell() throws Exception {
        // Between two CV entries held apart by a spacer, the empty paragraph that carries the
        // spacer's height was a line of text tall on top of it.
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> page
                .addParagraph("First entry")
                .addSpacer(spacer -> spacer.name("Gap").width(100).height(4.5))
                .addParagraph("Second entry"))) {
            List<XWPFParagraph> all = document.getParagraphs();
            XWPFParagraph spacer = all.get(1);

            assertThat(spacer.getText()).isEmpty();
            var spacing = spacer.getCTP().getPPr().getSpacing();
            assertThat(spacing != null && spacing.isSetLineRule() && spacing.isSetLine())
                    .as("the spacer's line is held to a hairline").isTrue();
            assertThat(spacing.getLineRule().toString()).isEqualTo("exact");
            assertThat(DocxTwips.of(spacing.getLine())).isLessThanOrEqualTo(2L);
            assertThat(before(all.get(2))).as("the spacer's height, above the next entry").isEqualTo(90L);
        }
    }

    private static List<XWPFParagraph> written(List<XWPFParagraph> paragraphs) {
        return paragraphs.stream().filter(paragraph -> !paragraph.getText().isBlank()).toList();
    }

    private static long before(XWPFParagraph paragraph) {
        CTPPr properties = paragraph.getCTP().getPPr();
        if (properties == null || !properties.isSetSpacing() || !properties.getSpacing().isSetBefore()) {
            return 0;
        }
        return DocxTwips.of(properties.getSpacing().getBefore());
    }

    private static long after(XWPFParagraph paragraph) {
        CTPPr properties = paragraph.getCTP().getPPr();
        if (properties == null || !properties.isSetSpacing() || !properties.getSpacing().isSetAfter()) {
            return 0;
        }
        return DocxTwips.of(properties.getSpacing().getAfter());
    }
}
