package com.demcha.compose.document.api;

import com.demcha.compose.document.layout.LayoutDepth;
import com.demcha.compose.document.layout.LayoutGraph;
import com.demcha.compose.document.layout.PlacedFragment;
import com.demcha.compose.document.layout.ResolvedLayoutAddition;
import com.demcha.compose.document.layout.ResolvedLayoutMetadata;
import com.demcha.compose.document.layout.ResolvedLayoutPass;
import com.demcha.compose.document.layout.payloads.LayoutAnchorPayload;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Runs the resolved-layout passes over a compiled graph and splices what they contribute.
 *
 * <p>Package-private: this is the wiring behind a built-in feature, not a seam authors
 * reach. It sits beside {@code DocumentPageBackgrounds} and {@code DocumentPageZones}, the
 * two other post-compilation splices, and runs before both.</p>
 *
 * <p>The order matters and is not arbitrary. Backgrounds prepend their fragments and zones
 * append theirs, so running passes first yields, with no index arithmetic:</p>
 *
 * <pre>
 * background  &lt;  pass-under  &lt;  body  &lt;  pass-over  &lt;  zone chrome
 * </pre>
 *
 * <p>A pass running after backgrounds would have its under-body fragment prepended to
 * index 0 — beneath an opaque page background, and invisible. Within the body the same
 * hazard is local rather than page-wide: an under-body fragment placed at the front of the
 * list also sits beneath the fill of every container drawn before the feature, including
 * the panel the feature is inside. So under-body means under the contributing feature's own
 * content — spliced immediately before the first fragment its anchors produced on that
 * page — and a pass that anchored nothing on a page keeps the front of the list.</p>
 *
 * @author Artem Demchyshyn
 * @since 2.4.0
 */
final class ResolvedLayoutPasses {

    private ResolvedLayoutPasses() {
    }

    /**
     * Applies the passes in registration order.
     *
     * @param base   freshly compiled layout graph
     * @param passes passes to run, in order; {@code null}/empty leaves {@code base} unchanged
     * @return a graph carrying the contributed fragments, or {@code base} itself
     * @throws NullPointerException  if {@code base} is null
     * @throws IllegalStateException if a pass returns null, or contributes a fragment for a
     *                               page the document does not have
     */
    static LayoutGraph apply(LayoutGraph base, List<ResolvedLayoutPass> passes) {
        Objects.requireNonNull(base, "base");

        // Collected once, from the compiled graph, and handed to every pass. Nothing a
        // pass contributes can seed a new anchor for a later pass, so the result does not
        // depend on how the passes happen to interleave.
        ResolvedLayoutMetadata metadata = ResolvedLayoutMetadata.from(base);

        List<ResolvedLayoutPass> running = discover(metadata);
        if (passes != null) {
            running.addAll(passes);
        }
        if (running.isEmpty()) {
            return base;
        }

        // Where each pass's own content starts, page by page, so that "under the body" can
        // mean under *its* body. Prepending to index 0 instead puts the fragment beneath
        // everything drawn before the feature — including the fill of a panel the feature
        // sits inside, which paints over it and leaves no trace in the geometry.
        Map<Object, Map<Integer, Integer>> ownContent = firstOwnFragmentPerPage(base);

        Map<Integer, List<PlacedFragment>> under = new TreeMap<>();
        List<PlacedFragment> over = new ArrayList<>();
        int underCount = 0;
        for (ResolvedLayoutPass pass : running) {
            List<ResolvedLayoutAddition> additions = pass.contribute(base, metadata);
            if (additions == null) {
                throw new IllegalStateException(
                        "Resolved-layout pass '" + pass.id() + "' returned null; return an empty list instead.");
            }
            for (ResolvedLayoutAddition addition : additions) {
                if (addition == null) {
                    throw new IllegalStateException(
                            "Resolved-layout pass '" + pass.id() + "' returned a null addition.");
                }
                PlacedFragment fragment = addition.fragment();
                if (fragment.pageIndex() < 0 || fragment.pageIndex() >= base.totalPages()) {
                    throw new IllegalStateException("Resolved-layout pass '" + pass.id()
                                                    + "' contributed a fragment on page " + fragment.pageIndex()
                                                    + ", but the document has " + base.totalPages()
                                                    + " page(s). A pass may not add pages.");
                }
                // A non-finite coordinate is not caught anywhere downstream: PlacedFragment
                // does not validate, and NaN reaches the content stream as a broken operator.
                requireFinite(pass, fragment.x(), "x");
                requireFinite(pass, fragment.y(), "y");
                requireFinite(pass, fragment.width(), "width");
                requireFinite(pass, fragment.height(), "height");
                if (addition.depth() == LayoutDepth.UNDER_BODY) {
                    under.computeIfAbsent(
                                    ownContent.getOrDefault(pass, Map.of())
                                            .getOrDefault(fragment.pageIndex(), 0),
                                    index -> new ArrayList<>())
                            .add(fragment);
                    underCount++;
                } else {
                    over.add(fragment);
                }
            }
        }

        if (underCount == 0 && over.isEmpty()) {
            return base;
        }

        List<PlacedFragment> body = base.fragments();
        List<PlacedFragment> combined = new ArrayList<>(underCount + body.size() + over.size());
        for (int i = 0; i < body.size(); i++) {
            combined.addAll(under.getOrDefault(i, List.of()));
            combined.add(body.get(i));
        }
        combined.addAll(under.getOrDefault(body.size(), List.of()));
        combined.addAll(over);
        // Nodes pass through untouched: a pass contributes drawing, never structure.
        return new LayoutGraph(base.canvas(), base.totalPages(), base.nodes(), combined);
    }

    /**
     * The first fragment each anchor group put on each page.
     *
     * <p>An anchor's own fragment precedes its child's, so this is where a feature's
     * content begins in draw order — and therefore where something drawn beneath that
     * feature has to be spliced. One scan, and the answer for every pass at once.</p>
     *
     * @param base the compiled graph
     * @return group key to page to first index; groups with no anchors are absent
     */
    private static Map<Object, Map<Integer, Integer>> firstOwnFragmentPerPage(LayoutGraph base) {
        Map<Object, Map<Integer, Integer>> first = new IdentityHashMap<>();
        List<PlacedFragment> fragments = base.fragments();
        for (int i = 0; i < fragments.size(); i++) {
            PlacedFragment fragment = fragments.get(i);
            if (fragment.payload() instanceof LayoutAnchorPayload anchor) {
                first.computeIfAbsent(anchor.id().groupKey(), key -> new HashMap<>())
                        .putIfAbsent(fragment.pageIndex(), i);
            }
        }
        return first;
    }

    /**
     * The passes the document itself asks for, found in what it anchored.
     *
     * <p>A built-in feature declares an owner on the semantic tree and keys its anchors on
     * it; an owner that is <em>also</em> a pass is a feature saying it has something to
     * draw once the layout is settled. Nothing is registered, no session is handed around,
     * and this class stays ignorant of every feature that uses it — it asks whether the
     * owner is a pass, not what kind of thing it is.</p>
     *
     * <p>Order is first appearance in the anchor list, which is the compiler's placement
     * order, which is reading order down the document. Two instances of one feature on a
     * page therefore draw in the order they were written.</p>
     *
     * @param metadata the anchors the document resolved
     * @return the passes to run, deduplicated by identity, in document order
     */
    private static List<ResolvedLayoutPass> discover(ResolvedLayoutMetadata metadata) {
        List<ResolvedLayoutPass> found = new ArrayList<>();
        for (var anchor : metadata.anchors()) {
            if (anchor.id().groupKey() instanceof ResolvedLayoutPass pass && !containsSame(found, pass)) {
                found.add(pass);
            }
        }
        return found;
    }

    /** Identity, not equality: two owners configured alike are still two features. */
    private static boolean containsSame(List<ResolvedLayoutPass> passes, ResolvedLayoutPass pass) {
        for (ResolvedLayoutPass known : passes) {
            if (known == pass) {
                return true;
            }
        }
        return false;
    }

    private static void requireFinite(ResolvedLayoutPass pass, double value, String name) {
        if (!Double.isFinite(value)) {
            throw new IllegalStateException("Resolved-layout pass '" + pass.id()
                                            + "' contributed a fragment whose " + name + " is " + value
                                            + ". A non-finite coordinate reaches the content stream unchecked.");
        }
    }
}
