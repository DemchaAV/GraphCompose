package com.demcha.mock.data;

public record OffsetData(double x, double y) {
    public static OffsetData zero() {
        return new OffsetData(0, 0);
    }
    public static OffsetData all(double offset) {
        return new OffsetData(offset, offset);
    }
}
