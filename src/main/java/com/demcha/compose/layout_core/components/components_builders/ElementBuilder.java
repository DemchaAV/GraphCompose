package com.demcha.compose.layout_core.components.components_builders;

import com.demcha.compose.layout_core.components.containers.abstract_builders.EmptyBox;
import com.demcha.compose.layout_core.components.core.Component;
import com.demcha.compose.layout_core.components.geometry.ContentSize;
import com.demcha.compose.layout_core.components.layout.coordinator.Position;
import com.demcha.compose.layout_core.components.renderable.Element;
import com.demcha.compose.layout_core.core.EntityManager;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;

import java.util.Optional;


public class ElementBuilder extends EmptyBox<ElementBuilder>  {
    private boolean filledHorizontally;
    private boolean filledVertically;


    ElementBuilder(EntityManager entityManager) {
        super(entityManager);
    }

    @Override
    public void initialize() {
        entity().addComponent(new Element());
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
       return fillPageSize(w, h);
    }

    public ElementBuilder fillPageSize(double width, double height) {
        float w = (float) width;
        float h = (float) height;
        // Store logical (CSS-like) top-left coordinates
        addComponent(new ContentSize(w, h));
        addComponent(new Position(0, 0));   // top-left origin for your layout system
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

    public <T extends Component> Optional<T> getComponent(Class<T> clazz) {
        return entity.getComponent(clazz);
    }


}
