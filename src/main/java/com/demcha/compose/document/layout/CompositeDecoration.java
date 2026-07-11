package com.demcha.compose.document.layout;

import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.style.DocumentBleed;
import com.demcha.compose.document.style.DocumentEdge;
import com.demcha.compose.engine.components.style.Margin;
import com.demcha.compose.engine.components.style.Padding;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds a composite node's per-page decoration bands — the fill / border band
 * drawn behind its children ({@link #fill}) and the overlay band drawn in front
 * ({@link #overlay}). A node that spans several pages is segmented so each
 * page's band clamps to that page's own content area (or, on a bled edge, the
 * physical page edge), and empty segments are dropped.
 *
 * <p>Pure geometry: every input is passed in and each call returns a fresh
 * fragment list. The caller ({@link LayoutCompiler}) owns where each list is
 * spliced — fills before the children, overlays appended after — because that
 * draw order is the load-bearing behaviour, not part of building the band.
 *
 * @author Artem Demchyshyn
 */
final class CompositeDecoration {

    private static final double EPS = 1e-6;

    private CompositeDecoration() {
    }

    /**
     * Resolves the decoration (fill / border) box for a composite. Without bleed
     * the box is the in-margin content geometry; each declared bleed edge instead
     * snaps that side of the box to the trimmed page edge (left/right adjust the x
     * span, top/bottom the y span) while the children stay in the content region.
     *
     * @param bleed   the node's bleed edges
     * @param x       content-box left (in-margin)
     * @param width   content-box width (in-margin)
     * @param topY    content-box top y (in-margin)
     * @param bottomY content-box bottom y (in-margin)
     * @param canvas  the page canvas, for the trimmed page-edge coordinates
     * @return the resolved decoration box
     */
    static BleedBox resolveBleedBox(DocumentBleed bleed,
                                    double x,
                                    double width,
                                    double topY,
                                    double bottomY,
                                    LayoutCanvas canvas) {
        double decorX = x;
        double decorWidth = width;
        double decorTopY = topY;
        double decorBottomY = bottomY;
        if (bleed.any()) {
            double pageWidth = canvas.width();
            double pageHeight = canvas.height();
            if (bleed.bleeds(DocumentEdge.LEFT)) {
                decorWidth += decorX;
                decorX = 0.0;
            }
            if (bleed.bleeds(DocumentEdge.RIGHT)) {
                decorWidth = Math.max(0.0, pageWidth - decorX);
            }
            if (bleed.bleeds(DocumentEdge.TOP)) {
                decorTopY = pageHeight;
            }
            if (bleed.bleeds(DocumentEdge.BOTTOM)) {
                decorBottomY = 0.0;
            }
        }
        return new BleedBox(decorX, decorWidth, decorTopY, decorBottomY);
    }

    /**
     * A resolved composite decoration box — the fill / border extent after content
     * bleed, in page space.
     *
     * @param x       decoration box left
     * @param width   decoration box width
     * @param topY    decoration box top y
     * @param bottomY decoration box bottom y
     */
    record BleedBox(double x, double width, double topY, double bottomY) {
    }

    static List<PlacedFragment> fill(PreparedNode<DocumentNode> prepared,
                                     NodeDefinition<DocumentNode> definition,
                                     String path,
                                     String parentPath,
                                     int childIndex,
                                     int depth,
                                     double placementX,
                                     double startPageTopY,
                                     double endPageBottomY,
                                     double placementWidth,
                                     int startPage,
                                     int endPage,
                                     Margin margin,
                                     Padding padding,
                                     LayoutCanvas canvas,
                                     DocumentBleed bleed,
                                     FragmentContext fragmentContext) {
        List<PlacedFragment> placed = new ArrayList<>();
        // The intermediate-page band edges follow each page's own margin, so a fill
        // spanning a per-page-margin boundary clamps to the right content area on
        // every page. With no per-page margins every page resolves to the canvas
        // margin and the bounds are identical for all pages, as before.
        PageGeometry geometry = fragmentContext.pageGeometry();
        for (int pageIndex = startPage; pageIndex <= endPage; pageIndex++) {
            // On bled edges the clamp bound is the physical page edge rather than the
            // content-area edge, so the fill reaches past the top/bottom margin.
            double pageTopY = bleed.bleeds(DocumentEdge.TOP)
                    ? canvas.height()
                    : canvas.height() - pageMarginTop(canvas, geometry, pageIndex);
            double pageBottomY = bleed.bleeds(DocumentEdge.BOTTOM)
                    ? 0.0
                    : pageMarginBottom(canvas, geometry, pageIndex);
            double segmentTopY = pageIndex == startPage ? startPageTopY : pageTopY;
            double segmentBottomY = pageIndex == endPage ? endPageBottomY : pageBottomY;
            segmentTopY = Math.min(segmentTopY, pageTopY);
            segmentBottomY = Math.max(pageBottomY, Math.min(segmentBottomY, segmentTopY));
            double segmentHeight = segmentTopY - segmentBottomY;
            if (segmentHeight <= EPS) {
                continue;
            }

            FragmentPlacement placement = new FragmentPlacement(
                    path,
                    parentPath,
                    childIndex,
                    depth,
                    pageIndex,
                    placementX,
                    segmentBottomY,
                    placementWidth,
                    segmentHeight,
                    startPage,
                    endPage,
                    margin,
                    padding);
            placed.addAll(toPlacedFragments(definition.emitFragments(prepared, fragmentContext, placement), placement));
        }

        return placed;
    }

    static List<PlacedFragment> overlay(PreparedNode<DocumentNode> prepared,
                                        NodeDefinition<DocumentNode> definition,
                                        String path,
                                        String parentPath,
                                        int childIndex,
                                        int depth,
                                        double placementX,
                                        double startPageTopY,
                                        double endPageBottomY,
                                        double placementWidth,
                                        int startPage,
                                        int endPage,
                                        Margin margin,
                                        Padding padding,
                                        LayoutCanvas canvas,
                                        FragmentContext fragmentContext) {
        List<PlacedFragment> placed = new ArrayList<>();
        PageGeometry geometry = fragmentContext.pageGeometry();
        for (int pageIndex = startPage; pageIndex <= endPage; pageIndex++) {
            double pageTopY = canvas.height() - pageMarginTop(canvas, geometry, pageIndex);
            double pageBottomY = pageMarginBottom(canvas, geometry, pageIndex);
            double segmentTopY = pageIndex == startPage ? startPageTopY : pageTopY;
            double segmentBottomY = pageIndex == endPage ? endPageBottomY : pageBottomY;
            segmentTopY = Math.min(segmentTopY, pageTopY);
            segmentBottomY = Math.max(pageBottomY, Math.min(segmentBottomY, segmentTopY));
            double segmentHeight = segmentTopY - segmentBottomY;
            if (segmentHeight <= EPS) {
                continue;
            }

            FragmentPlacement placement = new FragmentPlacement(
                    path,
                    parentPath,
                    childIndex,
                    depth,
                    pageIndex,
                    placementX,
                    segmentBottomY,
                    placementWidth,
                    segmentHeight,
                    startPage,
                    endPage,
                    margin,
                    padding);
            placed.addAll(toPlacedFragments(definition.emitOverlayFragments(prepared, fragmentContext, placement), placement));
        }

        return placed;
    }

    /**
     * Normalizes emitted fragments to a placement, re-indexing {@code fragmentIndex}
     * from zero per placement. Shared by the decoration bands and by the compiler's
     * non-decoration fragment sites.
     */
    static List<PlacedFragment> toPlacedFragments(List<LayoutFragment> emitted,
                                                  FragmentPlacement placement) {
        List<PlacedFragment> placed = new ArrayList<>(emitted.size());
        int fragmentIndex = 0;
        for (LayoutFragment fragment : emitted) {
            LayoutFragment normalized = new LayoutFragment(
                    placement.path(),
                    fragmentIndex++,
                    fragment.localX(),
                    fragment.localY(),
                    fragment.width(),
                    fragment.height(),
                    fragment.payload());
            placed.add(PlacedFragment.from(normalized, placement));
        }
        return placed;
    }

    private static double pageMarginTop(LayoutCanvas canvas, PageGeometry geometry, int pageIndex) {
        return geometry == null ? canvas.margin().top() : geometry.marginForPage(pageIndex).top();
    }

    private static double pageMarginBottom(LayoutCanvas canvas, PageGeometry geometry, int pageIndex) {
        return geometry == null ? canvas.margin().bottom() : geometry.marginForPage(pageIndex).bottom();
    }
}
