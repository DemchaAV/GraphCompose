package com.demcha.components.renderable;

import com.demcha.components.containers.abstract_builders.GuidesRenderer;
import com.demcha.components.content.text.Text;
import com.demcha.components.content.text.TextStyle;
import com.demcha.components.core.Entity;
import com.demcha.components.geometry.ContentSize;
import com.demcha.components.layout.coordinator.RenderingPosition;
import com.demcha.components.style.Padding;
import com.demcha.system.PdfRender;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDFontDescriptor;

import java.io.IOException;
import java.util.EnumSet;

@Slf4j
@Builder
@EqualsAndHashCode
@NoArgsConstructor
public class TextComponent implements PdfRender, GuidesRenderer {
    private static final EnumSet<Guide> DEFAULT_GUIDES =
            EnumSet.of(Guide.MARGIN, Guide.PADDING);


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
    public boolean pdfRender(Entity e, PDPageContentStream cs, PDDocument doc, int indexPage, boolean guideLines) throws IOException {
        if (!e.hasAssignable(TextComponent.class)) return false;

        var positionOpt = RenderingPosition.from(e);
        if (positionOpt.isEmpty()) return false;

        var position = positionOpt.get();
        Padding padding = e.getComponent(Padding.class).orElse(Padding.zero());

        ValidatedTextData v = getValidatedTextData(e);
        PDFont font = v.style().font();
        float fontSize =(float) v.style().size();
        String text = v.textValue().value();

        // ---- compute metrics once
        float scale = fontSize / 1000f;
        PDFontDescriptor fd = font.getFontDescriptor();

//        float ascentPx  = (fd != null ? fd.getAscent()  : font.getBoundingBox().getUpperRightY()) * scale;
        float descentPx = Math.abs((fd != null ? fd.getDescent() : font.getBoundingBox().getLowerLeftY()) * scale);
        // float leadingPx = (fd != null ? fd.getLeading() : 0) * scale; // use for multi-line spacing

        // ---- choose alignment rule for Y
        // If your position.y is the TOP edge of the text box:
        float topY = (float) position.y() - (float) padding.top();
        float baselineY = topY+descentPx;

        // If your position.x is the LEFT edge:
        float leftX = (float) position.x() + (float) padding.left();

        cs.saveGraphicsState();
        cs.setFont(font, fontSize);
        cs.setNonStrokingColor(v.style().color());

        cs.beginText();
        cs.newLineAtOffset(leftX, baselineY); // << baseline, no extra "different*2"
        cs.showText(text);
        cs.endText();

        cs.restoreGraphicsState();

        if (guideLines) renderGuides(e, cs, DEFAULT_GUIDES);
        return true;
    }


    public record ValidatedTextData(TextStyle style, Text textValue) {
    }


}
