package com.demcha.exeptions;

import java.io.IOException;

public class RenderGuideLinesException extends IOException {

    public RenderGuideLinesException(String info) {
        super(info);
    }

    public RenderGuideLinesException(String message, IOException io) {
        super(message, io);
    }
}
