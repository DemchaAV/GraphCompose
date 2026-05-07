package com.demcha.compose.document.dsl;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.layout.PlacedFragment;
import com.demcha.compose.document.layout.payloads.ParagraphFragmentPayload;
import com.demcha.compose.document.node.ContainerNode;
import com.demcha.compose.document.node.ListItem;
import com.demcha.compose.document.node.ListMarker;
import com.demcha.compose.document.node.ListNode;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.testing.layout.LayoutSnapshotAssertions;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Behavioural test suite for the v1.6 nested-list ergonomics
 * (Phase A). Covers the new {@link ListBuilder#addItem(String, java.util.function.Consumer)}
 * path, per-depth marker resolution, item-level marker overrides,
 * mixed flat-and-nested authoring, and the back-compat invariant
 * for v1.4 / v1.5 flat callers.
 *
 * <p><b>Whitespace note.</b> The nested layout pipeline indents each
 * level with non-breaking spaces (U+00A0) so the paragraph wrap
 * pipeline preserves them — Java's {@link Character#isWhitespace}
 * intentionally excludes NBSP from leading-trim. Expected render
 * strings in this suite spell those NBSP indents as {@code   }
 * escape sequences.</p>
 */
class ListBuilderNestedTest {

    /** Two non-breaking spaces — one indent unit per nesting depth. */
    private static final String NBSP2 = "  ";

    @Test
    void flatAddItemRemainsBackCompatAndProducesEmptyNestedItems() {
        ListNode node = new ListBuilder()
                .name("FlatList")
                .addItem("Alpha")
                .addItem("Beta")
                .addItem("Gamma")
                .build();

        assertThat(node.items()).containsExactly("Alpha", "Beta", "Gamma");
        assertThat(node.nestedItems()).isEmpty();
        // ListMarker.normalize appends a trailing space when the
        // marker glyph doesn't already end in whitespace, so the
        // bullet marker's value() is "• " (with trailing space).
        assertThat(node.marker().value()).isEqualTo("• ");
    }

    @Test
    void nestedAddItemPromotesListToNestedRepresentationAndClearsFlatItems() {
        ListNode node = new ListBuilder()
                .name("NestedList")
                .addItem("Parent", body -> body
                        .addItem("Child A")
                        .addItem("Child B"))
                .build();

        assertThat(node.items()).isEmpty();
        assertThat(node.nestedItems()).hasSize(1);
        ListItem parent = node.nestedItems().getFirst();
        assertThat(parent.label()).isEqualTo("Parent");
        assertThat(parent.children()).extracting(ListItem::label)
                .containsExactly("Child A", "Child B");
    }

    @Test
    void mixedFlatAndNestedAuthoringPreservesSourceOrderAtDepthZero() {
        ListNode node = new ListBuilder()
                .addItem("Flat A")
                .addItem("Nested", body -> body.addItem("Inner"))
                .addItem("Flat B")
                .build();

        assertThat(node.items()).isEmpty();
        assertThat(node.nestedItems()).extracting(ListItem::label)
                .containsExactly("Flat A", "Nested", "Flat B");
        ListItem nested = node.nestedItems().get(1);
        assertThat(nested.children()).extracting(ListItem::label).containsExactly("Inner");
        assertThat(node.nestedItems().get(0).children()).isEmpty();
        assertThat(node.nestedItems().get(2).children()).isEmpty();
    }

    @Test
    void nestedListRendersThreeLevelsWithDefaultDepthCascade() throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(320, 320)
                .margin(DocumentInsets.of(12))
                .create()) {

            session.pageFlow()
                    .name("ThreeLevels")
                    .addList(list -> list
                            .name("Tree")
                            .addItem("Top", body -> body
                                    .addItem("Mid", inner -> inner
                                            .addItem("Leaf"))))
                    .build();

            List<String> rendered = firstLineTexts(session);
            assertThat(rendered).containsExactly(
                    "• Top",
                    NBSP2 + "◦ Mid",
                    NBSP2 + NBSP2 + "▪ Leaf");
        }
    }

    @Test
    void perItemMarkerOverrideWinsOverDepthCascade() throws Exception {
        // Construct a node with an explicit per-item marker on the
        // depth-0 entry to exercise the item.marker() != null branch.
        ListNode tree = new ListNode(
                "PerItemMarker",
                List.of(),
                List.of(new ListItem(
                        "Override depth-0 with star",
                        ListMarker.custom("★"),
                        List.of(new ListItem("Default depth-1 cascade", null, List.of())))),
                ListMarker.bullet(),
                com.demcha.compose.document.style.DocumentTextStyle.DEFAULT,
                com.demcha.compose.document.node.TextAlign.LEFT,
                0.0,
                0.0,
                "",
                false,
                DocumentInsets.zero(),
                DocumentInsets.zero());

        try (DocumentSession session = GraphCompose.document()
                .pageSize(320, 240)
                .margin(DocumentInsets.of(12))
                .create()) {
            session.add(tree);

            List<String> rendered = firstLineTexts(session);
            assertThat(rendered).containsExactly(
                    "★ Override depth-0 with star",
                    NBSP2 + "◦ Default depth-1 cascade");
        }
    }

    @Test
    void markerForAtDepthOverridesCascadeButNotPerItemMarker() throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(360, 240)
                .margin(DocumentInsets.of(12))
                .create()) {

            session.pageFlow()
                    .name("MarkerForRoot")
                    .addList(list -> list
                            .name("MarkerForTree")
                            .markerFor(0, ListMarker.dash())
                            .markerFor(1, ListMarker.custom("→"))
                            .addItem("Top",
                                    body -> body.addItem("Mid",
                                            inner -> inner.addItem("Leaf"))))
                    .build();

            List<String> rendered = firstLineTexts(session);
            assertThat(rendered).containsExactly(
                    "- Top",
                    NBSP2 + "→ Mid",
                    NBSP2 + NBSP2 + "▪ Leaf");
        }
    }

    @Test
    void markerForAccepts10LevelsForDeepLists() {
        ListBuilder builder = new ListBuilder();
        builder.markerFor(0, ListMarker.dash());
        builder.markerFor(5, ListMarker.custom("»"));
        assertThat(builder.markerOverrides()).containsKeys(0, 5);
        // ListMarker.normalize appends a trailing space to non-empty
        // markers that do not already end in whitespace.
        assertThat(builder.markerOverrides().get(0).value()).isEqualTo("- ");
        assertThat(builder.markerOverrides().get(5).value()).isEqualTo("» ");
    }

    @Test
    void clearingMarkerForWithNullRemovesOverride() {
        ListBuilder builder = new ListBuilder()
                .markerFor(0, ListMarker.dash());
        assertThat(builder.markerOverrides()).containsKey(0);
        builder.markerFor(0, null);
        assertThat(builder.markerOverrides()).doesNotContainKey(0);
    }

    @Test
    void markerOverridesAreBakedIntoNodeNestedItemsAtBuildTime() {
        ListNode node = new ListBuilder()
                .markerFor(0, ListMarker.dash())
                .markerFor(1, ListMarker.custom("→"))
                .addItem("Top", body -> body.addItem("Inner"))
                .build();

        assertThat(node.nestedItems()).hasSize(1);
        ListItem top = node.nestedItems().getFirst();
        assertThat(top.marker()).isNotNull();
        assertThat(top.marker().value()).isEqualTo("- ");
        assertThat(top.children()).hasSize(1);
        assertThat(top.children().getFirst().marker().value()).isEqualTo("→ ");
    }

    @Test
    void deepNestingBeyondCascadeFallsBackToMidDot() throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(420, 320)
                .margin(DocumentInsets.of(12))
                .create()) {

            session.pageFlow()
                    .name("Depth4")
                    .addList(list -> list
                            .name("DeepTree")
                            .addItem("L0", b0 -> b0
                                    .addItem("L1", b1 -> b1
                                            .addItem("L2", b2 -> b2
                                                    .addItem("L3", b3 -> b3
                                                            .addItem("L4")))))
                    )
                    .build();

            List<String> rendered = firstLineTexts(session);
            assertThat(rendered).containsExactly(
                    "• L0",
                    NBSP2 + "◦ L1",
                    NBSP2 + NBSP2 + "▪ L2",
                    NBSP2 + NBSP2 + NBSP2 + "· L3",
                    NBSP2 + NBSP2 + NBSP2 + NBSP2 + "· L4");
        }
    }

    @Test
    void pageFlowBuilderWiresNestedListIntoContainerHierarchy() throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(320, 240)
                .margin(DocumentInsets.of(12))
                .create()) {

            ContainerNode root = session.pageFlow()
                    .name("RootFlow")
                    .addList(list -> list
                            .name("Tree")
                            .addItem("A", body -> body.addItem("a1")))
                    .build();

            assertThat(root.children()).hasSize(1);
            assertThat(root.children().getFirst()).isInstanceOf(ListNode.class);
            ListNode listNode = (ListNode) root.children().getFirst();
            assertThat(listNode.nestedItems()).hasSize(1);
            assertThat(listNode.nestedItems().getFirst().children())
                    .extracting(ListItem::label)
                    .containsExactly("a1");
        }
    }

    @Test
    void nestedListThreeLevelsLayoutMatchesSnapshot() throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(320, 320)
                .margin(DocumentInsets.of(12))
                .create()) {

            session.pageFlow()
                    .name("NestedListSnapshot")
                    .addList(list -> list
                            .name("Tree")
                            .addItem("Top",
                                    body -> body.addItem("Mid",
                                            inner -> inner.addItem("Leaf"))))
                    .build();

            LayoutSnapshotAssertions.assertMatches(session, "document/nested_list_three_levels");
        }
    }

    private static List<String> firstLineTexts(DocumentSession session) {
        return session.layoutGraph().fragments().stream()
                .map(PlacedFragment::payload)
                .filter(ParagraphFragmentPayload.class::isInstance)
                .map(ParagraphFragmentPayload.class::cast)
                .map(payload -> payload.lines().getFirst().text())
                .toList();
    }
}
