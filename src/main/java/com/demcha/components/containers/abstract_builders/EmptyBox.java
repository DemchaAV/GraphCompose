package com.demcha.components.containers.abstract_builders;

import com.demcha.components.core.Entity;
import com.demcha.components.layout.ParentComponent;
import com.demcha.core.PdfDocument;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

/**
 * Abstract base class for building entities that represent an empty box or a container
 * within a PDF document. This class extends {@link EntityBuilderBase} and implements
 * {@link BuildEntity}, providing a foundation for components that might not render
 * visible content themselves but serve as structural elements or placeholders.
 *
 * <p>It is designed to be extended by concrete builder classes that define how to
 * construct specific empty box-like entities within the PDF.
 *
 * @param <T> The type of the entity that this builder will construct.
 */
@Getter
@RequiredArgsConstructor
@Accessors(fluent = true)
public abstract class EmptyBox<T> extends EntityBuilderBase<T> implements BuildEntity {

    /**
     * The PDF document instance to which the built entity will be added or associated.
     * This document is typically provided during the construction of the builder
     * and is used by concrete implementations to interact with the PDF.
     */
    protected final PdfDocument document;

    T parrent(ParentComponent parent) {
        entity.addComponent(parent);
        return self();
    }
    T parrent(Entity parent) {
        return parrent(new ParentComponent(parent));
    }

}
