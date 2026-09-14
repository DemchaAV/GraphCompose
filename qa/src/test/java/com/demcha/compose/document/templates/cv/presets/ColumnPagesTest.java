package com.demcha.compose.document.templates.cv.presets;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.exceptions.AtomicNodeTooLargeException;
import com.demcha.compose.document.snapshot.LayoutNodeSnapshot;
import com.demcha.compose.document.snapshot.LayoutSnapshot;
import com.demcha.compose.document.style.DocumentInsets;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * How {@link ColumnPages} cuts a CV's columns into pages, on blocks of known height.
 *
 * <p>Every block here is a spacer, so the heights the plan measures are the numbers written in the
 * test and each boundary sits exactly where a rule is about to be crossed. The sheet is 200 by
 * 300 points with no margin.</p>
 */
class ColumnPagesTest {

    @Test
    void packsBlocksInOrderAndLeavesOutTheLeadOfABlockThatOpensAPage() {
        try (DocumentSession session = session()) {
            List<ColumnPages.Column> columns = List.of(column(300, 300,
                    block("A", 0, 120), block("B", 20, 100), block("C", 20, 100),
                    block("D", 20, 50)));

            ColumnPages.Plan plan = ColumnPages.plan(session, columns);

            assertThat(plan.pages()).isEqualTo(2);
            assertThat(plan.blocks(0, 0)).containsExactly(0, 1);
            assertThat(plan.blocks(0, 1)).containsExactly(2, 3);

            compose(session, plan, 0, columns);
            LayoutSnapshot snapshot = session.layoutSnapshot();
            assertThat(snapshot.totalPages()).isEqualTo(2);
            assertThat(drawn(snapshot, "BHairline")).isTrue();
            assertThat(drawn(snapshot, "CHairline")).isFalse();
            assertThat(drawn(snapshot, "DHairline")).isTrue();
            assertThat(node(snapshot, "CContent").startPage()).isEqualTo(1);
        }
    }

    @Test
    void holdsTheFirstPageToItsOwnHeight() {
        try (DocumentSession session = session()) {
            ColumnPages.Plan plan = ColumnPages.plan(session, List.of(column(200, 300,
                    block("A", 0, 150), block("B", 0, 100), block("C", 0, 150))));

            assertThat(plan.blocks(0, 0)).containsExactly(0);
            assertThat(plan.blocks(0, 1)).containsExactly(1, 2);
        }
    }

    @Test
    void startsEveryRowOnAPageOfItsOwnEvenWhenOnlyTheLeadDidNotFit() {
        // B and the hairline above it overrun the first page, so B opens the second without
        // that hairline — and its row, 10pt of padding and 20pt of block, is short enough to
        // fit the 40pt the first row leaves. Nothing but the page break keeps it off that page.
        try (DocumentSession session = session()) {
            List<ColumnPages.Column> columns = List.of(
                    new ColumnPages.Column(0, 100, 0, 0, 290, 290,
                            List.of(block("Aside", 0, 30))),
                    new ColumnPages.Column(100, 0, 0, 0, 290, 290,
                            List.of(block("A", 0, 250), block("B", 40, 20))));

            ColumnPages.Plan plan = ColumnPages.plan(session, columns);

            assertThat(plan.pages()).isEqualTo(2);
            assertThat(plan.blocks(0, 1)).isEmpty();

            compose(session, plan, 10, columns);
            LayoutSnapshot snapshot = session.layoutSnapshot();
            assertThat(snapshot.totalPages()).isEqualTo(2);
            assertThat(node(snapshot, "Grid_1").startPage()).isEqualTo(1);
            assertThat(drawn(snapshot, "BHairline")).isFalse();
        }
    }

    @Test
    void keepsOnOnePageWhatTheSingleRowAlwaysFittedThere() {
        // The paginator lets a block that cannot be split overrun the page by half a point, so
        // a single row 0.3pt taller than the page was always drawn on one page.
        try (DocumentSession session = session()) {
            List<ColumnPages.Column> columns = List.of(column(300, 300,
                    block("A", 0, 150), block("B", 0, 150.3)));

            ColumnPages.Plan plan = ColumnPages.plan(session, columns);

            assertThat(plan.pages()).isEqualTo(1);
            compose(session, plan, 0, columns);
            assertThat(session.layoutSnapshot().totalPages()).isEqualTo(1);
        }
    }

    @Test
    void refusesABlockTallerThanAPageAndNamesIt() {
        try (DocumentSession session = session()) {
            ColumnPages.Block tall = new ColumnPages.Block("Tall", null, section -> {
                section.addSpacer(spacer -> spacer.name("TallTop").height(200));
                section.addSpacer(spacer -> spacer.name("TallBottom").height(200));
            });
            List<ColumnPages.Column> columns = List.of(column(300, 300, block("A", 0, 50), tall));

            assertThatThrownBy(() -> ColumnPages.plan(session, columns))
                    .isInstanceOf(AtomicNodeTooLargeException.class)
                    .hasMessageContaining("'Tall'");
            assertThat(session.roots()).isEmpty();
        }
    }

    @Test
    void refusesTwoBlocksOfOneName() {
        try (DocumentSession session = session()) {
            List<ColumnPages.Column> columns = List.of(
                    new ColumnPages.Column(0, 100, 0, 0, 300, 300, List.of(block("Same", 0, 10))),
                    new ColumnPages.Column(100, 0, 0, 0, 300, 300, List.of(block("Same", 0, 20))));

            assertThatThrownBy(() -> ColumnPages.plan(session, columns))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Same");
        }
    }

    private static DocumentSession session() {
        DocumentSession session = GraphCompose.document().create();
        session.pageSize(200, 300).margin(DocumentInsets.zero());
        return session;
    }

    private static ColumnPages.Column column(double firstPage, double laterPages,
                                             ColumnPages.Block... blocks) {
        return new ColumnPages.Column(0, 0, 0, 0, firstPage, laterPages, List.of(blocks));
    }

    /** A block {@code height} tall, led by a hairline {@code lead} tall — none when zero. */
    private static ColumnPages.Block block(String name, double lead, double height) {
        return new ColumnPages.Block(name,
                lead == 0 ? null : section -> section.addSpacer(spacer -> spacer
                        .name(name + "Hairline")
                        .height(lead)),
                section -> section.addSpacer(spacer -> spacer
                        .name(name + "Content")
                        .height(height)));
    }

    /** One row per page, each column a section with {@code top} of padding over its blocks. */
    private static void compose(DocumentSession session, ColumnPages.Plan plan, double top,
                                List<ColumnPages.Column> columns) {
        session.pageFlow(flow -> {
            flow.padding(DocumentInsets.zero()).spacing(0);
            ColumnPages.addRows(flow, plan, "Grid", (row, page) -> {
                row.spacing(0);
                for (int index = 0; index < columns.size(); index++) {
                    int column = index;
                    row.addSection("Column_" + column, section -> {
                        section.spacing(0);
                        section.padding(new DocumentInsets(top, 0, 0, 0));
                        ColumnPages.compose(section, columns.get(column).blocks(),
                                plan.blocks(column, page));
                    });
                }
            });
        });
    }

    private static boolean drawn(LayoutSnapshot snapshot, String name) {
        return snapshot.nodes().stream().anyMatch(node -> name.equals(node.entityName()));
    }

    private static LayoutNodeSnapshot node(LayoutSnapshot snapshot, String name) {
        return snapshot.nodes().stream()
                .filter(node -> name.equals(node.entityName()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No node named " + name));
    }
}
