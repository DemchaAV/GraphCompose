package com.demcha.compose.document.layout;

/**
 * Page-agnostic fragment emitted by a node definition.
 *
 * @param path stable semantic path for the fragment owner
 * @param fragmentIndex index within the owner's emitted fragments
 * @param localX x offset relative to the owner placement
 * @param localY y offset relative to the owner placement
 * @param width fragment width
 * @param height fragment height
 * @param payload backend-specific fragment payload
 */
public record LayoutFragment(
        String path,
        int fragmentIndex,
        double localX,
        double localY,
        double width,
        double height,
        Object payload
) {
}


