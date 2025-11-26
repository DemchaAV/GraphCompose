package com.demcha.system.interfaces;

import com.demcha.components.components_builders.Canvas;
import com.demcha.components.content.shape.Side;
import com.demcha.components.layout.coordinator.RenderCoordinateContext;
import com.demcha.system.GuidLineSettings;
import com.demcha.system.interfaces.guides.GuidesRenderer;

import java.awt.*;
import java.io.IOException;
import java.util.Set;

/**
 * In current Class you can render a simple Figure like line rectangle cercl
 */
public interface RenderingSystemECS<S extends AutoCloseable> extends SystemECS {
    <T extends Canvas> T canvas();

    GuidesRenderer<S> guidesRenderer();

    <T extends RenderStream<S>> T stream();

    boolean renderBorder(S stream, RenderCoordinateContext context,
                         boolean lineDash,
                         Set<Side> sides) throws IOException;

    GuidLineSettings guidLineSettings();

    boolean renderRectangle(S stream, RenderCoordinateContext context, boolean lineDash) throws IOException;

    void fillCircle(S stream, float cx, float cy, float r, Color fill) throws IOException;

}
