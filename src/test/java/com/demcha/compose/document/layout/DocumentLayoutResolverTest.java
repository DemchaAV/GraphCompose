package com.demcha.compose.document.layout;

import com.demcha.compose.document.backend.fixed.MeasurementResources;
import com.demcha.compose.engine.components.style.Margin;
import com.demcha.compose.engine.components.style.Padding;
import com.demcha.compose.engine.measurement.TextMeasurementSystem;
import com.demcha.compose.font.FontLibrary;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Locks the extracted convergence loop's control flow in isolation. The
 * byte-identical layout output is guarded by the qa regression / parity suites;
 * these assert the recompile count and returned graph directly, driving the
 * resolver through a mocked {@link DocumentLayoutResolver.Context} and a mocked
 * {@link LayoutCompiler} so no real layout is computed.
 */
class DocumentLayoutResolverTest {

    private final LayoutCanvas canvas = mock(LayoutCanvas.class);
    private final LayoutCompiler compiler = mock(LayoutCompiler.class);
    private final DocumentLayoutResolver.Context context = mock(DocumentLayoutResolver.Context.class);

    DocumentLayoutResolverTest() {
        MeasurementResources measurementResources = mock(MeasurementResources.class);
        when(measurementResources.fontLibrary()).thenReturn(mock(FontLibrary.class));
        when(measurementResources.textMeasurementSystem()).thenReturn(mock(TextMeasurementSystem.class));
        when(context.registry()).thenReturn(mock(NodeRegistry.class));
        when(context.canvas()).thenReturn(canvas);
        when(context.compiler()).thenReturn(compiler);
        when(context.measurementResources()).thenReturn(measurementResources);
        when(context.sessionId()).thenReturn("test-session");
    }

    private LayoutGraph graph() {
        return new LayoutGraph(canvas, 1, List.of(), List.of());
    }

    private LayoutGraph graphWith(PlacedNode... nodes) {
        return new LayoutGraph(canvas, 1, List.of(nodes), List.of());
    }

    private static PlacedNode placed(String path, int startPage) {
        return new PlacedNode(path, path, "block", null, 0, 0, 0,
                0, 0, 0, 0, 0, 0, startPage, startPage, 0, 0, Margin.zero(), Padding.zero());
    }

    @Test
    void nullContextIsRejected() {
        assertThatNullPointerException()
                .isThrownBy(() -> new DocumentLayoutResolver(null))
                .withMessageContaining("context");
    }

    @Test
    void plainDocumentCompilesExactlyOnce() {
        LayoutGraph only = graph();
        when(context.pageGeometry()).thenReturn(null);
        when(context.hasPageReference()).thenReturn(false);
        when(compiler.compile(any(), any(), any())).thenReturn(only);

        LayoutGraph result = new DocumentLayoutResolver(context).resolve();

        assertThat(result).isSameAs(only);
        verify(compiler, times(1)).compile(any(), any(), any());
        verify(context, never()).resolvePageNumbers(any());
    }

    @Test
    void pageReferenceRecompilesUntilNumbersStabilize() {
        LayoutGraph first = graph();
        LayoutGraph settled = graph();
        when(context.pageGeometry()).thenReturn(null);
        when(context.hasPageReference()).thenReturn(true);
        when(compiler.compile(any(), any(), any())).thenReturn(first, settled);
        // Same resolved numbers on both reads → the layout settles after one recompile.
        when(context.resolvePageNumbers(any())).thenReturn(Map.of("intro", 1));

        LayoutGraph result = new DocumentLayoutResolver(context).resolve();

        assertThat(result).isSameAs(settled);
        verify(compiler, times(2)).compile(any(), any(), any());
    }

    @Test
    void perPageMarginRecompilesUntilStartPagesStabilize() {
        LayoutGraph first = graphWith(placed("body", 0));
        LayoutGraph settled = graphWith(placed("body", 0));
        when(context.pageGeometry()).thenReturn(mock(PageGeometry.class));
        when(context.hasPageReference()).thenReturn(false);
        when(compiler.compile(any(), any(), any())).thenReturn(first, settled);

        LayoutGraph result = new DocumentLayoutResolver(context).resolve();

        // Same node start-pages on both reads → the margin fixed point settles after
        // one recompile, with no page-reference resolution involved.
        assertThat(result).isSameAs(settled);
        verify(compiler, times(2)).compile(any(), any(), any());
        verify(context, never()).resolvePageNumbers(any());
    }

    @Test
    void coupledResolveWaitsForBothPageNumbersAndStartPages() {
        LayoutGraph pass0 = graphWith(placed("body", 0));
        LayoutGraph pass1 = graphWith(placed("body", 1));
        LayoutGraph pass2 = graphWith(placed("body", 1));
        when(context.pageGeometry()).thenReturn(mock(PageGeometry.class));
        when(context.hasPageReference()).thenReturn(true);
        when(compiler.compile(any(), any(), any())).thenReturn(pass0, pass1, pass2);
        // Page numbers are stable from the first read; only the start-pages keep
        // moving, so the loop must NOT settle until the margin fixed point also does.
        when(context.resolvePageNumbers(any())).thenReturn(Map.of("intro", 1));

        LayoutGraph result = new DocumentLayoutResolver(context).resolve();

        assertThat(result).isSameAs(pass2);
        verify(compiler, times(3)).compile(any(), any(), any());
    }

    @Test
    void nonConvergingDocumentIsCappedAndReturnsLastLayout() {
        when(context.pageGeometry()).thenReturn(null);
        when(context.hasPageReference()).thenReturn(true);
        // A distinct graph per compile so we can assert the LAST one is returned.
        LayoutGraph[] graphs = new LayoutGraph[1 + DocumentLayoutResolver.MAX_PASSES];
        Arrays.setAll(graphs, i -> graph());
        when(compiler.compile(any(), any(), any()))
                .thenReturn(graphs[0], Arrays.copyOfRange(graphs, 1, graphs.length));
        // Numbers never repeat → the loop never settles and runs to the cap.
        AtomicInteger tick = new AtomicInteger();
        when(context.resolvePageNumbers(any())).thenAnswer(inv -> Map.of("intro", tick.incrementAndGet()));

        LayoutGraph result = new DocumentLayoutResolver(context).resolve();

        assertThat(result).isSameAs(graphs[graphs.length - 1]);
        verify(compiler, times(1 + DocumentLayoutResolver.MAX_PASSES)).compile(any(), any(), any());
    }
}
