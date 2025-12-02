package com.demcha.loyaut_core.system.implemented_systems;

import com.demcha.loyaut_core.components.components_builders.Canvas;
import com.demcha.loyaut_core.components.content.shape.Side;
import com.demcha.loyaut_core.components.layout.coordinator.RenderCoordinateContext;
import com.demcha.loyaut_core.core.EntityManager;
import com.demcha.loyaut_core.system.GuidLineSettings;
import com.demcha.loyaut_core.system.interfaces.RenderStream;
import com.demcha.loyaut_core.system.interfaces.RenderingSystemECS;
import com.demcha.loyaut_core.system.interfaces.guides.GuidesRenderer;
import lombok.Data;
import lombok.experimental.Accessors;

import java.awt.*;
import java.io.IOException;
import java.util.Set;

/**
 * Abstract base class for rendering systems within the Entity-Component-System (ECS) architecture.
 * This class provides common functionalities and abstract methods for rendering various graphical elements
 * such as borders, rectangles, and circles onto a rendering stream. It integrates with a {@link Canvas},
 * {@link GuidLineSettings}, and a {@link RenderStream} to manage rendering operations.
 *
 * @param <T> The type of the rendering stream, which must implement {@link AutoCloseable}.
 */
@Data
@Accessors(fluent = true)
public abstract class RenderingSystemBase<T extends AutoCloseable> implements RenderingSystemECS<T> {
    protected final Canvas canvas;
    protected final GuidLineSettings guidLineSettings;
    protected final RenderStream<T> stream;
    protected  GuidesRenderer<T> guidesRenderer;

    /**
     * Renders a border around a specified context.
     *
     * @param stream            The rendering stream to draw on.
     * @param context           The rendering coordinate context defining the area for the border.
     * @param lineDash          A boolean indicating whether the border should be dashed.
     * @param sides             A set of {@link Side} enums indicating which sides of the context to render the border.
     * @return {@code true} if the border was rendered successfully, {@code false} otherwise.
     * @throws IOException If an I/O error occurs during rendering.
     */
    public abstract boolean renderBorder(T stream, RenderCoordinateContext context,
                                         boolean lineDash,
                                         Set<Side> sides) throws IOException;

    /**
     * Renders a rectangle on the rendering stream.
     *
     * @param stream   The rendering stream to draw on.
     * @param context  The rendering coordinate context defining the rectangle's position and dimensions.
     * @param lineDash A boolean indicating whether the rectangle's outline should be dashed.
     * @return {@code true} if the rectangle was rendered successfully, {@code false} otherwise.
     * @throws IOException If an I/O error occurs during rendering.
     */
    @Override
    public abstract boolean renderRectangle(T stream, RenderCoordinateContext context, boolean lineDash) throws IOException;

    /**
     * Fills a circle with a specified color on the rendering stream.
     *
     * @param stream The rendering stream to draw on.
     * @param cx     The x-coordinate of the circle's center.
     * @param cy     The y-coordinate of the circle's center.
     * @param r      The radius of the circle.
     * @param fill   The {@link Color} to fill the circle with.
     */
    public abstract void fillCircle(T stream, float cx, float cy, float r, Color fill) throws IOException;


    /**
     * Processes entities within the {@link EntityManager} for rendering.
     * This method defines the core logic for how the rendering system interacts with entities
     * to perform its rendering tasks.
     *
     * @param entityManager The entity manager containing the entities to be processed.
     */
    public abstract void process(EntityManager entityManager);

    /**
     * Initializes the {@link GuidesRenderer}.
     * <p>
     * Subclasses must implement this to provide a concrete, format-specific renderer (e.g., for PDF or images).
     * This method is intended to be called in the constructor to ensure the renderer is immediately available.
     *
     * <pre>{@code
     * // Example implementation:
     * public PdfRenderingSystemECS(PDDocument doc, Canvas canvas) {
     * super(canvas, new GuidLineSettings(), new PdfStream(doc, canvas));
     * // Init and set the guides renderer
     * guidesRendererInitializer(new PdfGuidesRenderer(this));
     * }
     *
     * @Override
     * protected void guidesRendererInitializer(GuidesRenderer<PDPageContentStream> renderer) {
     * guidesRenderer(renderer); // Call base setter
     * }
     * }</pre>
     *
     * @param guidesRenderer The {@link GuidesRenderer} instance used to draw debug guides.
     */
    protected abstract void guidesRendererInitializer(GuidesRenderer<T> guidesRenderer);

}
