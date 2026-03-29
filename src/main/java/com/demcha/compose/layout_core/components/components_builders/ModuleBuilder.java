package com.demcha.compose.layout_core.components.components_builders;

import com.demcha.compose.layout_core.components.containers.abstract_builders.ContainerBuilder;
import com.demcha.compose.layout_core.components.containers.abstract_builders.StackAxis;
import com.demcha.compose.layout_core.components.geometry.ContentSize;
import com.demcha.compose.layout_core.components.geometry.InnerBoxSize;
import com.demcha.compose.layout_core.components.layout.Align;
import com.demcha.compose.layout_core.components.renderable.Module;
import com.demcha.compose.layout_core.components.renderable.VContainer;
import com.demcha.compose.layout_core.core.Canvas;
import com.demcha.compose.layout_core.core.CanvasBox;
import com.demcha.compose.layout_core.core.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDPage;

@Slf4j
public class ModuleBuilder extends ContainerBuilder<ModuleBuilder> {
    private final Canvas canvas;

    /**
     * Constructs a new {@code ModuleBuilder} without a canvas.
     * Width will default to 0 in initialize().
     */
    ModuleBuilder(EntityManager entityManager, Align align) {
        super(entityManager, align);
        this.canvas = null;
    }

    /**
     * Constructs a new {@code ModuleBuilder} with a ContentSize-based canvas.
     */
    ModuleBuilder(EntityManager entityManager, Align align, ContentSize contentSize) {
        super(entityManager, align);
        if (contentSize == null) {
            this.canvas = null;
        } else {
            this.canvas = createCanvasFromContentSize(contentSize);
        }
    }

    /**
     * Constructs a new {@code ModuleBuilder} with an actual Canvas.
     */
    ModuleBuilder(EntityManager entityManager, Align align, Canvas canvas) {
        super(entityManager, align);
        this.canvas = canvas;
    }

    /**
     * Constructs a new {@code ModuleBuilder} with a PDPage.
     */
    ModuleBuilder(EntityManager entityManager, Align align, PDPage page) {
        this(entityManager, align, new ContentSize(page.getMediaBox().getWidth(), page.getMediaBox().getHeight()));
    }

    /**
     * Constructs a new {@code ModuleBuilder} with an InnerBoxSize.
     */
    ModuleBuilder(EntityManager entityManager, Align align, InnerBoxSize innerBoxSize) {
        this(entityManager, align, new ContentSize(innerBoxSize.width(), innerBoxSize.height()));
    }

    @Override
    public void initialize() {
        entity.addComponent(new Module());
        entity.addComponent(StackAxis.VERTICAL);
        entity.addComponentIfAbsent(new VContainer());
        entity.addComponent(canvas == null
                ? new ContentSize(0, 0)
                : new ContentSize(canvas.innerWidth(), 0));
    }

    /**
     * Creates a Canvas adapter from ContentSize.
     */
    private static Canvas createCanvasFromContentSize(ContentSize size) {
        return new CanvasBox((float) size.width(), (float) size.height(), 0, 0);
    }
}
