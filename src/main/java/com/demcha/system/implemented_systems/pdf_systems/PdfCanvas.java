package com.demcha.system.implemented_systems.pdf_systems;

import com.demcha.components.components_builders.Canvas;
import com.demcha.components.style.Margin;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.apache.pdfbox.pdmodel.common.PDRectangle;

@Getter
@Accessors(fluent = true)
public final class PdfCanvas implements Canvas {
    private final float width;
    private final float height;
    private final float x;
    private final float y;
    private  Margin margin;

    public PdfCanvas(float width, float height, float x, float y, Margin margin) {
        this.width = width;
        this.height = height;
        this.x = x;
        this.y = y;
        this.margin = margin;
    }

    public PdfCanvas(float width, float height, float x, float y) {
        this(width, height, x, y, null);
    }

    public PdfCanvas(PDRectangle rectangle, Margin margin) {
        this(rectangle.getWidth(), rectangle.getHeight(), rectangle.getLowerLeftX(), rectangle.getLowerLeftY(), margin);
    }

    public PdfCanvas(PDRectangle rectangle) {
        this(rectangle, null);
    }

    public PdfCanvas(PDRectangle rectangle, float x, float y, Margin margin) {
        this(rectangle.getWidth(), rectangle.getHeight(), x, y, margin);
    }

    public PdfCanvas(PDRectangle rectangle, float x, float y) {
        this(rectangle.getWidth(), rectangle.getHeight(), x, y, null);
    }

    public PdfCanvas(PDRectangle rectangle, float position, Margin margin) {
        this(rectangle.getWidth(), rectangle.getHeight(), position, position, margin);
    }

    public PdfCanvas(PDRectangle rectangle, float position) {
        this(rectangle.getWidth(), rectangle.getHeight(), position, position, null);
    }


    @Override
    public Margin margin() {
        if (this.margin == null) {
            return Margin.zero();
        }
        return margin;
    }

    @Override
    public void addMargin(Margin margin) {
        this.margin = margin;
    }
}


