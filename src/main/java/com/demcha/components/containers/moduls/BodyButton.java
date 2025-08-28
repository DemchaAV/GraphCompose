package com.demcha.components.containers.moduls;

import com.demcha.components.content.Stroke;
import com.demcha.components.content.rectangle.FillColor;
import com.demcha.components.content.rectangle.Radius;
import com.demcha.core.PdfDocument;


public class BodyButton extends EmptyBox<BodyButton> {
    //    protected final Rectangle rectangle;
    protected Radius radius;
    protected FillColor fillColor;
    protected Stroke stroke;

    public BodyButton(PdfDocument document) {
        super(document);
    }
}
