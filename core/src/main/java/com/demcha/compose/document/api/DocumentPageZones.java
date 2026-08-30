package com.demcha.compose.document.api;

import com.demcha.compose.document.backend.fixed.MeasurementResources;
import com.demcha.compose.document.exceptions.AtomicNodeTooLargeException;
import com.demcha.compose.document.layout.DocumentGraph;
import com.demcha.compose.document.layout.DocumentLayoutPassContext;
import com.demcha.compose.document.layout.LayoutCanvas;
import com.demcha.compose.document.layout.LayoutCompiler;
import com.demcha.compose.document.layout.LayoutGraph;
import com.demcha.compose.document.layout.NodeRegistry;
import com.demcha.compose.document.layout.definitions.PageFieldDefinition;
import com.demcha.compose.document.layout.PlacedFragment;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.output.DocumentHeaderFooterZone;
import com.demcha.compose.document.output.DocumentPageZone;
import com.demcha.compose.document.output.PageContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Lays out each page zone's node subtree and splices the result into a compiled
 * {@link LayoutGraph}.
 *
 * <p>The same move {@link DocumentPageBackgrounds} makes, one layer up: a
 * background contributes one rectangle per page, a zone contributes whatever its
 * content function builds. Because the result is ordinary fragments in the
 * ordinary graph, every fixed-layout backend draws a zone with no code of its
 * own — the fonts, the bidi reordering, the inline chips and the link
 * annotations are the ones the body already gets.</p>
 *
 * <p>Zone fragments are appended, so they paint over the body; page backgrounds
 * are prepended and paint under it. That ordering matches the chrome the text
 * header/footer has always drawn.</p>
 *
 * <p>The zone is compiled against a canvas the size of its own band, so its
 * content is laid out in the space it actually has. A zone cannot paginate, so
 * content that needs more than the band raises
 * {@link AtomicNodeTooLargeException} rather than being silently dropped — a
 * footer that quietly loses half of what it was given is the harder bug to
 * find. Flow content that splits without being atomic is bounded the same way:
 * only the band's own page survives, and the rest is reported on the lifecycle
 * logger.</p>
 *
 * @author Artem Demchyshyn
 */
final class DocumentPageZones {

    private static final Logger LIFECYCLE_LOG =
            LoggerFactory.getLogger("com.demcha.compose.document.lifecycle");

    private DocumentPageZones() {
    }

    /**
     * Emits every applicable zone on every page.
     *
     * @param base      freshly compiled layout graph
     * @param zones     zones registered on the session; {@code null}/empty leaves
     *                  {@code base} unchanged
     * @param compiler  the session's layout compiler
     * @param registry  the session's node registry
     * @param resources the session's measurement resources
     * @param markdown  whether markdown parsing is enabled for this session
     * @param sessionId session id, for the drop diagnostic
     * @return a layout graph with the zone fragments, or {@code base}
     */
    static LayoutGraph apply(LayoutGraph base,
                             List<DocumentPageZone> zones,
                             LayoutCompiler compiler,
                             NodeRegistry registry,
                             MeasurementResources resources,
                             boolean markdown,
                             String sessionId) {
        if (zones == null || zones.isEmpty() || base.totalPages() == 0) {
            return base;
        }

        double pageHeight = base.canvas().height();
        double bandWidth = base.canvas().innerWidth();
        double bandLeft = base.canvas().marginLeft();

        List<PlacedFragment> combined = new ArrayList<>(base.fragments());
        for (int page = 0; page < base.totalPages(); page++) {
            PageContext context = PageContext.paginated(page + 1, base.totalPages());
            for (int index = 0; index < zones.size(); index++) {
                DocumentPageZone zone = zones.get(index);
                if (!zone.appliesTo(context)) {
                    continue;
                }
                combined.addAll(placeZone(
                        zone, index, context, page, pageHeight, bandWidth, bandLeft,
                        compiler, registry, resources, markdown, sessionId));
            }
        }
        return new LayoutGraph(base.canvas(), base.totalPages(), base.nodes(), combined);
    }

    private static List<PlacedFragment> placeZone(DocumentPageZone zone,
                                                  int zoneIndex,
                                                  PageContext context,
                                                  int page,
                                                  double pageHeight,
                                                  double bandWidth,
                                                  double bandLeft,
                                                  LayoutCompiler compiler,
                                                  NodeRegistry registry,
                                                  MeasurementResources resources,
                                                  boolean markdown,
                                                  String sessionId) {
        DocumentNode content = zone.getContent() == null ? null : zone.getContent().apply(context);
        if (content == null) {
            return List.of();
        }

        LayoutCanvas band = LayoutCanvas.from(bandWidth, zone.getHeight(), zone.getPadding());
        // A page field carries the question rather than the answer, so the band
        // publishes the page it is drawing through the channel a page reference
        // already uses; PageFieldDefinition reads it back and renders the number.
        DocumentLayoutPassContext pass = new DocumentLayoutPassContext(
                registry, band, resources.fontLibrary(), resources.textMeasurementSystem(), markdown,
                Map.of(PageFieldDefinition.NUMBER_KEY, context.number(),
                        PageFieldDefinition.TOTAL_KEY, context.total()));
        LayoutGraph laidOut;
        try {
            laidOut = compiler.compile(new DocumentGraph(List.of(content)), pass, pass);
        } catch (AtomicNodeTooLargeException tooLarge) {
            // The compiler's message is written for a page, and reading "page
            // capacity is 14.0" about a 240pt page sends the author looking in the
            // wrong place. Same failure, named where it actually is.
            throw new AtomicNodeTooLargeException(
                    "The " + zone.getZone() + " zone's content does not fit its declared height of "
                            + zone.getHeight() + "pt on page " + context.number() + ". A zone does not"
                            + " paginate, so its content has to fit the band: raise the zone's height,"
                            + " reduce its padding, or build less into it. (" + tooLarge.getMessage() + ")");
        }

        // The band's own y origin on the page. Fragment y is PDF-native (up from
        // the page bottom), and the sub-compile measured from the band's bottom,
        // so a footer needs no shift at all and a header is lifted by the band's
        // distance from the bottom edge.
        double bandBottom = zone.getZone() == DocumentHeaderFooterZone.HEADER
                ? pageHeight - zone.getHeight()
                : 0.0;

        List<PlacedFragment> placed = new ArrayList<>(laidOut.fragments().size());
        int dropped = 0;
        for (PlacedFragment fragment : laidOut.fragments()) {
            if (fragment.pageIndex() != 0) {
                dropped++;
                continue;
            }
            placed.add(new PlacedFragment(
                    "@page-zone[" + page + "][" + zoneIndex + "]" + fragment.path(),
                    fragment.fragmentIndex(),
                    page,
                    bandLeft + fragment.x(),
                    bandBottom + fragment.y(),
                    fragment.width(),
                    fragment.height(),
                    fragment.margin(),
                    fragment.padding(),
                    fragment.payload()));
        }
        if (dropped > 0) {
            LIFECYCLE_LOG.warn(
                    "document.zone.overflow sessionId={} page={} zone={} height={} droppedFragments={}"
                            + " — a zone does not paginate; declare a height that fits its content",
                    sessionId, page + 1, zone.getZone(), zone.getHeight(), dropped);
        }
        return placed;
    }
}
