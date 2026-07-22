package com.demcha.compose.document.backend.fixed.pptx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.layout.LayoutGraph;
import com.demcha.compose.document.node.PageBreakNode;
import com.demcha.compose.document.output.DocumentHeaderFooter;
import com.demcha.compose.document.output.DocumentMetadata;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFTextBox;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The session's PPTX convenience trio: {@code toPptxBytes} / {@code writePptx}
 * / {@code buildPptx} resolve the backend through the format-keyed provider,
 * carry the session's chrome, and keep the PDF trio's stream and default-file
 * contracts.
 */
class PptxSessionConvenienceTest {

    private static DocumentSession composeWithChrome() {
        DocumentSession session = GraphCompose.document()
                .pageSize(400, 300)
                .margin(DocumentInsets.of(24))
                .create();
        session.metadata(DocumentMetadata.builder().title("Session Deck").build());
        session.footer(DocumentHeaderFooter.builder().centerText("{page} of {pages}").build());
        session.add(session.dsl().shape().name("Card").size(140, 40)
                .fillColor(DocumentColor.ROYAL_BLUE)
                .build());
        session.add(new PageBreakNode("Break", DocumentInsets.zero()));
        session.add(session.dsl().shape().name("SecondPage").size(120, 40)
                .fillColor(DocumentColor.ORANGE)
                .build());
        return session;
    }

    @Test
    void toPptxBytesCarriesSessionChromeAndSharedGeometry() throws Exception {
        try (DocumentSession session = composeWithChrome()) {
            byte[] pptx = session.toPptxBytes();
            LayoutGraph graph = session.render(new GraphCapturingBackend());
            assertThat(graph.totalPages()).isEqualTo(2);
            try (XMLSlideShow show = new XMLSlideShow(new ByteArrayInputStream(pptx))) {
                assertThat(show.getSlides()).hasSize(graph.totalPages());
                assertThat(show.getProperties().getCoreProperties().getTitle())
                        .isEqualTo("Session Deck");
                assertThat(show.getSlides().get(0).getShapes().stream()
                        .filter(shape -> "GraphCompose Footer".equals(shape.getShapeName()))
                        .map(shape -> ((XSLFTextBox) shape).getText()))
                        .containsExactly("1 of 2");
                assertThat(show.getSlides().get(1).getShapes().stream()
                        .filter(shape -> "GraphCompose Footer".equals(shape.getShapeName()))
                        .map(shape -> ((XSLFTextBox) shape).getText()))
                        .containsExactly("2 of 2");
            }
        }
    }

    @Test
    void writePptxLeavesTheCallerOwnedStreamOpen() throws Exception {
        class ProbeStream extends ByteArrayOutputStream {
            boolean closed;

            @Override
            public void close() {
                closed = true;
            }
        }
        try (DocumentSession session = composeWithChrome()) {
            ProbeStream output = new ProbeStream();
            session.writePptx(output);
            assertThat(output.closed).as("caller-owned stream must stay open").isFalse();
            try (XMLSlideShow show = new XMLSlideShow(
                    new ByteArrayInputStream(output.toByteArray()))) {
                assertThat(show.getSlides()).hasSize(2);
            }
        }
    }

    @Test
    void buildPptxWritesTheSuppliedFile(@TempDir Path tempDir) throws Exception {
        Path target = tempDir.resolve("session-deck.pptx");
        try (DocumentSession session = composeWithChrome()) {
            session.buildPptx(target);
            assertThat(Files.size(target)).isPositive();
            try (XMLSlideShow show = new XMLSlideShow(Files.newInputStream(target))) {
                assertThat(show.getSlides()).hasSize(2);
            }
        }
    }

    @Test
    void buildPptxUsesTheConfiguredDefaultOutputFile(@TempDir Path tempDir) throws Exception {
        Path target = tempDir.resolve("default-deck.pptx");
        try (DocumentSession session = GraphCompose.document(target)
                .pageSize(400, 300)
                .margin(DocumentInsets.of(24))
                .create()) {
            session.add(session.dsl().shape().name("Card").size(140, 40)
                    .fillColor(DocumentColor.ROYAL_BLUE)
                    .build());
            session.buildPptx();
            try (XMLSlideShow show = new XMLSlideShow(Files.newInputStream(target))) {
                assertThat(show.getSlides()).hasSize(1);
            }
        }
    }

    @Test
    void buildPptxWithoutADefaultOutputFileFails() {
        try (DocumentSession session = composeWithChrome()) {
            assertThatThrownBy(session::buildPptx)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("default output file");
        }
    }

    @Test
    void anEmptySessionRefusesToRender() {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(300, 200).create()) {
            assertThatThrownBy(session::toPptxBytes)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("empty document");
        }
    }

    @Test
    void nullStreamAndClosedSessionFailWithTypedDiagnostics() throws Exception {
        DocumentSession session = composeWithChrome();
        // A null stream is an argument error and passes through unwrapped.
        assertThatThrownBy(() -> session.writePptx(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("output");
        session.close();
        assertThatThrownBy(session::toPptxBytes)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already closed");
    }
}
