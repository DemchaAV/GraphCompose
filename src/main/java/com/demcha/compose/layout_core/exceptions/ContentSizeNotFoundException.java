package com.demcha.compose.layout_core.exceptions;

import com.demcha.compose.layout_core.components.core.Entity;

public class ContentSizeNotFoundException extends RuntimeException {
    public ContentSizeNotFoundException() {
        super(" All objects must have a ContentSize.");
    }

    public ContentSizeNotFoundException(String s) {
        super(s);
    }

    public ContentSizeNotFoundException(Entity entity) {
        super(entity.toString() + " All objects must have a ContentSize.");
    }
}
