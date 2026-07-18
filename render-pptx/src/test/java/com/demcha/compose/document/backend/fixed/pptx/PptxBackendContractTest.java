package com.demcha.compose.document.backend.fixed.pptx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.backend.fixed.SectionUnit;
import com.demcha.compose.document.backend.fixed.pdf.PdfFixedLayoutBackend;
import com.demcha.compose.document.layout.LayoutGraph;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Negative contracts of the backend's document-level surface: null and empty
 * inputs fail with typed, actionable diagnostics, and the documented
 * render-to-images gap points at the PDF backend.
 */
class PptxBackendContractTest {

    @Test
    void builderRejectsNullChromeEntriesAndTimestamp() {
        PptxFixedLayoutBackend.Builder builder = PptxFixedLayoutBackend.builder();
        assertThatThrownBy(() -> builder.header(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("options");
        assertThatThrownBy(() -> builder.footer(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("options");
        assertThatThrownBy(() -> builder.deterministic((Instant) null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("timestamp");
    }

    @Test
    void renderToImagesPointsAtThePdfBackend() throws Exception {
        try (DocumentSession session = composeOnePage()) {
            LayoutGraph graph = session.render(new GraphCapturingBackend());
            assertThatThrownBy(() -> new PptxFixedLayoutBackend()
                    .renderToImages(graph, null, 144, false, -1))
                    .isInstanceOf(UnsupportedOperationException.class)
                    .hasMessageContaining("PDF backend");
        }
    }

    @Test
    void emptySectionsAreRejected() {
        PptxFixedLayoutBackend backend = new PptxFixedLayoutBackend();
        assertThatThrownBy(() -> backend.writeSections(List.of(), new ByteArrayOutputStream()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least one section");
    }

    @Test
    void aForeignSectionChromeIsRejectedWithTheSectionIndex() throws Exception {
        try (DocumentSession session = composeOnePage()) {
            LayoutGraph graph = session.render(new GraphCapturingBackend());
            List<SectionUnit> sections = List.of(new SectionUnit(
                    graph, graph.canvas(), List.of(), new PdfFixedLayoutBackend()));
            assertThatThrownBy(() -> new PptxFixedLayoutBackend()
                    .writeSections(sections, new ByteArrayOutputStream()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Section 0")
                    .hasMessageContaining("PdfFixedLayoutBackend");
        }
    }

    private static DocumentSession composeOnePage() {
        DocumentSession session = GraphCompose.document()
                .pageSize(300, 200)
                .margin(DocumentInsets.of(20))
                .create();
        session.add(session.dsl().shape().name("Card").size(100, 30)
                .fillColor(DocumentColor.GRAY).build());
        return session;
    }
}
