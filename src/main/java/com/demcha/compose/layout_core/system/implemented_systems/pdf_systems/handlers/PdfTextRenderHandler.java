package com.demcha.compose.layout_core.system.implemented_systems.pdf_systems.handlers;

import com.demcha.compose.layout_core.components.core.Entity;
import com.demcha.compose.layout_core.components.layout.coordinator.Placement;
import com.demcha.compose.layout_core.components.renderable.TextComponent;
import com.demcha.compose.layout_core.components.style.Padding;
import com.demcha.compose.layout_core.core.EntityManager;
import com.demcha.compose.layout_core.system.implemented_systems.pdf_systems.PdfFont;
import com.demcha.compose.layout_core.system.implemented_systems.pdf_systems.PdfRenderingSystemECS;
import com.demcha.compose.layout_core.system.interfaces.Font;
import com.demcha.compose.layout_core.system.interfaces.guides.GuidesRenderer;
import com.demcha.compose.layout_core.system.rendering.RenderHandler;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;

import java.io.IOException;
import java.util.EnumSet;

/**
 * A render handler responsible for drawing single-line text entities onto a PDF document.
 * <p>
 * This class implements the {@link RenderHandler} interface specifically for
 * {@link TextComponent} entities within the context of a {@link PdfRenderingSystemECS}.
 * It manages the extraction of text properties (such as font, size, color, and padding)
 * from the entity's components, calculates the correct baseline position, and utilizes
 * Apache PDFBox to render the text onto the page's content stream.
 * <p>
 * It also optionally renders guide lines for margins and padding if requested.
 */
public final class PdfTextRenderHandler implements RenderHandler<TextComponent, PdfRenderingSystemECS> {
    private static final EnumSet<GuidesRenderer.Guide> DEFAULT_GUIDES =
            EnumSet.of(GuidesRenderer.Guide.MARGIN, GuidesRenderer.Guide.PADDING);

    /**
     * Returns the class type of the render component this handler is responsible for.
     *
     * @return The {@link TextComponent} class.
     */
    @Override
    public Class<TextComponent> renderType() {
        return TextComponent.class;
    }

    /**
     * Renders a text component associated with a specific entity onto the PDF stream.
     *
     * @param manager         The entity manager providing access to fonts and other shared resources.
     * @param entity          The entity containing the text component and its layout/style properties.
     * @param renderComponent The specific {@link TextComponent} to be rendered.
     * @param renderingSystem The PDF rendering system coordinating the document generation.
     * @param guideLines      If {@code true}, debug guide lines (margins, padding) will be drawn around the text.
     * @return {@code true} if the text was successfully rendered, {@code false} if required placement data is missing.
     * @throws IOException If an error occurs while writing to the PDF content stream.
     */
    @Override
    public boolean render(EntityManager manager,
                          Entity entity,
                          TextComponent renderComponent,
                          PdfRenderingSystemECS renderingSystem,
                          boolean guideLines) throws IOException {
        Placement position = entity.getComponent(Placement.class).orElse(null);
        if (position == null) {
            return false;
        }

        try (PDPageContentStream contentStream = renderingSystem.stream().openContentStream(entity)) {
            TextComponent.ValidatedTextData data = TextComponent.validatedTextData(entity);
            Font<PDFont> font = manager.getFonts().getFont(data.style().fontName(), PdfFont.class).orElseThrow();
            PDFont pdfFont = font.fontType(data.style().decoration());
            Padding padding = entity.getComponent(Padding.class).orElse(Padding.zero());
            PdfFont.VerticalMetrics metrics = resolveVerticalMetrics(font, data.style());

            float baselineX = (float) position.x() + (float) padding.left();
            float baselineY = (float) position.y()
                    + (float) padding.bottom()
                    + (float) metrics.baselineOffsetFromBottom();

            contentStream.saveGraphicsState();
            try {
                contentStream.setFont(pdfFont, (float) data.style().size());
                contentStream.setNonStrokingColor(data.style().color());
                contentStream.beginText();
                contentStream.newLineAtOffset(baselineX, baselineY);
                contentStream.showText(data.textValue().value());
                contentStream.endText();
            } finally {
                contentStream.restoreGraphicsState();
            }

            if (guideLines) {
                renderingSystem.guidesRenderer().guidesRender(entity, contentStream, DEFAULT_GUIDES);
            }
        }

        return true;
    }

    /**
     * Resolves the vertical metrics (such as baseline offset) for a given font and text style.
     *
     * @param font  The generic font interface retrieved from the entity manager.
     * @param style The text style detailing specific font settings like size or decoration.
     * @return The specific {@link PdfFont.VerticalMetrics} required to correctly position the text vertically.
     * @throws IllegalStateException If the provided font is not an instance of {@link PdfFont}.
     */
    private PdfFont.VerticalMetrics resolveVerticalMetrics(Font<PDFont> font, com.demcha.compose.layout_core.components.content.text.TextStyle style) {
        if (font instanceof PdfFont pdfFont) {
            return pdfFont.verticalMetrics(style);
        }
        throw new IllegalStateException("Expected PdfFont for PDF text rendering but got " + font.getClass().getName());
    }
}
