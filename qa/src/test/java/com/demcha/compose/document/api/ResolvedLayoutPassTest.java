package com.demcha.compose.document.api;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.layout.LayoutAnchorId;
import com.demcha.compose.document.layout.LayoutAnchorNode;
import com.demcha.compose.document.layout.LayoutDepth;
import com.demcha.compose.document.layout.LayoutGraph;
import com.demcha.compose.document.layout.PlacedFragment;
import com.demcha.compose.document.layout.ResolvedLayoutAddition;
import com.demcha.compose.document.layout.ResolvedLayoutAnchor;
import com.demcha.compose.document.layout.ResolvedLayoutMetadata;
import com.demcha.compose.document.layout.ResolvedLayoutPass;
import com.demcha.compose.document.layout.payloads.LayoutAnchorPayload;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.node.EllipseNode;
import com.demcha.compose.document.table.DocumentTableCell;
import com.demcha.compose.document.table.DocumentTableColumn;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.within;

/**
 * The resolved-layout seam: anchors report where a subtree landed, and passes add
 * fragments derived from that once the layout is settled.
 *
 * <p>The load-bearing case is the first one. A mechanism that runs on every document has
 * to prove it changes nothing when nobody uses it, and the only assertion a consistent
 * corruption cannot satisfy is that the driver hands back the same object it was given.
 * Comparing two compiles to each other would prove determinism, not identity.</p>
 */
class ResolvedLayoutPassTest {

    private static final DocumentColor INK = DocumentColor.rgb(20, 60, 160);

    private enum Kind { MARKER, OTHER }

    // --- 1. a registered-nothing document is untouched -----------------------

    @Test
    void withNoPassRegisteredTheDriverHandsBackTheVeryGraphItWasGiven() throws Exception {
        LayoutGraph compiled = compile(flow -> flow
                .addParagraph("First paragraph of the document.")
                .addParagraph("Second paragraph, a little longer so the page has content."),
                List.of());

        // Reference identity, not field-by-field equality. Comparing two compiles of the
        // same document to each other would prove only that the new code is deterministic:
        // an apply() that dropped a fragment would drop it from both and every assertion
        // would still pass. The claim being defended is that with nothing registered the
        // graph is untouched, and the only assertion that cannot be satisfied by a
        // consistent corruption is that it is the same object.
        assertThat(ResolvedLayoutPasses.apply(compiled, List.of()))
                .as("an empty pass list returns the compiled graph itself")
                .isSameAs(compiled);
        assertThat(ResolvedLayoutPasses.apply(compiled, null))
                .as("so does no pass list at all")
                .isSameAs(compiled);
    }

    @Test
    void aPassThatContributesNothingAlsoLeavesTheGraphAlone() throws Exception {
        LayoutGraph compiled = compile(flow -> flow.addParagraph("body"), List.of());

        assertThat(ResolvedLayoutPasses.apply(compiled, List.of(pass("silent", (g, m) -> List.of()))))
                .as("registering a pass is not itself a change; contributing is")
                .isSameAs(compiled);
    }

    @Test
    void anAnchorAddsExactlyOneFragmentAndMovesNothingElse() throws Exception {
        Object group = new Object();
        LayoutGraph without = compile(flow -> flow
                .addParagraph("Above")
                .add(dot(8))
                .addParagraph("Below"), List.of());
        LayoutGraph with = compile(flow -> flow
                .addParagraph("Above")
                .add(new LayoutAnchorNode("", new LayoutAnchorId(group, Kind.MARKER, 0), dot(8)))
                .addParagraph("Below"), List.of());

        assertThat(with.totalPages()).isEqualTo(without.totalPages());
        assertThat(with.fragments()).hasSize(without.fragments().size() + 1);

        // Geometry and payloads, not paths. The wrapper is a real node, so the anchored
        // child's path legitimately gains a segment — ContainerNode[0]/dot[1] becomes
        // ContainerNode[0]/LayoutAnchorNode[1]/dot[0]. Everything that decides what a
        // reader sees must be untouched.
        assertThat(geometry(nonAnchor(with)))
                .as("wrapping a node in an anchor must not move it or anything near it")
                .isEqualTo(geometry(without.fragments()));
        assertThat(payloadsOf(nonAnchor(with))).isEqualTo(payloadsOf(without.fragments()));

        // Pinned deliberately: paths do change, and anything keying on them — a layout
        // snapshot, for one — will see it. Better stated here than discovered downstream.
        assertThat(nonAnchor(with)).anySatisfy(fragment ->
                assertThat(fragment.path()).contains("LayoutAnchorNode"));
    }

    // --- 2. what an anchor resolves to --------------------------------------

    @Test
    void aResolvedAnchorReportsItsPageAndBox() throws Exception {
        Object group = new Object();
        LayoutGraph graph = compile(flow -> flow
                .addParagraph("Above the marker")
                .add(new LayoutAnchorNode("", new LayoutAnchorId(group, Kind.MARKER, 0), dot(8))),
                List.of());

        List<ResolvedLayoutAnchor> anchors = ResolvedLayoutMetadata.from(graph).anchors(group, Kind.MARKER);
        assertThat(anchors).hasSize(1);
        ResolvedLayoutAnchor anchor = anchors.get(0);
        assertThat(anchor.pageIndex()).isZero();
        assertThat(anchor.width()).isEqualTo(8.0, within(1e-9));
        assertThat(anchor.height()).isEqualTo(8.0, within(1e-9));

        PlacedFragment ellipse = graph.fragments().stream()
                .filter(f -> f.payload() != null && f.payload().getClass().getSimpleName().contains("Ellipse"))
                .findFirst().orElseThrow();
        assertThat(anchor.x()).as("the anchor lands on its child, not on its container")
                .isEqualTo(ellipse.x(), within(1e-9));
        assertThat(anchor.y()).isEqualTo(ellipse.y(), within(1e-9));
    }

    @Test
    void anAnchorPointIsFractionsOfItsOwnBox() {
        ResolvedLayoutAnchor anchor =
                new ResolvedLayoutAnchor(new LayoutAnchorId(new Object(), Kind.MARKER, 0), 0, 20.0, 100.0, 8.0, 8.0);

        assertThat(anchor.pointX(0.5)).as("centre").isEqualTo(24.0, within(1e-9));
        assertThat(anchor.pointX(0.0)).as("left edge").isEqualTo(20.0, within(1e-9));
        assertThat(anchor.pointY(0.5)).isEqualTo(104.0, within(1e-9));
    }

    // --- 3. the anchor is the logical owner, not a draw fragment -------------

    @Test
    void oneAnchorPerWrapperHoweverManyFragmentsTheChildDraws() throws Exception {
        Object group = new Object();
        // A container drawing several shapes stands in for a custom marker; the anchor
        // must still be one box, and the child's, not the container's stretched width.
        LayoutGraph graph = compile(flow -> flow
                .add(new LayoutAnchorNode("", new LayoutAnchorId(group, Kind.MARKER, 0), dot(12))),
                List.of());

        assertThat(ResolvedLayoutMetadata.from(graph).anchors(group, Kind.MARKER))
                .singleElement()
                .satisfies(a -> {
                    assertThat(a.width()).isEqualTo(12.0, within(1e-9));
                    assertThat(a.height()).isEqualTo(12.0, within(1e-9));
                });
    }

    @Test
    void insideATableCellTheAnchorIsTheMarkersBoxNotTheCells() throws Exception {
        // The sharpest version of "logical owner, not draw fragment". A bare 8×8 ellipse
        // dropped into a cell reports a 16×8 fragment — the cell stretches it, and a
        // consumer computing a centre from that lands 4pt off. Wrapping it in an anchor
        // has to give back the marker's own box and position.
        Object group = new Object();
        LayoutGraph graph = compile(flow -> flow.addTable(t -> {
            t.columns(DocumentTableColumn.fixed(40), DocumentTableColumn.fixed(90));
            t.rowCells(
                    new DocumentTableCell(List.of(), null, 1, 1,
                            new LayoutAnchorNode("", new LayoutAnchorId(group, Kind.MARKER, 0), dot(8))),
                    DocumentTableCell.text("beside"));
        }), List.of());

        ResolvedLayoutAnchor anchor =
                ResolvedLayoutMetadata.from(graph).anchors(group, Kind.MARKER).get(0);
        PlacedFragment ellipse = graph.fragments().stream()
                .filter(f -> f.payload() != null && f.payload().getClass().getSimpleName().contains("Ellipse"))
                .findFirst().orElseThrow();

        assertThat(anchor.width()).as("the marker's width, not the cell's").isEqualTo(8.0, within(1e-9));
        assertThat(anchor.height()).isEqualTo(8.0, within(1e-9));
        assertThat(anchor.x()).isEqualTo(ellipse.x(), within(1e-9));
        assertThat(anchor.y()).isEqualTo(ellipse.y(), within(1e-9));
        assertThat(anchor.pointX(0.5))
                .as("a centre taken from the cell's box would be 4pt to the right")
                .isEqualTo(ellipse.x() + 4.0, within(1e-9));
    }

    @Test
    void anchorsOfDifferentGroupsNeverMergeEvenWithEqualContent() throws Exception {
        Object first = new Object();
        Object second = new Object();
        LayoutGraph graph = compile(flow -> flow
                .add(new LayoutAnchorNode("", new LayoutAnchorId(first, Kind.MARKER, 0), dot(8)))
                .add(new LayoutAnchorNode("", new LayoutAnchorId(second, Kind.MARKER, 0), dot(8))),
                List.of());

        ResolvedLayoutMetadata metadata = ResolvedLayoutMetadata.from(graph);
        assertThat(metadata.anchors()).hasSize(2);
        assertThat(metadata.anchors(first, Kind.MARKER)).hasSize(1);
        assertThat(metadata.anchors(second, Kind.MARKER)).hasSize(1);
        assertThat(metadata.anchors(first, Kind.OTHER))
                .as("the kind narrows too")
                .isEmpty();
    }

    // --- 4. deterministic ordering ------------------------------------------

    @Test
    void passesContributeInRegistrationOrderAndDepthDecidesSides() throws Exception {
        RecordingPass under = new RecordingPass("under", LayoutDepth.UNDER_BODY, "u1", "u2");
        RecordingPass over = new RecordingPass("over", LayoutDepth.OVER_BODY, "o1");
        RecordingPass alsoUnder = new RecordingPass("alsoUnder", LayoutDepth.UNDER_BODY, "u3");

        LayoutGraph graph = compile(flow -> flow.addParagraph("body"),
                List.of(under, over, alsoUnder));

        List<String> tags = graph.fragments().stream()
                .map(PlacedFragment::payload)
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .toList();
        assertThat(tags)
                .as("registration order within a depth, and every under-body tag before every over-body one")
                .containsExactly("u1", "u2", "u3", "o1");

        int firstBody = indexOfFirstNonTag(graph);
        assertThat(graph.fragments().subList(0, 3))
                .as("the three under-body fragments precede the body")
                .allSatisfy(f -> assertThat(f.payload()).isInstanceOf(String.class));
        assertThat(firstBody).isEqualTo(3);
    }

    @Test
    void everyPassSeesTheSameMetadataRegardlessOfWhatAnEarlierPassAdded() throws Exception {
        Object group = new Object();
        List<Integer> seen = new ArrayList<>();
        ResolvedLayoutPass first = pass("first", (graph, metadata) -> {
            seen.add(metadata.anchors().size());
            // Contributing a fragment must not grow the anchor set a later pass sees.
            return List.of(new ResolvedLayoutAddition(LayoutDepth.UNDER_BODY,
                    PlacedFragment.withZeroInsets("@t", 0, 0, 0, 0, 1, 1, "tag")));
        });
        ResolvedLayoutPass second = pass("second", (graph, metadata) -> {
            seen.add(metadata.anchors().size());
            return List.of();
        });

        compile(flow -> flow
                .add(new LayoutAnchorNode("", new LayoutAnchorId(group, Kind.MARKER, 0), dot(8))),
                List.of(first, second));

        assertThat(seen).as("collected once, before any pass ran").containsExactly(1, 1);
    }

    @Test
    void ananchoredSubtreeThatSpansPagesReportsOneAnchorPerPage() throws Exception {
        // Pinned because it contradicts the obvious reading of "one anchor per wrapper".
        // A composite emits its fragments once per page it occupies, so an anchor wrapping
        // content that paginates reports one per page — sharing an id, and each carrying
        // the subtree's whole height rather than the slice on that page. Small atomic
        // content, which is what anchors are for, never hits it. A consumer that anchors
        // something tall has to group by page itself, and had better learn that here.
        Object group = new Object();
        StringBuilder body = new StringBuilder();
        for (int i = 0; i < 30; i++) {
            body.append("Sentence ").append(i).append(" of a body long enough to paginate. ");
        }
        LayoutGraph graph = compile(flow -> flow
                .add(new LayoutAnchorNode("", new LayoutAnchorId(group, Kind.MARKER, 0),
                        new SectionBuilder().addParagraph(body.toString()).build())),
                List.of());

        List<ResolvedLayoutAnchor> anchors = ResolvedLayoutMetadata.from(graph).anchors(group, Kind.MARKER);
        assertThat(graph.totalPages()).isGreaterThan(1);
        assertThat(anchors)
                .as("one per page the anchored subtree occupies, not one in total")
                .hasSize(graph.totalPages());
        assertThat(anchors.stream().map(ResolvedLayoutAnchor::pageIndex))
                .containsExactlyElementsOf(java.util.stream.IntStream.range(0, graph.totalPages()).boxed().toList());
    }

    // --- 4. the owner carries the feature's own configuration -----------------

    /**
     * Stands in for a feature's owner: one object per logical instance, carrying whatever
     * that instance needs, compared by reference. Deliberately a plain class and not a
     * record — a record invites the reader to think in value equality, which is exactly
     * what {@link LayoutAnchorId} does not do.
     */
    private static final class FeatureOwner {
        private final double railWidth;

        private FeatureOwner(double railWidth) {
            this.railWidth = railWidth;
        }
    }

    @Test
    void theOwnerReachesThePassByReferenceCarryingItsOwnConfiguration() throws Exception {
        // The shape a built-in feature needs: no registration call from the DSL, no session
        // reference, no lookup by name. One owner is allocated, the feature's configuration
        // hangs off it, every anchor is keyed on it, and the pass recovers both.
        FeatureOwner owner = new FeatureOwner(1.5);
        List<FeatureOwner> seenByThePass = new ArrayList<>();

        ResolvedLayoutPass reader = pass("reader", (graph, metadata) -> {
            for (ResolvedLayoutAnchor anchor : metadata.anchors()) {
                seenByThePass.add((FeatureOwner) anchor.id().groupKey());
            }
            return List.of();
        });

        // Per-page margins put this on the resolver's fixed point, so the document is
        // compiled more than once and emitFragments builds fresh payloads each time. The
        // id they carry comes from the semantic node, allocated once — that is the claim.
        StringBuilder filler = new StringBuilder();
        for (int i = 0; i < 20; i++) {
            filler.append("Filler sentence ").append(i).append(" pushing content onto another page. ");
        }
        try (DocumentSession session = GraphCompose.document()
                .pageSize(240, 200)
                .margin(DocumentInsets.of(20))
                .create()) {
            session.pageMargins(List.of(PageMarginRule.page(1, DocumentInsets.of(30))));
            session.registerLayoutPass(reader);
            session.pageFlow()
                    .add(new LayoutAnchorNode("", new LayoutAnchorId(owner, Kind.MARKER, 0), dot(8)))
                    .addParagraph(filler.toString())
                    .add(new LayoutAnchorNode("", new LayoutAnchorId(owner, Kind.MARKER, 1), dot(8)))
                    .build();

            LayoutGraph graph = session.layoutGraph();
            assertThat(graph.totalPages()).as("the recompiling path, not the single-pass one").isGreaterThan(1);
        }

        assertThat(seenByThePass).as("both anchors reached the pass").hasSize(2);
        assertThat(seenByThePass).allSatisfy(seen ->
                assertThat(seen).as("the very object the DSL allocated, not a copy").isSameAs(owner));
        assertThat(seenByThePass.get(0).railWidth)
                .as("the pass reads the feature's configuration straight off its owner")
                .isEqualTo(1.5, within(1e-9));
    }

    @Test
    void aPassFindsNothingWhenTheDocumentHasNoneOfItsFeature() throws Exception {
        FeatureOwner mine = new FeatureOwner(1.5);
        FeatureOwner someoneElses = new FeatureOwner(3.0);
        LayoutGraph graph = compile(flow -> flow
                .add(new LayoutAnchorNode("", new LayoutAnchorId(someoneElses, Kind.MARKER, 0), dot(8))),
                List.of());

        // How a built-in pass decides it has nothing to do: ask for its own owner and get
        // back an empty list. No document inspection, no feature flag, no session help.
        assertThat(ResolvedLayoutMetadata.from(graph).anchors(mine, Kind.MARKER)).isEmpty();
        assertThat(ResolvedLayoutMetadata.from(graph).anchors(someoneElses, Kind.MARKER)).hasSize(1);
    }

    // --- guards --------------------------------------------------------------

    @Test
    void anIdentityKeyMayNotBeAStringOrABoxedNumber() {
        // == decides whether two ids match, so a String key works or fails on interning:
        // the same literal twice matches, the same text computed does not. Caught at the
        // call site rather than as an anchor set that silently comes back empty.
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new LayoutAnchorId("timeline", Kind.MARKER, 0))
                .withMessageContaining("identity key");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new LayoutAnchorId(new Object(), 128, 0))
                .withMessageContaining("identity key");
    }

    @Test
    void aPassMayNotContributeANonFiniteCoordinate() {
        ResolvedLayoutPass rogue = pass("rogue", (graph, metadata) -> List.of(
                new ResolvedLayoutAddition(LayoutDepth.UNDER_BODY,
                        PlacedFragment.withZeroInsets("@x", 0, 0, Double.NaN, 0, 1, 1, "tag"))));

        assertThatIllegalStateException()
                .isThrownBy(() -> compile(flow -> flow.addParagraph("x"), List.of(rogue)))
                .withMessageContaining("NaN");
    }


    @Test
    void aPassMayNotInventAPage() {
        ResolvedLayoutPass rogue = pass("rogue", (graph, metadata) -> List.of(
                new ResolvedLayoutAddition(LayoutDepth.UNDER_BODY,
                        PlacedFragment.withZeroInsets("@x", 0, 7, 0, 0, 1, 1, "tag"))));

        assertThatIllegalStateException()
                .isThrownBy(() -> compile(flow -> flow.addParagraph("one page"), List.of(rogue)))
                .withMessageContaining("may not add pages");
    }

    @Test
    void aPassReturningNullIsRejectedByName() {
        ResolvedLayoutPass broken = pass("broken", (graph, metadata) -> null);

        assertThatIllegalStateException()
                .isThrownBy(() -> compile(flow -> flow.addParagraph("x"), List.of(broken)))
                .withMessageContaining("broken");
    }

    // --- helpers -------------------------------------------------------------

    private static EllipseNode dot(double size) {
        return new EllipseNode("dot", size, size, INK, null, null, null, null, null);
    }

    private static List<PlacedFragment> nonAnchor(LayoutGraph graph) {
        return graph.fragments().stream()
                .filter(f -> !(f.payload() instanceof LayoutAnchorPayload))
                .toList();
    }

    /** Everything that decides where ink lands, with the path left out. */
    private static List<String> geometry(List<PlacedFragment> fragments) {
        return fragments.stream()
                .map(f -> "p%d (%.6f,%.6f %.6fx%.6f)".formatted(
                        f.pageIndex(), f.x(), f.y(), f.width(), f.height()))
                .toList();
    }

    private static List<Object> payloadsOf(List<PlacedFragment> fragments) {
        return fragments.stream().map(PlacedFragment::payload).toList();
    }

    private static int indexOfFirstNonTag(LayoutGraph graph) {
        List<PlacedFragment> fragments = graph.fragments();
        for (int i = 0; i < fragments.size(); i++) {
            if (!(fragments.get(i).payload() instanceof String)) {
                return i;
            }
        }
        return fragments.size();
    }

    private static LayoutGraph compile(Consumer<com.demcha.compose.document.dsl.PageFlowBuilder> spec,
                                       List<ResolvedLayoutPass> passes) throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(240, 200)
                .margin(DocumentInsets.of(20))
                .create()) {
            for (ResolvedLayoutPass pass : passes) {
                session.registerLayoutPass(pass);
            }
            var flow = session.pageFlow();
            spec.accept(flow);
            flow.build();
            return session.layoutGraph();
        }
    }

    private interface Contribution {
        List<ResolvedLayoutAddition> apply(LayoutGraph graph, ResolvedLayoutMetadata metadata);
    }

    private static ResolvedLayoutPass pass(String id, Contribution contribution) {
        return new ResolvedLayoutPass() {
            @Override
            public String id() {
                return id;
            }

            @Override
            public List<ResolvedLayoutAddition> contribute(LayoutGraph graph, ResolvedLayoutMetadata metadata) {
                return contribution.apply(graph, metadata);
            }
        };
    }

    /** Contributes one tagged fragment per name, so ordering is readable in an assertion. */
    private record RecordingPass(String id, LayoutDepth depth, String... tags) implements ResolvedLayoutPass {
        @Override
        public List<ResolvedLayoutAddition> contribute(LayoutGraph graph, ResolvedLayoutMetadata metadata) {
            List<ResolvedLayoutAddition> additions = new ArrayList<>();
            for (String tag : tags) {
                additions.add(new ResolvedLayoutAddition(depth,
                        PlacedFragment.withZeroInsets("@" + tag, 0, 0, 0.0, 0.0, 1.0, 1.0, tag)));
            }
            return additions;
        }
    }
}
