package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.document.layout.TableGrid;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.table.DocumentTableCell;
import org.junit.jupiter.api.Test;

import java.util.OptionalDouble;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Row by row: a row the layout tells the height of and measures the text of counts what its
 * tallest cell comes to past that height; any other counts the most it can be, and only that row.
 */
class DocxRuleRoomTest {

    private static final TableGrid.Placement TEXT = new TableGrid.Placement(0, 0, 1, 1,
            DocumentTableCell.text("Alpha"));
    private static final TableGrid.Placement EMPTY = new TableGrid.Placement(0, 1, 1, 1,
            DocumentTableCell.text(""));
    private static final DocumentInsets NONE = DocumentInsets.zero();

    @Test
    void aMeasuredRowCountsItsTallestCellPastThePagesHeight() {
        DocxRuleRoom room = new DocxRuleRoom(1);
        room.count(0, TEXT, NONE, 4.5, 12);
        DocxRuleRoom.Taller taller = room.taller(row -> OptionalDouble.of(12), row -> 1);
        assertThat(taller.points()).isCloseTo(4.5, within(1e-9));
        assertThat(taller.atMost()).isFalse();
    }

    @Test
    void aRowTheLayoutDoesNotTellCountsTheMostItCanBeAndOnlyThatRow() {
        DocxRuleRoom room = new DocxRuleRoom(2);
        room.count(0, TEXT, NONE, 4.5, 12);
        room.count(1, TEXT, NONE, 3, 12);
        // The first row told and measured, padded so its cell needs no more than the page gives it;
        // the second not told.
        DocxRuleRoom.Taller taller = room.taller(row -> row == 0 ? OptionalDouble.of(16.5) : OptionalDouble.empty(),
                row -> 1);
        assertThat(taller.points()).as("nothing for the first, the most for the second").isCloseTo(3, within(1e-9));
        assertThat(taller.atMost()).isTrue();
    }

    @Test
    void aRowWhoseTextIsNotMeasuredCountsTheMostItCanBe() {
        DocxRuleRoom room = new DocxRuleRoom(1);
        room.count(0, TEXT, new DocumentInsets(1, 0, 1, 0), 4.5, Double.NaN);
        DocxRuleRoom.Taller taller = room.taller(row -> OptionalDouble.of(14), row -> 1);
        assertThat(taller.points()).isCloseTo(2.5, within(1e-9));
        assertThat(taller.atMost()).isTrue();
    }

    @Test
    void anEmptyCellCountsOnlyWhereItsRowIsNotHeld() {
        DocxRuleRoom room = new DocxRuleRoom(1);
        room.count(0, EMPTY, NONE, 4.5, 12);
        assertThat(room.taller(row -> OptionalDouble.of(12), row -> 1).points())
                .as("held at the page's height, its line is cut to the row's room").isZero();
        assertThat(room.taller(row -> OptionalDouble.empty(), row -> 1).points())
                .as("not held, its line keeps its height").isCloseTo(4.5, within(1e-9));
    }

    @Test
    void aRowPlacedOnSeveralPagesCountsOnEach() {
        DocxRuleRoom room = new DocxRuleRoom(1);
        room.count(0, TEXT, NONE, 1.5, 12);
        assertThat(room.taller(row -> OptionalDouble.of(12), row -> 3).points()).isCloseTo(4.5, within(1e-9));
    }
}
