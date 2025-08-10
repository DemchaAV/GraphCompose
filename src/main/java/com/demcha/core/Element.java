package com.demcha.core;

import java.util.*;
import java.util.function.Supplier;

/**
 * Represents an element that can hold multiple components, each identified by its type.
 * Components are stored in a map and can be added, retrieved, and checked for existence.
 * This class provides functionality for adding components, accessing them by type,
 * and ensuring that components are either retrieved or created and added as needed.
 *
 * <h2>Key Features:</h2>
 * <ul>
 *     <li>Add components of various types to the element.</li>
 *     <li>Retrieve components by their class type or interface.</li>
 *     <li>Check if a component of a certain type exists in the element.</li>
 *     <li>Automatically create and add a component if it doesn't exist, using a factory method.</li>
 * </ul>
 */
public class Element {

    // Map to store components by their class type
    private final Map<Class<?>, Component> components = new HashMap<>();

    /**
     * Adds a component to the element. The component is stored in a map, indexed by its class type.
     *
     * @param <T> The type of the component.
     * @param c   The component to add.
     * @return The current element instance for chaining.
     * @throws NullPointerException if the component is null.
     */
    public <T extends Component> Element add(T c) {
        Objects.requireNonNull(c, "Component cannot be null");
        components.put(c.getClass(), c);
        return this;
    }

    /**
     * Retrieves a component of the specified type from the element.
     * It first tries to get the component directly by its class type.
     * If not found, it searches through all components to see if any of them
     * are instances of the specified type (including subclasses or interfaces).
     *
     * @param <T>  The type of the component to retrieve.
     * @param type The class type of the component.
     * @return An Optional containing the component if found, or an empty Optional if not found.
     */
    public <T extends Component> Optional<T> get(Class<T> type) {
        // Direct lookup by type
        Component comp = components.get(type);
        if (comp != null) return Optional.of(type.cast(comp));

        // Search by interfaces and superclasses
        for (Component value : components.values()) {
            if (type.isInstance(value)) {
                return Optional.of(type.cast(value));
            }
        }
        return Optional.empty();
    }

    /**
     * Checks if the element contains a component of the specified type.
     *
     * @param <T>  The type of the component to check.
     * @param type The class type of the component.
     * @return true if the component exists in the element, false otherwise.
     */
    public <T extends Component> boolean has(Class<T> type) {
        return get(type).isPresent();
    }

    /**
     * Retrieves a component of the specified type, or creates and adds it if it doesn't exist.
     * Uses the provided factory method to create the component if not found.
     *
     * @param <T>     The type of the component to retrieve.
     * @param type    The class type of the component.
     * @param factory A factory method that creates the component if it doesn't exist.
     * @return The existing or newly created component.
     */
    public <T extends Component> T getOrAdd(Class<T> type, Supplier<T> factory) {
        return get(type).orElseGet(() -> {
            T v = factory.get();
            add(v);
            return v;
        });
    }
}
