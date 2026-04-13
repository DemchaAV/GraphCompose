package com.demcha.compose.layout_core.components.renderable;

import com.demcha.compose.layout_core.components.geometry.Expendable;
import com.demcha.compose.layout_core.system.interfaces.Render;
import com.demcha.compose.layout_core.system.interfaces.guides.GuidesRenderer;
import com.demcha.compose.layout_core.system.utils.page_breaker.Breakable;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.EnumSet;

/**
 * Represents a basic container render marker.
 */
@Slf4j
@Getter
public class Container implements Render, Expendable, Breakable {
    public static final EnumSet<GuidesRenderer.Guide> DEFAULT_GUIDES =
            EnumSet.of(GuidesRenderer.Guide.MARGIN, GuidesRenderer.Guide.PADDING, GuidesRenderer.Guide.BOX);
}
