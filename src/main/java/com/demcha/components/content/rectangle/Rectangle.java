package com.demcha.components.content.rectangle;

import com.demcha.components.content.Stroke;
import com.demcha.components.core.Component;
import com.demcha.components.core.Entity;
import com.demcha.components.geometry.ContentSize;
import com.demcha.components.layout.GuidesRenderer;
import com.demcha.components.layout.coordinator.RenderingPosition;
import com.demcha.components.style.ColorComponent;
import com.demcha.system.ContentSizeNotFoundException;
import com.demcha.system.PdfRender;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDPageContentStream;

import java.awt.*;
import java.io.IOException;
import java.util.EnumSet;

@Slf4j
public record Rectangle(Radius radius) implements Component, PdfRender, GuidesRenderer {
    private static final EnumSet<Guide> DEFAULT_GUIDES =
            EnumSet.of(Guide.MARGIN, Guide.PADDING);

    public Rectangle() {
        this(null);
    }

    @Override
    public boolean render(Entity e, PDPageContentStream cs, boolean withGuides) throws IOException {
        // По умолчанию рисуем ТОЛЬКО сам объект (без направляющих)
        boolean drawn = renderObject(e, cs); // ← рисуем сам прямоугольник
        if (withGuides) {
            renderGuides(e, cs, DEFAULT_GUIDES);

        }
        return drawn;
    }


    private boolean renderObject(Entity e, PDPageContentStream cs) throws IOException {
        // Обычно этот метод вызывается ТОЛЬКО если у сущности есть Rectangle,
        // проверка ниже избыточна, но пусть останется:
        if (!e.has(Rectangle.class)) {
            log.debug("No Rectangle on {}", e);
            return false;
        }

        var size = e.getComponent(ContentSize.class)
                .orElseThrow(ContentSizeNotFoundException::new);

        var rp = RenderingPosition.from(e); // x/y с учетом margin/anchor и т.п.

        // Цвет: дефолт если компонента нет
        Color color = e.getComponent(ColorComponent.class)
                .map(ColorComponent::color)
                .orElse(Color.BLACK);

        double x = rp.x();
        double y = rp.y();
        double w = size.width();
        double h = size.height();
        Stroke stroke = e.getComponent(Stroke.class).orElse(null);

        // Для заливки радиусного прямоугольника используем non-stroking color,
        // для контура — stroking color.
        return renderRectangle(stroke, cs, radius, x, y, w, h, color);
    }

    private boolean renderRectangle(Stroke stroke,
                                    PDPageContentStream cs,
                                    Radius r,
                                    double x, double y,
                                    double w, double h,
                                    @NonNull Color color) throws IOException {

        if (w <= 0 || h <= 0) {
            log.debug("Skip rectangle: non-positive size ({}, {})", w, h);
            return false;
        }

        // Ограничим радиус для корректной геометрии
        float radius = (r == null) ? 0f : (float) Math.min(r.radius(), Math.min(w, h) / 2.0);

        log.debug("Rendering rectangle at ({}, {}) size=({}, {}) radius={}", x, y, w, h, radius);

        cs.saveGraphicsState();
        try {
            if (radius > 0f) {
                drawRoundedRectangle(cs, (float) x, (float) y, (float) w, (float) h, radius);
                cs.setNonStrokingColor(color);
                cs.fill(); // Можно добавить stroke() если нужен контур: cs.stroke();
            } else {
                if (stroke != null) {
                    cs.setLineWidth((float) stroke.width());
                }
                cs.setStrokingColor(color);
                cs.addRect((float) x, (float) y, (float) w, (float) h);
                cs.stroke();
            }
        } finally {
            cs.restoreGraphicsState();
        }
        return true;
    }

    private void drawRoundedRectangle(PDPageContentStream contentStream,
                                      float x, float y, float width, float height, float radius) throws IOException {
        final float c = 0.552284749831f;
        contentStream.moveTo(x + radius, y + height);
        contentStream.lineTo(x + width - radius, y + height);
        contentStream.curveTo(x + width - radius + radius * c, y + height,
                x + width, y + height - radius + radius * c,
                x + width, y + height - radius);
        contentStream.lineTo(x + width, y + radius);
        contentStream.curveTo(x + width, y + radius - radius * c,
                x + width - radius + radius * c, y,
                x + width - radius, y);
        contentStream.lineTo(x + radius, y);
        contentStream.curveTo(x + radius - radius * c, y,
                x, y + radius - radius * c,
                x, y + radius);
        contentStream.lineTo(x, y + height - radius);
        contentStream.curveTo(x, y + height - radius + radius * c,
                x + radius - radius * c, y + height,
                x + radius, y + height);
        contentStream.closePath();
    }

}

