package com.demcha.compose.document.backend.fixed.pdf;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * What PDFBox's {@code Tc} operator actually does to the pen — measured, and
 * pinned.
 *
 * <p>The whole feature rests on one number: how many spacing units a string of
 * N code points adds to the pen advance. The specification can be read either
 * way by a careful person and CSS {@code letter-spacing} is a different
 * product, so this measures it rather than citing it. {@code PdfFont} adds the
 * same number to its measurement; if PDFBox ever changes the rule, this test
 * says so instead of leaving every tracked line silently misplaced.</p>
 *
 * <p>The measurement trick is a marker glyph drawn immediately after the
 * subject inside the same {@code BT}/{@code ET} with no repositioning: the
 * marker's left edge <em>is</em> the pen position the subject left behind,
 * trailing spacing included. Reading the subject's own ink extent instead would
 * report N-1 by construction and prove nothing.</p>
 */
class PdfCharacterSpacingContractTest {

    private static final String FONT = "fonts/google/lato/Lato-Regular.ttf";
    private static final float SIZE = 12f;
    private static final float START_X = 50f;
    /** Large enough that N and N-1 cannot be confused with rounding. */
    private static final float SPACING = 5f;
    private static final String MARKER = "|";

    /**
     * No bundled family encodes a supplementary code point, so one can never
     * reach the backend — {@code sanitizeForRender} folds it to {@code '?'}
     * first. That case is covered through the production path instead; putting
     * it here would only prove that PDFBox throws on a glyph it does not have.
     */
    @ParameterizedTest(name = "[{index}] \"{0}\" -> {1} unit(s)")
    @CsvSource({
            "JANE,      4",   // plain ASCII
            "'JANE DOE',8",   // a normal space is a code point like any other
            "J,         1",   // one code point
            "JOSÉ,      4",   // a covered non-ASCII code point is still one unit
    })
    void trackingAddsOneUnitPerCodePointIncludingTheTrailingOne(String subject, int expectedUnits)
            throws Exception {
        double untracked = advanceOf(subject, 0f);
        double tracked = advanceOf(subject, SPACING);

        assertThat(tracked - untracked)
                .as("%s: %d code points at %s pt of tracking", subject,
                        subject.codePointCount(0, subject.length()), SPACING)
                .isCloseTo(expectedUnits * SPACING, within(0.01));
    }

    @Test
    void theRuleIsNotOffByOneAgainstTheCodePointCount() throws Exception {
        // Stated separately so a change from N to N-1 reads as what it is
        // rather than as four arithmetic failures.
        String subject = "JANE";
        int codePoints = subject.codePointCount(0, subject.length());

        assertThat(codePoints).isEqualTo(4);
        assertThat(advanceOf(subject, SPACING) - advanceOf(subject, 0f))
                .as("N (trailing unit included), not N-1")
                .isCloseTo(codePoints * SPACING, within(0.01))
                .isNotCloseTo((codePoints - 1) * SPACING, within(0.01));
    }

    @Test
    void theEmptyStringTakesNoTrackingAtAll() throws Exception {
        assertThat(advanceOf("", SPACING)).isCloseTo(advanceOf("", 0f), within(0.001));
        assertThat(advanceOf("", SPACING)).isCloseTo(0.0, within(0.001));
    }

    @Test
    void trackingDoesNotTouchTheTextLayer() throws Exception {
        assertThat(extractionOf("JANE DOE", SPACING)).isEqualTo("JANE DOE|");
        assertThat(extractionOf("JANE DOE", 0f)).isEqualTo("JANE DOE|");
    }

    private static double advanceOf(String subject, float spacing) throws IOException {
        List<TextPosition> positions = positions(render(subject, spacing));
        TextPosition marker = positions.get(positions.size() - 1);
        return marker.getXDirAdj() - START_X;
    }

    private static String extractionOf(String subject, float spacing) throws IOException {
        StringBuilder extracted = new StringBuilder();
        for (TextPosition position : positions(render(subject, spacing))) {
            extracted.append(position.getUnicode());
        }
        return extracted.toString();
    }

    /** Draws {@code subject} at {@code spacing}, then a marker with no repositioning. */
    private static byte[] render(String subject, float spacing) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            PDFont font = loadFont(document);

            try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                stream.beginText();
                stream.setFont(font, SIZE);
                stream.newLineAtOffset(START_X, 700);
                stream.setCharacterSpacing(spacing);
                if (!subject.isEmpty()) {
                    stream.showText(subject);
                }
                stream.showText(MARKER);
                stream.endText();
            }
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            document.save(bytes);
            return bytes.toByteArray();
        }
    }

    private static PDFont loadFont(PDDocument document) throws IOException {
        try (InputStream ttf = PdfCharacterSpacingContractTest.class
                .getClassLoader().getResourceAsStream(FONT)) {
            if (ttf == null) {
                throw new IllegalStateException("font resource missing: " + FONT);
            }
            return PDType0Font.load(document, ttf, true);
        }
    }

    private static List<TextPosition> positions(byte[] pdf) throws IOException {
        List<TextPosition> all = new ArrayList<>();
        try (PDDocument document = Loader.loadPDF(pdf)) {
            new PDFTextStripper() {
                @Override
                protected void writeString(String text, List<TextPosition> positions) {
                    all.addAll(positions);
                }
            }.getText(document);
        }
        return all;
    }
}
