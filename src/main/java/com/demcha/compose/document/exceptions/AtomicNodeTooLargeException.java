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
}

