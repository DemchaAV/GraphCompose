package com.demcha.compose.engine.render.pdf.handlers;

import com.demcha.compose.engine.components.content.link.LinkUrl;
import com.demcha.compose.engine.components.core.Entity;
import com.demcha.compose.engine.components.geometry.ContentSize;
import com.demcha.compose.engine.components.layout.coordinator.Placement;
import com.demcha.compose.engine.components.renderable.Link;
import com.demcha.compose.engine.components.style.Padding;
import com.demcha.compose.engine.core.EntityManager;
import com.demcha.compose.engine.render.pdf.PdfCanvas;
import com.demcha.compose.engine.render.pdf.PdfRenderingSystemECS;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionURI;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PdfLinkRenderHandlerTest {

    @Test
    void shouldCreatePageAndAttachUriAnnotation() throws Exception {
        try (PDDocument document = new PDDocument()) {
            PdfRenderingSystemECS renderingSystem = new PdfRenderingSystemECS(document, new PdfCanvas(PDRectangle.A4, 0.0f));
            EntityManager manager = new EntityManager();
            Entity entity = new Entity();

            entity.addComponent(new Link());
            entity.addComponent(new Placement(25, 40, 120, 20, 0, 0));
            entity.addComponent(new ContentSize(120, 20));
            entity.addComponent(Padding.of(4));
            entity.addComponent(new LinkUrl("https://example.com/profile"));

            boolean rendered = new PdfLinkRenderHandler().render(
                    manager,
                    entity,
                    entity.getComponent(Link.class).orElseThrow(),
                    renderingSystem,
                    false);

            assertThat(rendered).isTrue();
            assertThat(document.getNumberOfPages()).isEqualTo(1);
            assertThat(document.getPage(0).getAnnotations()).hasSize(1);
            assertThat(document.getPage(0).getAnnotations().getFirst()).isInstanceOf(PDAnnotationLink.class);

            PDAnnotationLink annotation = (PDAnnotationLink) document.getPage(0).getAnnotations().getFirst();
            assertThat(annotation.getAction()).isInstanceOf(PDActionURI.class);
            assertThat(((PDActionURI) annotation.getAction()).getURI()).isEqualTo("https://example.com/profile");
            assertThat(annotation.getRectangle().getWidth()).isEqualTo(128.0f);
            assertThat(annotation.getRectangle().getHeight()).isEqualTo(28.0f);
        }
    }
}
