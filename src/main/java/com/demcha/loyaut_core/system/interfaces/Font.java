package com.demcha.loyaut_core.system.interfaces;

public interface Font<T> {
    T defaultFont();

    T bold();

    T italic();

    T boldItalic();

    T underline();

    T strikethrough();
}

