package com.demcha.compose.document.backend.fixed.pptx;

import com.demcha.compose.document.node.ExternalLinkTarget;
import com.demcha.compose.document.node.InternalLinkTarget;
import org.apache.poi.sl.usermodel.ShapeType;
import org.apache.poi.xslf.usermodel.XSLFAutoShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.openxmlformats.schemas.presentationml.x2006.main.CTShape;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.awt.geom.Rectangle2D;
import java.util.HashSet;
import java.util.Set;

/**
 * Emits the navigation the render pass recorded, once every fragment (and so
 * every anchor) is placed: fragment- and span-level link rectangles become
 * transparent click hotspots — external targets as URL hyperlinks, internal
 * targets resolved through the anchor map into slide-jump hyperlinks — and
 * bookmark records name their slides, the closest PPTX equivalent of a PDF
 * outline (slide names drive PowerPoint's navigation pane and grid view).
 *
 * <p>Runs after all sections of a render, so cross-section internal links
 * resolve against the combined deck exactly like the PDF backend's pass-B
 * link resolution.</p>
 *
 * @author Artem Demchyshyn
 */
final class PptxNavigationWriter {

    private static final Logger LOG = LoggerFactory.getLogger("com.demcha.compose.engine.render");

    private PptxNavigationWriter() {
        // Utility class, no instantiation.
    }

    /**
     * Emits every recorded link hotspot and bookmark slide name onto the
     * environment's slides. Hotspots land on the slide itself (page space),
     * so a link recorded inside a transform group stays clickable at the
     * fragment's untransformed page rectangle — the same behaviour as PDF
     * link annotations, which are page-space objects.
     */
    static void apply(PptxRenderEnvironment environment) {
        for (PptxRenderEnvironment.FragmentLink link : environment.fragmentLinks()) {
            if (link.target() instanceof ExternalLinkTarget external) {
                hotspot(environment.globalSlide(link.pageIndex()), link.rectangle())
                        .createHyperlink().linkToUrl(external.options().uri());
            } else if (link.target() instanceof InternalLinkTarget internal) {
                PptxRenderEnvironment.AnchorDestination destination =
                        environment.anchorDestinations().get(internal.anchor());
                if (destination == null) {
                    LOG.warn("render.pptx.link.unresolved anchor={} — no anchor with that "
                            + "name was placed, the hotspot is skipped", internal.anchor());
                    continue;
                }
                hotspot(environment.globalSlide(link.pageIndex()), link.rectangle())
                        .createHyperlink()
                        .linkToSlide(environment.globalSlide(destination.pageIndex()));
            }
        }

        // A slide carries one name, a page can carry several bookmarks: the
        // first (outermost) bookmark wins, matching reading order.
        Set<Integer> named = new HashSet<>();
        for (PptxRenderEnvironment.BookmarkRecord bookmark : environment.bookmarkRecords()) {
            if (named.add(bookmark.pageIndex())) {
                environment.globalSlide(bookmark.pageIndex())
                        .getXmlObject().getCSld().setName(bookmark.title());
            } else {
                LOG.debug("render.pptx.bookmark.collapsed title={} pageIndex={} — the slide "
                        + "is already named by an earlier bookmark", bookmark.title(),
                        bookmark.pageIndex());
            }
        }
    }

    /**
     * Creates one transparent, borderless hotspot rectangle. A fully
     * transparent solid fill keeps the whole rectangle clickable in
     * slide-show mode — noFill shapes only hit-test their outline.
     */
    private static XSLFAutoShape hotspot(XSLFSlide slide, Rectangle2D.Double rectangle) {
        XSLFAutoShape hotspot = slide.createAutoShape();
        hotspot.setShapeType(ShapeType.RECT);
        hotspot.setAnchor(rectangle);
        hotspot.setFillColor(new Color(0, 0, 0, 0));
        hotspot.setLineColor(null);
        ((CTShape) hotspot.getXmlObject()).getNvSpPr().getCNvPr()
                .setName("GraphCompose Link Hotspot");
        return hotspot;
    }
}
