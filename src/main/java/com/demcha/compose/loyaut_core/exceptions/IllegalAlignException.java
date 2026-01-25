package com.demcha.compose.loyaut_core.exceptions;

public class IllegalAlignException extends RuntimeException {
    public IllegalAlignException(String entityShouldHaveAlignComponent) {
        super(entityShouldHaveAlignComponent);
    }
}
