package com.demcha.components;

import com.demcha.components.components_builders.ElementBuilder;
import com.demcha.components.core.Entity;
import com.demcha.components.geometry.ContentSize;
import com.demcha.components.geometry.InnerBoxSize;
import com.demcha.components.geometry.OuterBoxSize;
import com.demcha.components.layout.Anchor;
import com.demcha.components.style.Margin;
import com.demcha.components.style.Padding;
import com.demcha.components.components_builders.Canvas;
import com.demcha.core.EntityManager;
import org.apache.pdfbox.pdmodel.PDPage;

public class ModuleContainer {
    private final ElementBuilder elementBuilder;
    private float width;
    private float height;

    public ModuleContainer(EntityManager entityManager, double width, double height) {
        this.elementBuilder = new ElementBuilder(entityManager)
                .anchor(Anchor.center())
                .fillPageSize(width, height)
        ;
        this.width = (float) width;
        this.height = (float) height;

    }
    public ModuleContainer(EntityManager entityManager, double width, double height, Margin margin) {
        this.elementBuilder = new ElementBuilder(entityManager)
                .anchor(Anchor.center())
                .addComponent(new Padding(margin.top(),  margin.right(), margin.bottom(), margin.left()))
                .fillPageSize(width, height)
        ;
        this.width = (float) width;
        this.height = (float) height;

    }

    public ModuleContainer(EntityManager entityManager, PDPage page) {
        this(entityManager, page.getMediaBox().getWidth(), page.getCropBox().getHeight());
    }
    public ModuleContainer(EntityManager entityManager, Canvas size) {
                this(entityManager, size.width(), size.height(),size.margin());
    }

    public ModuleContainer padding(Padding padding) {
        elementBuilder.padding(padding);
        return this;
    }

    public ModuleContainer padding(double top, double right, double bottom, double left) {
        Padding padding = new Padding(top, right, bottom, left);
        elementBuilder.padding(padding);
        return this;
    }

    public <T extends Entity> ModuleContainer addChild(T component) {
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
    public <T extends Entity> ModuleContainer addModule(T module) {
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
