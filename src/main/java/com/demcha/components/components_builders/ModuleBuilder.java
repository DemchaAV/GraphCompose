package com.demcha.components.components_builders;

import com.demcha.components.containers.abstract_builders.ContainerBuilder;
import com.demcha.components.containers.abstract_builders.StackAxis;
import com.demcha.components.core.Entity;
import com.demcha.components.geometry.ContentSize;
import com.demcha.components.geometry.InnerBoxSize;
import com.demcha.components.geometry.OuterBoxSize;
import com.demcha.components.layout.Align;
import com.demcha.components.renderable.HContainer;
import com.demcha.components.style.Margin;
import com.demcha.components.style.Padding;
import com.demcha.core.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDPage;

@Slf4j
public class ModuleBuilder extends ContainerBuilder<ModuleBuilder> {
    private final ContentSize canvasSize;

    /**
     * Constructs a new {@code ModuleBuilder} associated with a specific Entity Manager.
     *
     * @param entityManager The {@link EntityManager} to which the container and its entities will belong.
     */
    public ModuleBuilder(EntityManager entityManager,Align align) {
        this(entityManager, align,(ContentSize) null);
    }

    public ModuleBuilder(EntityManager entityManager,Align align, ContentSize canvasSize) {
        super(entityManager, align);
        this.canvasSize = canvasSize;

    }
    public ModuleBuilder(EntityManager entityManager,Align align, PDPage page) {
        this(entityManager,align, new ContentSize(page.getMediaBox().getWidth(), page.getMediaBox().getHeight()));

    }
    public ModuleBuilder(EntityManager entityManager, Align align,InnerBoxSize innerBoxSize) {
        this(entityManager,align, new ContentSize(innerBoxSize.innerW(), innerBoxSize.innerH()));
    }


    @Override
    public void initialize() {
        entity.addComponent(new com.demcha.components.renderable.Module());
        entity.addComponent(StackAxis.VERTICAL);
        entity.addComponentIfAbsent(new HContainer()); // Add the specific component
        entity.addComponent(canvasSize==null?new ContentSize(0,0): new ContentSize(canvasSize.width(), 0));
    }



    private  void fitInParent(Entity child) {
        var childOuter = OuterBoxSize.from(child).orElseThrow();
        var childSize = child.getComponent(ContentSize.class).orElseThrow();
        double availibleW = InnerBoxSize.from(entity).orElseThrow().innerW();
        double different = availibleW - childOuter.width();
        if (different > 0){
            child.addComponent(new ContentSize(availibleW, childSize.height()));
        }
    }
}
