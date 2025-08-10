package com.demcha.components;


import com.demcha.core.Component;

public record Margin(float top, float right, float bottom, float left) implements Component {

    public static Margin all(float value) {
        return new Margin(value, value, value, value);
    }

    public static Margin zero() {
        return new Margin(0, 0, 0, 0);
    }
}

