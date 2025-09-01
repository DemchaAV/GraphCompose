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
        double height = style.getTextHeight(text.value());
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

        if (!e.hasAssignable(TextComponent.class)) {
            log.debug("Entity doesn't have TextComponent; skipping: {}", e);
            return false;
        }

        var positionOpt = RenderingPosition.from(e);
        Padding padding = e.getComponent(Padding.class).orElse(Padding.zero());
        if (positionOpt.isEmpty()) {
            log.warn("TextComponent has no RenderingPosition; skipping: {}", e);
            return false;
        }
        var position = positionOpt.get();

        ValidatedTextData validateText = getValidatedTextData(e);


        float size = validateText.style().size();
        double textHeight = validateText.style().getTextHeight(validateText.textValue().value());
        double different = textHeight > size ? textHeight - size : 0;

        log.debug("Rendering text '{}' at ({}, {}) size={}", validateText.textValue(), position.x(), position.y(), size);
        log.debug("{}", validateText.style());

        cs.saveGraphicsState();
        cs.setFont(validateText.style().font(), size);
        cs.setNonStrokingColor(validateText.style().color());
        cs.beginText();
        cs.newLineAtOffset((float) position.x() + (float) padding.left(), (float) position.y() + (float) (different * 2) + (float) padding.bottom());
        String value = validateText.textValue().value();


        cs.showText(value);

        cs.endText();
        cs.restoreGraphicsState();


        if (guideLines) {
            renderGuides(e, cs, DEFAULT_GUIDES);
        }

        return true;
    }

    public record ValidatedTextData(TextStyle style, Text textValue) {
    }


}
