package com.demcha.components.style;


import com.demcha.components.core.Component;

public record Padding(double top, double right, double bottom, double left) implements Component {
    public double horizontal() {
        return right + left;
    }

    public double vertical() {
        return top + bottom;
    }
    public static Padding zero() {
        return new Padding(0.0, 0.0, 0.0, 0.0);
    }
    public static Padding of( double trbl) {
        return new Padding(trbl, trbl, trbl, trbl);
    }
}
