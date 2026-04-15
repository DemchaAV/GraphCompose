package com.demcha.compose.v2.exceptions;

/**
 * Raised when a backend or compiler capability is missing for a node or fragment.
 */
public final class UnsupportedNodeCapabilityException extends RuntimeException {
    public UnsupportedNodeCapabilityException(String message) {
        super(message);
    }
}
