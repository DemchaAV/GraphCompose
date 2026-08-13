package com.demcha.compose.document.backend.fixed.pptx;

import static org.assertj.core.api.Assertions.assertThat;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.node.TextDirection;
import com.demcha.compose.document.style.DocumentColor;
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
 * <p>Declaring the direction is necessary and not sufficient. It fixes <em>placement</em>:
 * the em-dash of a mixed line moved to the side it belongs on the moment it was written.
 * It does not make PowerPoint mirror the character — measured on a slide, a bracket
 * closing a right-to-left line still faced the way it was typed — so the swap is done
 * here too, as it is at the PDF's own seam. The cost, stated because it is real: a copy
 * out of the slide carries the mirrored bracket rather than the typed one.</p>
 */
class PptxRightToLeftFrameTest {

    /** Hebrew, then a Latin phrase in brackets: the brackets are the neutrals at issue. */
    private static final String MIXED = "2026 שנה טובה (AUTO resolves it)";
    private static final String LATIN = "Plain left-to-right text";
    /** A left-to-right line whose brackets must survive the mirroring gate untouched. */
    private static final String BRACKETED_LATIN = "Revenue (Q4)";
    /** A chip whose interior sits at the level opposite the one the chip is given. */
    private static final String CHIP = "(a > b)";

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
    void aBracketIsMirroredBeforeItIsHandedOver() throws Exception {
        // Declaring the direction gets a neutral placed on the correct side — the em-dash
        // of a mixed line moved the moment that was written — but PowerPoint does not go
        // on to mirror the character, so the bracket kept facing the way it was typed.
        // Measured on a slide: ")AUTO resolves it (" against the PDF's "(AUTO resolves
        // it)". So the swap happens here, as it does at the PDF's own seam.
        //
        // Each frame is its own paragraph, so the check is per frame: the one the author
        // ended with ')' is drawn leftmost and must therefore carry '(' — the bracket
        // that opens the phrase where a reader meets it.
        List<String> frames = texts(paragraphsOf(render(MIXED, TextDirection.RTL)));

        assertThat(frames)
                .describedAs("the lone bracket is stored as the one that draws correctly; "
                        + "frames were %s", frames)
                .contains("(");
        assertThat(frames.stream().filter(text -> text.contains("שנה טובה")).toList())
                .describedAs("and the one sharing a frame with Hebrew is swapped too")
                .isNotEmpty()
                .allSatisfy(text -> assertThat(text).contains(")").doesNotContain("("));
    }

    @Test
    void aLeftToRightLineDeclaresNothing() throws Exception {
        // The control: the path every existing deck takes must be untouched.
        assertThat(paragraphsOf(render(LATIN, TextDirection.LTR)))
                .describedAs("nothing about a left-to-right line changed")
                .noneMatch(PptxRightToLeftFrameTest::declaresRightToLeft);
    }

    @Test
    void aLeftToRightLineStoresItsBracketsAsTyped() throws Exception {
        // The other side of the mirroring gate. Made unconditional, this swap rewrites
        // every deck that ever drew a parenthesis: "Revenue (Q4)" would be stored as
        // "Revenue )Q4(" with nothing in the suite objecting.
        assertThat(texts(paragraphsOf(render(BRACKETED_LATIN, TextDirection.LTR))))
                .describedAs("a left-to-right line is handed the characters the author typed")
                .allSatisfy(text -> assertThat(text).doesNotContain(")Q4("));
    }

    @Test
    void aChipInsideARightToLeftLineDeclaresItsDirection() throws Exception {
        // The second frame-declaring site. Nothing else in the suite reaches it, so
        // reverting it alone left an undeclared frame and every test still green.
        // Located by the chip's letters rather than by its full text, so this case keeps
        // failing for its own reason if the mirroring exemption is ever lost.
        List<XSLFTextParagraph> chipFrames = paragraphsOf(renderChipLine()).stream()
                .filter(paragraph -> paragraph.getText().contains("a")
                        && paragraph.getText().contains("b"))
                .toList();

        assertThat(chipFrames).describedAs("the chip's own frame is found").isNotEmpty();
        assertThat(chipFrames)
                .describedAs("the chip frame says which way it reads")
                .allMatch(PptxRightToLeftFrameTest::declaresRightToLeft);
    }

    @Test
    void aChipKeepsThePunctuationItsInteriorLevelIsEntitledTo() throws Exception {
        // A chip is one rounded fill, so the wrapper cannot split it at a level boundary
        // and gives it its first character's level whole. Mirroring such a span whole
        // reaches an interior that sits at the opposite level, where UAX #9 L4 mirrors
        // nothing: "(a > b)" after a Hebrew word came out ")a < b(", inverting a
        // comparison in the only copy of the text the file has.
        assertThat(texts(paragraphsOf(renderChipLine())))
                .filteredOn(text -> text.contains("a ") && text.contains(" b"))
                .describedAs("the chip's text is found in the deck")
                .isNotEmpty()
                .allSatisfy(text -> assertThat(text)
                        .describedAs("the comparison reads the way it was written")
                        .contains(">")
                        .doesNotContain("<"));
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

    /**
     * A Hebrew line carrying an inline chip whose interior is Latin.
     *
     * <p>The chip's leading bracket sits next to Hebrew, so the wrapper gives the whole
     * chip a right-to-left level — while {@code a > b} inside it stays left-to-right.
     * That gap between the flag and the span is what the two chip cases exercise.</p>
     */
    private static byte[] renderChipLine() {
        try (DocumentSession document = GraphCompose.document()
                .pageSize(420, 140)
                .margin(DocumentInsets.of(20))
                .create()) {

            document.pageFlow(page -> page.addParagraph(p -> p
                    .rich(rich -> rich.plain("שלום ").chip(CHIP,
                            DocumentColor.rgb(0x24, 0x29, 0x2F),
                            DocumentColor.rgb(0xEF, 0xF1, 0xF3)))
                    .direction(TextDirection.RTL)
                    .textStyle(DocumentTextStyle.builder()
                            .fontName(FontName.DAVID_LIBRE).size(15).build())));

            return document.toPptxBytes();
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
