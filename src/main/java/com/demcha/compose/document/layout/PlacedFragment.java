package com.demcha.compose.document.layout;

import com.demcha.compose.engine.components.style.Margin;
import com.demcha.compose.engine.components.style.Padding;

/**
 * Concrete fragment resolved to a page and absolute page coordinates.
 */
public record PlacedFragment(
        String path,
        int fragmentIndex,
        int pageIndex,
        double x,
        double y,
        double width,
        double height,
        Margin margin,
        Padding padding,
        Object payload
) {
    public static PlacedFragment from(LayoutFragment fragment, FragmentPlacement placement) {
        return new PlacedFragment(
                fragment.path(),
                fragment.fragmentIndex(),
                placement.pageIndex(),
                placement.x() + fragment.localX(),
                placement.y() + fragment.localY(),
                fragment.width(),
                fragment.height(),
                placement.margin(),
                placement.padding(),
                fragment.payload());
    }
}


