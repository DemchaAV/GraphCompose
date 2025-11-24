package com.demcha.components.renderable;

import com.demcha.components.core.Entity;
import com.demcha.components.geometry.Expendable;
import com.demcha.system.interfaces.GuidesRenderer;
import com.demcha.system.implemented_systems.pdf_systems.PdfRender;
import com.demcha.system.implemented_systems.pdf_systems.PdfRenderingSystemECS;
import com.demcha.system.utils.page_breaker.Breakable;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.EnumSet;

/**
 * Represents a basic container component that can render itself and its guides on a Entity Manager.
 * This class serves as an abstract builder for more specific container types, providing
 * fundamental rendering capabilities and guide visualization.
 */
@Slf4j
@Getter
public class Container implements PdfRender, Expendable, Breakable {
    /**
     * A default set of guides to be rendered for this container.
     * By default, it includes MARGIN, PADDING, and BOX guides.
     */
    public static final EnumSet<GuidesRenderer.Guide> DEFAULT_GUIDES =
            EnumSet.of(GuidesRenderer.Guide.MARGIN, GuidesRenderer.Guide.PADDING, GuidesRenderer.Guide.BOX);

    /**
     * Renders the container component on the PDF content stream.
     * If {@code guideLines} is true, it also renders the default set of guides for the component.
     *
     * @param e          The {@link Entity} representing the component's data and properties.
     * @param guideLines A boolean indicating whether to render guide lines (margin, padding, box) for the component.
     * @throws IOException If an I/O error occurs during rendering.
     */
    @Override
    public boolean pdf(Entity e, PdfRenderingSystemECS renderingSystemECS, boolean guideLines) throws IOException {
        if (guideLines) return renderingSystemECS.guideRenderer().guidesRender(e, DEFAULT_GUIDES, this);
        return false;
    }


}
