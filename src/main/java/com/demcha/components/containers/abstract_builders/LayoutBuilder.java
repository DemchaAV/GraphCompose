package com.demcha.components.containers.abstract_builders;

import com.demcha.components.core.Component;
import com.demcha.components.core.Entity;
import com.demcha.components.core.EntityName;
import lombok.Getter;

public abstract class LayoutBuilder<B> implements Layout<B> {
    @Getter
    protected Entity entity  = new Entity();


    public B create() {
        String simpleName = self().getClass().getSimpleName();
        String defaultName = simpleName + entity.getId().toString().substring(0, 5);
        entity.addComponent(new EntityName(defaultName));
        return self();
    }

    @Override
    public B addComponent(Component component) {
        entity.addComponent(component);
        return self();
    }


    public B self() {
        return (B) this;
    }


}
