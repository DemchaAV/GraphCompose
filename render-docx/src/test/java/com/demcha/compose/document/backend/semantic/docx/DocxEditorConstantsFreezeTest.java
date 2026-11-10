package com.demcha.compose.document.backend.semantic.docx;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The numbers the Word export takes from an editor, from the file format or from a convention,
 * pinned as literals, each with where it came from.
 *
 * <p>Other tests name these constants by symbol: they pin the shape of a rule (which side of a
 * cell an editor's slack goes to, where a rule's ends are taken in), and a symbol keeps them true
 * whatever the value. This one pins the values. Changing one means checking it again against its
 * source — the editor it was measured in, the file format, the engine or the policy — and saying
 * so here, not editing an expectation to match.</p>
 *
 * <p>Where a source names an editor but not its version, the version is written as not recorded:
 * it is not guessed.</p>
 */
class DocxEditorConstantsFreezeTest {

    /**
     * Measured in Word on Windows (#946): on nine synthetic rules, half a point to 4pt thick,
     * Word drew a paragraph's border 1.36 to 1.49pt past its left indent and 1.44 to 1.59pt past
     * its right one. The constants are the means. Word's version is not recorded.
     * {@code DocxRuleTest} holds them in whole twips (28 and 30); this holds the exact values.
     */
    @Test
    void wordDrawsARuleThisFarPastItsIndents() {
        assertThat(DocxSemanticBackend.WORD_RULE_REACH_LEFT_POINTS).isEqualTo(1.42);
        assertThat(DocxSemanticBackend.WORD_RULE_REACH_RIGHT_POINTS).isEqualTo(1.51);
    }

    /**
     * Introduced with text headers and footers (#770) and measured in both editors since: through Word's COM,
     * 0.80 of the line within 0.1pt for Spectral, Lato and Arial at 10 to 46pt, and exactly 0.80
     * in LibreOffice (#789); then on the templates' fifteen faces and JetBrains Mono at 8 to 36pt,
     * Word within 0.04pt in median (#875). Neither editor's version is recorded.
     */
    @Test
    void bothEditorsStandAnExactLinesBaselineFourFifthsDownIt() {
        assertThat(DocxTextBands.BASELINE_SHARE).isEqualTo(0.8);
    }

    /**
     * Chosen after a measurement (#718): an auto column written exactly as wide as its widest
     * cell wrapped that cell in LibreOffice ("Additional storage, billed monthly in arrears"),
     * and a point more kept it on one line. LibreOffice's version is not recorded.
     */
    @Test
    void anAutoColumnIsGivenAPointForTheEditor() {
        assertThat(DocxSemanticBackend.EDITOR_COLUMN_SLACK_POINTS).isEqualTo(1.0);
    }

    /**
     * A pixel of the raster a shape in a line is drawn with, which draws four to a point (#743).
     * The clipping it prevents was measured in LibreOffice, whose version is not recorded: a 10pt
     * chevron came out 9.96pt tall. The value is the raster's pixel, not that measurement.
     */
    @Test
    void aShapeInALineHasAPixelOfRoomAroundIt() {
        assertThat(DocxShapePictures.EDGE).isEqualTo(0.25);
    }

    /**
     * Introduced in #770, which records no measurement of it; the constant's javadoc gives the
     * reason: an exact line leaves the editor no room to grow it, so a band's line is as tall as
     * its type set solid with some air.
     */
    @Test
    void aBandsLineIsItsTypeSizeAndAFifth() {
        assertThat(DocxTextBands.LINE_FACTOR).isEqualTo(1.2);
    }

    /**
     * Introduced in #789: the room kept below a stack's last letters. Neither #789 nor the
     * constant's javadoc records a measurement of the value itself; the javadoc reasons from
     * EditorialProposal's 41.8pt title and a raise rounded by at most a quarter point.
     */
    @Test
    void aStackKeepsThreeQuartersOfAPointBelowItsLastLetters() {
        assertThat(DocxStackedLines.INK_MARGIN).isEqualTo(0.75);
    }

    /**
     * Chosen after LibreOffice (#761): it read page backgrounds numbered 1, 2, 3 as one place in
     * its stack and painted a later fill under an earlier one, so they are spaced 1024 apart,
     * the step of Word's own numbering. LibreOffice's version is not recorded.
     */
    @Test
    void pageBackgroundsStandOneThousandAndTwentyFourApart() {
        assertThat(DocxPageBackgrounds.stackHeight(0)).isEqualTo(1024L);
        assertThat(DocxPageBackgrounds.stackHeight(1)).isEqualTo(2048L);
    }

    /**
     * The file format's (#737): {@code w:sz} counts eighths of a point and stops at 96, so Word's
     * thickest border is 12pt.
     */
    @Test
    void wordsThickestBorderIsTwelvePoints() {
        assertThat(DocxRules.MAX_RULE_POINTS).isEqualTo(12.0);
    }

    /**
     * The engine's own default, not an editor's (#711): a cell that states no padding is laid out
     * with 4pt. {@code DocxCellPaddingTest} holds it to {@code TableCellLayoutStyle.DEFAULT}.
     */
    @Test
    void aCellThatStatesNoPaddingIsWrittenWithTheEnginesFourPoints() {
        assertThat(DocxSemanticBackend.ENGINE_DEFAULT_CELL_PADDING_POINTS).isEqualTo(4.0);
    }

    /**
     * A policy (#777), stated as one in #1005: a badge holds a label of at most four characters,
     * and longer text is content, written in the flow.
     */
    @Test
    void aBadgeHoldsAtMostFourCharacters() {
        assertThat(DocxSemanticBackend.BADGE_TEXT_LIMIT).isEqualTo(4);
    }
}
