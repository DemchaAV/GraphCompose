package com.demcha.compose.engine.core;

import com.demcha.compose.testsupport.EngineComposerHarness;

import com.demcha.compose.testsupport.engine.assembly.ComponentBuilder;
import com.demcha.compose.engine.components.core.Entity;
import com.demcha.compose.engine.components.layout.Anchor;
import com.demcha.compose.engine.components.layout.coordinator.Placement;
import com.demcha.compose.engine.components.style.Margin;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EngineComposerHarnessMutationInvalidationTest {

    @Test
    void shouldReLayoutWhenEntityIsAddedAfterSnapshotBeforeRender() throws Exception {
        try (EngineComposerHarness composer = com.demcha.compose.testsupport.EngineComposerHarness.pdf()
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
        try (EngineComposerHarness composer = com.demcha.compose.testsupport.EngineComposerHarness.pdf()
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
