package com.demcha.components.renderable;

import com.demcha.components.containers.abstract_builders.GuidesRenderer;
import com.demcha.components.core.Entity;
import com.demcha.system.PdfRender;
import org.apache.pdfbox.pdmodel.PDPageContentStream;

import java.io.IOException;
import java.util.EnumSet;

/**
 * This is simply an empty element that can hold anything (image, text, etc.) as a single instance.
 */
public class Element implements PdfRender, GuidesRenderer {
    private static final EnumSet<Guide> DEFAULT_GUIDES =
            EnumSet.of(Guide.MARGIN, Guide.PADDING, Guide.BOX);

    /**
     * Renders the container component on the PDF content stream.
     * If {@code guideLines} is true, it also renders the default set of guides for the component.
     *
     * @param e          The {@link Entity} representing the component's data and properties.
     * @param cs         The {@link PDPageContentStream} to draw on.
     * @param guideLines A boolean indicating whether to render guide lines (margin, padding, box) for the component.
     * @return {@code true} if the rendering was successful, {@code false} otherwise.
     * @throws IOException If an I/O error occurs during rendering.
     */
    @Override
    public boolean pdfRender(Entity e, PDPageContentStream cs, boolean guideLines) throws IOException {
        if (guideLines) renderGuides(e, cs, DEFAULT_GUIDES);
        return true;
    }

}
