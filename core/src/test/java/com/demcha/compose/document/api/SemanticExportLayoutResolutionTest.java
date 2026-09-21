package com.demcha.compose.document.api;

import com.demcha.compose.document.backend.fixed.FixedLayoutRenderer;
import com.demcha.compose.document.backend.semantic.SemanticBackend;
import com.demcha.compose.document.backend.semantic.SemanticExportContext;
import com.demcha.compose.document.layout.DocumentGraph;
import com.demcha.compose.document.layout.LayoutCanvas;
import com.demcha.compose.document.layout.LayoutGraph;
import com.demcha.compose.document.output.DocumentOutputOptions;
import com.demcha.compose.engine.components.style.Margin;
import com.demcha.compose.font.FontFamilyDefinition;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The facade asks the session for a layout only when the backend asked for one.
 *
 * <p>Whether the backend is <em>handed</em> a layout is a different question, and the
 * qa suite answers it against a live session. This one answers the question that
 * matters for what the feature costs: was the layout <em>compiled</em> at all. The two
 * come apart — hoisting the call out of the ternary
 * ({@code var g = context.layoutGraph(); ... ? g : null}) hands a non-asking backend
 * the same {@code null} while compiling the layout anyway, which is exactly the cost the
 * flag exists to avoid, and every assertion about the handed-over value stays green.
 * So the session stands in as a counter here, and the assertion is on the count.</p>
 *
 * @author Artem Demchyshyn
 */
class SemanticExportLayoutResolutionTest {

    @Test
    void aBackendThatDoesNotAskShouldNotCauseALayoutToBeCompiled() throws Exception {
        CountingContext context = new CountingContext();

        new DocumentRenderingFacade(context).export(new ProbeBackend(false), null);

        assertThat(context.layoutRequests)
                .as("the session was never asked to compile a layout")
                .isZero();
    }

    @Test
    void aBackendThatAsksShouldCauseExactlyOneCompilation() throws Exception {
        CountingContext context = new CountingContext();

        new DocumentRenderingFacade(context).export(new ProbeBackend(true), null);

        assertThat(context.layoutRequests)
                .as("asked for once, not once per read")
                .isEqualTo(1);
    }

    /** Answers whatever it is asked, and counts how often the layout is wanted. */
    private static final class CountingContext implements DocumentRenderingFacade.Context {

        private final LayoutCanvas canvas = LayoutCanvas.from(595, 842, Margin.of(36));
        private final DocumentGraph graph = new DocumentGraph(List.of());
        private int layoutRequests;

        @Override
        public void ensureOpen() {
        }

        @Override
        public void ensureRenderable() {
        }

        @Override
        public String sessionId() {
            return "counting";
        }

        @Override
        public long revision() {
            return 1;
        }

        @Override
        public int rootCount() {
            return 0;
        }

        @Override
        public LayoutCanvas canvas() {
            return canvas;
        }

        @Override
        public List<FontFamilyDefinition> customFontFamilies() {
            return List.of();
        }

        @Override
        public LayoutGraph layoutGraph() {
            layoutRequests++;
            return new LayoutGraph(canvas, 1, List.of(), List.of());
        }

        @Override
        public DocumentGraph documentGraph() {
            return graph;
        }

        @Override
        public DocumentOutputOptions outputOptions() {
            return DocumentOutputOptions.EMPTY;
        }

        @Override
        public FixedLayoutRenderer convenienceBackend(String format) {
            throw new UnsupportedOperationException("no fixed-layout render in this test");
        }
    }

    /** Says whether it wants a layout and records what it was given. */
    private static final class ProbeBackend implements SemanticBackend<LayoutGraph> {

        private final boolean wantsLayout;

        ProbeBackend(boolean wantsLayout) {
            this.wantsLayout = wantsLayout;
        }

        @Override
        public String name() {
            return "probe";
        }

        @Override
        public boolean requiresResolvedLayout() {
            return wantsLayout;
        }

        @Override
        public LayoutGraph export(DocumentGraph graph, SemanticExportContext context) {
            return context.layoutGraph();
        }
    }
}
