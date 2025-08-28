package com.demcha.components.containers.abstract_builders;

import com.demcha.components.core.Entity;
import com.demcha.core.PdfDocument;

public interface BuildEntity {
    /**
     * Builds the entity.
     *
     * @return The built entity.
     */
    Entity build();
    PdfDocument document();
    Entity entity();
}
