package com.demcha.compose.testsupport.engine.assembly;
import com.demcha.compose.testsupport.engine.assembly.container.ContainerBuilder;
import com.demcha.compose.engine.components.layout.StackAxis;
import com.demcha.compose.engine.components.renderable.VContainer;
import com.demcha.compose.engine.components.layout.Align;
import com.demcha.compose.engine.core.EntityManager;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class VContainerBuilder extends ContainerBuilder<VContainerBuilder> {
    /**
     * Constructs a new {@code VContainerBuilder} with the specified Entity Manager.
     *
     * @param entityManager The {@link EntityManager} to which the container will be added.
     */
    VContainerBuilder(EntityManager entityManager, Align align) {
        super(entityManager,align);
    }

    /**
     * Initializes the container builder with the specified alignment.
     * This method calls the common creation logic from the superclass and then
     * adds a {@link VContainer} component to the entity.
     *
     * @return This builder instance for method chaining.
     */




    @Override
    public void initialize() {
        entity.addComponent(new VContainer());
        entity.addComponent(StackAxis.VERTICAL);
    }


}
