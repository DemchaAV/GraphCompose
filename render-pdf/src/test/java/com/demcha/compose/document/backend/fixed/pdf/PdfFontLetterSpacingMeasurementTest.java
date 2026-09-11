package com.demcha.compose.document.backend.fixed.pdf;

import com.demcha.compose.engine.components.content.text.TextDecoration;
import com.demcha.compose.engine.components.content.text.TextStyle;
import com.demcha.compose.engine.render.pdf.PdfFont;
import com.demcha.compose.font.FontLibrary;
import com.demcha.compose.font.FontName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * What tracking does to a measured width.
 *
 * <p>Held to the rule {@link PdfCharacterSpacingContractTest} measured off
 * PDFBox — one unit per code point, trailing unit included — because the
 * measurement and the pen have to produce the same number. A test that invented
 * its own expectation here would pass while the page drifted.</p>
 */
class PdfFontLetterSpacingMeasurementTest {

    private static final FontName FAMILY = FontName.LATO;
    private static final double SIZE = 12.0;

    private static PDDocument document;
    private static PdfFont font;

    @BeforeAll
    static void loadFont() {
        document = new PDDocument();
        FontLibrary library = PdfFontLibraryFactory.library(document, List.of());
        font = library.getFont(FAMILY, PdfFont.class).orElseThrow();
    }

    @AfterAll
    static void closeDocument() throws Exception {
        document.close();
    }

    private static TextStyle style(double letterSpacing) {
        return new TextStyle(FAMILY, SIZE, TextDecoration.DEFAULT, Color.BLACK, letterSpacing);
    }

    private static double units(String text, double spacing) {
        return font.getTextWidth(style(spacing), text) - font.getTextWidth(style(0), text);
    }

    @Test
    void withoutTrackingTheWidthIsTheIdenticalDoubleItAlwaysWas() {
        // Not "close to" — the same bits. The zero path short-circuits rather
        // than adding a zero, so every existing document measures as before.
        String text = "The quick brown fox jumps over the lazy dog";

        TextStyle legacy = new TextStyle(FAMILY, SIZE, TextDecoration.DEFAULT, Color.BLACK);
        TextStyle explicitZero = style(0.0);

        assertThat(font.getTextWidth(explicitZero, text))
                .isEqualTo(font.getTextWidth(legacy, text));
    }

    @Test
    void positiveTrackingAddsOneUnitPerCodePoint() {
        assertThat(units("JANE", 2.0)).isCloseTo(4 * 2.0, within(1e-9));
        assertThat(units("J", 2.0)).isCloseTo(2.0, within(1e-9));
    }

    @Test
    void aNormalSpaceCountsAsACodePointLikeAnyOther() {
        // "JANE DOE" is eight code points, not seven: the space is tracked too,
        // which is exactly what Tc does to the pen.
        assertThat(units("JANE DOE", 2.0)).isCloseTo(8 * 2.0, within(1e-9));
    }

    @Test
    void negativeTrackingTightensByTheSameRule() {
        assertThat(units("JANE", -0.5)).isCloseTo(4 * -0.5, within(1e-9));
        assertThat(font.getTextWidth(style(-0.5), "JANE"))
                .isLessThan(font.getTextWidth(style(0), "JANE"));
    }

    @Test
    void anEmptyStringIsZeroWideWhateverTheTracking() {
        assertThat(font.getTextWidth(style(5.0), "")).isZero();
        assertThat(font.getTextWidth(style(0), "")).isZero();
    }

    @Test
    void aSupplementaryCodePointCostsOneUnitNotTwo() {
        // No bundled face can encode one, so sanitizeForRender folds it to '?'
        // before measurement — one char, one code point, one unit. Counting
        // Java chars on the raw string would bill it twice.
        String astral = new String(Character.toChars(0x1D400));
        assertThat(astral.length()).isEqualTo(2);
        assertThat(astral.codePointCount(0, astral.length())).isEqualTo(1);

        assertThat(units(astral, 3.0)).isCloseTo(3.0, within(1e-9));
        assertThat(units("A" + astral + "B", 3.0)).isCloseTo(3 * 3.0, within(1e-9));
    }

    @Test
    void trackingIsBilledOnTheSanitizedStringNotTheAuthorsOne() {
        // A control character collapses to a single space before drawing, so it
        // is billed once — as the thing that is actually drawn, not as what was
        // typed. Measuring the raw string would bill a glyph that never appears.
        String withControl = "AB\u0000CD";
        String sanitized = font.sanitizeForRender(style(0), withControl);

        assertThat(sanitized).hasSize(5);
        assertThat(units(withControl, 2.0))
                .isCloseTo(sanitized.codePointCount(0, sanitized.length()) * 2.0, within(1e-9));
    }

    @Test
    void theTwoEntryPointsAgreeAndNeitherBillsTrackingTwice() {
        String alreadySanitized = "JANE";

        double trackedSanitizing = font.getTextWidth(style(2.0), alreadySanitized);
        double trackedNoSanitize = font.getTextWidthNoSanitize(style(2.0), alreadySanitized);

        // The tracking each one adds is the same number, exactly: one rule, one
        // string, applied once by each entry point and never twice. Four units,
        // not eight.
        assertThat(trackedNoSanitize - font.getTextWidthNoSanitize(style(0), alreadySanitized))
                .isEqualTo(4 * 2.0);
        assertThat(trackedSanitizing - font.getTextWidth(style(0), alreadySanitized))
                .isEqualTo(4 * 2.0);

        // The absolute widths agree only to float precision, and did so before
        // tracking existed: getTextWidthNoSanitize computes in float while
        // getTextWidth computes in double. That is pre-existing and untouched
        // here — it is asserted so the gap is documented rather than discovered.
        assertThat(trackedNoSanitize).isCloseTo(trackedSanitizing, within(1e-4));
    }

    @Test
    void trackingScalesTheWholeRunNotJustItsEnds() {
        // Guards the N vs N-1 boundary at the measurement layer: a one-code-point
        // string gets a full unit, which N-1 would make zero.
        assertThat(units("J", 4.0)).isCloseTo(4.0, within(1e-9)).isNotCloseTo(0.0, within(1e-6));
    }
}
