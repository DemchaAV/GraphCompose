package com.demcha.legacy.layout;

import com.demcha.components.geometry.BoxSize;
import com.demcha.components.core.Component;
import com.demcha.legacy.core.Element;
import lombok.NonNull;

import java.util.List;
import java.util.Objects;

/**
 * Interface representing a container that holds and arranges child elements.
 * The container manages its own {@link Element}, which represents the container itself,
 * as well as the list of child elements it contains. It also defines the layout used for arranging
 * the child elements within the container.
 *
 * <p>The container is responsible for providing methods to add, remove, and clear child elements,
 * as well as for setting the layout that determines how the elements are arranged.</p>
 *
 * <h2>Methods:</h2>
 * <ul>
 *     <li><strong>getElement()</strong> - Returns the {@link Element} representing the container itself.</li>
 *     <li><strong>getChildren()</strong> - Returns a list of the child {@link Element}s within the container.</li>
 *     <li><strong>getLayout()</strong> - Returns the current layout of the container.</li>
 *     <li><strong>setLayout(Layout layout)</strong> - Sets a new layout for the container.</li>
 *     <li><strong>add(Element child)</strong> - Adds a child element to the container. This method is provided as a default convenience method.</li>
 *     <li><strong>remove(Element child)</strong> - Removes a specified child element from the container. This method is provided as a default convenience method.</li>
 *     <li><strong>clear()</strong> - Clears all child elements from the container. This method is provided as a default convenience method.</li>
 * </ul>
 *
 * <h2>Usage Example:</h2>
 * <pre>
 * Container container = new SomeContainerImplementation();
 * Element child = new SomeChildElement();
 * container.add(child);  // Adds a child element
 * container.remove(child);  // Removes a child element
 * container.clear();  // Clears all child elements
 * </pre>
 *
 * <p>This interface can be implemented by concrete classes to define containers of various types,
 * such as panels, layouts, or any other component that holds and arranges child elements.</p>
 */
public interface Container extends Component {
    /**
     * Returns the {@link Element} representing the container itself.
     * @return the container's own {@link Element}.
     */
    Element getElement();

    /**
     * Returns a list of the child elements contained within this container.
     * @return the list of child {@link Element}s.
     */
    List<Element> getChildren();

    /**
     * Returns the layout used by this container to arrange its child elements.
     * @return the current layout of the container.
     */
    Layout getLayout();
    default void measure(MeasureCtx ctx) {
        getLayout().measure(this, ctx);
    }
    default void measure(@NonNull BoxSize boxSize) {
        double height = boxSize.height();
        double width = boxSize.width();
        getLayout().measure(this, new MeasureCtx(width, height));
    }

    default void arrange(ArrangeCtx ctx) {
        getLayout().arrange(this, ctx);
    }
    default BoxSize getSize(){
        return getElement().get(BoxSize.class).orElse(null);
    }

    /**
     * Sets a new layout for the container.
     * @param layout the new layout to set.
     */
    void setLayout(Layout layout);

    /**
     * Adds a child element to the container.
     * This is a default method that calls {@link #getChildren()} to add the child element.
     *
     * @param child the {@link Element} to add.
     */
    default void add(Element child) {
        getChildren().add(Objects.requireNonNull(child));
    }

    /**
     * Removes a specified child element from the container.
     * This is a default method that calls {@link #getChildren()} to remove the child element.
     *
     * @param child the {@link Element} to remove.
     * @return true if the child element was successfully removed, false otherwise.
     */
    default boolean remove(Element child) {
        return getChildren().remove(child);
    }

    /**
     * Clears all child elements from the container.
     * This is a default method that calls {@link #getChildren()} to clear the list of child elements.
     */
    default void clear() {
        getChildren().clear();
    }
}
