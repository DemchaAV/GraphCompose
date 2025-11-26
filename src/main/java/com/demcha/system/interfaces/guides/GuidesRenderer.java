package com.demcha.system.interfaces.guides;

import com.demcha.components.core.Component;
import com.demcha.components.core.Entity;
import com.demcha.components.layout.RenderCoordinate;
import com.demcha.components.layout.coordinator.Placement;
import com.demcha.components.layout.coordinator.RenderCoordinateContext;
import com.demcha.exceptions.RenderGuideLinesException;
import com.demcha.system.GuidLineSettings;
import com.demcha.system.implemented_systems.pdf_systems.PdfRenderingSystemECS;
import com.demcha.system.interfaces.RenderingSystemECS;
import com.demcha.system.utils.page_breaker.PageOutOfBoundException;
import lombok.Data;
import lombok.NonNull;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDPageContentStream;

import java.io.IOException;
import java.util.EnumSet;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * An interface for rendering visual debugging guides for layout components.
 * <p>
 * This renderer provides methods to draw visual representations of an {@link Entity}'s
 * margin, padding, and bounding box. It is designed to handle elements that may be
 * broken across multiple pages, providing distinct rendering logic for the start, middle,
 * and end parts of a broken element.
 * <p>
 * The primary entry point is {@link #guidesRender(Entity, EnumSet)}, which orchestrates
 * the rendering process based on the entity's {@link Placement} component.
 */
@Slf4j
@Data
@Accessors(fluent = true)
public abstract class GuidesRenderer<S extends AutoCloseable> {
    protected final RenderingSystemECS<S> renderingSystem;
    protected final BoxRender<S> box;
    protected final MarginRender<S> margin;
    protected final PaddingRender<S> padding;



    protected  <T extends RenderCoordinate & Component>
    Optional<RenderCoordinateContext> resolveCoordinateContext(
            Entity e,
            @NonNull GuidLineSettings guidLineSettings,
            Class<T> componentClass,
            Supplier<T> defaultSupplier
    ) {
        if (!renderingSystem.guidLineSettings().showOnlySetGuide()) {
            return Optional.empty();
        }

        // Берём компонент или default (Margin.zero() / Padding.zero())
        T context = e.getComponent(componentClass)
                .orElseGet(() -> {
                    log.info("{} is {} ", componentClass, defaultSupplier.get());
                    return defaultSupplier.get();
                });

        return context.renderCoordinate(e, renderingSystem());
    }

    public boolean guidesRender(Entity e, S stream, EnumSet<Guide> guides) throws RenderGuideLinesException {
        boolean any = false;

        if (guides.contains(Guide.MARGIN)) any |= margin().fromStream(e, stream);
        if (guides.contains(Guide.PADDING)) any |= padding().fromStream(e, stream);
        if (guides.contains(Guide.BOX)) any |= box().fromStream(e, stream);
        return any;

    }

    /**
     * The starting page for this element, counted from the end of the document.
     * For example, if a document has 3 pages and the element starts on the first page,
     * its startPage (from the end) would be 3.
     */
    public boolean guidesRender(Entity e, EnumSet<Guide> guides) throws IOException {
        var placement = e.getComponent(Placement.class).orElseThrow();
        // The starting page for this element, counted from the end of the document.
        // For example, if a document has 3 pages and the element starts on the first page,
        // its startPage (from the end) would be 3.
        var startPage = placement.startPage();
        var endPage = placement.endPage();
        if (startPage == endPage) {
            try (S stream = renderingSystem.stream().openContentStream(e)) {
                return guidesRender(e, stream, guides);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }


        } else {
            int i = startPage;

            while (i != endPage) {
                if (i < 0) {
                    throw new PageOutOfBoundException(i);
                }
                if (i == startPage) {
                    //StartRendering
                } else if (i == endPage) {
                    //EndRendering

                } else {
                    //MiddleRendering
                }
                i--;
            }


            return true;
        }


    }
    private static RenderGuideLinesException rethrowAsGuideLinesException(IOException io, String message) throws RenderGuideLinesException {
        return new RenderGuideLinesException(message, io);
    }


    /**
     * Enum representing the different types of visual guides that can be rendered.
     */
    public enum Guide {
        /**
         * Represents the margin area of an element.
         */
        MARGIN,
        /**
         * Represents the padding area of an element.
         */
        PADDING,
        /**
         * Represents the bounding box (content area) of an element.
         */
        BOX
    }


}
