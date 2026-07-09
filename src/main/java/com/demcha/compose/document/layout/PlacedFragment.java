package com.demcha.compose.document.layout;

import com.demcha.compose.engine.components.style.Margin;
import com.demcha.compose.engine.components.style.Padding;

/**
 * Concrete fragment resolved to a page and absolute page coordinates.
 *
 * @param path          stable semantic path for the fragment owner
 * @param fragmentIndex index within the owner's emitted fragments
 * @param pageIndex     page that contains this fragment
 * @param x             absolute page x coordinate
 * @param y             absolute page y coordinate
 * @param width         fragment width
 * @param height        fragment height
 * @param margin        owner margin used for diagnostics
 * @param padding       owner padding used for diagnostics
 * @param payload       backend-specific fragment payload
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
    /**
     * Places a local fragment into absolute page coordinates.
     *
     * @param fragment  fragment emitted by a node definition
     * @param placement owner placement metadata
     * @return placed fragment
     */
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

    /**
     * Places a synthetic fragment with no owner node — e.g. a page-background fill
     * spliced in after compilation — at absolute page coordinates. The owner
     * margin/padding (diagnostic only) are zero, letting a canonical caller build
     * the fragment without referencing an engine style type.
     *
     * @param path          stable semantic path for the fragment
     * @param fragmentIndex index within the owner's emitted fragments
     * @param pageIndex     page that contains this fragment
     * @param x             absolute page x coordinate
     * @param y             absolute page y coordinate
     * @param width         fragment width
     * @param height        fragment height
     * @param payload       backend-specific fragment payload
     * @return placed fragment with zero owner insets
     * @since 2.0.0
     */
    public static PlacedFragment withZeroInsets(String path, int fragmentIndex, int pageIndex,
            double x, double y, double width, double height, Object payload) {
        return new PlacedFragment(path, fragmentIndex, pageIndex, x, y, width, height,
                Margin.zero(), Padding.zero(), payload);
    }
}


