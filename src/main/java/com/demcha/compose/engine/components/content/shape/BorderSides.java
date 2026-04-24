package com.demcha.compose.engine.components.content.shape;

import com.demcha.compose.engine.components.core.Component;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * Selective border ownership for renderables that should only draw specific sides.
 */
public record BorderSides(Set<Side> sides) implements Component {

    public BorderSides {
        Objects.requireNonNull(sides, "sides");
        sides = sides.isEmpty() ? Set.of() : Set.copyOf(sides);
    }

    public static BorderSides of(Side... sides) {
        if (sides == null || sides.length == 0) {
            return new BorderSides(Set.of());
        }
        return new BorderSides(EnumSet.copyOf(Arrays.asList(sides)));
    }

    public static BorderSides all() {
        return of(Side.ALL);
    }
}
