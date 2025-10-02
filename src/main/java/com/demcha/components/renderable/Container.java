package com.demcha.components.renderable;

import com.demcha.components.containers.abstract_builders.GuidesRenderer;
import com.demcha.components.core.Entity;
import com.demcha.system.Expendable;
import com.demcha.system.pdf_systems.PdfRender;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPageContentStream;

import java.io.IOException;
import java.util.EnumSet;

/**
 * Represents a basic container component that can render itself and its guides on a Entity Manager.
 * This class serves as an abstract builder for more specific container types, providing
 * fundamental rendering capabilities and guide visualization.
 */
public class Container implements PdfRender, GuidesRenderer, Expendable {
    /**
     * A default set of guides to be rendered for this container.
     * By default, it includes MARGIN, PADDING, and BOX guides.
     */
    private static final EnumSet<Guide> DEFAULT_GUIDES =
            EnumSet.of(Guide.MARGIN, Guide.PADDING, Guide.BOX);

    /**
     * Renders the container component on the PDF content stream.
     * If {@code guideLines} is true, it also renders the default set of guides for the component.
     *
     * @param e The {@link Entity} representing the component's data and properties.
     * @param cs The {@link PDPageContentStream} to draw on.
     * @param guideLines A boolean indicating whether to render guide lines (margin, padding, box) for the component.
     * @return {@code true} if the rendering was successful, {@code false} otherwise.
     * @throws IOException If an I/O error occurs during rendering.
     */
    @Override
    public boolean pdfRender(Entity e, PDPageContentStream cs, PDDocument doc, int indexPage, boolean guideLines) throws IOException {
        if (guideLines) renderGuides(e, cs, DEFAULT_GUIDES);
        return true;
    }
}
