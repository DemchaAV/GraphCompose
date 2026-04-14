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
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

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

            composer.build();
            double lineXAfterBuild = firstLineX(composer, "ProbeBlock");

            assertThat(lineXAfterBuild).isEqualTo(lineXAfterSnapshot);
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

    private double firstLineX(PdfComposer composer, String entityName) throws Exception {
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

        return textData.lines().getFirst().x();
    }

    private EntityManager entityManager(PdfComposer composer) throws Exception {
        Field entityManagerField = AbstractDocumentComposer.class.getDeclaredField("entityManager");
        entityManagerField.setAccessible(true);
        return (EntityManager) entityManagerField.get(composer);
    }
}
