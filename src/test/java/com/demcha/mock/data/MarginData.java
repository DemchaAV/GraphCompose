package com.demcha.mock.data;

public record MarginData(double top, double bottom, double right, double left) {
    public static MarginData all(double i) {
        return new MarginData(i, i, i, i);
    }
}
