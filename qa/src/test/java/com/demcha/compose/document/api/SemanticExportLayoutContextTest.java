package com.demcha.compose.document.api;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.backend.semantic.SemanticBackend;
import com.demcha.compose.document.backend.semantic.SemanticExportContext;
import com.demcha.compose.document.layout.DocumentGraph;
import com.demcha.compose.document.layout.LayoutGraph;
import com.demcha.compose.document.style.DocumentInsets;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A semantic backend that asks for the compiled layout is given one, and one that does
 * not ask never causes it to be compiled.
 *
 * <p>The second half is the part worth protecting. Compiling a layout measures text, and
 * measurement needs a font runtime that lives in a render module — so if the session
 * resolved a layout for every semantic export, adding a DOCX dependency alone would stop
 * being enough to export DOCX. The counter is a backend that records whether it was
 * handed one.</p>
 *
 * @author Artem Demchyshyn
 */
class SemanticExportLayoutContextTest {

    /** Records what the session handed it, and optionally asks for a layout. */
    private static final class RecordingBackend implements SemanticBackend<String> {

        private final boolean wantsLayout;
        private final List<LayoutGraph> layouts = new ArrayList<>();
        private final List<DocumentGraph> graphs = new ArrayList<>();
        private final AtomicInteger calls = new AtomicInteger();

        RecordingBackend(boolean wantsLayout) {
            this.wantsLayout = wantsLayout;
        }

        @Override
        public String name() {
            return wantsLayout ? "layout-aware" : "graph-only";
        }

        @Override
        public boolean requiresResolvedLayout() {
            return wantsLayout;
        }

        @Override
        public String export(DocumentGraph graph, SemanticExportContext context) {
            calls.incrementAndGet();
            graphs.add(graph);
            layouts.add(context.layoutGraph());
            return name();
        }
    }

    @Test
    void aBackendThatAsksShouldBeGivenTheCompiledLayout() throws Exception {
        RecordingBackend backend = new RecordingBackend(true);

        try (DocumentSession session = GraphCompose.document()
                .pageSize(595, 842)
                .margin(DocumentInsets.of(36))
                .create()) {
            session.pageFlow(page -> page.addParagraph(p -> p.text("One line.")));
            assertThat(session.export(backend)).isEqualTo("layout-aware");
        }

        assertThat(backend.layouts).hasSize(1);
        assertThat(backend.layouts.get(0)).isNotNull();
        assertThat(backend.layouts.get(0).totalPages()).isPositive();
    }

    @Test
    void aBackendThatDoesNotAskShouldBeGivenNoLayout() throws Exception {
        RecordingBackend backend = new RecordingBackend(false);

        try (DocumentSession session = GraphCompose.document()
                .pageSize(595, 842)
                .margin(DocumentInsets.of(36))
                .create()) {
            session.pageFlow(page -> page.addParagraph(p -> p.text("One line.")));
            assertThat(session.export(backend)).isEqualTo("graph-only");
        }

        assertThat(backend.layouts).containsExactly((LayoutGraph) null);
    }

    @Test
    void theLayoutShouldBeCompiledFromTheGraphHandedToTheSameCall() throws Exception {
        RecordingBackend backend = new RecordingBackend(true);

        try (DocumentSession session = GraphCompose.document()
                .pageSize(595, 842)
                .margin(DocumentInsets.of(36))
                .create()) {
            session.pageFlow(page -> page.addParagraph(p -> p.text("One line.")));
            session.export(backend);

            // Edit, then export again. A layout cached from before the edit would report
            // the old document, which is the failure a backend reading resolved widths
            // could not detect for itself.
            session.pageFlow(page -> {
                for (int i = 0; i < 80; i++) {
                    page.addParagraph(p -> p.text(
                            "A further paragraph, added so the document outgrows one page."));
                }
            });
            session.export(backend);
        }

        assertThat(backend.calls).hasValue(2);
        assertThat(backend.layouts.get(1).totalPages())
                .as("the second export sees the document as it is now")
                .isGreaterThan(backend.layouts.get(0).totalPages());
        assertThat(backend.graphs.get(1).roots().size())
                .isGreaterThan(backend.graphs.get(0).roots().size());
    }

    @Test
    void theLayoutShouldDescribeTheSameCanvasTheContextCarries() throws Exception {
        RecordingBackend backend = new RecordingBackend(true);

        try (DocumentSession session = GraphCompose.document()
                .pageSize(420, 300)
                .margin(DocumentInsets.of(20))
                .create()) {
            session.pageFlow(page -> page.addParagraph(p -> p.text("One line.")));
            session.export(backend);
        }

        LayoutGraph layout = backend.layouts.get(0);
        assertThat(layout.canvas().width()).isEqualTo(420);
        assertThat(layout.canvas().innerWidth())
                .as("the page geometry the export was configured with, not a default")
                .isEqualTo(420 - 40);
    }
}
