package com.demcha.compose.loyaut_core.components.renderable;

import com.demcha.compose.loyaut_core.components.content.text.Text;
import com.demcha.compose.loyaut_core.components.content.text.TextStyle;
import com.demcha.compose.loyaut_core.components.core.Entity;
import com.demcha.compose.loyaut_core.components.geometry.ContentSize;
import com.demcha.compose.loyaut_core.components.layout.coordinator.Placement;
import com.demcha.compose.loyaut_core.core.EntityManager;
import com.demcha.compose.loyaut_core.system.LayoutSystem;
import com.demcha.compose.loyaut_core.system.implemented_systems.pdf_systems.PdfFont;
import com.demcha.compose.loyaut_core.system.implemented_systems.pdf_systems.PdfRender;
import com.demcha.compose.loyaut_core.system.implemented_systems.pdf_systems.PdfRenderingSystemECS;
import com.demcha.compose.loyaut_core.system.interfaces.Font;
import com.demcha.compose.loyaut_core.system.interfaces.guides.GuidesRenderer;
import com.demcha.compose.loyaut_core.components.style.Padding;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;

import java.io.IOException;
import java.util.EnumSet;

@Slf4j
@Builder
@EqualsAndHashCode
@NoArgsConstructor
public class TextComponent implements PdfRender {
    private static final EnumSet<GuidesRenderer.Guide> DEFAULT_GUIDES =
            EnumSet.of(GuidesRenderer.Guide.MARGIN, GuidesRenderer.Guide.PADDING);


    public static ContentSize autoMeasureText(Entity entity, EntityManager entityManager) throws IOException {
        Text text = entity.getComponent(Text.class).orElseThrow();
        TextStyle style = entity.getComponent(TextStyle.class).orElseThrow();
        var aClass = entityManager.getSystems().getSystem(LayoutSystem.class).orElseThrow().getRenderingSystem().fontClazz();
        var font = (Font) entityManager.getFonts().getFont(style.fontName(), aClass).orElseThrow();
        double width = font.getTextWidth(style, text.value());
        double height = font.getLineHeight(style);
        log.debug("Auto-measured single-line text entity {} -> width={} height={} style={}",
                entity, width, height, style);
        return new ContentSize(width, height);
    }

    private static ValidatedTextData getValidatedTextData(Entity e) {
        var textValueOpt = e.getComponent(Text.class);
        var styleOpt = e.getComponent(TextStyle.class);

        Text textValue;
        TextStyle style;
        if (textValueOpt.isEmpty()) {
            log.info("TextComponent has no TextValue; skipping: {}", e);
            textValue = textValueOpt.orElse(Text.empty());
        } else {
            textValue = textValueOpt.get();
        }
        if (styleOpt.isEmpty()) {
            log.info("TextComponent has no TextStyle; skipping: {}", e);
            style = styleOpt.orElse(TextStyle.DEFAULT_STYLE);
        } else {
            style = styleOpt.get();
        }

        return new ValidatedTextData(style, textValue);
    }

    @Override
    public boolean pdf(EntityManager manager, Entity e, PdfRenderingSystemECS renderingSystemECS, boolean guideLines) throws IOException {
        if (!e.hasAssignable(TextComponent.class)) return false;

        var placementOpt = e.getComponent(Placement.class);
        if (placementOpt.isEmpty()) return false;

        try (PDPageContentStream cs = renderingSystemECS.stream().openContentStream(e)) {


            var position = placementOpt.get();

            ValidatedTextData v = getValidatedTextData(e);
            Font<PDFont> font = manager.getFonts().getFont(v.style().fontName(), PdfFont.class).orElseThrow();
            PDFont pdFont = font.fontType(v.style().decoration());
            float fontSize = (float) v.style().size();
            String text = v.textValue().value();
            Padding padding = e.getComponent(Padding.class).orElse(Padding.zero());
            PdfFont.VerticalMetrics metrics = resolveVerticalMetrics(font, v.style());

            // Placement.x/y describe the bottom-left corner of the content box.
            // The text itself starts inside that box after applying the entity's own padding.
            float contentLeftX = (float) position.x();
            float contentBottomY = (float) position.y();
            float leftX = contentLeftX + (float) padding.left();
            float baselineY = contentBottomY
                    + (float) padding.bottom()
                    + (float) metrics.baselineOffsetFromBottom();

            log.debug("Rendering single-line text entity {} -> placement=({}, {}) padding={} baseline=({}, {}) metrics={}",
                    e, contentLeftX, contentBottomY, padding, leftX, baselineY, metrics);

            cs.saveGraphicsState();
            cs.setFont(pdFont, fontSize);
            cs.setNonStrokingColor(v.style().color());

            cs.beginText();
            cs.newLineAtOffset(leftX, baselineY);
            cs.showText(text);
            cs.endText();

            cs.restoreGraphicsState();

            if (guideLines) renderingSystemECS.guidesRenderer().guidesRender(e, cs, DEFAULT_GUIDES);
        } catch (IOException ioe) {
            throw new IOException(ioe);
        }
        return true;
    }


    public record ValidatedTextData(TextStyle style, Text textValue) {
    }

    private static PdfFont.VerticalMetrics resolveVerticalMetrics(Font<PDFont> font, TextStyle style) {
        if (font instanceof PdfFont pdfFont) {
            return pdfFont.verticalMetrics(style);
        }
        throw new IllegalStateException("Expected PdfFont for PDF text rendering but got " + font.getClass().getName());
    }


}
