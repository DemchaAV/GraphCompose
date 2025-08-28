package com.demcha.legacy.scene;

import com.demcha.components.containers.abstract_builders.EmptyBox;
import com.demcha.components.core.Entity;
import com.demcha.legacy.core.Element;
import com.demcha.components.containers.abstract_builders.Container;
import com.demcha.legacy.layout.Layout;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class Module extends EmptyBox<Module> {
    private final Element self = new Element();
    private final List<Element> children = new ArrayList<>();
    private Layout layout;


    @Override
    public Entity build() {
        return null;
    }
}
