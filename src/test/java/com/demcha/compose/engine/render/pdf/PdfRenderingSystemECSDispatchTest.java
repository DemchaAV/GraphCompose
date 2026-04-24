package com.demcha.compose.engine.render.pdf;

import com.demcha.compose.engine.components.core.Entity;
import com.demcha.compose.engine.components.layout.coordinator.ComputedPosition;
import com.demcha.compose.engine.components.layout.coordinator.Placement;
import com.demcha.compose.engine.components.style.Margin;
import com.demcha.compose.engine.core.EntityManager;
import com.demcha.compose.engine.render.RenderHandler;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
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
    void shouldFailWhenRenderHasNoHandler() throws Exception {
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

    @Test
    void shouldPreserveRenderOrderAndReuseSamePageSurfaceInsideProcess() throws Exception {
        try (PDDocument document = new PDDocument()) {
            PdfRenderingSystemECS renderingSystem = new PdfRenderingSystemECS(document, new PdfCanvas(PDRectangle.A4, 0.0f));
            EntityManager manager = new EntityManager();

            Entity lowerEntity = new Entity();
            lowerEntity.addComponent(new StubRender());
            lowerEntity.addComponent(new Placement(10, 10, 20, 20, 0, 0));
            lowerEntity.addComponent(new ComputedPosition(10, 10));

            Entity shiftedRightEntity = new Entity();
            shiftedRightEntity.addComponent(new StubRender());
            shiftedRightEntity.addComponent(new Placement(10, 10, 20, 20, 0, 0));
            shiftedRightEntity.addComponent(new ComputedPosition(10, 10));
            shiftedRightEntity.addComponent(Margin.left(5));

            Entity upperEntity = new Entity();
            upperEntity.addComponent(new StubRender());
            upperEntity.addComponent(new Placement(10, 50, 20, 20, 0, 0));
            upperEntity.addComponent(new ComputedPosition(10, 50));

            manager.putEntity(lowerEntity);
            manager.putEntity(shiftedRightEntity);
            manager.putEntity(upperEntity);
            manager.setLayers(Map.of(0, List.of(shiftedRightEntity.getUuid(), lowerEntity.getUuid(), upperEntity.getUuid())));

            List<java.util.UUID> callOrder = new ArrayList<>();
            List<Integer> surfaceIds = new ArrayList<>();
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
                                      boolean guideLines) throws IOException {
                    callOrder.add(entity.getUuid());
                    surfaceIds.add(System.identityHashCode(renderingSystem.pageSurface(entity)));
                    return true;
                }
            });

            renderingSystem.process(manager);

            assertThat(callOrder).containsExactly(upperEntity.getUuid(), lowerEntity.getUuid(), shiftedRightEntity.getUuid());
            assertThat(surfaceIds).hasSize(3);
            assertThat(surfaceIds.get(0)).isEqualTo(surfaceIds.get(1));
            assertThat(surfaceIds.get(1)).isEqualTo(surfaceIds.get(2));
            assertThat(document.getNumberOfPages()).isEqualTo(1);
        }
    }

    private static final class StubRender implements com.demcha.compose.engine.render.Render {
    }

    private static final class UnsupportedRender implements com.demcha.compose.engine.render.Render {
    }
}
