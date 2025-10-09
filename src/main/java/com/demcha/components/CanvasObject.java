package com.demcha.components;

import com.demcha.components.components_builders.ElementBuilder;
import com.demcha.components.core.Entity;
import com.demcha.components.geometry.ContentSize;
import com.demcha.components.geometry.InnerBoxSize;
import com.demcha.components.geometry.OuterBoxSize;
import com.demcha.components.layout.Anchor;
import com.demcha.components.style.Padding;
import com.demcha.core.EntityManager;
import org.apache.pdfbox.pdmodel.PDPage;

public class CanvasObject {
    private final ElementBuilder elementBuilder;
    private float width;
    private float height;

    public CanvasObject(EntityManager entityManager, double width, double height) {
        this.elementBuilder = new ElementBuilder(entityManager)
                .anchor(Anchor.center())
                .fillPageSize(width, height)
        ;
        this.width = (float) width;
        this.height = (float) height;

    }

    public CanvasObject(EntityManager entityManager, PDPage page) {
        this(entityManager, page.getMediaBox().getWidth(), page.getCropBox().getHeight());
    }

    public CanvasObject padding(Padding padding) {
        elementBuilder.padding(padding);
        return this;
    }

    public CanvasObject padding(double top, double right, double bottom, double left) {
        Padding padding = new Padding(top, right, bottom, left);
        elementBuilder.padding(padding);
        return this;
    }

    public <T extends Entity> CanvasObject addChild(T component) {
        elementBuilder.addChild(component);
        return this;
    }

    /**
     * same as addChild but the child Element will change the size to be fit in canvas
     *
     * @param module
     * @param <T>
     * @return
     */
    public <T extends Entity> CanvasObject addModule(T module) {
        var from = OuterBoxSize.from(module).orElseThrow();
        if (from.width() > width || from.height() > height) {
            float differentW = width - (float) from.width();
            float differentH = height - (float) from.height();
            var moduleSize = module.getComponent(ContentSize.class).orElseThrow();
            double newW = differentW > 0 ? moduleSize.width() - differentW : moduleSize.width();
            double newH = differentH > 0 ? moduleSize.height() - differentH : moduleSize.height();
            module.addComponent(new ContentSize(newW, newH));

        }

        elementBuilder.addChild(module)
                .anchor(Anchor.topCenter());
        return this;
    }

    public InnerBoxSize innerBoxSize() {
        Padding padding = elementBuilder.entity().getComponent(Padding.class).orElse(Padding.zero());
        return new InnerBoxSize(width - padding.horizontal(), height - padding.vertical());
    }

    public Entity build() {
        return elementBuilder.build();
    }


}
