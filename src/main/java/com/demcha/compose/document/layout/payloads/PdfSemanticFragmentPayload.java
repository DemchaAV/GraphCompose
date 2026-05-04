package com.demcha.compose.document.layout.payloads;

import com.demcha.compose.document.node.DocumentBookmarkOptions;
import com.demcha.compose.document.node.DocumentLinkOptions;

/**
 * Marker interface for fragment payloads that carry canonical PDF link or
 * bookmark metadata through the resolved semantic graph.
 */
public interface PdfSemanticFragmentPayload {
    /**
     * Returns link metadata for the resolved fragment, or {@code null} when
     * no PDF annotation should be emitted.
     *
     * @return fragment-level link options, or {@code null}
     */
    DocumentLinkOptions linkOptions();

    /**
     * Returns bookmark metadata for the resolved fragment, or {@code null}
     * when no PDF outline entry should be emitted.
     *
     * @return fragment-level bookmark options, or {@code null}
     */
    DocumentBookmarkOptions bookmarkOptions();
}
