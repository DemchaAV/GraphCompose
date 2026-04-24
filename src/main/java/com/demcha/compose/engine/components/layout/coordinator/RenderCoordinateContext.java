package com.demcha.compose.engine.components.layout.coordinator;

import com.demcha.compose.engine.components.content.shape.Stroke;
import com.demcha.compose.engine.components.core.Entity;

import java.awt.*;

public record RenderCoordinateContext(double x, double y, double width, double height, int startPage, int endPage, Stroke stroke, Color color){
public static  RenderCoordinateContext createBox    (Entity entity, Stroke stroke, Color color) {
    Placement placement = entity.getComponent(Placement.class).orElseThrow();
    return new RenderCoordinateContext(placement.x(), placement.y(), placement.width(), placement.height(), placement.startPage(), placement.endPage(), stroke, color);
}
}
