package com.demcha.compose.layout_core.components.renderable;

import com.demcha.compose.layout_core.components.content.text.Text;
import com.demcha.compose.layout_core.components.content.text.TextStyle;
import com.demcha.compose.layout_core.components.core.Entity;
import com.demcha.compose.layout_core.components.geometry.ContentSize;
import com.demcha.compose.layout_core.core.EntityManager;
import com.demcha.compose.layout_core.system.interfaces.Render;
import com.demcha.compose.layout_core.system.interfaces.TextMeasurementSystem;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Slf4j
@Builder
@EqualsAndHashCode
@NoArgsConstructor
public class TextComponent implements Render {

    public static ContentSize autoMeasureText(Entity entity, EntityManager entityManager) throws IOException {
        Text text = entity.getComponent(Text.class).orElseThrow();
        TextStyle style = entity.getComponent(TextStyle.class).orElseThrow();
        TextMeasurementSystem measurementSystem = entityManager.getSystems()
                .getSystem(TextMeasurementSystem.class)
                .orElseThrow(() -> new IllegalStateException("TextMeasurementSystem is required to auto-measure text."));
        ContentSize measured = measurementSystem.measure(style, text.value());
        double width = measured.width();
        double height = measured.height();
        log.debug("Auto-measured single-line text entity {} -> width={} height={} style={}",
                entity, width, height, style);
        return new ContentSize(width, height);
    }

    public static ValidatedTextData validatedTextData(Entity e) {
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

    public record ValidatedTextData(TextStyle style, Text textValue) {
    }
}
