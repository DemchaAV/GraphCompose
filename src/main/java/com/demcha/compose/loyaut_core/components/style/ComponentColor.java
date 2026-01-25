package com.demcha.compose.loyaut_core.components.style;

import com.demcha.compose.loyaut_core.components.core.Component;

import java.awt.*;

public record ComponentColor(Color color) implements Component {
    // --- Grayscale ---
    public static final Color BLACK = new Color(0, 0, 0);
    public static final Color DARK_GRAY = new Color(64, 64, 64);
    public static final Color GRAY = new Color(128, 128, 128);
    public static final Color LIGHT_GRAY = new Color(192, 192, 192);
    public static final Color WHITE = new Color(255, 255, 255);
    // --- Reds & Pinks ---
    public static final Color RED = new Color(255, 0, 0);
    public static final Color DARK_RED = new Color(139, 0, 0);
    public static final Color PINK = new Color(255, 192, 203);
    // --- Greens ---
    public static final Color GREEN = new Color(0, 255, 0);
    public static final Color DARK_GREEN = new Color(0, 100, 0);
    public static final Color LIGHT_GREEN = new Color(144, 238, 144);
    public static final Color LIME = new Color(50, 205, 50);
    // --- Blues ---
    public static final Color BLUE = new Color(0, 0, 255);
    public static final Color DARK_BLUE = new Color(0, 0, 139);
    public static final Color LIGHT_BLUE = new Color(173, 216, 230);
    public static final Color ROYAL_BLUE = new Color(65, 105, 225);
    // --- Yellows & Oranges ---
    public static final Color YELLOW = new Color(255, 255, 0);
    public static final Color DARK_YELLOW = new Color(204, 204, 0);
    public static final Color LIGHT_YELLOW = new Color(255, 255, 224);
    public static final Color ORANGE = new Color(255, 165, 0);
    public static final Color DARK_ORANGE = new Color(255, 140, 0);
    // --- Cyans ---
    public static final Color CYAN = new Color(0, 255, 255);
    public static final Color DARK_CYAN = new Color(0, 139, 139);
    public static final Color LIGHT_CYAN = new Color(224, 255, 255);
    // --- Purples ---
    public static final Color MAGENTA = new Color(255, 0, 255);
    public static final Color PURPLE = new Color(128, 0, 128);
    public static final Color INDIGO = new Color(75, 0, 130);
    // --- Other ---
    public static final Color BROWN = new Color(165, 42, 42);
    /**
     * The default color for hyperlinks.
     */
    public static final Color LINK_DEFAULT = new Color(6, 69, 173); // A standard web blue
    /**
     * The color for visited hyperlinks.
     */
    public static final Color LINK_VISITED = new Color(102, 51, 153); // A standard web purple
    /**
     * The color for a hyperlink when the mouse is hovering over it.
     */
    public static final Color LINK_HOVER = new Color(255, 87, 34);   // A bright orange

    public static final Color TITLE = new Color(44, 62, 80);
    public static final Color MODULE_TITLE = new Color(44, 128, 185);
    public static final Color MODULE_LINE_TEXT = new Color(51, 51, 51);


    public ComponentColor(int r, int g, int b) {
        this(new Color(r, g, b));
    }

    public ComponentColor(int rgb) {
        this(new Color(rgb));
    }
}
