package com.demcha.compose.engine.components.content.text;

public enum TextDecoration {
    DEFAULT(0),
    BOLD(1),
    ITALIC(2),
    BOLD_ITALIC(3),
    UNDERLINE(4),
    STRIKETHROUGH(5);

    private final int bit;

    TextDecoration(int bit) {
        this.bit = bit;
    }

    // Проверка, есть ли стиль в комбинации
    public static boolean hasStyle(int styles, TextDecoration style) {
        return (styles & style.getBit()) != 0;
    }

    public int getBit() {
        return bit;
    }
}
