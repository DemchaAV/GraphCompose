package com.demcha.compose.document.backend.fixed.pptx;

import static org.assertj.core.api.Assertions.assertThat;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.node.TextDirection;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.font.FontName;

import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFTextParagraph;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Holds a reordered line's frames to declaring which way they read.
 *
 * <p>A right-to-left line reaches PowerPoint as one frame per span, each pinned where the
 * layout put it — so the order <em>across</em> the line is settled before PowerPoint sees
 * it. What is not settled is what happens inside a frame. The text handed over is logical,
 * and paired punctuation still has to be mirrored (UAX #9 L4) and neutrals still have to
 * fall on the correct side; PowerPoint does both, from the paragraph's base direction.</p>
 *
 * <p>Without that direction the default is left-to-right, and a frame holding a lone
 * bracket has nothing to resolve against — so a parenthesis closing a right-to-left line
 * was drawn facing the way it was typed rather than the way the line reads, while the same
 * document as a PDF was correct.</p>
 *
 * <p>What the slide draws is PowerPoint's to decide, so this pins the two things the file
 * is responsible for: the frames that carry right-to-left text say so, and the text inside
 * them is still the author's — mirroring here instead would have baked reversed brackets
 * into what a reader copies out.</p>
 */
class PptxRightToLeftFrameTest {

    /** Hebrew, then a Latin phrase in brackets: the brackets are the neutrals at issue. */
    private static final String MIXED = "2026 שנה טובה (AUTO resolves it)";
    private static final String LATIN = "Plain left-to-right text";

    @Test
    void everyFrameCarryingRightToLeftTextDeclaresItsDirection() throws Exception {
        List<XSLFTextParagraph> paragraphs = paragraphsOf(render(MIXED, TextDirection.RTL));

        assertThat(paragraphs).describedAs("the line is drawn as several frames")
                .hasSizeGreaterThan(1);
        assertThat(paragraphs.stream().filter(PptxRightToLeftFrameTest::declaresRightToLeft).count())
                .describedAs("without this a frame holding a lone bracket has no direction to "
                        + "mirror it against; paragraphs were %s", texts(paragraphs))
                .isGreaterThan(0);
    }

    @Test
    void theBracketSharingAFrameWithHebrewIsInADeclaredFrame() throws Exception {
        // The specific failure: the frame whose text ends on '(' after Hebrew. Left
        // undeclared, PowerPoint reads the paragraph as left-to-right and draws the bracket
        // opening away from the phrase it closes.
        List<XSLFTextParagraph> paragraphs = paragraphsOf(render(MIXED, TextDirection.RTL));

        XSLFTextParagraph bracket = paragraphs.stream()
                .filter(paragraph -> paragraph.getText().contains("(")
                        || paragraph.getText().contains(")"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no frame carries a bracket: "
                        + texts(paragraphs)));

        assertThat(declaresRightToLeft(bracket))
                .describedAs("the frame carrying %s must say which way it reads",
                        bracket.getText())
                .isTrue();
    }

    @Test
    void theTextInsideStaysTheAuthorsRatherThanTheMirroredForm() throws Exception {
        // Declared rather than mirrored on purpose: what a reader copies out of the slide
        // is the written word, and PowerPoint's own Arabic shaper still gets the letters it
        // expects. Mirroring in the backend would have looked identical and cost both.
        String slideText = String.join("", texts(paragraphsOf(render(MIXED, TextDirection.RTL))));

        assertThat(slideText)
                .describedAs("brackets as typed, not swapped by us")
                .contains("(")
                .contains(")");
        assertThat(slideText).contains("שנה טובה");
    }

    @Test
    void aLeftToRightLineDeclaresNothing() throws Exception {
        // The control: the path every existing deck takes must be untouched.
        assertThat(paragraphsOf(render(LATIN, TextDirection.LTR)))
                .describedAs("nothing about a left-to-right line changed")
                .noneMatch(PptxRightToLeftFrameTest::declaresRightToLeft);
    }

    private static boolean declaresRightToLeft(XSLFTextParagraph paragraph) {
        var properties = paragraph.getXmlObject().getPPr();
        return properties != null && properties.isSetRtl() && properties.getRtl();
    }

    private static List<String> texts(List<XSLFTextParagraph> paragraphs) {
        return paragraphs.stream().map(XSLFTextParagraph::getText).toList();
    }

    private static List<XSLFTextParagraph> paragraphsOf(byte[] pptx) throws Exception {
        try (XMLSlideShow show = new XMLSlideShow(new ByteArrayInputStream(pptx))) {
            List<XSLFTextParagraph> paragraphs = new ArrayList<>();
            for (XSLFShape shape : show.getSlides().get(0).getShapes()) {
                if (shape instanceof XSLFTextShape text) {
                    for (XSLFTextParagraph paragraph : text.getTextParagraphs()) {
                        if (!paragraph.getText().isBlank()) {
                            paragraphs.add(paragraph);
                        }
                    }
                }
            }
            return paragraphs;
        }
    }

    private static byte[] render(String text, TextDirection direction) {
        try (DocumentSession document = GraphCompose.document()
                .pageSize(420, 140)
                .margin(DocumentInsets.of(20))
                .create()) {

            document.pageFlow(page -> page.addParagraph(p -> p.text(text)
                    .direction(direction)
                    .textStyle(DocumentTextStyle.builder()
                            .fontName(FontName.DAVID_LIBRE).size(15).build())));

            return document.toPptxBytes();
        }
    }
}
