package com.demcha.compose.loyaut_core.core;

import com.demcha.compose.loyaut_core.system.interfaces.SystemECS;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Stream;

@Slf4j
@Getter
@Accessors(fluent = true)
public class SystemRegistry {
    private final Map<Class<? extends SystemECS>, SystemECS> systems = new LinkedHashMap<>();

    public <T extends SystemECS> void addSystem(T system) {
        log.info("Adding SystemECS {}", system.getClass().getName());
        systems.put(system.getClass(), system);
    }
    public Stream<SystemECS> getStream(){
        return systems.values().stream();
    }

    public <T extends SystemECS> Optional<T> getSystem(Class<T> systemClass) {
        SystemECS system = systems.get(systemClass);
        if (systemClass.isInstance(system)) {
            return Optional.of(systemClass.cast(system));
        }
        return Optional.empty();
    }
    public <T extends SystemECS> void addAllSystems(List<T> systems) {
        systems.forEach(this::addSystem);
    }


}
