package com.demcha.system.interfaces.guides;

import com.demcha.components.content.shape.Stroke;
import com.demcha.components.core.Entity;
import com.demcha.components.layout.coordinator.RenderCoordinateContext;
import com.demcha.components.style.Margin;

import java.awt.*;
import java.util.Optional;
import java.util.function.Supplier;

public interface MarginRender<T extends AutoCloseable> extends GuideCoordinate<T> {

    default Optional<RenderCoordinateContext> margin(Entity entity) {
        Optional<RenderCoordinateContext> coordinateOpt =
                resolveCoordinateContext(entity, Margin.class, Margin::zero);
       return  coordinateOpt;
//
//        RenderCoordinateContext coordinateContext = null;
//
//
//        Optional<RenderCoordinateContext> coordinateOpt =
//                resolveCoordinateContext(entity, Margin.class, Margin::zero);
//
//        if (coordinateOpt.isEmpty()) {
//            return Optional.empty();
//        } else {
//            coordinateContext = coordinateOpt.get();
//        }
//
//
//        double x = coordinateContext.x() >= 0 ? coordinateContext.x() : 0;
//        double y;
//        double width;
//        double height;
//        y = (coordinateContext.y() >= 0) ? coordinateContext.y() : 0;
//        if (renderingSystem().canvas() != null) {
//            height = ((y + coordinateContext.height()) > renderingSystem().canvas().boundingTopLine())
//                    ? renderingSystem().canvas().boundingTopLine() - y
//                    : coordinateContext.height();
//        }
//        height = coordinateContext.height();
//        width = coordinateContext.width();
//        Color color = renderingSystem().guidLineSettings().MARGIN_COLOR();
//        Stroke stroke = renderingSystem().guidLineSettings().MARGIN_STROKE();
//        return Optional.of(new RenderCoordinateContext(x, y, width, height, coordinateContext.startPage(), coordinateContext.endPage(), stroke, color));

    }

    default <C extends Margin> Optional<RenderCoordinateContext> resolveCoordinateContext(Entity entity, Class<C> componentClass, Supplier<C> zero) {
        if (!renderingSystem().guidLineSettings().showOnlySetGuide()) {
            return Optional.empty();
        }

        C context = entity.getComponent(componentClass)
                .orElseGet(zero);

        return context.renderCoordinate(entity, renderingSystem());
    }


}
