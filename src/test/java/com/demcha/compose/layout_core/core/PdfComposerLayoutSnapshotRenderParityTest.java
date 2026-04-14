package com.demcha.compose.layout_core.core;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.layout_core.components.components_builders.ComponentBuilder;
import com.demcha.compose.layout_core.components.components_builders.BlockIndentStrategy;
import com.demcha.compose.layout_core.components.content.text.BlockTextData;
import com.demcha.compose.layout_core.components.content.text.TextStyle;
import com.demcha.compose.layout_core.components.core.Entity;
import com.demcha.compose.layout_core.components.core.EntityName;
import com.demcha.compose.layout_core.components.layout.Align;
import com.demcha.compose.layout_core.components.layout.Anchor;
import com.demcha.compose.layout_core.components.style.Padding;
import com.demcha.compose.layout_core.system.LayoutSystem;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PdfComposerLayoutSnapshotRenderParityTest {

    @Test
    void shouldNotShiftBlockTextWhenBuildFollowsLayoutSnapshot() throws Exception {
        try (PdfComposer composer = GraphCompose.pdf()
                .pageSize(PDRectangle.A4)
                .margin(18, 18, 18, 18)
                .markdown(true)
                .create()) {
            composeBlockTextProbe(composer);

            composer.layoutSnapshot();
            double lineXAfterSnapshot = firstLineX(composer, "ProbeBlock");
            double lineWidthAfterSnapshot = firstLineWidth(composer, "ProbeBlock");

            composer.build();
            double lineXAfterBuild = firstLineX(composer, "ProbeBlock");
            double lineWidthAfterBuild = firstLineWidth(composer, "ProbeBlock");

            assertThat(lineXAfterBuild).isEqualTo(lineXAfterSnapshot);
            assertThat(lineWidthAfterBuild).isEqualTo(lineWidthAfterSnapshot);
        }
    }

    @Test
    void shouldResetLayersAndDepthStateAcrossRepeatedLayoutPasses() throws Exception {
        try (PdfComposer composer = GraphCompose.pdf()
                .pageSize(PDRectangle.A4)
                .margin(18, 18, 18, 18)
                .markdown(true)
                .create()) {
            composeLayerProbe(composer);

            EntityManager entityManager = entityManager(composer);
            LayoutSystem<?> layoutSystem = layoutSystem(composer);

            layoutSystem.process(entityManager);
            List<UUID> firstLayerOrder = flattenedLayerOrder(entityManager);
            LinkedHashMap<UUID, Integer> firstDepths = new LinkedHashMap<>(entityManager.getDepthById());

            layoutSystem.process(entityManager);
            List<UUID> secondLayerOrder = flattenedLayerOrder(entityManager);

            assertThat(secondLayerOrder).containsExactlyElementsOf(firstLayerOrder);
            assertThat(secondLayerOrder).doesNotHaveDuplicates();
            assertThat(secondLayerOrder).hasSize(entityManager.getEntities().size());
            assertThat(entityManager.getDepthById()).containsExactlyEntriesOf(firstDepths);
        }
    }

    private void composeBlockTextProbe(PdfComposer composer) {
        ComponentBuilder cb = composer.componentBuilder();

        cb.vContainer(Align.left(8))
                .entityName("ProbeRoot")
                .anchor(Anchor.topLeft())
                .addChild(cb.blockText(Align.left(2), TextStyle.DEFAULT_STYLE)
                        .entityName("ProbeBlock")
                        .size(composer.canvas().innerWidth() - 40, 2)
                        .padding(Padding.of(12))
                        .bulletOffset("• ")
                        .strategy(BlockIndentStrategy.ALL_LINES)
                        .text(
                                java.util.List.of("Focused on platform engineering, document generation, backend integration, and resilient delivery."),
                                TextStyle.DEFAULT_STYLE,
                                null,
                                null)
                        .anchor(Anchor.topLeft())
                        .build())
                .build();
    }

    private void composeLayerProbe(PdfComposer composer) {
        ComponentBuilder cb = composer.componentBuilder();

        Entity left = cb.rectangle()
                .entityName("LeftProbe")
                .size(48, 24)
                .anchor(Anchor.topLeft())
                .build();
        Entity right = cb.rectangle()
                .entityName("RightProbe")
                .size(48, 24)
                .anchor(Anchor.topLeft())
                .build();

        Entity row = cb.hContainer(Align.left(4))
                .entityName("ProbeRow")
                .anchor(Anchor.topLeft())
                .addChild(left)
                .addChild(right)
                .build();

        cb.vContainer(Align.left(6))
                .entityName("ProbeRoot")
                .anchor(Anchor.topLeft())
                .addChild(row)
                .addChild(cb.blockText(Align.left(2), TextStyle.DEFAULT_STYLE)
                        .entityName("ProbeFooter")
                        .size(composer.canvas().innerWidth() - 40, 2)
                        .padding(Padding.of(8))
                        .text(
                                java.util.List.of("Repeated layout passes should not duplicate layer membership or depth state."),
                                TextStyle.DEFAULT_STYLE,
                                null,
                                null)
                        .anchor(Anchor.topLeft())
                        .build())
                .build();
    }

    private double firstLineX(PdfComposer composer, String entityName) throws Exception {
        return firstLine(composer, entityName).x();
    }

    private double firstLineWidth(PdfComposer composer, String entityName) throws Exception {
        return firstLine(composer, entityName).lineWidth();
    }

    private com.demcha.compose.layout_core.components.content.text.LineTextData firstLine(PdfComposer composer, String entityName) throws Exception {
        EntityManager entityManager = entityManager(composer);

        Entity blockEntity = entityManager.getEntities().values().stream()
                .filter(entity -> entity.getComponent(EntityName.class)
                        .map(EntityName::value)
                        .filter(entityName::equals)
                        .isPresent())
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing entity " + entityName));

        BlockTextData textData = blockEntity.getComponent(BlockTextData.class)
                .orElseThrow(() -> new AssertionError("Missing BlockTextData for " + entityName));

        return textData.lines().getFirst();
    }

    private EntityManager entityManager(PdfComposer composer) throws Exception {
        Field entityManagerField = AbstractDocumentComposer.class.getDeclaredField("entityManager");
        entityManagerField.setAccessible(true);
        return (EntityManager) entityManagerField.get(composer);
    }

    private LayoutSystem<?> layoutSystem(PdfComposer composer) throws Exception {
        Field layoutSystemField = PdfComposer.class.getDeclaredField("layoutSystem");
        layoutSystemField.setAccessible(true);
        return (LayoutSystem<?>) layoutSystemField.get(composer);
    }

    private List<UUID> flattenedLayerOrder(EntityManager entityManager) {
        return entityManager.getLayers().values().stream()
                .flatMap(List::stream)
                .toList();
    }
}
