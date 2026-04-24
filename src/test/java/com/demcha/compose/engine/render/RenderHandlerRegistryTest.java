package com.demcha.compose.engine.render;

import com.demcha.compose.engine.components.core.Entity;
import com.demcha.compose.engine.core.EntityManager;
import com.demcha.compose.engine.render.pdf.PdfRenderingSystemECS;
import com.demcha.compose.engine.render.Render;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RenderHandlerRegistryTest {

    @Test
    void shouldPreferDirectHandlerOverParentTypeHandler() {
        RenderHandlerRegistry registry = new RenderHandlerRegistry();
        ParentRenderHandler parent = new ParentRenderHandler();
        ChildRenderHandler child = new ChildRenderHandler();

        registry.register(parent);
        registry.register(child);

        var resolved = registry.find(new ChildRender());

        assertThat(resolved).containsSame(child);
    }

    private static class ParentRender implements Render {
    }

    private static final class ChildRender extends ParentRender {
    }

    private static final class ParentRenderHandler implements RenderHandler<ParentRender, PdfRenderingSystemECS> {
        @Override
        public Class<ParentRender> renderType() {
            return ParentRender.class;
        }

        @Override
        public boolean render(EntityManager manager, Entity entity, ParentRender renderComponent, PdfRenderingSystemECS renderingSystem, boolean guideLines) {
            return true;
        }
    }

    private static final class ChildRenderHandler implements RenderHandler<ChildRender, PdfRenderingSystemECS> {
        @Override
        public Class<ChildRender> renderType() {
            return ChildRender.class;
        }

        @Override
        public boolean render(EntityManager manager, Entity entity, ChildRender renderComponent, PdfRenderingSystemECS renderingSystem, boolean guideLines) {
            return true;
        }
    }
}
