package com.demcha.components.containers.abstract_builders;

import com.demcha.components.content.shape.Stroke;
import com.demcha.components.core.Entity;
import com.demcha.components.geometry.ContentSize;
import com.demcha.components.geometry.InnerBoxSize;
import com.demcha.components.geometry.OuterBoxSize;
import com.demcha.components.layout.coordinator.ComputedPosition;
import com.demcha.components.layout.coordinator.PaddingCoordinate;
import com.demcha.components.layout.coordinator.RenderingPosition;
import lombok.NonNull;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;

import java.awt.*;
import java.io.IOException;
import java.util.EnumSet;

public interface GuidesRenderer {

    //region  Configuration Constants
    //================================================================================
    // Here you can quickly configure the appearance of all guides.
    //================================================================================

    /**
     * The opacity for all guide elements. Value between 0.0f (transparent) and 1.0f (opaque).
     */
    float GUIDES_OPACITY = 0.8f;

    // --- Margin Guide ---
    Color MARGIN_COLOR =  new Color(0, 110, 255);
    Stroke MARGIN_STROKE = new Stroke(0.5);

    // --- Padding Guide ---
    Color PADDING_COLOR = new Color(255, 140, 0);
    Stroke PADDING_STROKE = new Stroke(0.5);

    // --- Content Box Guide ---
    Color BOX_COLOR = new Color(150, 150, 150); // Using a slightly lighter gray
    Stroke BOX_STROKE = new Stroke(1.0);

    //endregion

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

        double x = pos.x();
        double y = pos.y();
        double width = outer.width();
        double height = outer.height();

        //  Using constants for configuration
        renderMarkers(cs, x, y, width, height, MARGIN_COLOR);
        return renderRectangle(MARGIN_STROKE, cs, x, y, width, height, MARGIN_COLOR, true);
    }

    default boolean renderPadding(Entity e, PDPageContentStream cs) throws IOException {
        var pad = PaddingCoordinate.from(e);
        var inner = InnerBoxSize.from(e).orElseThrow();

        double x = pad.x();
        double y = pad.y();
        double width = inner.innerW();
        double height = inner.innerH();

        //  Using constants for configuration
        renderMarkers(cs, x, y, width, height, PADDING_COLOR);
        return renderRectangle(PADDING_STROKE, cs, x, y, width, height, PADDING_COLOR, true);
    }

    private boolean renderRectangle(Stroke stroke,
                                    PDPageContentStream cs,
                                    double x, double y,
                                    double w, double h,
                                    @NonNull Color color, boolean lineDash) throws IOException {

        if (w <= 0 || h <= 0) return false;

        cs.saveGraphicsState();
        try {
            PDExtendedGraphicsState gState = new PDExtendedGraphicsState();
            //  Using constant for opacity
            gState.setStrokingAlphaConstant(GUIDES_OPACITY);
            cs.setGraphicsStateParameters(gState);

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
        fillCircle(cs, cx, cy, radius, color);
        fillCircle(cs, cx, cy + (float) h, radius, color);
        fillCircle(cs, cx + (float) w, cy, radius, color);
        fillCircle(cs, cx + (float) w, cy + (float) h, radius, color);
    }

    default boolean boxRender(Entity e, PDPageContentStream cs) throws IOException {
        var boxSize = e.getComponent(ContentSize.class).orElseThrow();
        var rp = RenderingPosition.from(e).orElseThrow();

        double x = rp.x();
        double y = rp.y();
        double w = boxSize.width();
        double h = boxSize.height();

        //  Using constants for configuration
        return renderRectangle(BOX_STROKE, cs, x, y, w, h, BOX_COLOR, false);
    }

    default void fillCircle(PDPageContentStream cs, float cx, float cy, float r, Color fill) throws IOException {
        if (r <= 0) return;

        final float k = 0.552284749831f;

        cs.saveGraphicsState();
        try {
            PDExtendedGraphicsState gState = new PDExtendedGraphicsState();
            //  Using constant for opacity
            gState.setNonStrokingAlphaConstant(GUIDES_OPACITY);
            cs.setGraphicsStateParameters(gState);

            cs.setNonStrokingColor(fill);
            cs.moveTo(cx + r, cy);
            cs.curveTo(cx + r, cy + k * r, cx + k * r, cy + r, cx, cy + r);
            cs.curveTo(cx - k * r, cy + r, cx - r, cy + k * r, cx - r, cy);
            cs.curveTo(cx - r, cy - k * r, cx - k * r, cy - r, cx, cy - r);
            cs.curveTo(cx + k * r, cy - r, cx + r, cy - k * r, cx + r, cy);
            cs.closePath();
            cs.fill();
        } finally {
            cs.restoreGraphicsState();
        }
    }

    enum Guide {MARGIN, PADDING, BOX}
}
