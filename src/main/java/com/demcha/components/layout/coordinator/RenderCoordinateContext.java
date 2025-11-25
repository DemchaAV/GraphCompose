package com.demcha.components.layout.coordinator;

import com.demcha.components.content.shape.Stroke;

import java.awt.*;

public record RenderCoordinateContext(double x, double y, double width, double height, int startPage, int endPage, Stroke stroke, Color color){

}
