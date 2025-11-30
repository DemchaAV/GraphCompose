package com.demcha.system.utils.page_breaker;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
@Accessors(fluent = true)
@AllArgsConstructor
@NoArgsConstructor
public class Offset {
    private double y = 0.0;
    private double x = 0.0;

    public double incrementY(float increment) {
        return incrementY(Double.valueOf(increment));
    }

    public double incrementY(int increment) {
        return incrementY(Double.valueOf(increment));
    }

    public double incrementY(double increment) {
        log.debug("{} increment Y({})", this, increment);
        return this.y += increment;
    }

    public double incrementY(Offset offset) {
        log.debug("Increment offset Y from object {}", offset);
        return incrementY(offset.y);
    }

    public double incrementX(float increment) {
        return incrementY(Double.valueOf(increment));
    }

    public double incrementX(int increment) {
        return incrementY(Double.valueOf(increment));
    }

    public double incrementX(double increment) {
        log.debug("{} increment X ({})", this, increment);
        return this.x += increment;
    }

    public double incrementX(Offset offset) {
        log.debug("Increment offset X from object {}", offset);
        return incrementY(offset.x);
    }

}
