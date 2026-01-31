package com.demcha.compose.loyaut_core.components.components_builders;

import com.demcha.compose.loyaut_core.components.containers.abstract_builders.ContainerBuilder;
import com.demcha.compose.loyaut_core.components.containers.abstract_builders.StackAxis;
import com.demcha.compose.loyaut_core.components.geometry.ContentSize;
import com.demcha.compose.loyaut_core.components.geometry.InnerBoxSize;
import com.demcha.compose.loyaut_core.components.layout.Align;
import com.demcha.compose.loyaut_core.components.renderable.Module;
import com.demcha.compose.loyaut_core.components.renderable.VContainer;
import com.demcha.compose.loyaut_core.components.style.Margin;
import com.demcha.compose.loyaut_core.core.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDPage;

@Slf4j
public class ModuleBuilder extends ContainerBuilder<ModuleBuilder> {
    private final Canvas canvas;

    /**
     * Constructs a new {@code ModuleBuilder} without a canvas.
     * Width will default to 0 in initialize().
     */
    public ModuleBuilder(EntityManager entityManager, Align align) {
        super(entityManager, align);
        this.canvas = null;
    }

    /**
     * Constructs a new {@code ModuleBuilder} with a ContentSize-based canvas.
     */
    public ModuleBuilder(EntityManager entityManager, Align align, ContentSize contentSize) {
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
    public ModuleBuilder(EntityManager entityManager, Align align, Canvas canvas) {
        super(entityManager, align);
        this.canvas = canvas;
    }

    /**
     * Constructs a new {@code ModuleBuilder} with a PDPage.
     */
    public ModuleBuilder(EntityManager entityManager, Align align, PDPage page) {
        this(entityManager, align, new ContentSize(page.getMediaBox().getWidth(), page.getMediaBox().getHeight()));
    }

    /**
     * Constructs a new {@code ModuleBuilder} with an InnerBoxSize.
     */
    public ModuleBuilder(EntityManager entityManager, Align align, InnerBoxSize innerBoxSize) {
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
     * Note: ContentSize doesn't support margins.
     */
    private static Canvas createCanvasFromContentSize(ContentSize size) {
        return new Canvas() {
            @Override
            public float width() {
                return (float) size.width();
            }

            @Override
            public float x() {
                return 0;
            }

            @Override
            public float y() {
                return 0;
            }

            @Override
            public float height() {
                return (float) size.height();
            }

            @Override
            public Margin margin() {
                log.warn("ContentSize-based canvas doesn't support margin, returning Margin.zero()");
                return Margin.zero();
            }

            @Override
            public void addMargin(Margin margin) {
                log.warn("ContentSize-based canvas doesn't support margin, ignoring addMargin()");
            }
        };
    }
}
