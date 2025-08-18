package com.demcha.components.geometry;

import com.demcha.components.core.Component;

public record BoundingBox(double x, double y, double width, double height) implements Component {}
