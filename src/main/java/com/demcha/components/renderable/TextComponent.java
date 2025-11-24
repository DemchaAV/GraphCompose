package com.demcha.components.renderable;

import com.demcha.components.content.text.Text;
import com.demcha.components.content.text.TextStyle;
import com.demcha.components.core.Entity;
import com.demcha.components.geometry.ContentSize;
import com.demcha.components.layout.coordinator.Placement;
import com.demcha.system.interfaces.GuidesRenderer;
import com.demcha.system.implemented_systems.pdf_systems.PdfRender;
import com.demcha.system.implemented_systems.pdf_systems.PdfRenderingSystemECS;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDFontDescriptor;

import java.io.IOException;
import java.util.EnumSet;

@Slf4j
@Builder
@EqualsAndHashCode
@NoArgsConstructor
public class TextComponent implements PdfRender {
    private static final EnumSet<GuidesRenderer.Guide> DEFAULT_GUIDES =
            EnumSet.of(GuidesRenderer.Guide.MARGIN, GuidesRenderer.Guide.PADDING);


    public static <T extends TextComponent> ContentSize autoMeasureText(Entity entity) throws IOException {
        Text text = entity.getComponent(Text.class).orElseThrow();
        TextStyle style = entity.getComponent(TextStyle.class).orElseThrow();

        double width = style.getTextWidth(text.value());
        double height = style.getLineHeight();
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
            style = styleOpt.orElse(TextStyle.defaultStyle());
        } else {
            style = styleOpt.get();
        }

        return new ValidatedTextData(style, textValue);
    }

    @Override
    public boolean pdf(Entity e, PdfRenderingSystemECS renderingSystemECS, boolean guideLines) throws IOException {
        if (!e.hasAssignable(TextComponent.class)) return false;

        var placementOpt = e.getComponent(Placement.class);
        if (placementOpt.isEmpty()) return false;

        try (PDPageContentStream cs = renderingSystemECS.stream().openContentStream(e)) {


            var position = placementOpt.get();

            ValidatedTextData v = getValidatedTextData(e);
            PDFont font = v.style().font();
            float fontSize = (float) v.style().size();
            String text = v.textValue().value();

            // ---- compute metrics once
            float scale = fontSize / 1000f;
            PDFontDescriptor fd = font.getFontDescriptor();

            float descentPx = Math.abs((fd != null ? fd.getDescent() : font.getBoundingBox().getLowerLeftY()) * scale);

            float topY = (float) position.y();
            float baselineY = topY + descentPx;


            float leftX = (float) position.x();

            cs.saveGraphicsState();
            cs.setFont(font, fontSize);
            cs.setNonStrokingColor(v.style().color());

            cs.beginText();
            cs.newLineAtOffset(leftX, baselineY); // << baseline, no extra "different*2"
            cs.showText(text);
            cs.endText();

            cs.restoreGraphicsState();

            if (guideLines) renderingSystemECS.guideRenderer().guidesRender(e, cs, DEFAULT_GUIDES);
        } catch (IOException ioe) {
            throw new IOException(ioe);
        }
        return true;
    }


    public record ValidatedTextData(TextStyle style, Text textValue) {
    }


}
