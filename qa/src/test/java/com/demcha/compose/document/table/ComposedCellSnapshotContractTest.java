package com.demcha.compose.document.table;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.layout.PlacedFragment;
import com.demcha.compose.document.layout.payloads.ParagraphFragmentPayload;
import com.demcha.compose.document.node.TableNode;
import com.demcha.compose.document.snapshot.LayoutNodeSnapshot;
import com.demcha.compose.document.style.DocumentInsets;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A contract probe: a composed table cell renders, but the layout snapshot does
 * not see it.
 *
 * `DocumentTableCell.node(...)` puts a real node inside a cell, and that node
 * lays out and paints — the fragments are there. What it does not produce is a
 * snapshot entry of its own: the snapshot records the table, not the node
 * composed into one of its cells.
 *
 * That asymmetry is the point. A regression that emptied every composed cell
 * would leave the layout snapshot byte-identical, so the snapshot gate — the
 * usual guard for "did the geometry move" — would stay green over a document
 * that renders blank cells. Anything relying on composed cells has to assert on
 * fragments as well.
 *
 * This is a *documentation* contract, not an aspiration: it is claimed by
 * `docs/recipes/tables.md` and referenced by the knowledge pack, so it needs a
 * test that fails if the behaviour changes — in either direction. If composed
 * cells ever do appear in the snapshot, this test fails and the claim, the
 * recipe and the snapshot guidance all need revisiting.
 */
class ComposedCellSnapshotContractTest {

    private static final String CELL_TEXT = "composed-cell-marker";

    private static TableNode composedCellTable(DocumentSession session) {
        return new TableNode(
                "ComposedCellTable",
                List.of(DocumentTableColumn.auto()),
                List.of(List.of(DocumentTableCell.node(
                        session.dsl().paragraph().text(CELL_TEXT).build()))),
                DocumentTableStyle.empty(),
                null,
                DocumentInsets.zero(),
                DocumentInsets.zero());
    }

    private static long fragmentsCarrying(DocumentSession session, String text) {
        return session.layoutGraph().fragments().stream()
                .map(PlacedFragment::payload)
                .filter(ParagraphFragmentPayload.class::isInstance)
                .map(ParagraphFragmentPayload.class::cast)
                .flatMap(payload -> payload.lines().stream())
                .filter(line -> line.spans().stream().anyMatch(s -> s.toString().contains(text)))
                .count();
    }

    @Test
    void composedCellPaintsButLeavesNoSnapshotNodeOfItsOwn() throws Exception {
        try (DocumentSession session = GraphCompose.document().create()) {
            session.add(composedCellTable(session));

            // It renders: the composed paragraph reaches the fragment stream.
            assertThat(fragmentsCarrying(session, CELL_TEXT))
                    .describedAs("the composed cell's paragraph must reach the fragment stream")
                    .isGreaterThan(0);

            // It is invisible to the snapshot: no node carries the cell's text,
            // so a snapshot-only assertion cannot tell a rendered cell from an
            // empty one.
            List<LayoutNodeSnapshot> nodes = session.layoutSnapshot().nodes();
            assertThat(nodes)
                    .describedAs("the snapshot must record something — the table itself")
                    .isNotEmpty();
            assertThat(nodes)
                    .describedAs(
                            "no snapshot node should carry the composed cell's content; if one now does, "
                                    + "the claim in docs/recipes.md and the snapshot-testing guidance are stale")
                    .noneMatch(node -> node.toString().contains(CELL_TEXT));
        }
    }
}
