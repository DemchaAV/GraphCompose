package com.demcha.components.components_builders;

import com.demcha.components.style.Margin;

import java.util.Optional;

public interface Canvas {
    float width();

    float x();

    float y();

    float height();

    Optional<Margin> margin();

    default float BoundingTopLine() {
        return height() - (float) margin().orElse(Margin.zero()).top();
    }

    default float BoundingBottonLine() {
        return (float) margin().orElse(Margin.zero()).bottom();
    }

    default float BoundingRightLine() {
        return width() - (float) margin().orElse(Margin.zero()).right();
    }

    default float BoundingLeftLine() {
        return (float) margin().orElse(Margin.zero()).left();
    }

}
