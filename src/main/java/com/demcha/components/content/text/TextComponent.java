package com.demcha.components.content.text;

import com.demcha.components.core.Component;
import com.demcha.components.core.Entity;
import com.demcha.components.geometry.ContentSize;
import com.demcha.components.layout.GuidesRenderer;
import com.demcha.components.layout.coordinator.ComputedPosition;
import com.demcha.components.layout.coordinator.Position;
import com.demcha.components.layout.coordinator.RenderingPosition;
import com.demcha.components.style.Padding;
import com.demcha.system.PdfRender;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDPageContentStream;

import java.io.IOException;
import java.util.EnumSet;
import java.util.Optional;

@Slf4j
@Builder
public record TextComponent() implements Component, PdfRender, GuidesRenderer {
    private static final EnumSet<Guide> DEFAULT_GUIDES =
            EnumSet.of(Guide.MARGIN, Guide.PADDING);


    public static <T extends TextComponent> ContentSize autoMeasureText(Entity entity) throws IOException {
        Text text = entity.getComponent(Text.class).orElseThrow();
        TextStyle style = entity.getComponent(TextStyle.class).orElseThrow();

        double width = style.getTextWidth(text.value());
        double height = style.getTextHeight(text.value());
        return new ContentSize(width, height);
    }

    @Override
    public boolean render(Entity e, PDPageContentStream cs, boolean guideLine) throws IOException {

        var textOpt = e.getComponent(TextComponent.class);
        if (textOpt.isEmpty()) return false;
        var posOpt = e.getComponent(ComputedPosition.class);
        if (posOpt.isEmpty()) {
            log.warn("TextComponent has no ComputedPosition; skipping: {}", e);
            return false;
        }

        var text = e.getComponent(Text.class).orElseThrow();
        var pos = posOpt.get();
        RenderingPosition position = RenderingPosition.from(e);
        TextStyle style = e.getComponent(TextStyle.class).orElseThrow();
        Text textValue = e.getComponent(Text.class).orElseThrow();
        Padding padding = e.getComponent(Padding.class).orElse(Padding.zero());

        float size = style != null ? style.size() : 12f;
        double textHeight = style.getTextHeight(text.value());
        double different = textHeight > size ? textHeight - size : 0;

        log.debug("Rendering text '{}' at ({}, {}) size={}", textValue, pos.x(), pos.y(), size);
        log.debug("{}", style);

        cs.saveGraphicsState();
        cs.setFont(style.font(), size);
        cs.setNonStrokingColor(style.color());
        cs.beginText();
        cs.newLineAtOffset((float) position.x() + (float) padding.left(), (float) position.y() + (float) (different*2) + (float) padding.bottom());
        cs.showText(textValue.value());
        cs.endText();
        cs.restoreGraphicsState();
        if (guideLine) {
            renderGuides(e, cs, DEFAULT_GUIDES);
        }

        return true;
    }



}
