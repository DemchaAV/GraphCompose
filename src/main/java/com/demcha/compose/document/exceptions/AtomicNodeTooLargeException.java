package com.demcha.compose.document.exceptions;

/**
 * Raised when an atomic node cannot fit even on an empty page.
 */
public final class AtomicNodeTooLargeException extends RuntimeException {
    /**
     * Creates an exception for an atomic semantic node that cannot fit on one page.
     *
     * @param message diagnostic message without user document content
     */
    public AtomicNodeTooLargeException(String message) {
        super(message);
    }

    /**
     * Creates an exception for a node whose required outer height exceeds a full
     * page's capacity, with a diagnostic message that names the offending
     * measurements but no user document content.
     *
     * @param path        the node's layout path
     * @param outerHeight the node's required outer (margin-box) height
     * @param pageHeight  the available full-page capacity
     * @return the exception, ready to throw
     * @since 2.0.0
     */
    public static AtomicNodeTooLargeException forNode(String path, double outerHeight, double pageHeight) {
        return new AtomicNodeTooLargeException(
                "Node '" + path + "' requires outer height " + outerHeight
                + " but page capacity is " + pageHeight + ". "
                + "Reduce the node height, split content into multiple atomic blocks, "
                + "or increase the page size. Differences under 0.5 pt are tolerated as "
                + "rounding noise (v1.6.2+).");
    }
}

