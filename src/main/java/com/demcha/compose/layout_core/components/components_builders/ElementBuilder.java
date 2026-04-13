package com.demcha.compose.layout_core.components.components_builders;

import com.demcha.compose.layout_core.components.containers.abstract_builders.EmptyBox;
import com.demcha.compose.layout_core.components.core.Component;
import com.demcha.compose.layout_core.components.geometry.ContentSize;
import com.demcha.compose.layout_core.components.layout.coordinator.Position;
import com.demcha.compose.layout_core.components.renderable.Element;
import com.demcha.compose.layout_core.core.Canvas;
import com.demcha.compose.layout_core.core.EntityManager;

import java.util.Optional;


public class ElementBuilder extends EmptyBox<ElementBuilder>  {
    ElementBuilder(EntityManager entityManager) {
        super(entityManager);
    }

    @Override
    public void initialize() {
        entity().addComponent(new Element());
    }


    public ElementBuilder fillPageSize(Canvas canvas) {
        return fillPageSize(canvas.width(), canvas.height());
    }

    public ElementBuilder fillPageSize(double width, double height) {
        float w = (float) width;
        float h = (float) height;
        // Store logical (CSS-like) top-left coordinates
        addComponent(new ContentSize(w, h));
        addComponent(new Position(0, 0));   // top-left origin for your layout system
        return this;
    }

    public ElementBuilder fillHorizontal(Canvas canvas, double height) {
        return fillHorizontal(canvas.width(), height);
    }

    public ElementBuilder fillHorizontal(double width, double height) {
        addComponent(new ContentSize(width, height));
        return this;
    }

    public <T extends Component> Optional<T> getComponent(Class<T> clazz) {
        return entity.getComponent(clazz);
    }


}
