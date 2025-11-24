package com.demcha.system.utils.page_breaker;

public class PageOutOfBoundException extends Throwable {
    public PageOutOfBoundException(String s) {
        super(s);
    }

    public PageOutOfBoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
