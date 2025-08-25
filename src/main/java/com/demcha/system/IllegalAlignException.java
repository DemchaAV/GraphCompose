package com.demcha.system;

public class IllegalAlignException extends RuntimeException {
    public IllegalAlignException(String entityShouldHaveAlignComponent) {
        super(entityShouldHaveAlignComponent);
    }
}
