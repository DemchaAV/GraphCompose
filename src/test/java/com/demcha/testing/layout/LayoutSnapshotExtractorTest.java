package com.demcha.testing.layout;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.testsupport.engine.assembly.ComponentBuilder;
import com.demcha.compose.engine.components.content.text.TextStyle;
import com.demcha.compose.engine.components.core.Entity;
import com.demcha.compose.engine.components.core.EntityName;
import com.demcha.compose.engine.components.geometry.ContentSize;
import com.demcha.compose.engine.components.layout.Align;
import com.demcha.compose.engine.components.layout.Anchor;
import com.demcha.compose.engine.components.layout.Layer;
import com.demcha.compose.engine.components.layout.ParentComponent;
import com.demcha.compose.engine.components.layout.coordinator.ComputedPosition;
import com.demcha.compose.engine.components.layout.coordinator.Placement;
import com.demcha.compose.engine.components.style.Margin;
import com.demcha.compose.engine.components.style.Padding;
import com.demcha.compose.engine.core.Canvas;
import com.demcha.compose.engine.core.EntityManager;
import com.demcha.compose.testsupport.EngineComposerHarness;
import com.demcha.compose.engine.debug.LayoutSnapshotExtractor;
import com.demcha.compose.engine.debug.LayoutNodeSnapshot;
import com.demcha.compose.engine.debug.LayoutSnapshot;
import com.demcha.compose.testing.layout.LayoutSnapshotJson;
import com.demcha.compose.engine.render.pdf.PdfCanvas;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class LayoutSnapshotExtractorTest {

    @Test
    void shouldUseStablePathsAndFallbackNamesForUnnamedSiblings() throws Exception {
        try (EngineComposerHarness composer = com.demcha.compose.testsupport.EngineComposerHarness.pdf()
                .pageSize(PDRectangle.A4)
                .margin(11.1114f, 9.9999f, 8.8888f, 7.7777f)
                .create()) {

            ComponentBuilder cb = composer.componentBuilder();
            Entity left = cb.rectangle()
                    .size(40, 20)
                    .build();
            Entity right = cb.rectangle()
                    .size(50, 20)
                    .margin(Margin.of(1.1114))
                    .padding(Padding.of(2.2226))
                    .build();

            cb.vContainer(Align.left(4))
                    .entityName("RootFlow")
                    .anchor(Anchor.topLeft())
                    .addChild(left)
                    .addChild(right)
                    .build();

            LayoutSnapshot snapshot = composer.layoutSnapshot();

            assertThat(snapshot.canvas().margin().top()).isEqualTo(11.111);
            assertThat(snapshot.nodes())
                    .extracting(LayoutNodeSnapshot::path)
                    .containsExactly(
                            "RootFlow[0]",
                            "RootFlow[0]/Rectangle[0]",
                            "RootFlow[0]/Rectangle[1]");
            assertThat(snapshot.nodes().get(2).margin().top()).isEqualTo(1.111);
            assertThat(snapshot.nodes().get(2).padding().top()).isEqualTo(2.223);
        }
    }

    @Test
    void shouldPreserveDeterministicDepthLayerAndTraversalOrder() throws Exception {
        try (EngineComposerHarness composer = com.demcha.compose.testsupport.EngineComposerHarness.pdf().create()) {
            ComponentBuilder cb = composer.componentBuilder();

            Entity first = cb.rectangle().size(24, 12).build();
            Entity second = cb.rectangle().size(24, 12).build();
            Entity row = cb.hContainer(Align.left(3))
                    .entityName("ContentRow")
                    .anchor(Anchor.topLeft())
                    .addChild(first)
                    .addChild(second)
                    .build();

            cb.vContainer(Align.left(4))
                    .entityName("Root")
                    .anchor(Anchor.topLeft())
                    .addChild(row)
                    .build();

            LayoutSnapshot snapshot = composer.layoutSnapshot();

            assertThat(snapshot.nodes())
                    .extracting(LayoutNodeSnapshot::path)
                    .containsExactly(
                            "Root[0]",
                            "Root[0]/ContentRow[0]",
                            "Root[0]/ContentRow[0]/Rectangle[0]",
                            "Root[0]/ContentRow[0]/Rectangle[1]");
            assertThat(snapshot.nodes())
                    .extracting(LayoutNodeSnapshot::layer)
                    .containsExactly(1, 2, 3, 3);
            assertThat(snapshot.nodes())
                    .extracting(LayoutNodeSnapshot::depth)
                    .containsExactly(1, 2, 3, 3);
        }
    }

    @Test
    void shouldProduceByteStableJsonAcrossEquivalentCompositions() throws Exception {
        String firstJson = renderSnapshotJsonForStableDocument();
        String secondJson = renderSnapshotJsonForStableDocument();

        assertThat(secondJson).isEqualTo(firstJson);
    }

    @Test
    void shouldKeepStableRootOrderWhenRootsShareSameCoordinates() throws Exception {
        String firstJson = renderSnapshotJsonForEqualCoordinateRoots();
        String secondJson = renderSnapshotJsonForEqualCoordinateRoots();

        assertThat(secondJson).isEqualTo(firstJson);

        try (EngineComposerHarness composer = com.demcha.compose.testsupport.EngineComposerHarness.pdf()
                .pageSize(PDRectangle.A4)
                .margin(18, 18, 18, 18)
                .create()) {
            ComponentBuilder cb = composer.componentBuilder();
            cb.rectangle()
                    .size(80, 24)
                    .anchor(Anchor.topLeft())
                    .build();
            cb.rectangle()
                    .size(80, 24)
                    .anchor(Anchor.topLeft())
                    .build();

            LayoutSnapshot snapshot = composer.layoutSnapshot();
            assertThat(snapshot.nodes())
                    .extracting(LayoutNodeSnapshot::path)
                    .containsExactly("Rectangle[0]", "Rectangle[1]");
        }
    }

    @Test
    void shouldCaptureTotalPagesFromPlacementSpan() {
        EntityManager entityManager = new EntityManager(false);
        Entity entity = new Entity();
        entity.addComponent(new EntityName("PagedNode"));
        entity.addComponent(new ContentSize(160, 640));
        entity.addComponent(new ComputedPosition(20, -300));
        entity.addComponent(new Placement(20, 300, 160, 640, 2, 0));
        entity.addComponent(new Layer(1));
        entityManager.putEntity(entity);

        Canvas canvas = new PdfCanvas(PDRectangle.A4, 0, 0);
        canvas.addMargin(Margin.of(24));

        LayoutSnapshot snapshot = LayoutSnapshotExtractor.extract(entityManager, canvas);

        assertThat(snapshot.totalPages()).isEqualTo(3);
        assertThat(snapshot.nodes()).hasSize(1);
        assertThat(snapshot.nodes().getFirst().startPage()).isEqualTo(2);
        assertThat(snapshot.nodes().getFirst().endPage()).isEqualTo(0);
    }

    @Test
    void shouldUseCanonicalTraversalWhenParentComponentExistsButChildrenListIsMissingEntry() {
        EntityManager entityManager = new EntityManager(false);

        Entity parent = new Entity();
        parent.addComponent(new EntityName("Root"));
        parent.addComponent(new ContentSize(120, 80));
        parent.addComponent(new ComputedPosition(0, 0));
        parent.addComponent(new Placement(0, 0, 120, 80, 0, 0));
        parent.addComponent(new Layer(1));
        entityManager.putEntity(parent);

        Entity child = new Entity();
        child.addComponent(new EntityName("RecoveredChild"));
        child.addComponent(new ParentComponent(parent.getUuid()));
        child.addComponent(new ContentSize(40, 20));
        child.addComponent(new ComputedPosition(5, 5));
        child.addComponent(new Placement(5, 5, 40, 20, 0, 0));
        child.addComponent(new Layer(2));
        entityManager.putEntity(child);

        Canvas canvas = new PdfCanvas(PDRectangle.A4, 0, 0);
        LayoutSnapshot snapshot = LayoutSnapshotExtractor.extract(entityManager, canvas);

        assertThat(snapshot.nodes())
                .extracting(LayoutNodeSnapshot::path)
                .containsExactly("Root[0]", "Root[0]/RecoveredChild[0]");
    }

    @Test
    void shouldIgnoreStaleChildrenListEntriesAndFollowCanonicalParentComponentTraversal() {
        EntityManager entityManager = new EntityManager(false);

        Entity staleParent = new Entity();
        staleParent.addComponent(new EntityName("StaleParent"));
        staleParent.addComponent(new ContentSize(120, 80));
        staleParent.addComponent(new ComputedPosition(0, 0));
        staleParent.addComponent(new Placement(0, 0, 120, 80, 0, 0));
        staleParent.addComponent(new Layer(1));
        entityManager.putEntity(staleParent);

        Entity canonicalParent = new Entity();
        canonicalParent.addComponent(new EntityName("CanonicalParent"));
        canonicalParent.addComponent(new ContentSize(120, 80));
        canonicalParent.addComponent(new ComputedPosition(140, 0));
        canonicalParent.addComponent(new Placement(140, 0, 120, 80, 0, 0));
        canonicalParent.addComponent(new Layer(1));
        entityManager.putEntity(canonicalParent);

        Entity child = new Entity();
        child.addComponent(new EntityName("Child"));
        child.addComponent(new ParentComponent(canonicalParent.getUuid()));
        child.addComponent(new ContentSize(40, 20));
        child.addComponent(new ComputedPosition(145, 5));
        child.addComponent(new Placement(145, 5, 40, 20, 0, 0));
        child.addComponent(new Layer(2));
        entityManager.putEntity(child);

        staleParent.getChildren().add(child.getUuid());
        staleParent.getChildren().add(UUID.randomUUID());

        Canvas canvas = new PdfCanvas(PDRectangle.A4, 0, 0);
        LayoutSnapshot snapshot = LayoutSnapshotExtractor.extract(entityManager, canvas);

        assertThat(snapshot.nodes())
                .extracting(LayoutNodeSnapshot::path)
                .containsExactly(
                        "StaleParent[0]",
                        "CanonicalParent[1]",
                        "CanonicalParent[1]/Child[0]");
    }

    private String renderSnapshotJsonForStableDocument() throws Exception {
        try (EngineComposerHarness composer = com.demcha.compose.testsupport.EngineComposerHarness.pdf()
                .pageSize(PDRectangle.A4)
                .margin(18, 18, 18, 18)
                .create()) {
            ComponentBuilder cb = composer.componentBuilder();
            Entity heading = cb.text()
                    .entityName("StableHeading")
                    .textWithAutoSize("Stable JSON")
                    .textStyle(TextStyle.DEFAULT_STYLE)
                    .anchor(Anchor.topLeft())
                    .build();
            Entity box = cb.rectangle()
                    .size(120, 32)
                    .margin(Margin.top(4))
                    .build();

            cb.vContainer(Align.left(8))
                    .entityName("StableRoot")
                    .anchor(Anchor.topLeft())
                    .addChild(heading)
                    .addChild(box)
                    .build();

            return LayoutSnapshotJson.toJson(composer.layoutSnapshot());
        }
    }

    private String renderSnapshotJsonForEqualCoordinateRoots() throws Exception {
        try (EngineComposerHarness composer = com.demcha.compose.testsupport.EngineComposerHarness.pdf()
                .pageSize(PDRectangle.A4)
                .margin(18, 18, 18, 18)
                .create()) {
            ComponentBuilder cb = composer.componentBuilder();
            cb.rectangle()
                    .size(80, 24)
                    .anchor(Anchor.topLeft())
                    .build();
            cb.rectangle()
                    .size(80, 24)
                    .anchor(Anchor.topLeft())
                    .build();

            return LayoutSnapshotJson.toJson(composer.layoutSnapshot());
        }
    }
}
