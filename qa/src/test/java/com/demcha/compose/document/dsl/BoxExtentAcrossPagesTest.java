package com.demcha.compose.document.dsl;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.layout.LayoutGraph;
import com.demcha.compose.document.layout.PlacedFragment;
import com.demcha.compose.document.layout.PlacedNode;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A box does not claim a page none of its content reaches.
 *
 * <p>A node's box is recorded where the flow stands when the node is compiled, and its
 * children are placed afterwards. When the first thing inside it cannot start in the space
 * left — because it asked to be kept whole, or because it is indivisible and too tall — the
 * child moves to the next page and the box is left spanning a page it holds nothing on. Every
 * consumer of that box then believes it: a section's accent border paints a stub at the foot of
 * the page beside nothing at all, and anything deriving an extent from the box, a timeline's
 * rail included, runs down to the bottom margin.</p>
 *
 * <p>So the box has to open where its first indivisible unit can actually start. That is the
 * same rule {@code keepTogether()} already applies to a whole node, asked of its leading unit
 * instead — and it fires only in the case that was broken, because a box whose first unit does
 * fit is unaffected.</p>
 *
 * <p>The last test is the other half of the contract: a box whose content genuinely does span
 * pages must keep a slice on every one of them. The rule must not turn a real span into a
 * relocation.</p>
 */
class BoxExtentAcrossPagesTest {

    private static final DocumentColor EDGE = DocumentColor.rgb(200, 60, 60);

    @Test
    void aBoxDoesNotOpenOnAPageItsFirstUnitCannotStartOn() throws Exception {
        LayoutGraph graph = hostWhoseChildIsKeptWhole();

        PlacedNode host = node(graph, "Host");
        assertThat(host.startPage())
                .as("the host opens where its content is, not where the flow happened to stand")
                .isEqualTo(1);
        assertThat(host.endPage())
                .as("and it does not span a page it holds nothing on")
                .isEqualTo(1);

        // The border is the visible half of it: two slices means a stub painted at the foot
        // of page 0 with no content beside it.
        assertThat(fragments(graph, "Host"))
                .as("one slice, on the page the content reached")
                .hasSize(1);
        assertThat(fragments(graph, "Host").get(0).pageIndex()).isEqualTo(1);
    }

    @Test
    void theChildStillLandsWhereItDidAndNothingIsLost() throws Exception {
        // The fix moves the box, not the content: the child was already on page 1.
        LayoutGraph graph = hostWhoseChildIsKeptWhole();
        PlacedNode child = node(graph, "KeptWhole");
        assertThat(child.startPage()).isEqualTo(1);
        assertThat(child.endPage()).isEqualTo(1);
        assertThat(graph.totalPages()).isEqualTo(2);
    }

    @Test
    void aBoxWhoseContentGenuinelySpansKeepsASliceOnEveryPage() throws Exception {
        // The other half of the contract. Nothing here asks to be kept whole and the
        // content is splittable, so the host really does cross the boundary and has to
        // keep saying so on both pages.
        LayoutGraph graph;
        try (DocumentSession session = GraphCompose.document()
                .pageSize(320, 220).margin(DocumentInsets.of(20)).create()) {
            PageFlowBuilder flow = session.pageFlow();
            flow.addSection("Filler", block -> {
                for (int i = 0; i < 8; i++) {
                    block.addParagraph(p -> p.text("Filler line.").margin(DocumentInsets.zero()));
                }
            });
            flow.addSection("Spanner", block -> {
                block.accentLeft(EDGE, 1.5);
                for (int i = 0; i < 12; i++) {
                    block.addParagraph(p -> p.text("Spanning line.").margin(DocumentInsets.zero()));
                }
            });
            flow.build();
            graph = session.layoutGraph();
        }

        PlacedNode spanner = node(graph, "Spanner");
        assertThat(spanner.startPage()).as("it starts where the flow stood").isEqualTo(0);
        assertThat(spanner.endPage()).as("and genuinely crosses").isEqualTo(1);
        assertThat(fragments(graph, "Spanner"))
                .as("a slice on each page it crosses, so the border draws on both")
                .hasSize(2);
    }

    /** A bordered host with no keep-together of its own, holding one child that has it. */
    private static LayoutGraph hostWhoseChildIsKeptWhole() throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(320, 220).margin(DocumentInsets.of(20)).create()) {
            PageFlowBuilder flow = session.pageFlow();
            flow.addSection("Filler", block -> {
                for (int i = 0; i < 12; i++) {
                    block.addParagraph(p -> p.text("Filler line.").margin(DocumentInsets.zero()));
                }
            });
            flow.addSection("Host", block -> {
                block.accentLeft(EDGE, 1.5);
                block.addSection("KeptWhole", inner -> {
                    inner.keepTogether();
                    for (int i = 0; i < 4; i++) {
                        inner.addParagraph(p -> p.text("Inner line.")
                                .margin(DocumentInsets.zero()));
                    }
                });
            });
            flow.build();
            return session.layoutGraph();
        }
    }

    private static PlacedNode node(LayoutGraph graph, String name) {
        return graph.nodes().stream()
                .filter(n -> name.equals(n.semanticName()))
                .findFirst().orElseThrow(() -> new AssertionError("no node named " + name));
    }

    private static List<PlacedFragment> fragments(LayoutGraph graph, String name) {
        return graph.fragments().stream()
                .filter(f -> f.path() != null && f.path().endsWith(name + "[1]"))
                .toList();
    }
}
