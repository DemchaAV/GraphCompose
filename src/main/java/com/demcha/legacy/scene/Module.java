package com.demcha.legacy.scene;

import com.demcha.components.containers.abstract_builders.EmptyBox;
import com.demcha.components.core.Entity;
import com.demcha.core.PdfDocument;
import com.demcha.legacy.core.Element;
import com.demcha.legacy.layout.Layout;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a module within a legacy scene, extending {@link EmptyBox} for container functionality.
 * This class is designed to hold and manage a collection of {@link Element} objects,
 * and can be associated with a {@link Layout}.
 */
@Getter
@Setter
public class Module extends EmptyBox<Module> {
    /**
     * The primary element representing this module itself.
     */
    private final Element self = new Element();

    /**
     * A list of child elements contained within this module.
     */
    private final List<Element> children = new ArrayList<>();

    /**
     * The layout applied to this module.
     */
    private Layout layout;

    /**
     * Constructs a new Module instance.
     *
     * @param document The PDF document associated with this module.
     */
    public Module(PdfDocument document) {
        super(document);
    }


    @Override

    public Entity build() {
        return null;
    }
}
