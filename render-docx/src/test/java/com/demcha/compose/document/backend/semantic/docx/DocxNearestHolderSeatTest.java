package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.document.dsl.LayerStackBuilder;
import com.demcha.compose.document.dsl.LineBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentStroke;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.xmlbeans.XmlObject;
import org.junit.jupiter.api.Test;
import org.openxmlformats.schemas.drawingml.x2006.wordprocessingDrawing.CTAnchor;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTP;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * A block that writes more than one paragraph carries each of its drawings in the paragraph
 * nearest where that drawing is drawn, not in the first one it writes.
 *
 * <p>A block's paragraphs are all its own, so any of them moves the drawing with the block; which
 * one it is only sets the offset. Taking the first — a list's heading — writes the offset of the
 * whole list into its last bars: measured over the fidelity corpus, 142 of 728 carried drawings
 * stood further than 50pt from where they are drawn, out to 577pt, and an edit that broke the page
 * between such a heading and its bar left the bar behind.</p>
 */
class DocxNearestHolderSeatTest {

    private static final DocumentColor TRACK = DocumentColor.rgb(220, 226, 236);

    /** A line of the default face at the default size, which each paragraph of these fixtures is. */
    private static final double A_LINE = 16;

    /** A skill's bar: a line in a stack of its own, which writes no text to be anchored in. */
    private static DocumentNode bar() {
        return new LayerStackBuilder().name("Bar")
                .layer(new LineBuilder().horizontal(120).stroke(DocumentStroke.of(TRACK, 4)).build())
                .build();
    }

    @Test
    void noBarOfAListIsCarriedByTheListsHeadingAndNoneStandsMoreThanALineFromItsCarrier() throws Exception {
        // Taking the first paragraph the block writes, all three bars hung off 'SKILLS', the last
        // of them 42pt down: the height of the list above it.
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> page
                .add(new SectionBuilder().name("Skills")
                        .addParagraph(p -> p.text("SKILLS"))
                        .addParagraph(p -> p.text("Negotiation"))
                        .add(bar())
                        .addParagraph(p -> p.text("Forecasting"))
                        .add(bar())
                        .addParagraph(p -> p.text("Drafting"))
                        .add(bar())
                        .build()))) {
            Map<String, List<CTAnchor>> carried = carriedByParagraph(document);

            assertThat(carried).as("every bar is carried, and none by the list's heading")
                    .doesNotContainKey("SKILLS");
            assertThat(carried.values().stream().mapToInt(List::size).sum()).as("all three bars").isEqualTo(3);
            carried.forEach((text, bars) -> bars.forEach(bar ->
                    assertThat(Math.abs(points(bar.getPositionV().getPosOffset())))
                            .as("the bar stands within a line of '%s', which carries it", text)
                            .isLessThan(A_LINE)));
        }
    }

    @Test
    void aBlocksOnlyParagraphStillCarriesWhatStandsFarFromIt() throws Exception {
        // The reach of a holder's paragraph is unchanged: it carries its block's drawing however
        // far off, which is what keeps a sidebar's portrait with its sidebar.
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> page
                .add(new SectionBuilder().name("Sidebar")
                        .addParagraph(p -> p.text("Profile"))
                        .addParagraph(p -> p.text(" "))
                        .addParagraph(p -> p.text(" "))
                        .addParagraph(p -> p.text(" "))
                        .add(bar())
                        .build()))) {
            Map<String, List<CTAnchor>> carried = carriedByParagraph(document);

            assertThat(carried.keySet()).as("the block's one paragraph of text").containsExactly("Profile");
            assertThat(points(carried.get("Profile").get(0).getPositionV().getPosOffset()))
                    .as("placed down from it, however far").isGreaterThan(20.0);
        }
    }

    @Test
    void aDrawingOfABlockThatWritesNoParagraphStillGoesToTheNearestTextOnItsPage() throws Exception {
        // Nothing of the holder's is on the page, so the fallback is unchanged: the nearest text.
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> page
                .addParagraph(p -> p.text("Head of the page"))
                .addParagraph(p -> p.text("Beside the rule"))
                .add(new SectionBuilder().name("Rule").add(bar()).build()))) {
            Map<String, List<CTAnchor>> carried = carriedByParagraph(document);

            assertThat(carried.keySet()).as("the nearest text, not the first on the page")
                    .containsExactly("Beside the rule");
            assertThat(points(carried.get("Beside the rule").get(0).getPositionV().getPosOffset()))
                    .as("just below it").isCloseTo(12.0, within(12.0));
        }
    }

    /** The drawings each paragraph of the body carries, by the start of that paragraph's text. */
    private static Map<String, List<CTAnchor>> carriedByParagraph(XWPFDocument document) {
        Map<String, List<CTAnchor>> carried = new LinkedHashMap<>();
        for (XmlObject found : document.getDocument().selectPath("declare namespace w='"
                                                                 + "http://schemas.openxmlformats.org/wordprocessingml/2006/main'"
                                                                 + " .//w:p")) {
            XWPFParagraph paragraph = document.getParagraph((CTP) found);
            if (paragraph == null) {
                continue;
            }
            List<CTAnchor> anchors = anchorsIn(found);
            if (!anchors.isEmpty()) {
                carried.computeIfAbsent(paragraph.getText().trim(), ignored -> new ArrayList<>()).addAll(anchors);
            }
        }
        return carried;
    }

    private static List<CTAnchor> anchorsIn(XmlObject root) {
        List<CTAnchor> anchors = new ArrayList<>();
        for (XmlObject found : root.selectPath("declare namespace wp='"
                                               + "http://schemas.openxmlformats.org/drawingml/2006/wordprocessingDrawing'"
                                               + " .//wp:anchor")) {
            anchors.add((CTAnchor) found);
        }
        return anchors;
    }

    private static double points(Object emu) {
        return ((Number) emu).doubleValue() / 12700.0;
    }
}
