package com.demcha.compose.engine.render.pdf;

import com.demcha.compose.engine.components.content.shape.FillColor;
import com.demcha.compose.engine.components.core.Entity;
import com.demcha.compose.engine.components.geometry.ContentSize;
import com.demcha.compose.engine.components.layout.coordinator.Placement;
import com.demcha.compose.engine.components.renderable.Rectangle;
import com.demcha.compose.engine.core.EntityManager;
import com.demcha.compose.engine.render.pdf.handlers.PdfRectangleRenderHandler;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class PdfRenderSessionTest {

    @Test
    void shouldReuseSamePageSurfaceWithinOneRenderPass() throws Exception {
        try (PDDocument document = new PDDocument()) {
            PdfStream stream = new PdfStream(document, new PdfCanvas(PDRectangle.A4, 0.0f));

            try (PdfRenderSession session = (PdfRenderSession) stream.openRenderPass()) {
                PDPageContentStream first = session.pageSurface(2);
                PDPageContentStream second = session.pageSurface(2);
                PDPageContentStream third = session.pageSurface(1);

                assertThat(first).isSameAs(second);
                assertThat(third).isNotSameAs(first);
                assertThat(session.cachedPageCount()).isEqualTo(2);
                assertThat(document.getNumberOfPages()).isEqualTo(3);
            }
        }
    }

    @Test
    void shouldEnsurePagesWithoutOpeningAnySurface() throws Exception {
        try (PDDocument document = new PDDocument()) {
            PdfStream stream = new PdfStream(document, new PdfCanvas(PDRectangle.A4, 0.0f));

            try (PdfRenderSession session = (PdfRenderSession) stream.openRenderPass()) {
                session.ensurePage(2);

                assertThat(document.getNumberOfPages()).isEqualTo(3);
                assertThat(session.cachedPageCount()).isZero();
            }
        }
    }

    @Test
    void shouldCloseIdempotently() throws Exception {
        try (PDDocument document = new PDDocument()) {
            PdfStream stream = new PdfStream(document, new PdfCanvas(PDRectangle.A4, 0.0f));
            PdfRenderSession session = (PdfRenderSession) stream.openRenderPass();

            session.pageSurface(0);
            session.close();

            assertThat(session.isClosed()).isTrue();
            assertThatCode(session::close).doesNotThrowAnyException();
        }
    }

    @Test
    void shouldKeepSessionOwnedSurfaceOpenAfterHandlerReturns() throws Exception {
        try (PDDocument document = new PDDocument()) {
            PdfRenderingSystemECS renderingSystem = new PdfRenderingSystemECS(document, new PdfCanvas(PDRectangle.A4, 0.0f));
            Entity entity = new Entity();
            entity.addComponent(new Rectangle());
            entity.addComponent(new Placement(12, 18, 40, 24, 0, 0));
            entity.addComponent(new ContentSize(40, 24));
            entity.addComponent(FillColor.defaultColor());

            try (PdfRenderSession session = (PdfRenderSession) renderingSystem.stream().openRenderPass()) {
                renderingSystem.activeRenderSession(session);

                new PdfRectangleRenderHandler().render(
                        new EntityManager(),
                        entity,
                        entity.getComponent(Rectangle.class).orElseThrow(),
                        renderingSystem,
                        false);

                PDPageContentStream surface = session.pageSurface(0);
                assertThatCode(() -> {
                    surface.saveGraphicsState();
                    surface.addRect(1, 1, 2, 2);
                    surface.restoreGraphicsState();
                }).doesNotThrowAnyException();
            } finally {
                renderingSystem.activeRenderSession(null);
            }
        }
    }
}
