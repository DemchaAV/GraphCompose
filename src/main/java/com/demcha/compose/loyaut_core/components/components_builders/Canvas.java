package com.demcha.compose.loyaut_core.components.components_builders;

import com.demcha.compose.loyaut_core.components.style.Margin;

public interface Canvas {
    float width();

    float x();

    float y();

    float height();

    Margin margin();

    default float boundingTopLine() {
        return height() - (float) margin().top();
    }

    default float boundingBottomLine() {
        return (float) margin().bottom();
    }

    default float boundingRightLine() {
        return width() - (float) margin().right();
    }

    default float boundingLeftLine() {
        return (float) margin().left();
    }

    default double innerHeigh() {
        return height() - margin().vertical();
    }

    default double innerWidth() {
        return width() - margin().horizontal();
    }


    void addMargin(Margin margin);

}
