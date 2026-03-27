package com.demcha.compose.layout_core.exceptions;

public class IllegalAlignException extends RuntimeException {
    public IllegalAlignException(String entityShouldHaveAlignComponent) {
        super(entityShouldHaveAlignComponent);
    }
}
