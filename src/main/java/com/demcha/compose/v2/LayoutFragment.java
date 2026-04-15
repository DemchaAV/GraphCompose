package com.demcha.compose.v2;

/**
 * Page-agnostic fragment emitted by a node definition.
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
