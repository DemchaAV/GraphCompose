package com.demcha.exceptions;

import java.io.IOException;

public class RenderGuideLinesException extends RuntimeException {

    public RenderGuideLinesException(String info) {
        super(info);
    }

    public RenderGuideLinesException(String message, Throwable io) {
        super(message, io);
    }
}
