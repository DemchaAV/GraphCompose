package com.demcha.compose.engine.components.layout;


/**
 * Horizontal + vertical placement anchor — a plain value pairing an
 * {@link HAnchor} and a {@link VAnchor}. Consumers read {@code h()} / {@code v()}
 * and the static factories to describe how content aligns within its area.
 */
public record Anchor(HAnchor h, VAnchor v) {
    public static Anchor topLeft() {
        return new Anchor(HAnchor.LEFT, VAnchor.TOP);
    }

    public static Anchor left() {
        return new Anchor(HAnchor.LEFT, VAnchor.DEFAULT);
    }

    public static Anchor center() {
        return new Anchor(HAnchor.CENTER, VAnchor.MIDDLE);
    }

    public static Anchor centerLeft() {
        return new Anchor(HAnchor.LEFT, VAnchor.MIDDLE);
    }

    public static Anchor centerRight() {
        return new Anchor(HAnchor.RIGHT, VAnchor.MIDDLE);
    }

    public static Anchor topRight() {
        return new Anchor(HAnchor.RIGHT, VAnchor.TOP);
    }

    public static Anchor bottomLeft() {
        return new Anchor(HAnchor.LEFT, VAnchor.BOTTOM);
    }

    public static Anchor bottomRight() {
        return new Anchor(HAnchor.RIGHT, VAnchor.BOTTOM);
    }

    public static Anchor topCenter() {
        return new Anchor(HAnchor.CENTER, VAnchor.TOP);
    }

    public static Anchor bottomCenter() {
        return new Anchor(HAnchor.CENTER, VAnchor.BOTTOM);
    }

    public static Anchor defaultAnchor() {
        return new Anchor(HAnchor.DEFAULT, VAnchor.DEFAULT);
    }
}

