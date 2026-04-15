package com.demcha.compose.v2;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Registry of semantic node definitions.
 */
public final class NodeRegistry {
    private final Map<Class<?>, NodeDefinition<?>> definitions = new LinkedHashMap<>();

    public <E extends DocumentNode> NodeRegistry register(NodeDefinition<E> definition) {
        NodeDefinition<E> safeDefinition = Objects.requireNonNull(definition, "definition");
        definitions.put(safeDefinition.nodeType(), safeDefinition);
        return this;
    }

    @SuppressWarnings("unchecked")
    public <E extends DocumentNode> NodeDefinition<E> definitionFor(E node) {
        Objects.requireNonNull(node, "node");
        NodeDefinition<?> direct = definitions.get(node.getClass());
        if (direct != null) {
            return (NodeDefinition<E>) direct;
        }
        for (Map.Entry<Class<?>, NodeDefinition<?>> entry : definitions.entrySet()) {
            if (entry.getKey().isAssignableFrom(node.getClass())) {
                return (NodeDefinition<E>) entry.getValue();
            }
        }
        throw new IllegalStateException("No node definition registered for " + node.getClass().getName());
    }
}
