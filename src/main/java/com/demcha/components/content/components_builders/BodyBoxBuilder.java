package com.demcha.components.content.components_builders;

import com.demcha.components.geometry.OuterBoxSize;
import com.demcha.components.geometry.ContentSize;
import com.demcha.components.layout.Position;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;

public class BodyBoxBuilder extends ComponentBoxBuilder<BodyBoxBuilder> {
    private BodyBoxBuilder() {
    }

    public static BodyBoxBuilder create() {
        return new BodyBoxBuilder();
    }

    @Override
    protected BodyBoxBuilder self() {
        return this;
    }

    public BodyBoxBuilder fillPageSize(PDPage page) {
        PDRectangle box = page.getCropBox() != null ? page.getCropBox() : page.getMediaBox();
        float w = box.getWidth();
        float h = box.getHeight();

        // If the page has rotation, adjust width/height as PDFBox still returns the unrotated box
        Integer rot = page.getRotation();
        if (rot != null && (rot == 90 || rot == 270)) {
            float tmp = w; w = h; h = tmp;
        }

        // Store logical (CSS-like) top-left coordinates
        addComponent(new ContentSize(w, h));
        addComponent(new Position(0, h));   // top-left origin for your layout system
        return this;
    }

}
