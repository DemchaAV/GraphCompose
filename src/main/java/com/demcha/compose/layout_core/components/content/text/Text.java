package com.demcha.compose.layout_core.components.content.text;

import com.demcha.compose.layout_core.components.core.Component;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public record Text(String value) implements Component {
    public static final Text EMPTY = new Text("");

    public Text(String value) {

//        this.value = TextSanitizer.sanitize(value);
        this.value = value;
    }

    public static Text empty() {
        log.debug("Getting empty text");
        return EMPTY;
    }
}
