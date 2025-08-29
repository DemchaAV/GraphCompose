package com.demcha.components.containers.abstract_builders;

import com.demcha.components.core.Entity;
import com.demcha.core.PdfDocument;

/**
 * The {@code BuildEntity} interface defines a contract for classes that are responsible for building an {@link Entity} object.
 * It provides methods to build the entity, access the associated {@link PdfDocument}, and retrieve the entity being built.
 * <p>
 * Implementations of this interface are typically used in builder patterns to construct complex {@link Entity} objects step-by-step.
 */
public interface BuildEntity {

    /**
     * Builds the {@link Entity} object. This method is responsible for assembling all
     * components and child entities, if any, into a complete {@link Entity} object.
     *
     * @return The fully constructed and assembled {@link Entity} object.
     */
    Entity build();

    default Entity buildInto(){
      return   document().putEntity(build());
    }

    /**
     * Returns the {@link PdfDocument} associated with this builder.
     *
     * @return The associated {@link PdfDocument}.
     */
    PdfDocument document();

    /**
     * Returns the {@link Entity} object that is currently being built by this builder.
     *
     * @return The entity being built.
     */
    Entity entity();
}
