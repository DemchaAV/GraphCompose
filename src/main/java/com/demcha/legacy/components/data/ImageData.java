package com.demcha.legacy.components.data;

import com.demcha.components.core.Component;

public record ImageData(byte[] bytes, double width, double height) implements Component {
}
