package com.demcha.compose.layout_core.components.components_builders;

import com.demcha.compose.layout_core.components.containers.abstract_builders.EmptyBox;
import com.demcha.compose.layout_core.components.content.shape.LinePath;
import com.demcha.compose.layout_core.components.content.shape.Stroke;
import com.demcha.compose.layout_core.components.renderable.Line;
import com.demcha.compose.layout_core.core.EntityManager;

public class LineBuilder extends EmptyBox<LineBuilder> {
    LineBuilder(EntityManager entityManager) {
        super(entityManager);
    }

    public LineBuilder line(Line line) {
        return addComponent(line);
    }

    public LineBuilder stroke(Stroke stroke) {
        return addComponent(stroke);
    }

    public LineBuilder path(LinePath linePath) {
        return addComponent(linePath);
    }

    public LineBuilder path(double startX, double startY, double endX, double endY) {
        return path(new LinePath(startX, startY, endX, endY));
    }

    public LineBuilder horizontal() {
        return path(LinePath.horizontal());
    }

    public LineBuilder vertical() {
        return path(LinePath.vertical());
    }

    public LineBuilder diagonalAscending() {
        return path(LinePath.diagonalAscending());
    }

    public LineBuilder diagonalDescending() {
        return path(LinePath.diagonalDescending());
    }

    @Override
    public void initialize() {
        entity.addComponent(new Line());
        entity.addComponent(LinePath.horizontal());
        entity.addComponent(new Stroke());
    }
}
