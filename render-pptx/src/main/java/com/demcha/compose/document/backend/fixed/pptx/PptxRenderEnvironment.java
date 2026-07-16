package com.demcha.compose.document.backend.fixed.pptx;

import com.demcha.compose.document.layout.PlacedFragment;
import com.demcha.compose.document.node.DocumentBookmarkOptions;
import com.demcha.compose.document.node.DocumentLinkTarget;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Shared per-render-pass state for the fixed-layout PPTX backend.
 *
 * <p>The environment owns the slide surfaces for one document render pass plus
 * the navigation bookkeeping that resolves after all fragments have been
 * painted: bookmark records, anchor destinations, and fragment-level link
 * rectangles. Handlers draw through {@link #slide(int)} and never create
 * slides themselves.</p>
 *
 * <p>Navigation records are collected in slide top-down space (rectangles are
 * top-left anchored, in points). Their emission — slide-jump hyperlinks and
 * the bookmark-to-slide-name mapping — lands together with the navigation
 * support; the records are collected from the first release so no handler
 * needs retrofitting.</p>
 *
 * <p><b>Thread-safety:</b> mutable and confined to one render pass.</p>
 *
 * @since 2.1.0
 */
public final class PptxRenderEnvironment {

    private static final Logger LOG = LoggerFactory.getLogger("com.demcha.compose.engine.render");

    private final XMLSlideShow show;
    private final PptxRenderSession session;
    private final int pageIndexOffset;
    private final double canvasHeight;
    private final List<BookmarkRecord> bookmarkRecords = new ArrayList<>();
    private final Map<String, AnchorDestination> anchorDestinations = new LinkedHashMap<>();
    private final List<FragmentLink> fragmentLinks = new ArrayList<>();

    PptxRenderEnvironment(XMLSlideShow show,
                          PptxRenderSession session,
                          int pageIndexOffset,
                          double canvasHeight) {
        this.show = show;
        this.session = session;
        this.pageIndexOffset = pageIndexOffset;
        this.canvasHeight = canvasHeight;
    }

    /**
     * Returns the live slide show for the current render pass.
     *
     * @return mutable POI slide show owned by the backend
     */
    public XMLSlideShow slideShow() {
        return show;
    }

    /**
     * Returns the drawing surface for one resolved page.
     *
     * @param pageIndex zero-based page index
     * @return the slide backing that page
     */
    public XSLFSlide slide(int pageIndex) {
        return session.slide(pageIndex);
    }

    /**
     * Returns the page height in points — the constant every vertical
     * coordinate flips against when moving from the engine's bottom-left page
     * space into the slide's top-down space.
     *
     * @return canvas height in points
     */
    public double canvasHeight() {
        return canvasHeight;
    }

    void registerBookmark(PlacedFragment fragment, DocumentBookmarkOptions bookmarkOptions) {
        bookmarkRecords.add(new BookmarkRecord(
                bookmarkOptions.title(),
                bookmarkOptions.level(),
                fragment.pageIndex() + pageIndexOffset));
    }

    /**
     * Records the resolved slide and top-left corner of a named anchor declared
     * by an {@code AnchorMarkerPayload} fragment. A duplicate name keeps the
     * last registration (and logs a warning), matching the engine contract.
     *
     * @param fragment placed anchor marker fragment
     * @param anchor   non-blank anchor name
     */
    public void registerAnchor(PlacedFragment fragment, String anchor) {
        if (anchor == null || anchor.isBlank()) {
            return;
        }
        AnchorDestination destination = new AnchorDestination(
                fragment.pageIndex() + pageIndexOffset,
                fragment.x(),
                PptxCoordinates.topY(canvasHeight, fragment.y(), fragment.height()));
        AnchorDestination previous = anchorDestinations.put(anchor, destination);
        if (previous != null) {
            LOG.warn("render.pptx.anchor.duplicate name={} — last registration wins", anchor);
        }
    }

    void recordFragmentLink(PlacedFragment fragment, DocumentLinkTarget target) {
        fragmentLinks.add(new FragmentLink(
                fragment.pageIndex() + pageIndexOffset,
                PptxCoordinates.anchorOf(canvasHeight, fragment),
                target));
    }

    List<BookmarkRecord> bookmarkRecords() {
        return List.copyOf(bookmarkRecords);
    }

    Map<String, AnchorDestination> anchorDestinations() {
        return Map.copyOf(anchorDestinations);
    }

    List<FragmentLink> fragmentLinks() {
        return List.copyOf(fragmentLinks);
    }

    record BookmarkRecord(String title, int level, int pageIndex) {
    }

    /**
     * Resolved slide and top-left corner of a named anchor destination, in
     * slide top-down space.
     *
     * @param pageIndex zero-based slide index the anchor resolved to
     * @param left      left edge in points
     * @param top       top edge in points
     */
    record AnchorDestination(int pageIndex, double left, double top) {
    }

    /**
     * A fragment-level clickable rectangle awaiting link emission, in slide
     * top-down space.
     *
     * @param pageIndex zero-based slide index the rectangle lives on
     * @param rectangle clickable rectangle in points
     * @param target    resolved link target
     */
    record FragmentLink(int pageIndex, Rectangle2D.Double rectangle, DocumentLinkTarget target) {
    }
}
