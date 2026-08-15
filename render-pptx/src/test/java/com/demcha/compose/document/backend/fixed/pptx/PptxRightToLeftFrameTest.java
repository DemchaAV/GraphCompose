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
    void aChipSplitsAtEveryLevelBoundaryItHolds() throws Exception {
        // Three runs, not one: the brackets take the right-to-left level from the line
        // around them while the comparison between them stays left-to-right. Each gets
        // its own frame, placed left to right, so the viewer has nothing to re-resolve —
        // and the comparison survives as typed. Reversed whole, which is what a single
        // frame invited, this chip stored "a < b": inverted meaning in the only copy of
        // the text the file has.
        assertThat(chipRunsOf("(a > b)"))
                .describedAs("bracket, comparison, bracket — three frames, in that order")
                .containsExactly("(", "a > b", ")");
    }

    @Test
    void aChipHoldingHebrewAndAYearStoresItsLettersInLogicalOrder() throws Exception {
        // The case a visual-order hand-off breaks: strong letters are reordered by
        // PowerPoint's own engine whatever the frame declares, so letters stored
        // pre-reversed would display re-reversed — scrambled. Each run goes over as the
        // author typed it, and the digits are a run of their own because they resolve to
        // the other level.
        assertThat(chipRunsOf("שנה 2026"))
                .describedAs("digits placed left of the word, both as written")
                .containsExactly("2026", "שנה ");
    }

    @Test
    void aSingleLevelChipGetsThePlainSpanTreatment() throws Exception {
        // A chip that is wholly right-to-left has nothing mixed to settle: it keeps
        // logical order for PowerPoint to place, declares its direction, and its
        // paired punctuation is pre-mirrored — exactly what a plain span gets.
        // Built through highlight() so the chip run carries the Hebrew-capable font
        // itself; chip() leaves the run's font to the default, which has no Hebrew.
        List<XSLFTextParagraph> chipFrames = paragraphsOf(
                renderHighlightLine("(" + "שנה" + ")")).stream()
                .filter(paragraph -> paragraph.getText().contains("שנה"))
                .toList();

        assertThat(chipFrames).describedAs("the chip's own frame is found").isNotEmpty();
        assertThat(chipFrames)
                .describedAs("a logical-order frame must say which way it reads")
                .allMatch(PptxRightToLeftFrameTest::declaresRightToLeft);
        assertThat(chipFrames)
                .describedAs("one uniformly right-to-left run, so one frame, mirrored — "
                        + "the case the viewer reverses for itself and does not swap")
                .allSatisfy(paragraph -> assertThat(paragraph.getText())
                        .isEqualTo(")" + "שנה" + "("));
    }

    @Test
    void aChipThatOpensOnLatinCarriesNothingToMirrorWhenItHoldsOnlyLetters() throws Exception {
        // The chip's flag is its first character's, so this one is left-to-right. Its
        // Hebrew still needs no help: PowerPoint orders the letters itself, and with no
        // neutral in the chip there is nothing to mirror for it either.
        assertThat(chipRunsOf("a בית"))
                .describedAs("the Latin placed first, the Hebrew after it, both as typed")
                .containsExactly("a ", "בית");
    }

    @Test
    void aChipThatOpensOnLatinSplitsAroundALoneNeutral() throws Exception {
        // A neutral standing between two right-to-left words takes THEIR level even under
        // a left-to-right base, so it belongs to the Hebrew run rather than to the Latin
        // one — and that run, being uniformly right-to-left, is the kind the viewer
        // reverses itself, so its comparison is mirrored on the way over.
        assertThat(chipRunsOf("a בית > ספר", "בית"))
                .describedAs("Latin first, then one right-to-left run carrying the neutral")
                .containsExactly("a ", "בית < ספר");
    }

    @Test
    void aChipThatOpensOnLatinSplitsAtItsLevelBoundary() throws Exception {
        // The whole point of splitting. Handed over as one frame, this chip reached
        // PowerPoint as a fragment with no line around it, its engine re-resolved it on
        // its own terms, and the deck disagreed with the PDF about where the Latin sat.
        // One frame per directional run leaves it nothing to re-resolve.
        assertThat(chipRunsOf("a בית (ספר)", "בית"))
                .describedAs("Latin first, then the Hebrew run with its pair mirrored")
                .containsExactly("a ", "בית )ספר(");
    }

    /**
     * The chip's frames, left to right — a chip is one frame per directional run.
     *
     * <p>Filtered by a marker where the line's own words would otherwise be picked up
     * too; the chip's runs are the frames that are not the surrounding text.</p>
     */
    private static List<String> chipRunsOf(String chipText) throws Exception {
        return chipRunsOf(chipText, null);
    }

    private static List<String> chipRunsOf(String chipText, String marker) throws Exception {
        byte[] pptx = renderHighlightLine(chipText);
        try (XMLSlideShow show = new XMLSlideShow(new ByteArrayInputStream(pptx))) {
            List<XSLFTextShape> chipBoxes = new ArrayList<>();
            for (XSLFShape shape : show.getSlides().get(0).getShapes()) {
                if (shape instanceof XSLFTextShape box
                        && "GraphCompose Inline Chip Text".equals(box.getShapeName())
                        && !box.getText().isBlank()) {
                    chipBoxes.add(box);
                }
            }
            chipBoxes.sort(java.util.Comparator.comparingDouble(box -> box.getAnchor().getX()));
            return chipBoxes.stream().map(XSLFTextShape::getText).toList();
        }
    }

    /** The text stored for the chip frame carrying {@code marker}. */
    private static String storedChipOf(String chipText, String marker) throws Exception {
        List<XSLFTextParagraph> frames = paragraphsOf(renderHighlightLine(chipText)).stream()
                .filter(paragraph -> paragraph.getText().contains(marker))
                .toList();

        assertThat(frames).describedAs("the chip's own frame is found").hasSize(1);
        return frames.get(0).getText();
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
        return renderChipLine(CHIP);
    }

    /** As {@link #renderChipLine(String)}, but the chip run carries its own full style. */
    private static byte[] renderHighlightLine(String chipText) {
        try (DocumentSession document = GraphCompose.document()
                .pageSize(420, 140)
                .margin(DocumentInsets.of(20))
                .create()) {

            document.pageFlow(page -> page.addParagraph(p -> p
                    .rich(rich -> rich.plain("שלום ").highlight(chipText,
                            DocumentTextStyle.builder()
                                    .fontName(FontName.DAVID_LIBRE).size(15).build(),
                            DocumentColor.rgb(0xEF, 0xF1, 0xF3),
                            3, DocumentInsets.of(2)))
                    .direction(TextDirection.RTL)
                    .textStyle(DocumentTextStyle.builder()
                            .fontName(FontName.DAVID_LIBRE).size(15).build())));

            return document.toPptxBytes();
        }
    }

    private static byte[] renderChipLine(String chipText) {
        try (DocumentSession document = GraphCompose.document()
                .pageSize(420, 140)
                .margin(DocumentInsets.of(20))
                .create()) {

            document.pageFlow(page -> page.addParagraph(p -> p
                    .rich(rich -> rich.plain("שלום ").chip(chipText,
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
