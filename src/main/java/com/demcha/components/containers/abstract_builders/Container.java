package com.demcha.components.containers.abstract_builders;

import com.demcha.components.core.Component;
import com.demcha.components.core.Entity;
import com.demcha.components.layout.GuidesRenderer;
import com.demcha.components.layout.ParentComponent;
import com.demcha.system.PdfRender;
import org.apache.pdfbox.pdmodel.PDPageContentStream;

import java.io.IOException;
import java.util.EnumSet;
import java.util.Set;

/**
 * Interface representing a container that holds and arranges child {@link Entity}s.
 * The container manages its own {@link Entity}, which represents the container itself,
 * as well as the list of child {@link Entity}s it contains. It also defines the layout used for arranging
 * the child {@link Entity}s within the container.
 *
 * <p>The container is responsible for providing methods to add, remove, and clear child Entitys,
 * as well as for setting the layout that determines how the entities are arranged.</p>
 *
 * <h2>Methods:</h2>
 * <ul>
 *     <li><strong>getEntity()</strong> - Returns the {@link Entity} representing the container itself.</li>
 *     <li><strong>getChildren()</strong> - Returns a set of the child {@link Entity}s within the container.</li>
 *     <li><strong>getLayout()</strong> - Returns the current layout of the container.</li>
 *     <li><strong>setLayout(Layout layout)</strong> - Sets a new layout for the container.</li>
 *     <li><strong>add(Entity child)</strong> - Adds a child {@link Entity} to the container. This method is provided as a default convenience method.</li>
 *     <li><strong>remove(Entity child)</strong> - Removes a specified child Entity from the container. This method is provided as a default convenience method.</li>
 *     <li><strong>clear()</strong> - Clears all child Entitys from the container. This method is provided as a default convenience method.</li>
 * </ul>
 *
 * <h2>Usage Example:</h2>
 * <pre>
 * Container container = new SomeContainerImplementation();
 * Entity child = new SomeChildEntity(); // Assume SomeChildEntity implements {@link Entity}
 * container.add(child);  // Adds a child {@link Entity}
 * container.remove(child);  // Removes a child {@link Entity}
 * container.clear();  // Clears all child {@link Entity}s
 * </pre>
 *
 * <p>This interface can be implemented by concrete classes to define containers of various types, such as panels, layouts, or any other component that holds and arranges child {@link Entity}s.</p>
 */
public class Container implements Component, PdfRender, GuidesRenderer {
    private static final EnumSet<Guide> DEFAULT_GUIDES =
            EnumSet.of(Guide.MARGIN, Guide.PADDING, Guide.BOX);

    @Override
    public boolean render(Entity e, PDPageContentStream cs, boolean guideLines) throws IOException {
        if (guideLines) renderGuides(e, cs, DEFAULT_GUIDES);
        return true;
    }
}
