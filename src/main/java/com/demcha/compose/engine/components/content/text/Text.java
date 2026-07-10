package com.demcha.compose.engine.components.content.text;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public record Text(String value) {
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
