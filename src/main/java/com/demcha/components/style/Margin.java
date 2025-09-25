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

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        Margin margin = (Margin) o;
        return Double.compare(top, margin.top) == 0 && Double.compare(left, margin.left) == 0 && Double.compare(right, margin.right) == 0 && Double.compare(bottom, margin.bottom) == 0;
    }

    @Override
    public int hashCode() {
        int result = Double.hashCode(top);
        result = 31 * result + Double.hashCode(right);
        result = 31 * result + Double.hashCode(bottom);
        result = 31 * result + Double.hashCode(left);
        return result;
    }
}

