package com.demcha.structure;


import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Getter
@Setter
public class Element {
    private final Map<Class<? extends Component>, Component> components = new HashMap<>();

    public <T extends Component> void add(T component) {
        components.put(component.getClass(), component);
    }

    public <T extends Component> Optional<T> get(Class<T> type) {
        return Optional.ofNullable(type.cast(components.get(type)));
    }

    public <T extends Component> boolean has(Class<T> type) {
        return components.containsKey(type);
    }
}


