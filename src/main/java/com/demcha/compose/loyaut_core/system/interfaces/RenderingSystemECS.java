package com.demcha.compose.loyaut_core.system.interfaces;

import com.demcha.compose.loyaut_core.core.Canvas;
import com.demcha.compose.loyaut_core.components.content.shape.Side;
import com.demcha.compose.loyaut_core.components.layout.coordinator.RenderCoordinateContext;
import com.demcha.compose.loyaut_core.system.GuidLineSettings;
import com.demcha.compose.loyaut_core.system.interfaces.guides.GuidesRenderer;

import java.awt.*;
import java.io.IOException;
import java.util.Set;

/**
 * In current Class you can render a simple Figure like line rectangle cercl
 */
public interface RenderingSystemECS<S extends AutoCloseable> extends SystemECS {
    <T extends Canvas> T canvas();

    GuidesRenderer<S> guidesRenderer();
    Class<? extends Font<?>> fontClazz();

    <T extends RenderStream<S>> T stream();

    boolean renderBorder(S stream, RenderCoordinateContext context,
                         boolean lineDash,
                         Set<Side> sides) throws IOException;

    GuidLineSettings guidLineSettings();

    boolean renderRectangle(S stream, RenderCoordinateContext context, boolean lineDash) throws IOException;

    void fillCircle(S stream, float cx, float cy, float r, Color fill) throws IOException;

}

