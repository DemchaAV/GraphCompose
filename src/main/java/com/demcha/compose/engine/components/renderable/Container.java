package com.demcha.compose.engine.components.renderable;

import com.demcha.compose.engine.components.geometry.Expendable;
import com.demcha.compose.engine.render.Render;
import com.demcha.compose.engine.render.guides.GuidesRenderer;
import com.demcha.compose.engine.pagination.Breakable;
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
