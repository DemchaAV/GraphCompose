package com.demcha.exeptions;

public class BigSizeElementException extends RuntimeException {
    public BigSizeElementException(String message) {
        super(message);
    }

    public BigSizeElementException(String message, Throwable cause) {
        super(message, cause);
    }
}
