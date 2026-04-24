package com.demcha.compose.document.layout;

import com.demcha.compose.engine.components.style.Margin;
import com.demcha.compose.engine.components.style.Padding;

/**
 * Resolved placement context for fragment emission.
 *
 * @param path stable semantic path for the placed node
 * @param parentPath parent semantic path, or {@code null} for a root
 * @param childIndex index within the parent child list
 * @param depth depth in the semantic tree
 * @param pageIndex page that receives this fragment
 * @param x absolute page x coordinate
 * @param y absolute page y coordinate
 * @param width resolved fragment width
 * @param height resolved fragment height
 * @param startPage first page occupied by the owning node
 * @param endPage last page occupied by the owning node
 * @param margin resolved outer margin
 * @param padding resolved inner padding
 */
public record FragmentPlacement(
        String path,
        String parentPath,
        int childIndex,
        int depth,
        int pageIndex,
        double x,
        double y,
        double width,
        double height,
        int startPage,
        int endPage,
        Margin margin,
        Padding padding
) {
}


