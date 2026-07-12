package com.demcha.compose.document.snapshot;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * Backend-neutral index resolving every declared {@code anchor(...)} to its
 * final page in a single pass over the laid-out document — the read-side
 * primitive behind cross-references ("see page N") and clickable tables of
 * contents.
 *
 * <p>Computed from the resolved layout graph, not from rendered PDF bytes, so it
 * is independent of the output backend. A duplicate anchor name resolves to its
 * last registration — the same destination a {@code linkTo(anchor)} jumps to.
 * (Edge case: an anchor on a node nested inside a <em>repeated table header</em>
 * resolves to the header's last repetition, since the marker re-emits per
 * continuation — consistent with where the link jumps.) Obtained via
 * {@code DocumentSession.pageIndex()} (cached per layout revision). Instances are
 * immutable.</p>
 *
 * @author Artem Demchyshyn
 * @see PageReference
 * @since 1.9.0
 */
public final class PageIndex {

    private final Map<String, PageReference> byAnchor;
    private final int totalPages;

    /**
     * Creates a page index over the given resolved references, keyed by
     * {@link PageReference#anchor()} in layout (document-flow) order; a repeated
     * anchor name keeps its last reference.
     *
     * @param references resolved anchor references in layout order
     * @param totalPages total page count of the laid-out document
     */
    public PageIndex(Collection<PageReference> references, int totalPages) {
        Map<String, PageReference> map = new LinkedHashMap<>();
        if (references != null) {
            for (PageReference reference : references) {
                if (reference != null) {
                    map.put(reference.anchor(), reference);
                }
            }
        }
        this.byAnchor = Collections.unmodifiableMap(map);
        this.totalPages = totalPages;
    }

    /**
     * Returns the resolved reference for an anchor.
     *
     * @param anchor anchor name; a {@code null} or undeclared name yields empty
     * @return the reference, or empty if the anchor is not declared
     */
    public Optional<PageReference> forAnchor(String anchor) {
        return anchor == null ? Optional.empty() : Optional.ofNullable(byAnchor.get(anchor));
    }

    /**
     * Returns the zero-based page of an anchor.
     *
     * @param anchor anchor name; a {@code null} or undeclared name yields empty
     * @return the page, or empty if the anchor is not declared
     */
    public OptionalInt pageOf(String anchor) {
        PageReference reference = anchor == null ? null : byAnchor.get(anchor);
        return reference == null ? OptionalInt.empty() : OptionalInt.of(reference.page());
    }

    /**
     * Returns the 1-based printable page number of an anchor.
     *
     * @param anchor anchor name; a {@code null} or undeclared name yields empty
     * @return the page number, or empty if the anchor is not declared
     */
    public OptionalInt pageNumberOf(String anchor) {
        PageReference reference = anchor == null ? null : byAnchor.get(anchor);
        return reference == null ? OptionalInt.empty() : OptionalInt.of(reference.pageNumber());
    }

    /**
     * Returns every resolved anchor in layout (document-flow) order.
     *
     * @return an unmodifiable anchor-to-reference map
     */
    public Map<String, PageReference> all() {
        return byAnchor;
    }

    /**
     * Returns the total page count of the laid-out document.
     *
     * @return total pages
     */
    public int totalPages() {
        return totalPages;
    }
}
