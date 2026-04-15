package com.demcha.compose.v2;

import com.demcha.compose.layout_core.components.style.Margin;
import com.demcha.compose.layout_core.components.style.Padding;

/**
 * Resolved placement context for fragment emission.
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
