package com.demcha.system.utils.page_breaker;

public class PageOutOfBoundException extends RuntimeException {
    private static String message = "Page out of bound, current page: %s";
    public PageOutOfBoundException(String s) {
        super(s);
    }

    public PageOutOfBoundException(String message, Throwable cause) {
        super(message, cause);
    }
    public PageOutOfBoundException(int pageNumber) {
        super(String.format(message, pageNumber) );
    }
}
