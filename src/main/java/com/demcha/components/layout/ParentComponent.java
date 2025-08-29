package com.demcha.components.layout;

import com.demcha.components.core.Component;
import com.demcha.components.core.Entity;

import java.util.Objects;
import java.util.UUID;

public record ParentComponent(UUID uuid) implements Component{
    public ParentComponent(Entity entity) {
        this(Objects.requireNonNull(entity.getId(), "Entity ID cannot be null.") );
    }
}
