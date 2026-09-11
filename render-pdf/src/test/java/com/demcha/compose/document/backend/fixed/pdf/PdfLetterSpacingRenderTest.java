package com.demcha.compose.document.backend.fixed.pdf;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.node.DocumentLinkOptions;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentLetterSpacing;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.font.FontName;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.contentstream.operator.Operator;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSNumber;
import org.apache.pdfbox.pdfparser.PDFStreamParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Tracking on a real page: what it moves, and what it must not touch.
 *
 * <p>The point of the feature is that the picture changes and the text does
 * not. A headline set in spaced caps has to still <em>be</em> "JANE DOE" to
 * anything that reads the file — search, copy/paste, a screen reader, an
 * applicant-tracking parser — which is exactly what padding the string with
 * spaces destroys.</p>
 *
 * <p>The geometry cases are not four more implementations being checked.
 * Alignment, wrapping, decoration rules and link rectangles all consume one
 * measured span width, so they are evidence that the width carries tracking,
 * not evidence that four code paths each learned about it.</p>
 *
 * <p>Geometry is read through {@link DrawnPen} rather than the text stripper:
 * tracked runs state their own {@code ActualText}, and a stripper honouring
 * that reports the stated string instead of the glyphs it covers.</p>
 */
class PdfLetterSpacingRenderTest {

    private static final String NAME = "JANE DOE";
    private static final FontName FAMILY = FontName.LATO;

    private static DocumentTextStyle style(DocumentLetterSpacing spacing) {
        return DocumentTextStyle.builder()
                .fontName(FAMILY).size(20).letterSpacing(spacing).build();
    }

    private static DocumentTextStyle underlined(DocumentLetterSpacing spacing) {
        return DocumentTextStyle.builder()
                .fontName(FAMILY).size(20)
                .decoration(DocumentTextDecoration.UNDERLINE)
                .letterSpacing(spacing).build();
    }

    // --- the text layer -------------------------------------------------

    @Test
    void aTrackedHeadlineStillReadsAsTheWordThatWasWritten() throws Exception {
        byte[] pdf = render(page -> page.addParagraph(p -> p
                .text(NAME).textStyle(style(DocumentLetterSpacing.ofFontSize(0.3)))));

        // Not "J A N E   D O E". The string was never touched; only the pen was.
        assertThat(extractedText(pdf)).isEqualTo(NAME);
    }

    @Test
    void theTextLayerSurvivesTrackingWideEnoughToFoolAnExtractor() throws Exception {
        // Without the run stating its own text this comes back "J A N E  D O E":
        // an extractor decides where words are by how far apart glyphs sit, and
        // tracking is the act of moving them apart. The file was always right —
        // eight glyphs, correct ToUnicode — and a reader still got it wrong.
        for (double points : new double[] {2, 4, 8, 16}) {
            byte[] pdf = render(page -> page.addParagraph(p -> p
                    .text(NAME).textStyle(style(DocumentLetterSpacing.points(points)))));

            assertThat(extractedText(pdf))
                    .as("extraction at %s pt of tracking", points)
                    .isEqualTo(NAME);
        }
    }

    @Test
    void aTrackedRunDrawsTheGlyphsItWasGivenAndNoExtraSpaces() throws Exception {
        byte[] tracked = render(page -> page.addParagraph(p -> p
                .text(NAME).textStyle(style(DocumentLetterSpacing.points(4)))));

        // One glyph per character of the original string — the spaced-out
        // imitation this replaces would have painted seven extra space glyphs.
        assertThat(DrawnPen.placements(tracked)).hasSize(NAME.length());
    }

    @Test
    void trackingActuallySpreadsTheGlyphs() throws Exception {
        byte[] plain = render(page -> page.addParagraph(p -> p.text(NAME)
                .textStyle(style(DocumentLetterSpacing.NONE))));
        byte[] tracked = render(page -> page.addParagraph(p -> p.text(NAME)
                .textStyle(style(DocumentLetterSpacing.points(4)))));

        // Seven steps between eight glyphs, each four points longer. The eighth
        // unit is past the last glyph, so no glyph records it — which is exactly
        // why measurement counts N and this counts N-1.
        assertThat(DrawnPen.firstToLastX(tracked) - DrawnPen.firstToLastX(plain))
                .isCloseTo(7 * 4.0, within(0.01));
    }

    @Test
    void everyStepIsWidenedByExactlyTheTracking() throws Exception {
        List<DrawnPen.Placement> plain = DrawnPen.placements(
                render(page -> page.addParagraph(p -> p.text(NAME)
                        .textStyle(style(DocumentLetterSpacing.NONE)))));
        List<DrawnPen.Placement> tracked = DrawnPen.placements(
                render(page -> page.addParagraph(p -> p.text(NAME)
                        .textStyle(style(DocumentLetterSpacing.points(4))))));

        assertThat(tracked).hasSameSizeAs(plain);
        for (int i = 1; i < tracked.size(); i++) {
            assertThat(DrawnPen.step(tracked, i) - DrawnPen.step(plain, i))
                    .as("step %d", i)
                    .isCloseTo(4.0, within(0.01));
        }
    }

    // --- text-state management ------------------------------------------

    @Test
    void trackingDoesNotLeakIntoTheParagraphAfterIt() throws Exception {
        byte[] mixed = render(page -> {
            page.addParagraph(p -> p.text(NAME).textStyle(style(DocumentLetterSpacing.points(6))));
            page.addParagraph(p -> p.text(NAME).textStyle(style(DocumentLetterSpacing.NONE)));
        });
        byte[] reference = render(page -> {
            page.addParagraph(p -> p.text(NAME).textStyle(style(DocumentLetterSpacing.NONE)));
            page.addParagraph(p -> p.text(NAME).textStyle(style(DocumentLetterSpacing.NONE)));
        });

        List<DrawnPen.Placement> mixedGlyphs = DrawnPen.placements(mixed);
        List<DrawnPen.Placement> plainGlyphs = DrawnPen.placements(reference);
        assertThat(mixedGlyphs).hasSize(16);
        assertThat(plainGlyphs).hasSize(16);

        // First paragraph's steps are six points longer...
        assertThat(DrawnPen.step(mixedGlyphs, 1) - DrawnPen.step(plainGlyphs, 1))
                .isCloseTo(6.0, within(0.01));
        // ...and the untracked paragraph after it is drawn exactly as if the
        // tracked one were not there. Tc is restored with the graphics state.
        for (int i = 9; i < 16; i++) {
            assertThat(DrawnPen.step(mixedGlyphs, i))
                    .as("second paragraph, step %d", i)
                    .isCloseTo(DrawnPen.step(plainGlyphs, i), within(0.01));
        }
    }

    @Test
    void trackedAndUntrackedRunsOnOneLineEachKeepTheirOwn() throws Exception {
        // Two runs inside one paragraph are drawn inside a single BT/ET on the
        // implicit pen, so this is where a leaked Tc would show: the second run
        // would spread and the line would end somewhere else entirely.
        List<DrawnPen.Placement> mixed = DrawnPen.placements(
                render(page -> page.addParagraph(p -> p
                        .inlineText("AAAA", style(DocumentLetterSpacing.points(5)))
                        .inlineText("BBBB", style(DocumentLetterSpacing.NONE)))));
        List<DrawnPen.Placement> neither = DrawnPen.placements(
                render(page -> page.addParagraph(p -> p
                        .inlineText("AAAA", style(DocumentLetterSpacing.NONE))
                        .inlineText("BBBB", style(DocumentLetterSpacing.NONE)))));

        assertThat(mixed).hasSize(8);
        assertThat(neither).hasSize(8);

        // Steps inside "AAAA" are five points longer...
        for (int i = 1; i < 4; i++) {
            assertThat(DrawnPen.step(mixed, i) - DrawnPen.step(neither, i))
                    .as("tracked run, step %d", i).isCloseTo(5.0, within(0.01));
        }
        // ...while steps inside "BBBB" are the untracked ones, however far the
        // A's pushed them to the right.
        for (int i = 5; i < 8; i++) {
            assertThat(DrawnPen.step(mixed, i))
                    .as("untracked run, step %d", i)
                    .isCloseTo(DrawnPen.step(neither, i), within(0.01));
        }
    }

    // --- consumers of the measured width --------------------------------

    @Test
    void rightAlignmentUsesTheTrackedWidth() throws Exception {
        double plainLast = DrawnPen.lastX(render(page -> page.addParagraph(p -> p
                .text(NAME).align(TextAlign.RIGHT).textStyle(style(DocumentLetterSpacing.NONE)))));
        double trackedLast = DrawnPen.lastX(render(page -> page.addParagraph(p -> p
                .text(NAME).align(TextAlign.RIGHT).textStyle(style(DocumentLetterSpacing.points(3))))));

        // A right-aligned line ends at the margin whatever its width, which only
        // holds if the aligner used the tracked width. The trailing unit is part
        // of that width, so the last glyph is placed one unit short of where the
        // untracked line's last glyph sat.
        assertThat(trackedLast).isCloseTo(plainLast - 3.0, within(0.05));
    }

    @Test
    void centreAlignmentUsesTheTrackedWidth() throws Exception {
        double plainFirst = DrawnPen.firstX(render(page -> page.addParagraph(p -> p
                .text(NAME).align(TextAlign.CENTER).textStyle(style(DocumentLetterSpacing.NONE)))));
        double trackedFirst = DrawnPen.firstX(render(page -> page.addParagraph(p -> p
                .text(NAME).align(TextAlign.CENTER).textStyle(style(DocumentLetterSpacing.points(3))))));

        // Centring a wider line starts it further left, by half the extra width
        // — half of all eight units, trailing one included.
        assertThat(plainFirst - trackedFirst).isCloseTo(8 * 3.0 / 2.0, within(0.05));
    }

    @Test
    void trackingMovesTheWrappingBoundary() throws Exception {
        // A phrase that fits on one line untracked and cannot once tracked. If
        // wrapping measured the untracked width, both would be one line.
        String phrase = "SENIOR ENGINEER";

        assertThat(lineCount(renderNarrow(phrase, DocumentLetterSpacing.NONE))).isEqualTo(1);
        assertThat(lineCount(renderNarrow(phrase, DocumentLetterSpacing.points(6))))
                .isGreaterThan(1);
    }

    @Test
    void anUnderlineCoversTheWholeTrackedRun() throws Exception {
        double plainRule = widestFilledRectangle(render(page -> page.addParagraph(p -> p
                .text(NAME).textStyle(underlined(DocumentLetterSpacing.NONE)))));
        double trackedRule = widestFilledRectangle(render(page -> page.addParagraph(p -> p
                .text(NAME).textStyle(underlined(DocumentLetterSpacing.points(4))))));

        // The decoration segment is built from the span's measured width, so a
        // wider run draws a wider rule rather than one that stops short of it.
        assertThat(trackedRule - plainRule).isCloseTo(8 * 4.0, within(0.5));
    }

    @Test
    void aLinkRectangleCoversTheWholeTrackedRun() throws Exception {
        double plainWidth = linkWidth(render(page -> page.addParagraph(p -> p
                .text(NAME).link(new DocumentLinkOptions("https://example.com"))
                .textStyle(style(DocumentLetterSpacing.NONE)))));
        double trackedWidth = linkWidth(render(page -> page.addParagraph(p -> p
                .text(NAME).link(new DocumentLinkOptions("https://example.com"))
                .textStyle(style(DocumentLetterSpacing.points(4))))));

        assertThat(trackedWidth - plainWidth).isCloseTo(8 * 4.0, within(0.5));
    }

    // --- helpers ---------------------------------------------------------

    private static int lineCount(byte[] pdf) throws IOException {
        return DrawnGlyphs.byLine(pdf).size();
    }

    private static String extractedText(byte[] pdf) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            return new PDFTextStripper().getText(document).trim();
        }
    }

    private static double linkWidth(byte[] pdf) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            List<PDAnnotation> annotations = document.getPage(0).getAnnotations();
            assertThat(annotations).isNotEmpty();
            return annotations.get(0).getRectangle().getWidth();
        }
    }

    /**
     * The widest rectangle the page fills — the underline rule, read off the
     * content stream rather than guessed from the text.
     */
    private static double widestFilledRectangle(byte[] pdf) throws IOException {
        double widest = 0.0;
        try (PDDocument document = Loader.loadPDF(pdf)) {
            PDFStreamParser parser = new PDFStreamParser(document.getPage(0));
            List<COSBase> operands = new ArrayList<>();
            Object token;
            double pendingWidth = 0.0;
            while ((token = parser.parseNextToken()) != null) {
                if (token instanceof COSBase operand) {
                    operands.add(operand);
                    continue;
                }
                if (token instanceof Operator operator) {
                    if ("re".equals(operator.getName()) && operands.size() >= 4) {
                        pendingWidth = ((COSNumber) operands.get(operands.size() - 2)).floatValue();
                    } else if ("f".equals(operator.getName()) || "f*".equals(operator.getName())) {
                        widest = Math.max(widest, pendingWidth);
                    }
                    operands.clear();
                }
            }
        }
        return widest;
    }

    private static byte[] render(Consumer<PageFlowBuilder> body) {
        try (DocumentSession document = GraphCompose.document()
                .pageSize(400, 200)
                .margin(DocumentInsets.of(20))
                .create()) {
            document.pageFlow(body);
            return document.toPdfBytes();
        }
    }

    private static byte[] renderNarrow(String text, DocumentLetterSpacing spacing) {
        try (DocumentSession document = GraphCompose.document()
                .pageSize(180, 200)
                .margin(DocumentInsets.of(10))
                .create()) {
            document.pageFlow(page -> page.addParagraph(p -> p.text(text).textStyle(
                    DocumentTextStyle.builder().fontName(FAMILY).size(14)
                            .letterSpacing(spacing).build())));
            return document.toPdfBytes();
        }
    }
}
