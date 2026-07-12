package com.demcha.compose.document.layout.payloads;

import com.demcha.compose.document.node.DocumentBookmarkOptions;
import com.demcha.compose.document.node.DocumentLinkTarget;

import java.util.Objects;

/**
 * Non-visual marker fragment payload that declares a PDF outline (bookmark) entry
 * at a container's resolved top-left.
 *
 * <p>A bookmarked {@code Section} / {@code Container} / page flow emits one of
 * these at its top on its start page. Because it implements
 * {@link PdfSemanticFragmentPayload} and returns non-null {@link #bookmarkOptions()},
 * the PDF backend registers it as an outline entry through the generic semantic
 * branch — no deferred resolution, unlike an anchor. The marker draws nothing and
 * carries no link.</p>
 *
 * @param bookmarkOptions the outline entry (title, level); never {@code null}
 * @author Artem Demchyshyn
 * @since 1.9.0
 */
public record BookmarkMarkerPayload(DocumentBookmarkOptions bookmarkOptions)
        implements PdfSemanticFragmentPayload {

    /**
     * Validates that the marker carries a bookmark (its sole purpose).
     */
    public BookmarkMarkerPayload {
        Objects.requireNonNull(bookmarkOptions, "bookmarkOptions");
    }

    @Override
    public DocumentLinkTarget linkTarget() {
        return null;
    }
}
