package com.demcha.utils.page_brecker;

public class PageOutOfBoundException extends RuntimeException {
    public PageOutOfBoundException(String s) {
        super(s);
    }

    public PageOutOfBoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
