package com.demcha.compose.document.exceptions;

/**
 * Raised when a backend or compiler capability is missing for a node or fragment.
 */
public final class UnsupportedNodeCapabilityException extends RuntimeException {
    /**
     * Creates an exception describing the missing capability.
     *
     * @param message diagnostic message
     */
    public UnsupportedNodeCapabilityException(String message) {
        super(message);
    }
}

