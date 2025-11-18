package com.demcha.components.renderable;

import com.demcha.components.core.Entity;
import com.demcha.system.Expendable;
import com.demcha.system.GuidesRenderer;
import com.demcha.system.pdf_systems.PdfRender;
import com.demcha.system.pdf_systems.PdfRenderingSystemECS;
import org.apache.pdfbox.pdmodel.PDPageContentStream;

import java.io.IOException;
import java.util.EnumSet;

/**
 * This is simply an empty element that can hold anything (image, text, etc.) as a single instance.
 */
public class Element implements PdfRender,  Expendable {
    private static final EnumSet<GuidesRenderer.Guide> DEFAULT_GUIDES =
            EnumSet.of(GuidesRenderer.Guide.MARGIN, GuidesRenderer.Guide.PADDING, GuidesRenderer.Guide.BOX);

    /**
     * Renders the container component on the PDF content stream.
     * If {@code guideLines} is true, it also renders the default set of guides for the component.
     *
     * @param e          The {@link Entity} representing the component's data and properties.
     * @param pdfRenderingSystem        The {@link PDPageContentStream} to draw on.
     * @param guideLines A boolean indicating whether to render guide lines (margin, padding, box) for the component.
     * @return {@code true} if the rendering was successful, {@code false} otherwise.
     * @throws IOException If an I/O error occurs during rendering.
     */
    @Override
    public boolean pdf(Entity e, PdfRenderingSystemECS pdfRenderingSystem, boolean guideLines) throws IOException {
        TextComponent textComponent = new TextComponent();
        try (PDPageContentStream pdPageContentStream = pdfRenderingSystem.stream().openContentStream (e)) {
            if (guideLines) pdfRenderingSystem .guideRenderer().guidesRender(e,pdPageContentStream, DEFAULT_GUIDES);
        }

        return true;
    }

}
