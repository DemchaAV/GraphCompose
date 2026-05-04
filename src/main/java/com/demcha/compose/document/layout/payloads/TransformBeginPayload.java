package com.demcha.compose.document.layout.payloads;

import com.demcha.compose.document.style.DocumentTransform;

import java.util.Objects;

/**
 * Marker payload that opens a graphics-state transform region for the
 * outline + layers of a {@code ShapeContainerNode} that carries a
 * non-identity {@link DocumentTransform}. The fragment uses the
 * outline's placement rectangle so the renderer can compute the
 * rotation/scale centre as
 * {@code (x + width/2, y + height/2)}.
 *
 * <p>The PDF backend turns this into
 * {@code saveGraphicsState() + cm(matrix)}; a matching
 * {@link TransformEndPayload} fragment that arrives after every
 * other fragment of the same container then emits
 * {@code restoreGraphicsState()}. The transform brackets the entire
 * shape composite — outline, optional clip path, and every child
 * layer — so the whole unit rotates / scales together.</p>
 *
 * @param transform render-time transform to apply
 * @param ownerPath semantic path of the owning container — used by
 *                  architecture-guard tests to verify the begin/end
 *                  pair balance
 */
public record TransformBeginPayload(
        DocumentTransform transform,
        String ownerPath
) {
    /**
     * Validates the transform and normalizes the owner path.
     */
    public TransformBeginPayload {
        Objects.requireNonNull(transform, "transform");
        ownerPath = ownerPath == null ? "" : ownerPath;
    }
}
