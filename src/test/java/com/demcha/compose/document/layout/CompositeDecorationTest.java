package com.demcha.compose.document.layout;

import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.style.DocumentBleed;
import com.demcha.compose.document.style.DocumentEdge;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.engine.components.style.Margin;
import com.demcha.compose.engine.components.style.Padding;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Engine-level contract for the composite decoration / overlay band builder
 * extracted from {@link LayoutCompiler}. Whole-document geometry is guarded
 * end-to-end by the qa parity / snapshot suites (DocumentBleedTest,
 * PageMarginTest, PageIndexTest, …); these pin the per-page segmentation math in
 * isolation, driving a stub {@link NodeDefinition} that emits one band-sized
 * fragment so the resulting placement reflects each page's segment directly.
 */
class CompositeDecorationTest {

    private static final double EPS = 1e-9;

    // 200×100 canvas, 10pt margins → content band spans y=10 (bottom) to y=90 (top).
    private final LayoutCanvas canvas = LayoutCanvas.from(200, 100, Margin.of(10));

    @SuppressWarnings("unchecked")
    private final NodeDefinition<DocumentNode> definition = mock(NodeDefinition.class);
    private final FragmentContext ctx = mock(FragmentContext.class);

    CompositeDecorationTest() {
        // Emit a single fragment sized to the placement band, so each returned
        // PlacedFragment carries that page-segment's y (bottom) and height.
        when(definition.emitFragments(any(), any(), any())).thenAnswer(inv -> bandFragment(inv.getArgument(2)));
        when(definition.emitOverlayFragments(any(), any(), any())).thenAnswer(inv -> bandFragment(inv.getArgument(2)));
    }

    private static List<LayoutFragment> bandFragment(FragmentPlacement p) {
        return List.of(new LayoutFragment(p.path(), 0, 0.0, 0.0, p.width(), p.height(), "band"));
    }

    private List<PlacedFragment> fill(int startPage, int endPage, double startTopY, double endBottomY,
                                      DocumentBleed bleed, PageGeometry geometry) {
        when(ctx.pageGeometry()).thenReturn(geometry);
        return CompositeDecoration.fill(null, definition, "deco", null, 0, 0, 10, startTopY, endBottomY,
                180, startPage, endPage, Margin.zero(), Padding.zero(), canvas, bleed, ctx);
    }

    private List<PlacedFragment> overlay(int startPage, int endPage, double startTopY, double endBottomY,
                                         PageGeometry geometry) {
        when(ctx.pageGeometry()).thenReturn(geometry);
        return CompositeDecoration.overlay(null, definition, "deco", null, 0, 0, 10, startTopY, endBottomY,
                180, startPage, endPage, Margin.zero(), Padding.zero(), canvas, ctx);
    }

    @Test
    void singlePageBandKeepsItsRequestedTopAndBottom() {
        List<PlacedFragment> placed = fill(0, 0, 80, 30, DocumentBleed.none(), null);

        assertThat(placed).singleElement().satisfies(f -> {
            assertThat(f.pageIndex()).isZero();
            assertThat(f.x()).isEqualTo(10);
            assertThat(f.width()).isEqualTo(180);
            assertThat(f.y()).isCloseTo(30, within(EPS));      // segment bottom
            assertThat(f.height()).isCloseTo(50, within(EPS)); // 80 - 30
        });
    }

    @Test
    void multiPageBandClampsIntermediatePagesToTheContentBand() {
        List<PlacedFragment> placed = fill(0, 2, 80, 40, DocumentBleed.none(), null);

        assertThat(placed).hasSize(3);
        // Page 0: top follows the requested start (80), bottom drops to the margin (10).
        assertThat(placed.get(0).pageIndex()).isZero();
        assertThat(placed.get(0).y()).isCloseTo(10, within(EPS));
        assertThat(placed.get(0).height()).isCloseTo(70, within(EPS));
        // Page 1 (intermediate): full content band 10..90.
        assertThat(placed.get(1).pageIndex()).isEqualTo(1);
        assertThat(placed.get(1).y()).isCloseTo(10, within(EPS));
        assertThat(placed.get(1).height()).isCloseTo(80, within(EPS));
        // Page 2: top at the margin (90), bottom at the requested end (40).
        assertThat(placed.get(2).pageIndex()).isEqualTo(2);
        assertThat(placed.get(2).y()).isCloseTo(40, within(EPS));
        assertThat(placed.get(2).height()).isCloseTo(50, within(EPS));
    }

    @Test
    void intermediatePageClampsToThatPagesOwnMargin() {
        // Page 1 gets a 20pt margin override; pages 0 and 2 keep the 10pt canvas margin.
        PageGeometry geometry = PageGeometry.of(canvas,
                List.of(PageMarginOverride.fromInsets(1, 2, DocumentInsets.of(20))));

        List<PlacedFragment> placed = fill(0, 2, 95, 5, DocumentBleed.none(), geometry);

        assertThat(placed).hasSize(3);
        // Page 1 uses its OWN margin (20): band 20..80, not the canvas 10..90.
        assertThat(placed.get(1).y()).isCloseTo(20, within(EPS));
        assertThat(placed.get(1).height()).isCloseTo(60, within(EPS));
    }

    @Test
    void bledIntermediatePageReachesThePhysicalPageEdges() {
        List<PlacedFragment> placed = fill(0, 2, 95, 5, DocumentBleed.all(), null);

        // The intermediate page spans the full physical height (0..100), not the
        // content band (10..90) — the bleed pushes the clamp to the page edges.
        assertThat(placed.get(1).y()).isCloseTo(0, within(EPS));
        assertThat(placed.get(1).height()).isCloseTo(100, within(EPS));
    }

    @Test
    void collapsedSegmentIsSkipped() {
        // Requested top == bottom → zero-height segment, nothing emitted.
        List<PlacedFragment> placed = fill(0, 0, 50, 50, DocumentBleed.none(), null);

        assertThat(placed).isEmpty();
        verify(definition, never()).emitFragments(any(), any(), any());
    }

    @Test
    void overlayBuildsTheBandFromTheOverlayEmitHook() {
        when(ctx.pageGeometry()).thenReturn(null);

        List<PlacedFragment> placed = CompositeDecoration.overlay(null, definition, "deco", null, 0, 0, 10,
                80, 30, 180, 0, 0, Margin.zero(), Padding.zero(), canvas, ctx);

        assertThat(placed).singleElement().satisfies(f -> {
            assertThat(f.y()).isCloseTo(30, within(EPS));
            assertThat(f.height()).isCloseTo(50, within(EPS));
        });
        verify(definition).emitOverlayFragments(any(), any(), any());
        verify(definition, never()).emitFragments(any(), any(), any());
    }

    @Test
    void overlaySegmentsMultiplePagesClampingIntermediatePagesToTheContentBand() {
        List<PlacedFragment> placed = overlay(0, 2, 80, 40, null);

        assertThat(placed).hasSize(3);
        assertThat(placed.get(0).y()).isCloseTo(10, within(EPS));
        assertThat(placed.get(0).height()).isCloseTo(70, within(EPS));
        // Intermediate page fills the whole content band, same as fill() minus bleed.
        assertThat(placed.get(1).y()).isCloseTo(10, within(EPS));
        assertThat(placed.get(1).height()).isCloseTo(80, within(EPS));
        assertThat(placed.get(2).y()).isCloseTo(40, within(EPS));
        assertThat(placed.get(2).height()).isCloseTo(50, within(EPS));
    }

    @Test
    void toPlacedFragmentsReindexesFragmentIndexPerPlacement() {
        FragmentPlacement placement = new FragmentPlacement("owner", null, 0, 0, 3,
                10, 20, 180, 40, 0, 0, Margin.zero(), Padding.zero());
        List<LayoutFragment> emitted = List.of(
                new LayoutFragment("stale-a", 99, 0, 0, 5, 5, "a"),
                new LayoutFragment("stale-b", 42, 0, 0, 5, 5, "b"));

        List<PlacedFragment> placed = CompositeDecoration.toPlacedFragments(emitted, placement);

        assertThat(placed).hasSize(2);
        assertThat(placed.get(0).fragmentIndex()).isZero();
        assertThat(placed.get(1).fragmentIndex()).isEqualTo(1);
        assertThat(placed).allSatisfy(f -> {
            assertThat(f.path()).isEqualTo("owner");
            assertThat(f.pageIndex()).isEqualTo(3);
        });
    }
}
