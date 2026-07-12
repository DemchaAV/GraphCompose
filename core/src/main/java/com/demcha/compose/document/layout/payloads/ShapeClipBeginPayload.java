package com.demcha.compose.document.layout.payloads;

import com.demcha.compose.document.style.ClipPolicy;
import com.demcha.compose.document.style.ShapeOutline;

import java.util.Objects;

/**
 * Marker payload that opens a graphics-state clip region for the layers
 * inside a {@code ShapeContainerNode}. The fragment carries the absolute
 * outline rectangle (its {@code x}/{@code y}/{@code width}/{@code height}
 * already match the outline once placed) plus the policy that decided
 * whether the clip applies to the bounding box, the outline path, or is
 * skipped.
 *
 * <p>The PDF backend uses this payload to emit
 * {@code saveGraphicsState() + add path + clip()}; a matching
 * {@link ShapeClipEndPayload} fragment that arrives after every layer
 * fragment of the same container then emits {@code restoreGraphicsState()}.
 * Backends that cannot express path clipping (e.g. DOCX/POI) skip both
 * fragments and emit a capability warning instead.</p>
 *
 * @param outline   outline geometry to clip against
 * @param policy    clip policy chosen for the container; renderers may
 *                  degrade {@link ClipPolicy#CLIP_PATH} to
 *                  {@link ClipPolicy#CLIP_BOUNDS} when path clipping is
 *                  unavailable
 * @param ownerPath semantic path of the owning container — used by
 *                  architecture-guard tests to verify the begin/end
 *                  pair balance
 */
public record ShapeClipBeginPayload(
        ShapeOutline outline,
        ClipPolicy policy,
        String ownerPath
) {
    /**
     * Validates the outline / policy and normalizes the owner path.
     */
    public ShapeClipBeginPayload {
        Objects.requireNonNull(outline, "outline");
        Objects.requireNonNull(policy, "policy");
        ownerPath = ownerPath == null ? "" : ownerPath;
    }
}
