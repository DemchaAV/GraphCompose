package com.demcha.system.utils.page_breaker;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.DoubleAdder;

@Slf4j
@Data
@Accessors(fluent = true)
@NoArgsConstructor
public class Offset {
    // Use DoubleAdder for thread safety and performance
    private final DoubleAdder y = new DoubleAdder();
    private final DoubleAdder x = new DoubleAdder();

    // Custom getters because DoubleAdder doesn't return primitive double automatically
    public double y() {
        return y.sum();
    }

    public double x() {
        return x.sum();
    }

    // --- Y Increments ---
    public void incrementY(double increment) {
        log.debug("{} increment Y({})", this, increment);
        this.y.add(increment);
    }

    // Overloads
    public void incrementY(float increment) { incrementY((double) increment); }
    public void incrementY(int increment) { incrementY((double) increment); }

    public void incrementY(Offset offset) {
        log.debug("Increment offset Y from object {}", offset);
        incrementY(offset.y());
    }

    // --- X Increments (FIXED BUGS HERE) ---
    public void incrementX(double increment) {
        log.debug("{} increment X ({})", this, increment);
        this.x.add(increment); // Was adding to Y in your code
    }

    // Overloads
    public void incrementX(float increment) { incrementX((double) increment); } // Fixed call
    public void incrementX(int increment) { incrementX((double) increment); }   // Fixed call

    public void incrementX(Offset offset) {
        log.debug("Increment offset X from object {}", offset);
        incrementX(offset.x()); // Fixed call
    }
}
