package com.demcha.compose.layout_core.components.content;

public record ImageMetadata(int width, int height, String format) {

    public ImageIntrinsicSize intrinsicSize() {
        return new ImageIntrinsicSize(width, height);
    }
}
