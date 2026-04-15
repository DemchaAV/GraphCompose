package com.demcha.compose.document.exceptions;

/**
 * Raised when a backend or compiler capability is missing for a node or fragment.
 */
public final class UnsupportedNodeCapabilityException extends RuntimeException {
    public UnsupportedNodeCapabilityException(String message) {
        super(message);
    }
}

