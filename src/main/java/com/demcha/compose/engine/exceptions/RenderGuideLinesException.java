package com.demcha.compose.engine.exceptions;

public class RenderGuideLinesException extends RuntimeException {

    public RenderGuideLinesException(String info) {
        super(info);
    }

    public RenderGuideLinesException(String message, Throwable io) {
        super(message, io);
    }
}
