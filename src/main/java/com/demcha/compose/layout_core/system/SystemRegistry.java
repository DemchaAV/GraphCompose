package com.demcha.compose.layout_core.system;

import com.demcha.compose.layout_core.system.interfaces.SystemECS;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Stream;

/**
 * A registry for managing and retrieving Entity Component System (ECS) systems.
 * Provides methods to add, retrieve, and stream registered systems.
 */
@Slf4j
@Getter
@Accessors(fluent = true)
public class SystemRegistry {

    /**
     * A map storing registered systems, keyed by their class type.
     * Uses a LinkedHashMap to maintain the order of insertion.
     */
    private final Map<Class<? extends SystemECS>, SystemECS> systems = new LinkedHashMap<>();

    /**
     * Adds a single system to the registry.
     *
     * @param system the system to add
     * @param <T>    the type of the system, extending SystemECS
     */
    public <T extends SystemECS> void addSystem(T system) {
        log.info("Adding SystemECS {}", system.getClass().getName());
        systems.put(system.getClass(), system);
    }

    /**
     * Returns a stream of all registered systems.
     *
     * @return a stream of SystemECS instances
     */
    public Stream<SystemECS> getStream() {
        return systems.values().stream();
    }

    /**
     * Retrieves a system from the registry by its class type.
     * If an exact match is not found, it searches for a subclass or implementation.
     *
     * @param systemClass the class of the system to retrieve
     * @param <T>         the expected type of the system
     * @return an Optional containing the system if found, or an empty Optional otherwise
     */
    public <T extends SystemECS> Optional<T> getSystem(Class<T> systemClass) {
        SystemECS system = systems.get(systemClass);
        if (systemClass.isInstance(system)) {
            return Optional.of(systemClass.cast(system));
        }
        for (SystemECS candidate : systems.values()) {
            if (systemClass.isInstance(candidate)) {
                return Optional.of(systemClass.cast(candidate));
            }
        }
        return Optional.empty();
    }

    /**
     * Adds a list of systems to the registry.
     *
     * @param systems the list of systems to add
     * @param <T>     the type of the systems, extending SystemECS
     */
    public <T extends SystemECS> void addAllSystems(List<T> systems) {
        systems.forEach(this::addSystem);
    }

}
