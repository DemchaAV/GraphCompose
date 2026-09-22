package com.demcha.compose.document.backend.semantic;

import com.demcha.compose.document.layout.DocumentGraph;
import com.demcha.compose.document.layout.LayoutCanvas;
import com.demcha.compose.document.layout.LayoutGraph;
import com.demcha.compose.document.output.DocumentOutputOptions;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The layout a semantic backend may now be given, and the shape of the context that
 * carries it.
 *
 * <p>These are the parts that need no session: that a backend which says nothing wants
 * no layout, that the published constructors still work and leave the layout absent, and
 * that asking for one that was never supplied says why rather than handing back
 * {@code null}. Whether the session actually compiles one, and whether it compiles the
 * layout of the graph beside it, is a question about a live document and is asked in the
 * qa module, where a font runtime exists to measure with.</p>
 *
 * @author Artem Demchyshyn
 */
class SemanticExportContextTest {

    private static final LayoutCanvas CANVAS =
            LayoutCanvas.from(595, 842, com.demcha.compose.engine.components.style.Margin.of(36));

    @Test
    void aBackendThatSaysNothingShouldNotBeGivenALayout() {
        SemanticBackend<String> quiet = new SemanticBackend<>() {
            @Override
            public String name() {
                return "quiet";
            }

            @Override
            public String export(DocumentGraph graph, SemanticExportContext context) {
                return "ok";
            }
        };

        // The default is what keeps a render backend from becoming a hard requirement of
        // exports that do not render: compiling a layout measures text.
        assertThat(quiet.requiresResolvedLayout()).isFalse();
    }

    @Test
    void thePublishedFourArgumentConstructorShouldStillBuildAContext() {
        SemanticExportContext context = new SemanticExportContext(
                CANVAS, List.of(), null, DocumentOutputOptions.EMPTY);

        assertThat(context.canvas()).isSameAs(CANVAS);
        assertThat(context.layoutGraph())
                .as("a caller that predates the layout component gets none")
                .isNull();
    }

    @Test
    void theThreeArgumentConstructorShouldStillDefaultTheOutputOptions() {
        SemanticExportContext context = new SemanticExportContext(CANVAS, List.of(), null);

        assertThat(context.outputOptions()).isSameAs(DocumentOutputOptions.EMPTY);
        assertThat(context.layoutGraph()).isNull();
    }

    @Test
    void askingForALayoutThatWasNeverSuppliedShouldSayWhy() {
        SemanticExportContext context = new SemanticExportContext(
                CANVAS, List.of(), null, DocumentOutputOptions.EMPTY);

        assertThatThrownBy(context::requireLayoutGraph)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("requiresResolvedLayout");
    }

    @Test
    void askingForALayoutThatWasSuppliedShouldHandItBack() {
        // The throwing path was covered and the returning one was not, so the method
        // could have been gutted to `return null` with nothing going red.
        LayoutGraph layout = new LayoutGraph(CANVAS, 1, List.of(), List.of());
        SemanticExportContext context = new SemanticExportContext(
                CANVAS, List.of(), null, DocumentOutputOptions.EMPTY, layout);

        assertThat(context.requireLayoutGraph()).isSameAs(layout);
    }

    @Test
    void nullOutputOptionsShouldStillBecomeTheEmptyBundle() {
        // The three-argument constructor passes EMPTY itself, so this normalisation was
        // never exercised: a caller writing null explicitly would have handed a backend
        // outputOptions() == null and it would have failed inside the backend.
        SemanticExportContext context = new SemanticExportContext(
                CANVAS, List.of(), null, null, null);

        assertThat(context.outputOptions()).isSameAs(DocumentOutputOptions.EMPTY);
    }

    @Test
    void theCustomFontCollectionShouldBeCopiedOutOfTheCallersHands() {
        List<com.demcha.compose.font.FontFamilyDefinition> mutable = new java.util.ArrayList<>();
        SemanticExportContext context = new SemanticExportContext(
                CANVAS, mutable, null, DocumentOutputOptions.EMPTY, null);

        assertThatThrownBy(() -> context.customFontFamilies().add(null))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
