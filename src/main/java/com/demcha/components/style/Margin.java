package com.demcha.components.style;


import com.demcha.components.core.Component;

public record Margin(double top, double right, double bottom, double left) implements Component {

    public static Margin of(double value) {
        return new Margin(value, value, value, value);
    }

    public static Margin zero() {
        return new Margin(0, 0, 0, 0);
    }
    public static Margin bottom(double value) {
        return new Margin(0, 0, value, 0);
    }
    public static Margin top(double value) {
        return new Margin(value, 0, 0, 0);
    }
    public static Margin right(double value) {
        return new Margin(0, value, 0, 0);
    }
    public static Margin left(double value) {
        return new Margin(0, 0, 0, value);
    }

    public double horizontal() {
        return left + right;
    }
    public double vertical() {
        return top + bottom;
    }
}

