package com.demcha.system.interfaces.guides;

import com.demcha.components.content.shape.Side;
import com.demcha.components.core.Entity;
import com.demcha.components.layout.coordinator.RenderCoordinateContext;
import com.demcha.exceptions.RenderGuideLinesException;
import com.demcha.system.interfaces.RenderingSystemECS;
import lombok.NonNull;

import java.io.IOException;
import java.util.Set;

/**
 * An interface for rendering coordinate guides for broken elements.
 *
 * @param <T> The type of the stream used for rendering.
 */
public interface GuideCoordinate<T extends AutoCloseable> {

    RenderingSystemECS<T> renderingSystem();

    // ---------------------------------------------------------
    // 2. The "Execute Around" Method (Handles the boilerplate)
    // ---------------------------------------------------------
    default boolean executeOnStream(@NonNull RenderCoordinateContext context, int pageNumber, StreamRenderer<T> action) throws RenderGuideLinesException {
        try (T stream = renderingSystem().stream().openContentStream(pageNumber)) {
            // 💡 This runs the specific logic passed in
            return action.render(context, stream);
        } catch (RenderGuideLinesException e) {
            throw e;
        } catch (IOException e) {
            throw new RenderGuideLinesException("Error occurred during an opening stream", e);
        } catch (Exception e) {
            throw new RenderGuideLinesException("Failed to close or process the stream", e);
        }
    }

    default boolean start(@NonNull RenderCoordinateContext context, int pageNumber) throws RenderGuideLinesException {
        // We pass the method reference 'this::startFromStream'
        return executeOnStream(context, pageNumber, this::startFromStream);
    }

    // ---------------------------------------------------------
    // 3. Public API (Clean and simple)
    // ---------------------------------------------------------

    default boolean middle(@NonNull RenderCoordinateContext context, int pageNumber) throws RenderGuideLinesException {
        return executeOnStream(context, pageNumber, this::middleFromStream);
    }

    default boolean end(@NonNull RenderCoordinateContext context, int pageNumber) throws RenderGuideLinesException {
        return executeOnStream(context, pageNumber, this::endFromStream);
    }

    default boolean startFromStream(@NonNull RenderCoordinateContext context, @NonNull T stream) throws RenderGuideLinesException, IOException {
        Set<Side> sides = Set.of(Side.LEFT, Side.RIGHT, Side.BOTTOM);
        return renderingSystem().renderBorder(stream, context, true, sides);
    }

    // ---------------------------------------------------------
    // 4. Logic Implementations (Focus only on drawing)
    // ---------------------------------------------------------

    default boolean middleFromStream(@NonNull RenderCoordinateContext context, @NonNull T stream) throws RenderGuideLinesException, IOException {
        Set<Side> sides = Set.of(Side.LEFT, Side.RIGHT);
        return renderingSystem().renderBorder(stream, context, true, sides);
    }

    default boolean endFromStream(@NonNull RenderCoordinateContext context, @NonNull T stream) throws RenderGuideLinesException, IOException {
        Set<Side> sides = Set.of(Side.LEFT, Side.RIGHT, Side.TOP);
        return renderingSystem().renderBorder(stream, context, true, sides);
    }

    // Existing methods kept as requested
    boolean fromStream(@NonNull Entity entity, T stream);

    default void renderMarkers(T stream, RenderCoordinateContext context) throws IOException {
        final float radius = 3.5f;
        float cx = (float) context.x();
        float cy = (float) context.y();
        renderingSystem().fillCircle(stream, cx, cy, radius, context.color());
        renderingSystem().fillCircle(stream, cx, cy + (float) context.width(), radius, context.color());
        renderingSystem().fillCircle(stream, cx + (float) context.width(), cy, radius, context.color());
        renderingSystem().fillCircle(stream, cx + (float) context.width(), cy + (float) context.height(), radius, context.color());
    }
}

// ---------------------------------------------------------
// 1. The Functional Interface (Contract for the logic)
// ---------------------------------------------------------
@FunctionalInterface
interface StreamRenderer<T> {
    boolean render(RenderCoordinateContext context, T stream) throws Exception;
}
