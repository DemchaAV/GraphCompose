package com.demcha.compose.layout_core.system.interfaces.guides;

import com.demcha.compose.layout_core.components.content.shape.Stroke;
import com.demcha.compose.layout_core.components.core.Component;
import com.demcha.compose.layout_core.components.core.Entity;
import com.demcha.compose.layout_core.components.layout.RenderCoordinate;
import com.demcha.compose.layout_core.components.layout.coordinator.Placement;
import com.demcha.compose.layout_core.components.layout.coordinator.RenderCoordinateContext;
import com.demcha.compose.layout_core.exceptions.RenderGuideLinesException;
import com.demcha.compose.layout_core.system.GuidLineSettings;
import com.demcha.compose.layout_core.system.interfaces.RenderPassSession;
import com.demcha.compose.layout_core.system.interfaces.RenderingSystemECS;
import com.demcha.compose.layout_core.system.utils.page_breaker.PageOutOfBoundException;
import lombok.Data;
import lombok.NonNull;
import lombok.ToString;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Renders margin, padding, and box guides for resolved entities.
 *
 * <p>The renderer prefers the currently active render session when guide drawing
 * happens during a render pass. If guide rendering is invoked outside an active
 * pass, it falls back to a short-lived backend session so the public guide API
 * remains usable.</p>
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

    @NotNull
    private static RenderCoordinateContext update(RenderCoordinateContext source, double y, double height, int page) {
        return new RenderCoordinateContext(
                source.x(),
                y,
                source.width(),
                height,
                page,
                page,
                source.stroke(),
                source.color());
    }

    protected <T extends RenderCoordinate & Component> Optional<RenderCoordinateContext> resolveCoordinateContext(
            Entity entity,
            @NonNull GuidLineSettings guidLineSettings,
            Class<T> componentClass,
            Supplier<T> defaultSupplier
    ) {
        if (!renderingSystem.guidLineSettings().showOnlySetGuide()) {
            return Optional.empty();
        }

        T context = entity.getComponent(componentClass)
                .orElseGet(() -> {
                    log.info("{} is {} ", componentClass, defaultSupplier.get());
                    return defaultSupplier.get();
                });

        return context.renderCoordinate(entity, renderingSystem());
    }

    public boolean guidesRender(Entity entity, S stream, EnumSet<Guide> guides) throws RenderGuideLinesException {
        boolean any = false;
        if (guides.contains(Guide.MARGIN)) {
            any |= margin().fromStream(entity, stream);
        }
        if (guides.contains(Guide.PADDING)) {
            any |= padding().fromStream(entity, stream);
        }
        if (guides.contains(Guide.BOX)) {
            any |= box().fromStream(entity, stream);
        }
        return any;
    }

    public boolean guidesRender(Entity entity, EnumSet<Guide> guides) {
        try {
            return withSession(session -> guidesRender(entity, guides, session));
        } catch (Exception ex) {
            log.error("Failed to render guides for entity {}", entity, ex);
            throw new RuntimeException("Rendering failed", ex);
        }
    }

    private boolean guidesRender(Entity entity,
                                 EnumSet<Guide> guides,
                                 RenderPassSession<S> session) throws Exception {
        Placement placement = entity.getComponent(Placement.class)
                .orElseThrow(() -> new IllegalStateException("Entity missing Placement component"));

        int startPage = placement.startPage();
        int endPage = placement.endPage();

        if (startPage == endPage || startPage == 0 && endPage == -1) {
            return guidesRender(entity, session.pageSurface(startPage), guides);
        }

        List<RenderCoordinateContext> boxFragments = breakCoordinate(Optional.of(boxCoordinate(entity)), startPage);
        List<RenderCoordinateContext> marginFragments = breakCoordinate(margin().margin(entity), startPage);
        List<RenderCoordinateContext> paddingFragments = breakCoordinate(padding().padding(entity), startPage);

        log.debug("Rendering spanned multiple pages for entity: {}. Start: {}, End: {}", entity, startPage, endPage);
        int boxFragmentIndex = 0;
        int marginFragmentIndex = 0;
        int paddingFragmentIndex = 0;

        for (int renderingPage = startPage; renderingPage >= endPage; renderingPage--) {
            if (renderingPage < 0) {
                log.error("Rendering page is out of bound, negative number: {}", renderingPage);
                throw new PageOutOfBoundException("Rendering page is out of bound, negative number: " + renderingPage);
            }

            RenderCoordinateContext currentBox =
                    boxFragmentIndex < boxFragments.size() ? boxFragments.get(boxFragmentIndex) : null;
            RenderCoordinateContext currentMargin =
                    marginFragmentIndex < marginFragments.size() ? marginFragments.get(marginFragmentIndex) : null;
            RenderCoordinateContext currentPadding =
                    paddingFragmentIndex < paddingFragments.size() ? paddingFragments.get(paddingFragmentIndex) : null;

            try {
                S stream = session.pageSurface(renderingPage);
                if (renderingPage == startPage) {
                    startGuidesFromStream(stream, currentBox, currentMargin, currentPadding);
                } else if (renderingPage == endPage) {
                    endGuidesFromStream(stream, currentBox, currentMargin, currentPadding);
                } else {
                    middleGuidesFromStream(stream, currentBox, currentMargin, currentPadding);
                }
            } catch (Exception ex) {
                log.error("Failed to render guides on multiple page for entity {} \n {}", entity, entity.printInfo(), ex);
                throw new RuntimeException(
                        String.format("Failed to render guides on multiple page %s for entity  \n %s ",
                                renderingPage,
                                entity.printInfo()),
                        ex);
            }

            boxFragmentIndex++;
            marginFragmentIndex++;
            paddingFragmentIndex++;
        }

        return true;
    }

    /**
     * Executes a guide-rendering action within a render session.
     *
     * <p>Prefers the currently active session from the rendering system. If no
     * session is active (e.g. guide rendering triggered outside a normal render
     * pass), a short-lived session is opened and closed automatically.</p>
     *
     * @param action the rendering action to execute within a session
     * @return {@code true} if the action rendered any guides
     * @throws Exception if the action or session lifecycle fails
     */
    private boolean withSession(SessionAction<S> action) throws Exception {
        Optional<RenderPassSession<S>> activeSession = renderingSystem.activeRenderSession();
        if (activeSession.isPresent()) {
            return action.render(activeSession.get());
        }
        try (RenderPassSession<S> session = renderingSystem.stream().openRenderPass()) {
            return action.render(session);
        }
    }

    private void middleGuidesFromStream(S stream,
                                        RenderCoordinateContext boxContext,
                                        RenderCoordinateContext marginContext,
                                        RenderCoordinateContext paddingContext) throws IOException {
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

    private void endGuidesFromStream(S stream,
                                     RenderCoordinateContext boxContext,
                                     RenderCoordinateContext marginContext,
                                     RenderCoordinateContext paddingContext) throws IOException {
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
        if (contextOptional.isEmpty()) {
            return Collections.emptyList();
        }

        RenderCoordinateContext sourceContext = contextOptional.get();
        List<RenderCoordinateContext> resultSegments = new ArrayList<>();
        float canvasHeight = renderingSystem().canvas().height();

        double remainingHeight = sourceContext.height();
        double currentY = sourceContext.y();
        int currentPage = pages - 1;

        while (remainingHeight > 0) {
            double heightOnThisPage = currentY + remainingHeight <= canvasHeight
                    ? remainingHeight
                    : canvasHeight - currentY;

            resultSegments.add(update(sourceContext, currentY, heightOnThisPage, currentPage));

            remainingHeight -= heightOnThisPage;
            currentY = 0;
            currentPage--;
        }

        return resultSegments;
    }

    private void startGuidesFromStream(S stream,
                                       RenderCoordinateContext boxContext,
                                       RenderCoordinateContext marginContext,
                                       RenderCoordinateContext paddingContext) throws IOException {
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
    private RenderCoordinateContext boxCoordinate(Entity entity) {
        Stroke stroke = renderingSystem().guidLineSettings().BOX_STROKE();
        Color color = renderingSystem().guidLineSettings().BOX_COLOR();
        return RenderCoordinateContext.createBox(entity, stroke, color);
    }

    /**
     * Functional callback for guide-rendering operations that require a render session.
     *
     * @param <S> backend-specific surface type
     */
    @FunctionalInterface
    private interface SessionAction<S extends AutoCloseable> {
        boolean render(RenderPassSession<S> session) throws Exception;
    }

    /**
     * Types of guides that can be rendered around an entity.
     */
    public enum Guide {
        MARGIN,
        PADDING,
        BOX
    }
}
