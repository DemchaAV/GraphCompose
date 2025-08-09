package com.demcha.structure;

public enum TextDecoration {
    DEFAULT(0),
    BOLD(1),
    ITALIC(2),
    UNDERLINE(4);

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
