package com.demcha.components.layout.coordinator;

import com.demcha.components.core.Component;

/**
 * Component that marks which PDF page an entity should be rendered on.
 */
public record PageIndex(int value) implements Component {
    public static final PageIndex ZERO = new PageIndex(0);
}
