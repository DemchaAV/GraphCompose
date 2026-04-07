package com.demcha.compose.layout_core.system.implemented_systems.pdf_systems;

import com.demcha.compose.layout_core.components.core.Entity;
import com.demcha.compose.layout_core.core.EntityManager;
import com.demcha.compose.layout_core.system.rendering.RenderHandler;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PdfRenderingSystemECSDispatchTest {

    @Test
    void shouldUseRegisteredHandler() throws Exception {
        try (PDDocument document = new PDDocument()) {
            PdfRenderingSystemECS renderingSystem = new PdfRenderingSystemECS(document, new PdfCanvas(PDRectangle.A4, 0.0f));
            EntityManager manager = new EntityManager();
            Entity entity = new Entity();
            entity.addComponent(new StubRender());
            manager.putEntity(entity);
            manager.setLayers(Map.of(0, List.of(entity.getUuid())));

            AtomicBoolean handlerCalled = new AtomicBoolean(false);
            renderingSystem.renderHandlers().register(new RenderHandler<StubRender, PdfRenderingSystemECS>() {
                @Override
                public Class<StubRender> renderType() {
                    return StubRender.class;
                }

                @Override
                public boolean render(EntityManager manager,
                                      Entity entity,
                                      StubRender renderComponent,
                                      PdfRenderingSystemECS renderingSystem,
                                      boolean guideLines) {
                    handlerCalled.set(true);
                    return true;
                }
            });

            renderingSystem.process(manager);

            assertThat(handlerCalled).isTrue();
        }
    }

    @Test
    void shouldFallbackToLegacyPdfRenderWhenNoHandlerExists() throws Exception {
        try (PDDocument document = new PDDocument()) {
            PdfRenderingSystemECS renderingSystem = new PdfRenderingSystemECS(document, new PdfCanvas(PDRectangle.A4, 0.0f));
            EntityManager manager = new EntityManager();
            Entity entity = new Entity();
            LegacyPdfRender legacyRender = new LegacyPdfRender();
            entity.addComponent(legacyRender);
            manager.putEntity(entity);
            manager.setLayers(Map.of(0, List.of(entity.getUuid())));

            renderingSystem.process(manager);

            assertThat(legacyRender.called).isTrue();
        }
    }

    @Test
    void shouldFailWhenRenderHasNoHandlerAndNoLegacyFallback() throws Exception {
        try (PDDocument document = new PDDocument()) {
            PdfRenderingSystemECS renderingSystem = new PdfRenderingSystemECS(document, new PdfCanvas(PDRectangle.A4, 0.0f));
            EntityManager manager = new EntityManager();
            Entity entity = new Entity();
            entity.addComponent(new UnsupportedRender());
            manager.putEntity(entity);
            manager.setLayers(Map.of(0, List.of(entity.getUuid())));

            assertThatThrownBy(() -> renderingSystem.process(manager))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining(UnsupportedRender.class.getName());
        }
    }

    private static final class StubRender implements com.demcha.compose.layout_core.system.interfaces.Render {
    }

    private static final class UnsupportedRender implements com.demcha.compose.layout_core.system.interfaces.Render {
    }

    private static final class LegacyPdfRender implements PdfRender {
        private boolean called;

        @Override
        public boolean pdf(EntityManager manager, Entity e, PdfRenderingSystemECS pdfRenderingSystem, boolean guideLines) throws IOException {
            called = true;
            return true;
        }
    }
}
