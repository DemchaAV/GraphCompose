package com.demcha.compose.document.api;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.dsl.LayerStackBuilder;
import com.demcha.compose.document.layout.LayoutGraph;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.LayerAlign;
import com.demcha.compose.document.node.LayerStackNode;
import com.demcha.compose.document.node.RowNode;
import com.demcha.compose.document.node.SectionNode;
import com.demcha.compose.document.node.SpacerNode;
import com.demcha.compose.document.style.DocumentInsets;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pins the v1.6.2 R3 relaxation: a {@code Row} may sit directly inside
 * a {@link LayerStackNode} content layer (and at any depth below it
 * through vertical sections), while a {@code Row} nested inside another
 * {@code Row} band remains rejected by the validator.
 *
 * <p>Before R3 every test in this class would throw
 * {@code IllegalStateException("Row '...' cannot contain a nested
 * horizontal row")} because the compiler validator could not tell a
 * row-band parent apart from a layer rectangle parent. The R3.a
 * refactor introduced {@code FixedSlotKind} and R3.b taught the
 * validator to relax only when {@code STACK_LAYER_SLOT}.</p>
 *
 * @author Artem Demchyshyn
 */
class LayerStackRowCompositionTest {

    @Test
    void rowSitsDirectlyInsideLayerStackContentLayer() throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(400, 300)
                .margin(DocumentInsets.of(20))
                .create()) {

            RowNode contentRow = rowOf(
                    sectionOf("Sidebar", new SpacerNode("SidebarFiller", 120.0, 80.0,
                            DocumentInsets.zero(), DocumentInsets.zero())),
                    sectionOf("Main", new SpacerNode("MainFiller", 200.0, 80.0,
                            DocumentInsets.zero(), DocumentInsets.zero())));

            LayerStackNode stack = new LayerStackBuilder()
                    .name("PageWithRowLayer")
                    .layer(contentRow, LayerAlign.TOP_LEFT)
                    .build();

            session.add(stack);

            LayoutGraph graph = session.layoutGraph();
            assertThat(graph.totalPages()).isEqualTo(1);
            assertThat(graph.nodes())
                    .extracting("semanticName")
                    .contains("PageWithRowLayer", "Sidebar", "Main");
        }
    }

    @Test
    void layerStackAtRootWithBackgroundLayerAndContentRowRenders() throws Exception {
        // Mirrors the CV-style use case from the Noir corporate feedback:
        // a dark full-width band as one layer, a sidebar + main row as
        // the content layer.
        try (DocumentSession session = GraphCompose.document()
                .pageSize(595, 500)
                .margin(DocumentInsets.of(0))
                .create()) {

            SpacerNode darkBand = new SpacerNode("DarkBand", 595.0, 80.0,
                    DocumentInsets.zero(), DocumentInsets.zero());

            RowNode contentRow = rowOf(
                    sectionOf("Sidebar", new SpacerNode("SidebarBody", 180.0, 400.0,
                            DocumentInsets.zero(), DocumentInsets.zero())),
                    sectionOf("Main", new SpacerNode("MainBody", 415.0, 400.0,
                            DocumentInsets.zero(), DocumentInsets.zero())));

            LayerStackNode stack = new LayerStackBuilder()
                    .name("NoirCv")
                    .layer(darkBand, LayerAlign.TOP_LEFT)
                    .layer(contentRow, LayerAlign.TOP_LEFT)
                    .build();

            session.add(stack);

            LayoutGraph graph = session.layoutGraph();
            assertThat(graph.totalPages()).isEqualTo(1);
            assertThat(graph.nodes())
                    .extracting("semanticName")
                    .contains("NoirCv", "DarkBand", "Sidebar", "Main");
        }
    }

    @Test
    void rowDeepInsideLayerStackThroughVerticalSectionsRenders() throws Exception {
        // STACK_LAYER_SLOT must propagate through nested VERTICAL
        // composites — section -> section -> row should still relax.
        try (DocumentSession session = GraphCompose.document()
                .pageSize(400, 300)
                .margin(DocumentInsets.of(20))
                .create()) {

            RowNode innerRow = rowOf(
                    sectionOf("ColA", new SpacerNode("ColAFiller", 100.0, 60.0,
                            DocumentInsets.zero(), DocumentInsets.zero())),
                    sectionOf("ColB", new SpacerNode("ColBFiller", 100.0, 60.0,
                            DocumentInsets.zero(), DocumentInsets.zero())));

            SectionNode outerSection = sectionOf("Outer",
                    sectionOf("Inner", innerRow));

            LayerStackNode stack = new LayerStackBuilder()
                    .name("StackWithDeepRow")
                    .layer(outerSection, LayerAlign.TOP_LEFT)
                    .build();

            session.add(stack);

            LayoutGraph graph = session.layoutGraph();
            assertThat(graph.totalPages()).isEqualTo(1);
            assertThat(graph.nodes())
                    .extracting("semanticName")
                    .contains("Outer", "Inner", "ColA", "ColB");
        }
    }

    @Test
    void rowInsideRowStillThrowsAfterRelaxation() throws Exception {
        // Negative guard: the relaxation must NOT leak into the
        // ROW_SLOT path. A row whose direct child is another row is
        // still real composition conflict and must throw.
        try (DocumentSession session = GraphCompose.document()
                .pageSize(400, 300)
                .margin(DocumentInsets.of(20))
                .create()) {

            RowNode innerRow = rowOf(
                    new SpacerNode("Inner1", 50.0, 30.0,
                            DocumentInsets.zero(), DocumentInsets.zero()),
                    new SpacerNode("Inner2", 50.0, 30.0,
                            DocumentInsets.zero(), DocumentInsets.zero()));

            RowNode outerRow = rowOf(
                    innerRow,
                    new SpacerNode("Sibling", 50.0, 30.0,
                            DocumentInsets.zero(), DocumentInsets.zero()));

            session.add(outerRow);

            assertThatThrownBy(session::layoutGraph)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("cannot contain a nested horizontal row")
                    .hasMessageContaining("LayerStack layer");
        }
    }

    private static SectionNode sectionOf(String name, DocumentNode child) {
        return new SectionNode(
                name,
                List.of(child),
                0.0,
                DocumentInsets.zero(),
                DocumentInsets.zero(),
                null,
                null);
    }

    private static RowNode rowOf(DocumentNode... children) {
        return new RowNode(
                "Row",
                List.of(children),
                List.of(),
                0.0,
                DocumentInsets.zero(),
                DocumentInsets.zero(),
                null,
                null,
                null);
    }
}
