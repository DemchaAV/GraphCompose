package com.demcha.components.content.text;

import com.demcha.components.core.Component;
import com.demcha.components.core.Entity;
import com.demcha.components.geometry.ContentSize;
import com.demcha.components.layout.ComputedPosition;
import com.demcha.system.PdfRender;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.io.IOException;
import java.util.Optional;

@Slf4j
public record Text(String text, TextStyle textStyle) implements Component, PdfRender {

    public static Text of(String text) {
        return new Text(text, TextStyle.standard14("HELVETICA", 15, TextDecoration.DEFAULT));
    }

    public static <T extends Text> ContentSize autoMeasureText(T textComponent) throws IOException {
        double width = textComponent.textStyle().getTextWidth(textComponent.text());
        double height = textComponent.textStyle().font().getFontDescriptor().getCapHeight();
        return new ContentSize(width, height);
    }

    @Override
    public boolean render(Entity e, PDPageContentStream cs) throws IOException {
        var textOpt = e.getComponent(Text.class);
        if (textOpt.isEmpty()) return false;
        var posOpt = e.getComponent(ComputedPosition.class);
        if (posOpt.isEmpty()) {
            log.warn("Text has no ComputedPosition; skipping: {}", e);
            return false;
        }

        var text = textOpt.get();
        var pos = posOpt.get();
        TextStyle style = text.textStyle();
        float size = style != null ? style.size() : 12f;

        log.debug("Rendering text '{}' at ({}, {}) size={}", text.text(), pos.x(), pos.y(), size);

        cs.saveGraphicsState();
        cs.setFont(style != null ? style.font() : new PDType1Font(Standard14Fonts.FontName.HELVETICA), size);
        cs.beginText();
        cs.newLineAtOffset((float) pos.x(), (float) pos.y());
        cs.showText(text.text());
        cs.endText();
        cs.restoreGraphicsState();
        return true;
    }

    public Optional<ContentSize> autoMeasureText() throws IOException {
        double width = textStyle.getTextWidth(text);
        double height = textStyle.getTextHeight(text);
        return Optional.of(new ContentSize(width, height));
    }

}
