package com.demcha.compose.document.layout.payloads;

/**
 * Marker payload that closes a graphics-state transform region opened
 * by a matching {@link TransformBeginPayload}. Carries the
 * {@code ownerPath} for balance verification.
 *
 * @param ownerPath semantic path of the owning container; matches the
 *                  begin payload that opened the transform region
 */
public record TransformEndPayload(String ownerPath) {
    /**
     * Normalizes the owner path.
     */
    public TransformEndPayload {
        ownerPath = ownerPath == null ? "" : ownerPath;
    }
}
