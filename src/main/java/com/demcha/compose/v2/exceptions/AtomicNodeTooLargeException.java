package com.demcha.compose.v2.exceptions;

/**
 * Raised when an atomic node cannot fit even on an empty page.
 */
public final class AtomicNodeTooLargeException extends RuntimeException {
    public AtomicNodeTooLargeException(String message) {
        super(message);
    }
}
