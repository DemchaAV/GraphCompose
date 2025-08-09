package com.demcha.structure.interfaces;

import com.demcha.structure.Container;
import com.demcha.structure.MeasureCtx;

public interface Element {
    void measure(Container container, MeasureCtx ctx); // расчет желаемых размеров
    void arrange(Container container, ArrangeCtx ctx);
}
