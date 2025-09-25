package com.demcha.exeptions;

public class IllegalAlignException extends RuntimeException {
    public IllegalAlignException(String entityShouldHaveAlignComponent) {
        super(entityShouldHaveAlignComponent);
    }
}
