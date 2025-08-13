package com.demcha.components.data;

import com.demcha.core.Component;

public record ImageData(byte[] bytes, double width, double height) implements Component {
}
