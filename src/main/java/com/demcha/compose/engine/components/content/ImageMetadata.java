package com.demcha.compose.engine.components.content;

public record ImageMetadata(int width, int height, String format) {

    public ImageIntrinsicSize intrinsicSize() {
        return new ImageIntrinsicSize(width, height);
    }
}
