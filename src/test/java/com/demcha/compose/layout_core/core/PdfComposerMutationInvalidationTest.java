package com.demcha.compose.layout_core.core;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.layout_core.components.components_builders.ComponentBuilder;
import com.demcha.compose.layout_core.components.core.Entity;
import com.demcha.compose.layout_core.components.layout.Anchor;
import com.demcha.compose.layout_core.components.layout.coordinator.Placement;
import com.demcha.compose.layout_core.components.style.Margin;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PdfComposerMutationInvalidationTest {

    @Test
    void shouldReLayoutWhenEntityIsAddedAfterSnapshotBeforeRender() throws Exception {
        try (PdfComposer composer = GraphCompose.pdf()
                .pageSize(new PDRectangle(200, 200))
                .margin(10, 10, 10, 10)
                .create()) {

            ComponentBuilder cb = composer.componentBuilder();
            cb.rectangle()
                    .entityName("First")
                    .size(80, 30)
                    .anchor(Anchor.topLeft())
                    .build();

            composer.layoutSnapshot();

            Entity second = cb.rectangle()
                    .entityName("Second")
                    .size(80, 30)
                    .anchor(Anchor.topLeft())
                    .build();

            composer.toPDDocument();

            assertThat(second.getComponent(Placement.class)).isPresent();
        }
    }

    @Test
    void shouldReLayoutWhenCanvasMarginChangesAfterSnapshot() throws Exception {
        try (PdfComposer composer = GraphCompose.pdf()
                .pageSize(new PDRectangle(200, 200))
                .margin(10, 10, 10, 10)
                .create()) {

            Entity entity = composer.componentBuilder()
                    .rectangle()
                    .entityName("Probe")
                    .size(80, 30)
                    .anchor(Anchor.topLeft())
                    .build();

            composer.layoutSnapshot();
            double xBefore = entity.getComponent(Placement.class).orElseThrow().x();

            composer.margin(Margin.of(20));
            composer.toPDDocument();

            double xAfter = entity.getComponent(Placement.class).orElseThrow().x();
            assertThat(xAfter).isGreaterThan(xBefore);
        }
    }
}
