package com.demcha.components.renderable;

import com.demcha.components.LineTextData;
import com.demcha.components.containers.abstract_builders.GuidesRenderer;
import com.demcha.components.content.text.BlockTextData;
import com.demcha.components.content.text.Text;
import com.demcha.components.content.text.TextStyle;
import com.demcha.components.core.Entity;
import com.demcha.components.geometry.ContentSize;
import com.demcha.components.geometry.InnerBoxSize;
import com.demcha.components.layout.Align;
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
import org.apache.pdfbox.util.Matrix;

import java.awt.*;
import java.io.IOException;
import java.util.EnumSet;

@Slf4j
@Builder
@EqualsAndHashCode
@NoArgsConstructor
public class BlockText implements PdfRender, GuidesRenderer {
    private static final EnumSet<Guide> DEFAULT_GUIDES =
            EnumSet.of(Guide.MARGIN, Guide.PADDING, Guide.BOX);


    public static <T extends TextComponent> ContentSize autoMeasureText(Entity entity) throws IOException {
        Text text = entity.getComponent(Text.class).orElseThrow();
        TextStyle style = entity.getComponent(TextStyle.class).orElseThrow();

        double width = style.getTextWidth(text.value());
        double height = style.getTextHeight(text.value());
        return new ContentSize(width, height);
    }

    private static ValidatedTextData getValidatedTextData(Entity e) {
        var textValueOpt = e.getComponent(BlockTextData.class);
        var styleOpt = e.getComponent(TextStyle.class);

        BlockTextData textValue;
        TextStyle style;
        if (textValueOpt.isEmpty()) {
            log.info("TextComponent has no BlockTextData; skipping: {}", e);
            textValue = textValueOpt.orElse(BlockTextData.empty());
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

        if (!e.hasAssignable(BlockTextData.class)) {
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
        InnerBoxSize innerBoxSize = InnerBoxSize.from(e).orElseThrow();

        ValidatedTextData validateText = getValidatedTextData(e);


        var style = validateText.style();
        float fontSize = style.size();
        PDFont font = style.font();
        Color color = validateText.style().color();
        var textHeight = (float) style.getTextHeight();

        var blockTextData = validateText.textValue().lines();

        double spacing = e.getComponent(Align.class).orElseGet(() -> {
            log.warn("TextComponent has no Align; using default: {}", e);
            return Align.defaultAlign(2);
        }).spacing() * -1;

        log.debug("Rendering textBlock '{}' at position ({}, {}) fontSize={}  textStyle= ", blockTextData, position.x(), position.y());
        log.debug("fontSize={}  textStyle= {}", fontSize, style);


        cs.saveGraphicsState();
        cs.setFont(font, fontSize);
        cs.setNonStrokingColor(color);
        cs.beginText();

        // стартовая позиция (левый верх «абзаца»)
        float startX = (float) position.x();
        float startY = (float) (position.y() + innerBoxSize.innerH() - textHeight - (spacing < 0 ? spacing  : spacing* -1));

// Устанавливаем начальную позицию
        float prevX = 0; // отслеживаем предыдущее смещение по X

        for (LineTextData ltd : blockTextData) {
            float currenPosition = (float) ltd.getX() + startX;
            cs.setTextMatrix(new Matrix(1, 0, 0, 1, currenPosition, startY));
            cs.showText(ltd.getLine());
            startY -= (textHeight - spacing);
        }

        cs.endText();
        cs.restoreGraphicsState();


        if (guideLines) {
            renderGuides(e, cs, DEFAULT_GUIDES);
        }

        return true;
    }

    public record ValidatedTextData(TextStyle style, BlockTextData textValue) {
    }


}
