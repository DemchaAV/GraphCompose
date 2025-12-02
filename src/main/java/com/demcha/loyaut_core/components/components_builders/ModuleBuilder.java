package com.demcha.loyaut_core.components.components_builders;

import com.demcha.loyaut_core.components.containers.abstract_builders.ContainerBuilder;
import com.demcha.loyaut_core.components.containers.abstract_builders.StackAxis;
import com.demcha.loyaut_core.components.core.Entity;
import com.demcha.loyaut_core.components.geometry.ContentSize;
import com.demcha.loyaut_core.components.geometry.InnerBoxSize;
import com.demcha.loyaut_core.components.geometry.OuterBoxSize;
import com.demcha.loyaut_core.components.layout.Align;
import com.demcha.loyaut_core.components.renderable.VContainer;
import com.demcha.loyaut_core.components.style.Margin;
import com.demcha.loyaut_core.core.EntityManager;
import com.demcha.loyaut_core.components.renderable.Module;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDPage;

@Slf4j
public class ModuleBuilder extends ContainerBuilder<ModuleBuilder> {
    private final Canvas canvas;

    /**
     * Constructs a new {@code ModuleBuilder} associated with a specific Entity Manager.
     *
     * @param entityManager The {@link EntityManager} to which the container and its entities will belong.
     */
    public ModuleBuilder(EntityManager entityManager, Align align) {
        this(entityManager, align, (ContentSize) null);
    }

    public ModuleBuilder(EntityManager entityManager, Align align, ContentSize canvas) {
        super(entityManager, align);
        this.canvas = new Canvas() {
            @Override
            public float width() {
                return (float) canvas.width();
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
                return (float) canvas.height();
            }

            @Override
            public Margin margin() {
                log.warn("You didn't, set a Canvas object, ContentSize doesn't support margin retorn Margin is zero()");
                return Margin.zero();
            }

            @Override
            public void addMargin(Margin margin) {
                log.warn("You didn't, set a Canvas object, ContentSize doesn't support margin");
            }
        };

    }

    public ModuleBuilder(EntityManager entityManager, Align align, Canvas canvas) {
        super(entityManager, align);
        this.canvas = canvas;

    }

    public ModuleBuilder(EntityManager entityManager, Align align, PDPage page) {
        this(entityManager, align, new ContentSize(page.getMediaBox().getWidth(), page.getMediaBox().getHeight()));

    }

    public ModuleBuilder(EntityManager entityManager, Align align, InnerBoxSize innerBoxSize) {
        this(entityManager, align, new ContentSize(innerBoxSize.width(), innerBoxSize.height()));
    }


    @Override
    public void initialize() {
        entity.addComponent(new Module());
        entity.addComponent(StackAxis.VERTICAL);
        entity.addComponentIfAbsent(new VContainer()); // Add the specific component
        entity.addComponent(canvas == null ? new ContentSize(0, 0) : new ContentSize(canvas.innerWidth(), 0));
    }


    private void fitInParent(Entity child) {
        var childOuter = OuterBoxSize.from(child).orElseThrow();
        var childSize = child.getComponent(ContentSize.class).orElseThrow();
        double availableW = InnerBoxSize.from(entity).orElseThrow().width();
        double different = availableW - childOuter.width();
        if (different > 0) {
            child.addComponent(new ContentSize(availableW, childSize.height()));
        }
    }
}
