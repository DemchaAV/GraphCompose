package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.junit.jupiter.api.Test;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPPr;

import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The space a block holds above and below itself reaches Word.
 *
 * <p>None of it used to: a paragraph's {@code margin} and {@code padding} were dropped, and
 * a container's were dropped twice over, since a container is not a Word object and its
 * children are written where it stood. Every exported document ran its blocks together and
 * leaned on whatever Word puts between paragraphs — which was invisible only while the line
 * height was Word's too, tall enough to stand in for the gaps.</p>
 *
 * <p>Vertical space is one of the few parts of a node's box Word holds natively, which is
 * why this is written where the horizontal half is not. A container hands its top edge to
 * the first paragraph written inside it and its bottom edge to the last, because that is
 * where a reader sees it either way.</p>
 *
 * @author Artem Demchyshyn
 */
class DocxVerticalSpacingTest {

    private static final double TWIPS_PER_POINT = 20.0;
    private static final DocumentColor SURFACE = DocumentColor.rgb(238, 243, 249);

    @Test
    void aParagraphCarriesItsOwnMarginAndPadding() throws Exception {
        List<XWPFParagraph> paragraphs = bodyOf(page -> page
                .addParagraph(p -> p.text("Above"))
                .addParagraph(p -> p.text("Spaced")
                        .padding(DocumentInsets.top(16))
                        .margin(DocumentInsets.bottom(6))));

        assertThat(before(paragraphs.get(1))).isEqualTo(Math.round(16 * TWIPS_PER_POINT));
        assertThat(after(paragraphs.get(1))).isEqualTo(Math.round(6 * TWIPS_PER_POINT));
    }

    @Test
    void marginAndPaddingOnTheSameEdgeAddUp() throws Exception {
        // Different things to the engine — outside the box and inside it — but Word has one
        // gap above a paragraph, and the page shows their sum.
        List<XWPFParagraph> paragraphs = bodyOf(page -> page.addParagraph(p -> p.text("Both")
                .padding(DocumentInsets.top(10))
                .margin(DocumentInsets.top(4))));

        assertThat(before(paragraphs.get(0))).isEqualTo(Math.round(14 * TWIPS_PER_POINT));
    }

    @Test
    void aContainersEdgesGoToItsFirstAndLastParagraph() throws Exception {
        List<XWPFParagraph> paragraphs = bodyOf(page -> page.addSection("Card", card -> card
                .fillColor(SURFACE)
                .padding(DocumentInsets.symmetric(14, 0))
                .margin(DocumentInsets.symmetric(6, 0))
                .addParagraph(p -> p.text("First"))
                .addParagraph(p -> p.text("Middle"))
                .addParagraph(p -> p.text("Last"))));

        assertThat(paragraphs).hasSize(3);
        assertThat(before(paragraphs.get(0)))
                .as("14pt of padding and 6pt of margin, on the paragraph that starts the card")
                .isEqualTo(Math.round(20 * TWIPS_PER_POINT));
        assertThat(before(paragraphs.get(1))).as("nothing in the middle").isZero();
        assertThat(after(paragraphs.get(1))).isZero();
        assertThat(after(paragraphs.get(2)))
                .as("and the same below, on the one that ends it")
                .isEqualTo(Math.round(20 * TWIPS_PER_POINT));
    }

    @Test
    void nestingAddsUpOnTheSameParagraph() throws Exception {
        // An outer section and an inner card both start at the same paragraph, so it
        // carries both edges — which is what the page shows.
        List<XWPFParagraph> paragraphs = bodyOf(page -> page.addSection("Outer", outer -> outer
                .padding(DocumentInsets.top(8))
                .addSection("Inner", inner -> inner
                        .padding(DocumentInsets.top(12))
                        .addParagraph(p -> p.text("Deep").padding(DocumentInsets.top(3))))));

        assertThat(before(paragraphs.get(0))).isEqualTo(Math.round(23 * TWIPS_PER_POINT));
    }

    @Test
    void aContainerWithNoParagraphLeavesNothingBehindForTheNextOne() throws Exception {
        // A container of tables has nowhere to put its top edge — Word has no space-before
        // on a table. What must not happen is the edge waiting around and landing on
        // whatever paragraph comes next, which would put it somewhere the document never
        // asked for.
        List<XWPFParagraph> paragraphs = bodyOf(page -> page
                .addSection("TablesOnly", section -> section
                        .padding(DocumentInsets.symmetric(30, 0))
                        .addTable(t -> t.autoColumns(2).row("A", "B")))
                .addParagraph(p -> p.text("After")));

        assertThat(before(paragraphs.get(paragraphs.size() - 1)))
                .as("the section's 30pt did not follow the table out")
                .isZero();
    }

    @Test
    void aBlockThatAsksForNothingCarriesNoSpacingAtAll() throws Exception {
        List<XWPFParagraph> paragraphs = bodyOf(page -> page.addParagraph(p -> p.text("Plain")));

        CTPPr properties = paragraphs.get(0).getCTP().getPPr();
        assertThat(properties == null || !properties.isSetSpacing()
                   || (!properties.getSpacing().isSetBefore() && !properties.getSpacing().isSetAfter()))
                .as("no w:before and no w:after, rather than zeros")
                .isTrue();
    }

    private static long before(XWPFParagraph paragraph) {
        CTPPr properties = paragraph.getCTP().getPPr();
        if (properties == null || !properties.isSetSpacing() || !properties.getSpacing().isSetBefore()) {
            return 0;
        }
        return Long.parseLong(String.valueOf(properties.getSpacing().getBefore()));
    }

    private static long after(XWPFParagraph paragraph) {
        CTPPr properties = paragraph.getCTP().getPPr();
        if (properties == null || !properties.isSetSpacing() || !properties.getSpacing().isSetAfter()) {
            return 0;
        }
        return Long.parseLong(String.valueOf(properties.getSpacing().getAfter()));
    }

    private static List<XWPFParagraph> bodyOf(Consumer<PageFlowBuilder> content) throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, content)) {
            return document.getParagraphs().stream()
                    .filter(paragraph -> !paragraph.getText().isBlank())
                    .toList();
        }
    }
}
