package com.demcha.loyaut_core.exceptions;

public class IllegalAlignException extends RuntimeException {
    public IllegalAlignException(String entityShouldHaveAlignComponent) {
        super(entityShouldHaveAlignComponent);
    }
}
