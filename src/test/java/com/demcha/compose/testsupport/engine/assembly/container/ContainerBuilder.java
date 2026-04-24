package com.demcha.compose.testsupport.engine.assembly.container;


import com.demcha.compose.testsupport.engine.assembly.HContainerBuilder;
import com.demcha.compose.testsupport.engine.assembly.VContainerBuilder;
import com.demcha.compose.engine.components.core.Entity;
import com.demcha.compose.engine.components.layout.Align;
import com.demcha.compose.engine.core.EntityManager;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Base class for builders that own child entities and participate in container layout.
 * <p>
 * Container builders extend the basic entity registration behavior from
 * {@link EmptyBox} and add a parent/child contract. Child entities are linked
 * through {@code ParentComponent}, and the layout system later uses those links
 * to compute hierarchy-aware size expansion, alignment, and placement.
 * </p>
 *
 * @param <T> the concrete container builder type
 * @see HContainerBuilder
 * @see VContainerBuilder
 * @see EmptyBox
 */
@Slf4j
public abstract class ContainerBuilder<T extends ContainerBuilder<T>> extends EmptyBox<T> implements Box {


    public ContainerBuilder(EntityManager entityManager, Align align) {
        super(entityManager);
        entity.addComponent(align);
    }


    /**
     * Returns the UUIDs of entities currently attached as children.
     *
     * <p>The list reflects builder-time hierarchy; the layout system later uses
     * the same relationship to resolve geometry.</p>
     *
     * @return child entity identifiers in insertion order
     */
    public List<UUID> children() {
        return this.entity.getChildren();
    }


    /**
     * Replaces the container alignment metadata.
     *
     * <p>This alignment is later read during container layout to determine how
     * children should be arranged inside the container's inner box.</p>
     *
     * @return the current builder
     */
    public T addAlin(Align align) {
        log.debug("add alin to entity {}", this.entity);
        this.entity.addComponent(align);
        return self();
    }

    /**
     * Registers the container entity after child relationships have been declared.
     *
     * <p>Container geometry is still finalized later by layout systems; the build
     * step mainly persists the entity graph into the registry.</p>
     *
     * @return the built container entity
     */
    @Override
    public Entity build() {
        log.debug("building entity {}", this.entity);
        return registerBuiltEntity();
    }


}


