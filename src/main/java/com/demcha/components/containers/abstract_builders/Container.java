package com.demcha.components.containers.abstract_builders;

import com.demcha.components.core.Component;
import com.demcha.components.core.Entity;
import com.demcha.components.layout.ParentComponent;
import lombok.NonNull;

import java.util.Set;

/**
 * Interface representing a container that holds and arranges child Entitys.
 * The container manages its own {@link Entity}, which represents the container itself,
 * as well as the list of child Entitys it contains. It also defines the layout used for arranging
 * the child Entitys within the container.
 *
 * <p>The container is responsible for providing methods to add, remove, and clear child Entitys,
 * as well as for setting the layout that determines how the Entitys are arranged.</p>
 *
 * <h2>Methods:</h2>
 * <ul>
 *     <li><strong>getEntity()</strong> - Returns the {@link Entity} representing the container itself.</li>
 *     <li><strong>getChildren()</strong> - Returns a list of the child {@link Entity}s within the container.</li>
 *     <li><strong>getLayout()</strong> - Returns the current layout of the container.</li>
 *     <li><strong>setLayout(Layout layout)</strong> - Sets a new layout for the container.</li>
 *     <li><strong>add(Entity child)</strong> - Adds a child Entity to the container. This method is provided as a default convenience method.</li>
 *     <li><strong>remove(Entity child)</strong> - Removes a specified child Entity from the container. This method is provided as a default convenience method.</li>
 *     <li><strong>clear()</strong> - Clears all child Entitys from the container. This method is provided as a default convenience method.</li>
 * </ul>
 *
 * <h2>Usage Example:</h2>
 * <pre>
 * Container container = new SomeContainerImplementation();
 * Entity child = new SomeChildEntity();
 * container.add(child);  // Adds a child Entity
 * container.remove(child);  // Removes a child Entity
 * container.clear();  // Clears all child Entitys
 * </pre>
 *
 * <p>This interface can be implemented by concrete classes to define containers of various types,
 * such as panels, layouts, or any other component that holds and arranges child Entitys.</p>
 */
public interface Container extends Component {
    /**
     * Returns the {@link Entity} representing the container itself.
     *
     * @return the container's own {@link Entity}.
     */
    Entity entity();

}
