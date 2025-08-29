package com.demcha.components.content.components_builders;

import com.demcha.components.content.Element;
import com.demcha.components.geometry.ContentSize;
import com.demcha.components.layout.coordinator.Position;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;


public class ElementBuilder extends ComponentBoxBuilder<ElementBuilder> {
    private ElementBuilder() {
    }

    public static ElementBuilder create() {
        ElementBuilder elementBuilder = new ElementBuilder();
        elementBuilder.addComponent(new Element());
        return elementBuilder;
    }

    @Override
    protected ElementBuilder self() {
        return this;
    }

    public ElementBuilder fillPageSize(PDPage page) {
        PDRectangle box = page.getCropBox() != null ? page.getCropBox() : page.getMediaBox();
        float w = box.getWidth();
        float h = box.getHeight();

        // If the page has rotation, adjust width/height as PDFBox still returns the unrotated box
        Integer rot = page.getRotation();
        if (rot != null && (rot == 90 || rot == 270)) {
            float tmp = w;
            w = h;
            h = tmp;
        }

        // Store logical (CSS-like) top-left coordinates
        addComponent(new ContentSize(w, h));
        addComponent(new Position(0, h));   // top-left origin for your layout system
        return this;
    }
    public ElementBuilder fillHorizontal(PDPage page, float high) {
        PDRectangle box = page.getCropBox() != null ? page.getCropBox() : page.getMediaBox();
        this.filledHorizontally = true;
        float w = box.getWidth();

        // If the page has rotation, adjust width/height as PDFBox still returns the unrotated box
        Integer rot = page.getRotation();
        if (rot != null && (rot == 90 || rot == 270)) {
            float tmp = w;
            high = tmp;
        }

        // Store logical (CSS-like) top-left coordinates
        addComponent(new ContentSize(w, high));
        return this;
    }




}
