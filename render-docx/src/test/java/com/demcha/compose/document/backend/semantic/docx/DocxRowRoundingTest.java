package com.demcha.compose.document.backend.semantic.docx;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * What a table's rows are written taller than the page by is carried from row to row and settled
 * in whole twips, on its page, and only where it is rounding.
 *
 * @author Artem Demchyshyn
 */
class DocxRowRoundingTest {

    /** Two lines, a space after the first and the row's own four: six rounded parts. */
    private static final int PARTS = 6;
    private static final long ROOM = 79;

    @Test
    void rowsWrittenTallerTakeATwipOffOnceTheyComeToHalfOne() {
        // ConsultingInvoice's item rows, each 607 twips where the page makes them 606.6.
        assertThat(settle(new DocxRowRounding(), 0, 0.4, 5)).containsExactly(0L, 1L, 0L, 1L, 0L);
    }

    @Test
    void aRowWrittenTallerByMoreThanATwipTakesOffWhatItComesTo() {
        // Three lines, each 0.36 twip taller than the page's: 1.08 a row, rounding all the same.
        DocxRowRounding rounding = new DocxRowRounding();

        assertThat(rounding.settle(0, 1.08, 7, ROOM)).isEqualTo(1);
        assertThat(rounding.settle(0, 1.08, 7, ROOM)).as("0.08 carried and 1.08 more").isEqualTo(1);
        assertThat(rounding.settle(0, 1.84, 7, ROOM)).as("0.16 carried and 1.84 more").isEqualTo(2);
    }

    @Test
    void rowsWrittenShorterCarryNothing() {
        // They do not stand shorter in Word or LibreOffice: given a twip back, they stood lower.
        DocxRowRounding rounding = new DocxRowRounding();

        assertThat(settle(rounding, 0, -0.4, 5)).containsOnly(0L);
        assertThat(rounding.settle(0, 0.4, PARTS, ROOM)).as("nothing owed from the short rows above it").isZero();
        assertThat(rounding.settle(0, 0.4, PARTS, ROOM)).isEqualTo(1);
    }

    @Test
    void whatIsCarriedStaysOnItsPage() {
        DocxRowRounding rounding = new DocxRowRounding();

        assertThat(rounding.settle(0, 0.4, PARTS, ROOM)).isZero();
        assertThat(rounding.settle(1, 0.4, PARTS, ROOM)).as("the next page's first row starts clear").isZero();
        assertThat(rounding.settle(1, 0.4, PARTS, ROOM)).as("what its own page carries").isEqualTo(1);
    }

    @Test
    void aRowTallerThanItsRoundedPartsCanComeToStartsTheCarryAgain() {
        DocxRowRounding rounding = new DocxRowRounding();

        assertThat(rounding.settle(0, 0.4, PARTS, ROOM)).isZero();
        assertThat(rounding.settle(0, 3.2, PARTS, ROOM)).as("past six half twips: not rounding, left as it is").isZero();
        assertThat(rounding.settle(0, 0.4, PARTS, ROOM)).as("nothing carried from above it").isZero();
        assertThat(rounding.settle(0, 2.9, PARTS, ROOM)).as("within six half twips: rounding").isEqualTo(3);
    }

    @Test
    void aRowUnreadStartsTheCarryAgain() {
        DocxRowRounding rounding = new DocxRowRounding();

        assertThat(rounding.settle(0, 0.4, PARTS, ROOM)).isZero();
        rounding.forget();
        assertThat(rounding.settle(0, 0.4, PARTS, ROOM)).isZero();
    }

    @Test
    void aRowTakesOffNoMoreThanItsMarginHoldsAndPassesTheRestOn() {
        DocxRowRounding rounding = new DocxRowRounding();

        assertThat(rounding.settle(0, 0.4, PARTS, ROOM)).isZero();
        assertThat(rounding.settle(0, 0.4, PARTS, 0)).as("no margin below to take it from").isZero();
        assertThat(rounding.settle(0, 0.4, PARTS, ROOM)).as("1.2 twips carried: the next row takes one").isEqualTo(1);
        assertThat(rounding.settle(0, 0.4, PARTS, ROOM)).as("0.6 left").isEqualTo(1);
    }

    private static List<Long> settle(DocxRowRounding rounding, int page, double offBy, int rows) {
        List<Long> steps = new ArrayList<>();
        for (int row = 0; row < rows; row++) {
            steps.add(rounding.settle(page, offBy, PARTS, ROOM));
        }
        return steps;
    }
}
