package com.demcha.system.implemented_systems.pdf_systems;

import com.demcha.components.core.Component;
import com.demcha.components.core.Entity;
import com.demcha.components.geometry.ContentSize;
import com.demcha.components.layout.RenderCoordinate;
import com.demcha.components.layout.coordinator.Placement;
import com.demcha.components.layout.coordinator.RenderCoordinateContext;
import com.demcha.components.style.Margin;
import com.demcha.components.style.Padding;
import com.demcha.exceptions.RenderGuideLinesException;
import com.demcha.system.GuidLineSettings;
import com.demcha.system.interfaces.GuidesRenderer;
import com.demcha.system.utils.page_breaker.Breakable;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDPageContentStream;

import java.awt.*;
import java.io.IOException;
import java.util.EnumSet;
import java.util.Optional;
import java.util.function.Supplier;

@Slf4j
public record PdfGuidesRenderer(PdfRenderingSystemECS renderingSystemECS) implements GuidesRenderer {
    private static RenderGuideLinesException rethrowAsGuideLinesException(IOException io, String message) throws RenderGuideLinesException {
        return new RenderGuideLinesException(message, io);
    }

    /**
     * Renders the middle part of a bounding box for a broken element.
     * This typically includes only the vertical sides of the box.
     *
     * @param entity     The entity to render.
     * @param pageNumber The page number on which to render.
     * @return {@code true} if rendering was successful.
     * @throws RenderGuideLinesException if a rendering error occurs.
     */
    @Override
    public boolean boxRenderMiddle(@NonNull Entity entity, int pageNumber) throws RenderGuideLinesException {
        return false;
    }

    /**
     * Renders the middle part of a padding guide for a broken element.
     *
     * @param entity     The entity to render.
     * @param pageNumber The page number on which to render.
     * @return {@code true} if rendering was successful.
     * @throws RenderGuideLinesException if a rendering error occurs.
     */
    @Override
    public boolean paddingRenderMiddle(@NonNull Entity entity, int pageNumber) throws RenderGuideLinesException {
        return false;
    }

    /**
     * Renders the middle part of a margin guide for a broken element.
     *
     * @param entity     The entity to render.
     * @param pageNumber The page number on which to render.
     * @return {@code true} if rendering was successful.
     * @throws RenderGuideLinesException if a rendering error occurs.
     */
    @Override
    public boolean marginRenderMiddle(@NonNull Entity entity, int pageNumber) throws RenderGuideLinesException {
        return false;
    }

    /**
     * Renders the final part of a margin guide for a broken element.
     * This typically excludes the top border.
     *
     * @param entity     The entity to render.
     * @param pageNumber The page number on which to render.
     * @return {@code true} if rendering was successful.
     * @throws RenderGuideLinesException if a rendering error occurs.
     */
    @Override
    public boolean marginRenderEnd(@NonNull Entity entity, int pageNumber) throws RenderGuideLinesException {
        return false;
    }

    /**
     * Renders the final part of a padding guide for a broken element.
     *
     * @param entity     The entity to render.
     * @param pageNumber The page number on which to render.
     * @return {@code true} if rendering was successful.
     * @throws RenderGuideLinesException if a rendering error occurs.
     */
    @Override
    public boolean paddingRenderEnd(@NonNull Entity entity, int pageNumber) throws RenderGuideLinesException {
        return false;
    }

    /**
     * Renders the final part of a bounding box for a broken element.
     *
     * @param entity     The entity to render.
     * @param pageNumber The page number on which to render.
     * @return {@code true} if rendering was successful.
     * @throws RenderGuideLinesException if a rendering error occurs.
     */
    @Override
    public boolean boxRenderEnd(@NonNull Entity entity, int pageNumber) throws RenderGuideLinesException {
        return false;
    }

    /**
     * Renders the starting part of a bounding box for a broken element.
     * This typically excludes the bottom border.
     *
     * @param entity     The entity to render.
     * @param pageNumber The page number on which to render.
     * @return {@code true} if rendering was successful.
     * @throws RenderGuideLinesException if a rendering error occurs.
     */
    @Override
    public boolean boxRenderStart(@NonNull Entity entity, int pageNumber) throws RenderGuideLinesException {
        return false;
    }

    /**
     * Renders the starting part of a padding guide for a broken element.
     *
     * @param entity     The entity to render.
     * @param pageNumber The page number on which to render.
     * @return {@code true} if rendering was successful.
     * @throws RenderGuideLinesException if a rendering error occurs.
     */
    @Override
    public boolean paddingRenderStart(@NonNull Entity entity, int pageNumber) throws RenderGuideLinesException {
        return false;
    }

    /**
     * Renders the starting part of a margin guide for a broken element.
     *
     * @param entity     The entity to render.
     * @param pageNumber The page number on which to render.
     * @return {@code true} if rendering was successful.
     * @throws RenderGuideLinesException if a rendering error occurs.
     */
    @Override
    public boolean marginRenderStart(@NonNull Entity entity, int pageNumber) throws RenderGuideLinesException {
        return false;
    }

    @Override
    public boolean boxRender(Entity e) throws RenderGuideLinesException {

        try (PDPageContentStream cs = renderingSystemECS.stream().openContentStream(e)) {
            return boxRenderFromStream(e, cs);
        } catch (IOException ex) {
            String msg = "Error opening content stream for Box render";
            log.error(msg, ex);
            throw rethrowAsGuideLinesException(ex, msg);

        }

    }

    @Override
    public boolean paddingRender(Entity e) throws RenderGuideLinesException {

        try (PDPageContentStream cs = renderingSystemECS.stream().openContentStream(e)) {
            return renderPaddingFromStream(e, cs);
        } catch (IOException ex) {
            String msg = "Error opening content stream for Padding render";
            log.error(msg, ex);
            throw rethrowAsGuideLinesException(ex, msg); // Stop execution
        }

    }

    @Override
    public boolean marginRender(Entity e) throws RenderGuideLinesException {

        // 3. Prepare the PDF "canvas"
        try (PDPageContentStream cs = renderingSystemECS.stream().openContentStream(e)) {
            return renderMarginFromStream(e, cs);

        } catch (IOException ex) {
            String msg = "Error opening content stream for marginRender";
            log.error(msg, ex);
            throw rethrowAsGuideLinesException(ex, msg); // Stop execution
        }

    }

    public <T extends Component> boolean guidesRender(Entity entity, EnumSet<Guide> defaultGuides, T component) throws RenderGuideLinesException {
        try (PDPageContentStream pdPageContentStream = renderingSystemECS.stream().openContentStream(entity)) {
            return guidesRender(entity, pdPageContentStream, defaultGuides);
        } catch (IOException e) {
            throw rethrowAsGuideLinesException(e, "Error opening content stream for Guides render component :" + component.getClass().getSimpleName());
        }
    }


    private void renderMarkers(PDPageContentStream cs, double x, double y, double w, double h, Color color) throws IOException {
        final float radius = 3.5f;
        float cx = (float) x;
        float cy = (float) y;
        renderingSystemECS.fillCircle(cs, cx, cy, radius, color);
        renderingSystemECS.fillCircle(cs, cx, cy + (float) h, radius, color);
        renderingSystemECS.fillCircle(cs, cx + (float) w, cy, radius, color);
        renderingSystemECS.fillCircle(cs, cx + (float) w, cy + (float) h, radius, color);
    }

    private boolean boxRenderFromStream(@NonNull Entity e, @NonNull PDPageContentStream cs) throws RenderGuideLinesException {
        var boxSize = e.getComponent(ContentSize.class).orElseThrow();
        var placement = e.getComponent(Placement.class).orElseThrow();
        double x = placement.x();
        double y = placement.y();
        double w = boxSize.width();
        double h = boxSize.height();

        //  Using constants for configuration

        try {
            return renderingSystemECS.renderRectangle(renderingSystemECS.guidLineSettings().BOX_STROKE(), cs, x, y, w, h, renderingSystemECS.guidLineSettings().BOX_COLOR(), false);
        } catch (IOException ex) {
            throw rethrowAsGuideLinesException(ex, "An error occurred while rendering box guide lines.");
        }

    }

    public boolean guidesRender(Entity e, PDPageContentStream cs, EnumSet<Guide> guides) throws RenderGuideLinesException {
        boolean any = false;

        if (guides.contains(Guide.MARGIN)) any |= renderMarginFromStream(e, cs);
        if (guides.contains(Guide.PADDING)) any |= renderPaddingFromStream(e, cs);
        if (guides.contains(Guide.BOX)) any |= boxRenderFromStream(e, cs);
        return any;
    }


    public boolean guidesRender(Entity e, PdfRenderingSystemECS renderingSystemECS, EnumSet<Guide> guides) throws RenderGuideLinesException {
        if (e.hasAssignable(Breakable.class)) {
            Placement placement = e.getComponent(Placement.class)
                    .orElseThrow(() -> new IllegalArgumentException("Didn't find any Placement"));
            //top part;
            try (PDPageContentStream pdPageContentStream = renderingSystemECS.stream().openContentStream(placement.endPage())) {
                guidesRender(e, pdPageContentStream, guides);
                log.info("Render topParts has been completed.");

            } catch (IOException exception) {
                throw new RenderGuideLinesException("Rendering guideLine process was incorporated");


            }

            //bottom part
            try (PDPageContentStream pdPageContentStream = renderingSystemECS.stream().openContentStream(placement.startPage())) {
                guidesRender(e, pdPageContentStream, guides);
                log.info("Render bottom has been completed.");
                return true;
            } catch (IOException exception) {
                throw new RenderGuideLinesException("Rendering guideLine process was incorporated");


            }


        }

        return false;
    }


    private boolean renderMarginFromStream(Entity e, PDPageContentStream cs) throws RenderGuideLinesException {
        RenderCoordinateContext coordinateContext = null;
        Optional<RenderCoordinateContext> coordinateOpt =
                resolveCoordinateContext(e, renderingSystemECS.guidLineSettings(), Margin.class, Margin::zero);

        if (coordinateOpt.isEmpty()) {
            return false;
        } else {
            coordinateContext = coordinateOpt.get();
        }


        double x = coordinateContext.x() >= 0 ? coordinateContext.x() : 0;
        double y;
        double width;
        double height;
        y = (coordinateContext.y() >= 0) ? coordinateContext.y() : 0;
        if (renderingSystemECS.canvas() != null) {
            height = ((y + coordinateContext.height()) > renderingSystemECS.canvas().boundingTopLine())
                    ? renderingSystemECS.canvas().boundingTopLine() - y
                    : coordinateContext.height();
        }
        height = coordinateContext.height();
        width = coordinateContext.width();


        //  Using constants for configuration
        try {
            renderMarkers(cs, x, y, width, height, renderingSystemECS.guidLineSettings().MARGIN_COLOR());
            renderingSystemECS.renderRectangle(renderingSystemECS.guidLineSettings().MARGIN_STROKE(), cs, x, y, width, height, renderingSystemECS.guidLineSettings().MARGIN_COLOR(), true);
        } catch (IOException ex) {
            rethrowAsGuideLinesException(ex, "An error occurred while rendering margin guide lines.");

        }
        return true;
    }

    private <T extends RenderCoordinate & Component>
    Optional<RenderCoordinateContext> resolveCoordinateContext(
            Entity e,
            @NonNull GuidLineSettings guidLineSettings,
            Class<T> componentClass,
            Supplier<T> defaultSupplier
    ) {
        if (!renderingSystemECS.guidLineSettings().showOnlySetGuide()) {
            return Optional.empty();
        }

        // Берём компонент или default (Margin.zero() / Padding.zero())
        T context = e.getComponent(componentClass)
                .orElseGet(() -> {
                    log.info("{} is {} ", componentClass, defaultSupplier.get());
                    return defaultSupplier.get();
                });

        return context.renderCoordinate(e);
    }

    private boolean renderPaddingFromStream(Entity e, PDPageContentStream cs) throws RenderGuideLinesException {


        RenderCoordinateContext coordinateContext;
        Optional<RenderCoordinateContext> coordinateOpt =
                resolveCoordinateContext(e, renderingSystemECS.guidLineSettings(), Padding.class, Padding::zero);

        if (coordinateOpt.isEmpty()) {
            return false;
        } else {
            coordinateContext = coordinateOpt.get();
        }


        double x = coordinateContext.x();
        double y = coordinateContext.y();
        double width = coordinateContext.width();
        double height = coordinateContext.height();


        try {
            renderingSystemECS.renderRectangle(renderingSystemECS.guidLineSettings().PADDING_STROKE(), cs, x, y, width, height, renderingSystemECS.guidLineSettings().PADDING_COLOR(), true);
            renderMarkers(cs, x, y, width, height, renderingSystemECS.guidLineSettings().PADDING_COLOR());
        } catch (IOException io) {
            rethrowAsGuideLinesException(io, "An error occurred while rendering padding guide lines.");
        }


        return true;
    }
}
