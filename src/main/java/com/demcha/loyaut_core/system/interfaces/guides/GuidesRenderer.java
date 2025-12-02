package com.demcha.loyaut_core.system.interfaces.guides;

import com.demcha.loyaut_core.components.content.shape.Stroke;
import com.demcha.loyaut_core.components.core.Component;
import com.demcha.loyaut_core.components.core.Entity;
import com.demcha.loyaut_core.components.layout.RenderCoordinate;
import com.demcha.loyaut_core.components.layout.coordinator.Placement;
import com.demcha.loyaut_core.components.layout.coordinator.RenderCoordinateContext;
import com.demcha.loyaut_core.exceptions.RenderGuideLinesException;
import com.demcha.loyaut_core.system.GuidLineSettings;
import com.demcha.loyaut_core.system.interfaces.RenderingSystemECS;
import com.demcha.loyaut_core.system.utils.page_breaker.PageOutOfBoundException;
import lombok.Data;
import lombok.NonNull;
import lombok.ToString;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.io.IOException;
import java.util.*;
import java.util.List;
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
    @ToString.Exclude
    protected final RenderingSystemECS<S> renderingSystem;
    @ToString.Exclude
    protected final BoxRender<S> box;
    @ToString.Exclude
    protected final MarginRender<S> margin;
    @ToString.Exclude
    protected final PaddingRender<S> padding;

    private static RenderGuideLinesException rethrowAsGuideLinesException(IOException io, String message) throws RenderGuideLinesException {
        return new RenderGuideLinesException(message, io);
    }

    @NotNull
    private static RenderCoordinateContext update(RenderCoordinateContext contextBreakable, double y, double height, int page) {
        return new RenderCoordinateContext(contextBreakable.x(), y, contextBreakable.width(), height, page, page, contextBreakable.stroke(), contextBreakable.color());
    }

    protected <T extends RenderCoordinate & Component>
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
    public boolean guidesRender(Entity e, EnumSet<Guide> guides) {
        // Java 17+ var usage
        var placement = e.getComponent(Placement.class)
                .orElseThrow(() -> new IllegalStateException("Entity missing Placement component"));

        var startPage = placement.startPage();
        var endPage = placement.endPage();

        // Single page scenario
        if (startPage == endPage || startPage == 0 && endPage == -1) {
            try (var stream = renderingSystem.stream().openContentStream(e)) {
                return guidesRender(e, stream, guides);
            } catch (Exception ex) {
                log.error("Failed to render guides on single page for entity {}", e, ex);
                throw new RuntimeException("Rendering failed", ex);
            }
        }

        // Multi-page scenario
        // 1. Break coordinates into fragments (List<Context>)
        var boxFragments = breakCoordinate(Optional.of(boxCoordinate(e)), startPage);
        var marginFragments = breakCoordinate(margin().margin(e), startPage);
        var paddingFragments = breakCoordinate(padding().padding(e), startPage);

        int renderingPage = startPage;

        log.debug("Rendering spanned multiple pages for entity: {}. Start: {}, End: {}", e, startPage, endPage);
        int boxFragmentsSize = boxFragments.size() - 1;
        int marginFragmentSize = marginFragments.size() - 1;
        int paddingFragmentSize = paddingFragments.size() - 1;


        while (renderingPage >= endPage) {
            if (renderingPage < 0) {
                log.error("Rendering page is out of bound, negative number: {}", renderingPage);
                throw new PageOutOfBoundException("Rendering page is out of bound, negative number: " + renderingPage);
            }


            try (var stream = renderingSystem.stream().openContentStream(renderingPage)) {

                // 4. Retrieve correct fragments safely
                // Box is guaranteed by the check above
                var currentBox = (boxFragmentsSize < boxFragments.size() && boxFragmentsSize >= 0) ? boxFragments.get(boxFragmentsSize) : null;

                // Margin and Padding might be empty lists if the entity doesn't have them.
                // We use a safe check to return null if the list is empty or index is out of bounds.
                var currentMargin = (marginFragmentSize < marginFragments.size() && marginFragmentSize >= 0) ? marginFragments.get(marginFragmentSize) : null;
                var currentPadding = (paddingFragmentSize < paddingFragments.size() && paddingFragmentSize >= 0) ? paddingFragments.get(paddingFragmentSize) : null;

                // 5. Delegate rendering based on position
                if (renderingPage == startPage) {
                    try {
                        startGuidesFromStream(stream, currentBox, currentMargin, currentPadding);
                    } catch (Exception ex) {
                        throw new RuntimeException("Error during rendering Start page  " + renderingPage, ex);
                    }


                } else if (renderingPage == endPage) {
                    try {
                        endGuidesFromStream(stream, currentBox, currentMargin, currentPadding);
                    } catch (Exception ex) {
                        throw new RuntimeException("Error during rendering end page  " + renderingPage, ex);
                    }
                } else {
                    try {
                        middleGuidesFromStream(stream, currentBox, currentMargin, currentPadding);
                    } catch (Exception ex) {
                        throw new RuntimeException("Error during rendering midlle page  " + renderingPage, ex);
                    }
                }

            } catch (Exception ex) {
                log.error("Failed to render guides on multiple page for entity {} \n {}", e, e.printInfo(), ex);
                throw new RuntimeException(String.format("Failed to render guides on multiple page %s for entity  \n %s ", renderingPage, e.printInfo()), ex);
            }
            boxFragmentsSize--;
            marginFragmentSize--;
            paddingFragmentSize--;
            renderingPage--;
        }

        return true;
    }

    private void middleGuidesFromStream(S stream, RenderCoordinateContext boxContext, RenderCoordinateContext marginContext, RenderCoordinateContext paddingContext) throws IOException {

        if (marginContext != null) {

            margin().middleFromStream(marginContext, stream);
        }
        if (paddingContext != null) {

            padding().middleFromStream(paddingContext, stream);
        }
        if (boxContext != null) {

            box().middleFromStream(boxContext, stream);
        }

    }

    private void endGuidesFromStream(S stream, RenderCoordinateContext boxContext, RenderCoordinateContext marginContext, RenderCoordinateContext paddingContext) throws IOException {

        if (marginContext != null) {
            margin().endFromStream(marginContext, stream);
            margin().endMarkers(marginContext, stream);
        }
        if (paddingContext != null) {
            padding().endFromStream(paddingContext, stream);
            padding().endMarkers(paddingContext, stream);
        }
        if (boxContext != null) {

            box().endFromStream(boxContext, stream);
        }

    }


    public List<RenderCoordinateContext> breakCoordinate(Optional<RenderCoordinateContext> contextOptional, int pages) {
        // 1. Guard Clause: Handle empty Optional immediately
        if (contextOptional.isEmpty()) {
            return Collections.emptyList(); // Return immutable empty list
        }
        RenderCoordinateContext sourceContext = contextOptional.get();
        var resultSegments = new ArrayList<RenderCoordinateContext>();

        // 2. Setup rendering constants
        // assuming 'renderingSystem()' is available in this scope
        var canvas = renderingSystem().canvas();
        float canvasHeight = canvas.height();

        // 3. Logic Setup
        double initializedYPosition = sourceContext.y();
        double sourceHeight = sourceContext.height();
        double remainingHeight = sourceHeight;
        double currentY = initializedYPosition;
        int currentPage = pages;

        // 4. Loop to slice the content
        while (remainingHeight > 0) {
            double heightOnThisPage;
            // Check if the content fits on the current page from its starting Y
            if (currentY + remainingHeight <= canvasHeight) {
                heightOnThisPage = remainingHeight;
            } else {
                // Content spills over to the next page
                heightOnThisPage = canvasHeight - currentY;
            }

            resultSegments.add(update(sourceContext, currentY, heightOnThisPage, currentPage));

            remainingHeight -= heightOnThisPage;
            currentY = 0; // For subsequent pages, the content starts at the top.
            currentPage--;
        }

        return resultSegments.reversed();
    }

    private void startGuidesFromStream(S stream, RenderCoordinateContext boxContext, RenderCoordinateContext marginContext, RenderCoordinateContext paddingContext) throws IOException {

        if (marginContext != null) {

            margin().startFromStream(marginContext, stream);
            margin().startMarkers(marginContext, stream);
        }
        if (paddingContext != null) {
            padding().startFromStream(paddingContext, stream);
            padding().startMarkers(paddingContext, stream);

        }
        if (boxContext != null) {

            box().startFromStream(boxContext, stream);
        }


    }

    @NotNull
    private RenderCoordinateContext boxCoordinate(Entity e) {
        Stroke stroke = renderingSystem().guidLineSettings().BOX_STROKE();
        Color color = renderingSystem().guidLineSettings().BOX_COLOR();
        var context = RenderCoordinateContext.createBox(e, stroke, color);
        return context;
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
