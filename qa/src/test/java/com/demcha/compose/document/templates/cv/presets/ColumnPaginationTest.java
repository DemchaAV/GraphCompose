package com.demcha.compose.document.templates.cv.presets;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link ColumnPagination} decides page boundaries without losing lines.
 *
 * <p>The property that matters is conservation: a preset reached for this
 * because dropping the overflow is the bug it replaced, so every case below
 * checks that what went in comes back out, whatever the budget.</p>
 */
class ColumnPaginationTest {

    /** A sidebar-shaped column: 148pt wide, 7.5pt body, 12.5pt heading. */
    private static ColumnPagination sidebar() {
        return ColumnPagination.forColumn(148.0, 7.5, 1.0, 12.5, 22.0);
    }

    private static ColumnPagination.Block block(String title, String... lines) {
        return new ColumnPagination.Block(title, List.of(lines), false);
    }

    private static List<String> allLines(List<List<ColumnPagination.Block>> pages) {
        List<String> flat = new ArrayList<>();
        for (List<ColumnPagination.Block> page : pages) {
            for (ColumnPagination.Block block : page) {
                flat.addAll(block.lines());
            }
        }
        return flat;
    }

    @Test
    void aColumnThatFitsStaysOnOnePage() {
        List<List<ColumnPagination.Block>> pages = sidebar().paginate(
                List.of(block("Education", "MSc", "BEng")), 600.0);

        assertThat(pages).hasSize(1);
        assertThat(pages.get(0)).hasSize(1);
        assertThat(pages.get(0).get(0).lines()).containsExactly("MSc", "BEng");
    }

    @Test
    void everyLineSurvivesHoweverManyPagesItTakes() {
        List<String> lines = IntStream.range(0, 60)
                .mapToObj(i -> "entry " + i).toList();

        List<List<ColumnPagination.Block>> pages = sidebar().paginate(
                List.of(new ColumnPagination.Block("Skills", lines, false)), 200.0);

        assertThat(pages.size())
                .describedAs("60 lines cannot fit a 200pt page")
                .isGreaterThan(1);
        assertThat(allLines(pages))
                .describedAs("conservation is the whole point — the previous "
                        + "behaviour kept a prefix and dropped the rest")
                .containsExactlyElementsOf(lines);
    }

    @Test
    void aBlockCarriedAcrossPagesRepeatsItsHeading() {
        List<String> lines = IntStream.range(0, 40)
                .mapToObj(i -> "entry " + i).toList();

        List<List<ColumnPagination.Block>> pages = sidebar().paginate(
                List.of(new ColumnPagination.Block("Skills", lines, false)), 200.0);

        assertThat(pages).allSatisfy(page ->
                assertThat(page).allSatisfy(block ->
                        assertThat(block.title())
                                .describedAs("a continuation without its heading "
                                        + "reads as part of the block above it")
                                .isEqualTo("Skills")));
    }

    @Test
    void aBlockThatDoesNotFitTheRemainderStartsTheNextPageWhole() {
        ColumnPagination column = sidebar();
        // Sized so the first block fills most of the page and the second
        // cannot start on what is left.
        List<List<ColumnPagination.Block>> pages = column.paginate(
                List.of(block("First", "a", "b", "c", "d", "e", "f"),
                        block("Second", "x", "y", "z")),
                140.0);

        assertThat(pages.size()).isGreaterThan(1);
        List<ColumnPagination.Block> secondBlockPieces = pages.stream()
                .flatMap(List::stream)
                .filter(b -> b.title().equals("Second"))
                .toList();
        assertThat(secondBlockPieces)
                .describedAs("the second block should move whole rather than "
                        + "leave one orphaned line under a heading at the foot")
                .hasSize(1);
        assertThat(secondBlockPieces.get(0).lines()).containsExactly("x", "y", "z");
    }

    @Test
    @Timeout(5)
    void aBudgetTooSmallForEvenAHeadingStillTerminatesAndKeepsEveryLine() {
        List<String> lines = List.of("a", "b", "c");

        List<List<ColumnPagination.Block>> pages = sidebar().paginate(
                List.of(new ColumnPagination.Block("Skills", lines, false)), 1.0);

        assertThat(allLines(pages))
                .describedAs("a degenerate budget must not spin forever, and "
                        + "must not resolve the deadlock by discarding lines")
                .containsExactlyElementsOf(lines);
    }

    @Test
    void anEmptyColumnStillYieldsOnePage() {
        assertThat(sidebar().paginate(List.of(), 600.0)).hasSize(1);
        assertThat(sidebar().paginate(List.of(), 600.0).get(0)).isEmpty();
    }

    @Test
    void aLongLineCountsAsTheSeveralItWrapsTo() {
        ColumnPagination column = sidebar();
        // 148pt at 7.5pt ≈ 41 characters per rendered line.
        assertThat(column.wrappedLines("short")).isEqualTo(1);
        assertThat(column.wrappedLines("x".repeat(41))).isEqualTo(1);
        assertThat(column.wrappedLines("x".repeat(42))).isEqualTo(2);
        assertThat(column.wrappedLines("x".repeat(130))).isEqualTo(4);
    }

    @Test
    void anEmptyLineStillOccupiesOne() {
        assertThat(sidebar().wrappedLines("")).isEqualTo(1);
        assertThat(sidebar().wrappedLines(null)).isEqualTo(1);
    }

    @Test
    void aWiderColumnWrapsLater() {
        ColumnPagination main = ColumnPagination.forColumn(348.0, 7.8, 1.2, 13.5, 24.0);

        assertThat(main.wrappedLines("x".repeat(90)))
                .describedAs("the main column is more than twice the sidebar's "
                        + "width, so the same text wraps to fewer lines")
                .isLessThan(sidebar().wrappedLines("x".repeat(90)));
    }

    @Test
    void aColumnWithNoWidthOrNoFontIsRejected() {
        assertThatThrownBy(() -> ColumnPagination.forColumn(0, 7.5, 1.0, 12.5, 22.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must be positive");
        assertThatThrownBy(() -> ColumnPagination.forColumn(148.0, 0, 1.0, 12.5, 22.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must be positive");
        assertThatThrownBy(() -> ColumnPagination.forColumn(148.0, 7.5, 1.0, -1, 22.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must be positive");
    }
}
