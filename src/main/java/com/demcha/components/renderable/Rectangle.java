package com.demcha.components.renderable;

import com.demcha.components.containers.abstract_builders.GuidesRenderer;
import com.demcha.components.content.shape.CornerRadius;
import com.demcha.components.content.shape.FillColor;
import com.demcha.components.content.shape.Stroke;
import com.demcha.components.core.Entity;
import com.demcha.components.geometry.ContentSize;
import com.demcha.components.layout.coordinator.Placement;
import com.demcha.exeptions.ContentSizeNotFoundException;
import com.demcha.system.Expendable;
import com.demcha.system.RenderingSystemECS;
import com.demcha.system.pdf_systems.PdfRender;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPageContentStream;

import java.io.IOException;
import java.util.EnumSet;

@Slf4j
@EqualsAndHashCode
public class Rectangle implements PdfRender, GuidesRenderer, Expendable {
    private static final EnumSet<Guide> DEFAULT_GUIDES =
            EnumSet.of(Guide.MARGIN, Guide.PADDING);


    @Override
    public boolean pdfRender(Entity e, PDDocument doc, RenderingSystemECS renderingSystemECS, boolean guideLines) throws IOException {
        // draw an object first
        boolean drawn;
        try (PDPageContentStream cs = openContentStream(e,doc, renderingSystemECS)) {
            drawn = pdfRenderObject(e, cs);
            // if was specified, draw guides
            if (guideLines) {
                renderGuides(e, cs, DEFAULT_GUIDES);

            }
        }
        return drawn;
    }


    private boolean pdfRenderObject(Entity e,PDPageContentStream cs) throws IOException {
        if (!e.hasAssignable(Rectangle.class)) {
            log.debug("No Rectangle on {}", e);
            return false;
        }

        var size = e.getComponent(ContentSize.class)
                .orElseThrow(ContentSizeNotFoundException::new);

        var rpOpt = e.getComponent(Placement.class); // x/y с учетом margin/anchor и т.п.

        // Color Default if not specified
        FillColor fillColor = e.getComponent(FillColor.class).orElse(FillColor.defaultColor());

        var rp = rpOpt.orElseThrow();
        double x = rp.x();
        double y = rp.y();
        double w = size.width();
        double h = size.height();
        //Should be null  if not specified
        Stroke stroke = e.getComponent(Stroke.class).orElse(null);
        //Should be null  if not specified
        CornerRadius radius = e.getComponent(CornerRadius.class).orElse(null);
        return pdfRenderRectangle(cs, x, y, stroke, fillColor, radius, w, h);
    }

    private boolean pdfRenderRectangle(PDPageContentStream cs,
                                       double x, double y,
                                       Stroke stroke,          // may be null
                                       FillColor fillColor,    // may be null
                                       CornerRadius r,               // may be null
                                       double w, double h) throws IOException {

        if (w <= 0 || h <= 0) {
            log.debug("Skip rectangle: non-positive size ({}, {})", w, h);
            return false;
        }

        final float fx = (float) x;
        final float fy = (float) y;
        final float fw = (float) w;
        final float fh = (float) h;

        // Clamp radius
        final float radius = (r == null) ? 0f : (float) Math.min(r.radius(), Math.min(w, h) / 2.0);

        // Determine paint ops (null-safe)
        final boolean hasFill = (fillColor != null && fillColor.color() != null);
        final boolean hasStroke = (stroke != null && stroke.strokeColor() != null && stroke.width() > 0);

        if (!hasFill && !hasStroke) {
            log.debug("Skip rectangle: neither fill nor stroke specified.");
            return false;
        }

        log.debug("Rendering rectangle at ({}, {}) size=({}, {}) radius={} fill={} stroke={}",
                x, y, w, h, radius, hasFill, hasStroke);

        cs.saveGraphicsState();
        try {
            // Configure stroke if needed
            if (hasStroke) {
                cs.setLineWidth((float) stroke.width());
                cs.setStrokingColor(stroke.strokeColor().color());
                // Optional: nicer corners for thick borders
                // cs.setLineJoin(1); // 0=miter (default), 1=round, 2=bevel
            }

            // Configure fill if needed
            if (hasFill) {
                cs.setNonStrokingColor(fillColor.color());
            }

            // Build path once
            if (radius > 0f) {
                pdfDrawRoundedRectangle(cs, fx, fy, fw, fh, radius); // must close the path inside
            } else {
                cs.addRect(fx, fy, fw, fh); // auto-closed
            }

            // Paint with correct operator
            if (hasFill && hasStroke) {
                cs.fillAndStroke();
            } else if (hasFill) {
                cs.fill();
            } else { // hasStroke
                cs.stroke();
            }
        } finally {
            cs.restoreGraphicsState();
        }
        return true;
    }


    private void pdfDrawRoundedRectangle(PDPageContentStream contentStream,
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

