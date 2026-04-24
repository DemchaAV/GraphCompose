package com.demcha.compose.engine.components.layout;

import com.demcha.compose.engine.components.core.Component;
import com.demcha.compose.engine.components.core.Entity;

import java.util.Objects;
import java.util.UUID;

public record ParentComponent(UUID uuid) implements Component{
    public ParentComponent(Entity entity) {
        this(Objects.requireNonNull(entity.getUuid(), "Entity ID cannot be null.") );
    }
}
