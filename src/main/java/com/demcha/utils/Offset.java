package com.demcha.utils;

import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
@Accessors(fluent = true)
public class Offset {
    private double position = 0.0;

    public double increment(float increment) {
        return increment(Double.valueOf(increment));
    }

    public double increment(int increment) {
        return increment(Double.valueOf(increment));
    }

    public double increment(double increment) {
        log.debug("{} increment({})", this, increment);
        return this.position += increment;
    }

    public double increment(Offset offset) {
        log.debug("Increment offset from object {}", offset);
        return increment(offset.position);
    }

}
