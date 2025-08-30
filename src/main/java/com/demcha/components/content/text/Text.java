package com.demcha.components.content.text;

import com.demcha.components.core.Component;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public record Text(String value) implements Component {
    public static final Text EMPTY = new Text("");

    public static Text empty() {
        log.debug("Getting empty text");
        return EMPTY;
    }
}
