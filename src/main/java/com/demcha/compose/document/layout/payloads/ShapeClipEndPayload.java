package com.demcha.compose.document.layout.payloads;

/**
 * Marker payload that closes the graphics-state clip region opened by a
 * matching {@link ShapeClipBeginPayload}. The {@code ownerPath} is
 * carried so balance can be verified — every begin must have an end with
 * the same path, and they must arrive on the same page.
 *
 * @param ownerPath semantic path of the owning container; matches the
 *                  begin payload that opened the clip region
 */
public record ShapeClipEndPayload(String ownerPath) {
    /**
     * Normalizes the owner path.
     */
    public ShapeClipEndPayload {
        ownerPath = ownerPath == null ? "" : ownerPath;
    }
}
