package com.demcha.loyaut_core.components.renderable;

import com.demcha.loyaut_core.components.core.Entity;
import com.demcha.loyaut_core.components.geometry.Expendable;
import com.demcha.loyaut_core.core.EntityManager;
import com.demcha.loyaut_core.system.implemented_systems.pdf_systems.PdfRender;
import com.demcha.loyaut_core.system.implemented_systems.pdf_systems.PdfRenderingSystemECS;
import com.demcha.loyaut_core.system.interfaces.guides.GuidesRenderer;
import org.apache.pdfbox.pdmodel.PDPageContentStream;

import java.io.IOException;
import java.util.EnumSet;

/**
 * This is simply an empty element that can hold anything (image, text, etc.) as a single instance.
 */
public class Element implements PdfRender, Expendable {
    private static final EnumSet<GuidesRenderer.Guide> DEFAULT_GUIDES =
            EnumSet.of(GuidesRenderer.Guide.MARGIN, GuidesRenderer.Guide.PADDING, GuidesRenderer.Guide.BOX);

    /**
     * Renders the container component on the PDF content stream.
     * If {@code guideLines} is true, it also renders the default set of guides for the component.
     *
     * @param e                  The {@link Entity} representing the component's data and properties.
     * @param pdfRenderingSystem The {@link PDPageContentStream} to draw on.
     * @param guideLines         A boolean indicating whether to render guide lines (margin, padding, box) for the component.
     * @return {@code true} if the rendering was successful, {@code false} otherwise.
     * @throws IOException If an I/O error occurs during rendering.
     */
    @Override
    public boolean pdf(EntityManager manager, Entity e, PdfRenderingSystemECS pdfRenderingSystem, boolean guideLines) throws IOException {

        if (guideLines) pdfRenderingSystem.guidesRenderer().guidesRender(e, DEFAULT_GUIDES);


        return true;
    }

}
