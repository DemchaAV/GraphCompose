package com.demcha.compose.document.backend.fixed.pdf;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.layout.PlacedFragment;
import com.demcha.compose.document.layout.payloads.ShapeFragmentPayload;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;

import java.awt.Color;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

class PdfBackendExtensibilityTest {

    @Test
    void customHandlerReplacesBuiltInForSamePayloadType() {
        AtomicInteger renderCallCount = new AtomicInteger();

        PdfFragmentRenderHandler<ShapeFragmentPayload> custom =
                new PdfFragmentRenderHandler<>() {
                    @Override
                    public Class<ShapeFragmentPayload> payloadType() {
                        return ShapeFragmentPayload.class;
                    }

                    @Override
                    public void render(PlacedFragment fragment,
                                       ShapeFragmentPayload payload,
                                       PdfRenderEnvironment environment) {
                        renderCallCount.incrementAndGet();
                    }
                };

        PdfFixedLayoutBackend backend = PdfFixedLayoutBackend.builder()
                .addHandler(custom)
                .build();

        try (DocumentSession session = GraphCompose.document()
                .pageSize(400, 300)
                .margin(DocumentInsets.of(20))
                .pageBackground(DocumentColor.of(Color.LIGHT_GRAY))
                .create()) {

            session.compose(dsl -> dsl.pageFlow(flow -> flow.addText("body")));
            byte[] bytes = session.render(backend);

            assertThat(bytes)
                    .describedAs("backend should still produce non-empty PDF bytes when a custom handler is installed")
                    .isNotEmpty();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        assertThat(renderCallCount.get())
                .describedAs("custom ShapeFragmentPayload handler should have fired at least once for the page background")
                .isGreaterThanOrEqualTo(1);
    }

    @Test
    void addHandlerRejectsDuplicateCustomHandlersForSamePayloadType() {
        PdfFragmentRenderHandler<ShapeFragmentPayload> first = noopShapeHandler();
        PdfFragmentRenderHandler<ShapeFragmentPayload> second = noopShapeHandler();

        PdfFixedLayoutBackend.Builder builder = PdfFixedLayoutBackend.builder().addHandler(first);

        assertThatThrownBy(() -> builder.addHandler(second))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Duplicate custom PDF handler")
                .hasMessageContaining(ShapeFragmentPayload.class.getName());
    }

    @Test
    void buildWithoutCustomHandlersStillProducesAFunctionalBackend() {
        PdfFixedLayoutBackend backend = PdfFixedLayoutBackend.builder().build();

        try (DocumentSession session = GraphCompose.document().create()) {
            session.compose(dsl -> dsl.pageFlow(flow -> flow.addText("body")));
            byte[] bytes = session.render(backend);
            assertThat(bytes).isNotEmpty();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static PdfFragmentRenderHandler<ShapeFragmentPayload> noopShapeHandler() {
        return new PdfFragmentRenderHandler<>() {
            @Override
            public Class<ShapeFragmentPayload> payloadType() {
                return ShapeFragmentPayload.class;
            }

            @Override
            public void render(PlacedFragment fragment,
                               ShapeFragmentPayload payload,
                               PdfRenderEnvironment environment) {
                // intentional no-op
            }
        };
    }
}
