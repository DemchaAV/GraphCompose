package com.demcha.compose.layout_core.components.containers;

import com.demcha.compose.layout_core.components.containers.abstract_builders.ContainerBuilder;
import com.demcha.compose.layout_core.components.containers.abstract_builders.StackAxis;
import com.demcha.compose.layout_core.components.core.Entity;
import com.demcha.compose.layout_core.components.geometry.ContentSize;
import com.demcha.compose.layout_core.components.geometry.InnerBoxSize;
import com.demcha.compose.layout_core.components.geometry.OuterBoxSize;
import com.demcha.compose.layout_core.components.layout.Align;
import com.demcha.compose.layout_core.components.layout.Anchor;
import com.demcha.compose.layout_core.components.layout.coordinator.Position;
import com.demcha.compose.layout_core.components.renderable.Container;
import com.demcha.compose.layout_core.components.style.Margin;
import com.demcha.compose.layout_core.components.style.Padding;
import com.demcha.compose.layout_core.core.Canvas;
import com.demcha.compose.layout_core.core.EntityManager;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;

public class ModulesContainer extends ContainerBuilder<ModulesContainer> {
    private float width;
    private float height;

    public ModulesContainer(EntityManager entityManager, double width, double height) {
        this(entityManager, 0.0, 0.0, null);

    }

    public ModulesContainer(EntityManager entityManager, double width, double height, Margin margin) {
        super(entityManager, Align.middle(2));
        if (margin != null) {
            addComponent(new Padding(margin.top(), margin.right(), margin.bottom(), margin.left()));
        }
        fillPageSize(width, height);
        this.width = (float) width;
        this.height = (float) height;

    }


    public ModulesContainer(EntityManager entityManager, PDPage page) {
        this(entityManager, page.getMediaBox().getWidth(), page.getCropBox().getHeight());
    }

    public ModulesContainer(EntityManager entityManager, Canvas size) {
        this(entityManager, size.width(), size.height(), size.margin());
    }


    public ModulesContainer fillPageSize(PDPage page) {
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

    public ModulesContainer fillPageSize(double width, double height) {
        float w = (float) width;
        float h = (float) height;
        // Store logical (CSS-like) top-left coordinates
        addComponent(new ContentSize(w, h));
        addComponent(new Position(0, 0));   // top-left origin for your layout system
        return this;
    }

    /**
     * same as addChild but the child Element will change the size to be fit in canvas
     *
     * @param module
     * @param <T>
     * @return
     */
    public <T extends Entity> ModulesContainer addModule(T module) {
        var from = OuterBoxSize.from(module).orElseThrow();
        if (from.width() > width || from.height() > height) {
            float differentW = width - (float) from.width();
            float differentH = height - (float) from.height();
            var moduleSize = module.getComponent(ContentSize.class).orElseThrow();
            double newW = differentW > 0 ? moduleSize.width() - differentW : moduleSize.width();
            double newH = differentH > 0 ? moduleSize.height() - differentH : moduleSize.height();
            module.addComponent(new ContentSize(newW, newH));

        }

        addChild(module)
                .anchor(Anchor.topCenter());
        return this;
    }

    public InnerBoxSize innerBoxSize() {
        Padding padding = entity().getComponent(Padding.class).orElse(Padding.zero());
        return new InnerBoxSize(width - padding.horizontal(), height - padding.vertical());
    }


    /**
     * This method provides an initialization step for components within the entity,
     * ensuring that all components are properly associated with this entity.
     */
    @Override
    public void initialize() {
        addComponent(new Container());
        addComponent(StackAxis.DEFAULT);
        addComponent(Anchor.topLeft());
    }
}
