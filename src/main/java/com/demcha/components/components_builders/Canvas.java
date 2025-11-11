package com.demcha.components.components_builders;

import com.demcha.components.style.Margin;

import java.util.Optional;

public interface Canvas {
    float width();

    float x();

    float y();

    float height();

    Optional<Margin> margin();

    default float boundingTopLine() {
        return height() - (float) margin().orElse(Margin.zero()).top();
    }

    default float boundingBottonLine() {
        return (float) margin().orElse(Margin.zero()).bottom();
    }

    default float boundingRightLine() {
        return width() - (float) margin().orElse(Margin.zero()).right();
    }

    default float boundingLeftLine() {
        return (float) margin().orElse(Margin.zero()).left();
    }

    void addMargin(Margin margin);

}
