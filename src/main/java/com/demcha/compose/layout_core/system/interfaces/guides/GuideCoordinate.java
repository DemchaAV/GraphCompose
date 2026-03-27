package com.demcha.compose.layout_core.system.interfaces.guides;

import com.demcha.compose.layout_core.components.content.shape.Side;
import com.demcha.compose.layout_core.components.core.Entity;
import com.demcha.compose.layout_core.components.layout.coordinator.RenderCoordinateContext;
import com.demcha.compose.layout_core.exceptions.RenderGuideLinesException;
import com.demcha.compose.layout_core.system.interfaces.RenderingSystemECS;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

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
        var startHeight = context.y();
        RenderCoordinateContext coordinateContext = new RenderCoordinateContext(context.x(), startHeight, context.width(), context.height(), context.startPage(), context.endPage(), context.stroke(), context.color());


        Set<Side> sides = Set.of(Side.LEFT, Side.RIGHT, Side.BOTTOM);

        boolean lineDash = !context.stroke().equals(
                renderingSystem().guidLineSettings().BOX_STROKE()
        );

        return renderingSystem().renderBorder(stream, coordinateContext, lineDash, sides);
    }

    default void startMarkers(@NotNull RenderCoordinateContext context, @NotNull T stream) throws IOException {
        final float radius = renderingSystem().guidLineSettings().MARKER_RADIUS();
        float cx = (float) context.x();
        float cy = (float) context.y();
        var color = context.color();
        float w = (float) context.width();

        renderingSystem().fillCircle(stream, cx, cy, radius, color);
        renderingSystem().fillCircle(stream, cx + w, cy, radius, color);
    }

    // ---------------------------------------------------------
    // 4. Logic Implementations (Focus only on drawing)
    // ---------------------------------------------------------

    default boolean middleFromStream(@NonNull RenderCoordinateContext context, @NonNull T stream) throws RenderGuideLinesException, IOException {
        Set<Side> sides = Set.of(Side.LEFT, Side.RIGHT);
        boolean lineDash = !context.stroke().equals(
                renderingSystem().guidLineSettings().BOX_STROKE()
        );

        renderingSystem().renderBorder(stream, context, lineDash, sides);

        return true;
    }

    default boolean endFromStream(@NonNull RenderCoordinateContext context, @NonNull T stream) throws RenderGuideLinesException, IOException {
        Set<Side> sides = Set.of(Side.LEFT, Side.RIGHT, Side.TOP);
        boolean lineDash = !context.stroke().equals(
                renderingSystem().guidLineSettings().BOX_STROKE()
        );
        renderingSystem().renderBorder(stream, context, lineDash, sides);
        return true;
    }

    default void endMarkers(@NotNull RenderCoordinateContext context, @NotNull T stream) throws IOException {
        final float radius = renderingSystem().guidLineSettings().MARKER_RADIUS();
        float cx = (float) context.x();
        float cy = (float) context.y();
        var color = context.color();
        float w = (float) context.width();
        float h = (float) context.height();

        renderingSystem().fillCircle(stream, cx, cy + h, radius, color);
        renderingSystem().fillCircle(stream, cx + w, cy + h, radius, color);
    }

    // Existing methods kept as requested
    boolean fromStream(@NonNull Entity entity, T stream);

    default void markers(T stream, RenderCoordinateContext context) throws IOException {
        final float radius = renderingSystem().guidLineSettings().MARKER_RADIUS();
        float cx = (float) context.x();
        float cy = (float) context.y();
        var color = context.color();
        float w = (float) context.width();
        float h = (float) context.height();

        renderingSystem().fillCircle(stream, cx, cy, radius, color);
        renderingSystem().fillCircle(stream, cx, cy + h, radius, color);
        renderingSystem().fillCircle(stream, cx + w, cy, radius, color);
        renderingSystem().fillCircle(stream, cx + w, cy + h, radius, color);
    }

    // ---------------------------------------------------------
    // 1. The Functional Interface (Contract for the logic)
    // ---------------------------------------------------------
    @FunctionalInterface
    interface StreamRenderer<T> {
        boolean render(RenderCoordinateContext context, T stream) throws Exception;
    }
}


