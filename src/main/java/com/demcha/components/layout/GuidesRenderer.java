package com.demcha.components.layout;

import com.demcha.components.content.Stroke;
import com.demcha.components.core.Entity;
import com.demcha.components.geometry.ContentSize;
import com.demcha.components.geometry.InnerBoxSize;
import com.demcha.components.geometry.OuterBoxSize;
import com.demcha.components.layout.coordinator.ComputedPosition;
import com.demcha.components.layout.coordinator.PaddingCoordinate;
import com.demcha.components.style.ColorComponent;
import lombok.NonNull;
import org.apache.pdfbox.pdmodel.PDPageContentStream;

import java.awt.*;
import java.io.IOException;
import java.util.EnumSet;

public interface GuidesRenderer {

    default boolean renderGuides(Entity e, PDPageContentStream cs, EnumSet<Guide> guides) throws IOException {
        boolean any = false;
        if (guides.contains(Guide.MARGIN)) any |= renderMargin(e, cs);
        if (guides.contains(Guide.PADDING)) any |= renderPadding(e, cs);
        if (guides.contains(Guide.BOX)) any |= boxRender(e, cs);
        return any;
    }

    default boolean renderMargin(Entity e, PDPageContentStream cs) throws IOException {
        var pos = e.getComponent(ComputedPosition.class).orElseThrow();
        var outer = OuterBoxSize.from(e).orElseThrow();

        // лёгкий пунктир и тонкая линия, чтобы гайды не “забивали” контент
        var stroke = new Stroke(0.5);

        double x = pos.x();
        double y = pos.y();
        double width = outer.width();
        double height = outer.height();

        Color color = ColorComponent.ROYAL_BLUE;
        renderMarkers(cs, x, y, width, height, color);
        return renderRectangle(stroke, cs,
                x, y, width, height,
                color, true);
    }

    default boolean renderPadding(Entity e, PDPageContentStream cs) throws IOException {
        var pad = PaddingCoordinate.from(e);
        var inner = InnerBoxSize.from(e).orElseThrow();

        var stroke = new Stroke(0.5);
        double x = pad.x();
        double y = pad.y();
        double width = inner.innerW();
        double height = inner.innerH();

        Color color = ColorComponent.DARK_GREEN;
        renderMarkers(cs, x, y, width, height, color);

        return renderRectangle(stroke, cs,
                x, y, width, height,
                color, true);
    }

    private boolean renderRectangle(Stroke stroke,
                                    PDPageContentStream cs,
                                    double x, double y,
                                    double w, double h,
                                    @NonNull Color color, boolean lineDash) throws IOException {

        if (w <= 0 || h <= 0) return false;


        cs.saveGraphicsState();
        try {
            // пунктир для наглядности
            if (lineDash) {
                cs.setLineDashPattern(new float[]{3f}, 0);
            }
            if (stroke != null) cs.setLineWidth((float) stroke.width());
            cs.setStrokingColor(color);


            cs.addRect((float) x, (float) y, (float) w, (float) h);

            cs.stroke();
        } finally {
            cs.restoreGraphicsState();
        }
        return true;
    }

    default void renderMarkers(PDPageContentStream cs, double x, double y, double w, double h, Color color) throws IOException {
        final float radius = 3.5f;
        float cx = (float) x;
        float cy = (float) y;
        //bottom left
        fillCircle(cs, cx, cy, radius, color);
        //bottom right
        fillCircle(cs, cx, cy + (float) h, radius, color);
        //top Left
        fillCircle(cs, cx + (float) w, cy, radius, color);
        //top right
        fillCircle(cs, cx + (float) w, cy + (float) h, radius, color);
    }

    default boolean boxRender(Entity e, PDPageContentStream cs) throws IOException {
        var pos = e.getComponent(ComputedPosition.class).orElseThrow();
        var boxSize = e.getComponent(ContentSize.class).orElseThrow();

        // лёгкий пунктир и тонкая линия, чтобы гайды не “забивали” контент
        var stroke = new Stroke(1);

        double x = pos.x();
        double y = pos.y();
        double width = boxSize.width();
        double height = boxSize.height();

        Color color = ColorComponent.GRAY;
        renderMarkers(cs, x, y, width, height, color);

        return renderRectangle(stroke, cs,
                x, y, width, height,
                color, false);

    }

    default void fillCircle(PDPageContentStream cs, float cx, float cy, float r, Color fill) throws IOException {
        if (r <= 0) return;

        final float k = 0.552284749831f; // коэффициент аппроксимации окружности 4 дугами Безье

        cs.saveGraphicsState();
        try {
            cs.setNonStrokingColor(fill);   // цвет заливки

            // путь круга
            cs.moveTo(cx + r, cy);
            cs.curveTo(cx + r, cy + k * r, cx + k * r, cy + r, cx, cy + r);
            cs.curveTo(cx - k * r, cy + r, cx - r, cy + k * r, cx - r, cy);
            cs.curveTo(cx - r, cy - k * r, cx - k * r, cy - r, cx, cy - r);
            cs.curveTo(cx + k * r, cy - r, cx + r, cy - k * r, cx + r, cy);
            cs.closePath();

            cs.fill(); // заливка
        } finally {
            cs.restoreGraphicsState();
        }
    }

    enum Guide {MARGIN, PADDING, BOX}


}
