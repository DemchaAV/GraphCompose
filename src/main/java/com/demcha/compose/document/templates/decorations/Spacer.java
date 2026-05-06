package com.demcha.compose.document.templates.decorations;

import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.SpacerNode;
import com.demcha.compose.document.style.DocumentInsets;

/**
 * Templates v2 vertical spacer decoration.
 *
 * <p>Produces an invisible {@link SpacerNode} that contributes a fixed
 * vertical extent to the parent flow. Use it whenever a preset needs
 * deliberate breathing room that is part of the document semantics
 * (rather than a stylistic margin on a neighbouring node).</p>
 *
 * <p>Three named magnitudes ({@link #small()}, {@link #medium()},
 * {@link #large()}) cover the common cases; for arbitrary heights use
 * {@link #height(double)} or {@link #size(double, double)}.</p>
 *
 * <p>This class is a stateless utility — all factories are static and
 * the produced {@link SpacerNode} is immutable.</p>
 */
public final class Spacer {

    /** Vertical extent of {@link #small()} in points. */
    public static final double SMALL = 4.0;

    /** Vertical extent of {@link #medium()} in points. */
    public static final double MEDIUM = 8.0;

    /** Vertical extent of {@link #large()} in points. */
    public static final double LARGE = 16.0;

    private Spacer() {
        // utility class — not instantiable
    }

    /**
     * Returns a small (4 pt) vertical spacer.
     *
     * @return spacer node
     */
    public static DocumentNode small() {
        return verticalSpacer("Spacer.small", SMALL);
    }

    /**
     * Returns a medium (8 pt) vertical spacer — the default vertical
     * gap recommended between a section heading and its body.
     *
     * @return spacer node
     */
    public static DocumentNode medium() {
        return verticalSpacer("Spacer.medium", MEDIUM);
    }

    /**
     * Returns a large (16 pt) vertical spacer — a deliberate visual
     * break between major document sections.
     *
     * @return spacer node
     */
    public static DocumentNode large() {
        return verticalSpacer("Spacer.large", LARGE);
    }

    /**
     * Returns a vertical spacer with a custom height.
     *
     * @param height vertical extent in points; must be finite and
     *               non-negative
     * @return spacer node
     * @throws IllegalArgumentException if {@code height} is negative,
     *         {@code NaN}, or infinite
     */
    public static DocumentNode height(double height) {
        return verticalSpacer("Spacer.height", height);
    }

    /**
     * Returns a spacer with explicit width and height, useful for
     * horizontal layouts (a fixed gap inside a row).
     *
     * @param width  horizontal extent in points; must be finite and
     *               non-negative
     * @param height vertical extent in points; must be finite and
     *               non-negative
     * @return spacer node
     * @throws IllegalArgumentException if either dimension is invalid
     */
    public static DocumentNode size(double width, double height) {
        return new SpacerNode(
                "Spacer.size",
                width,
                height,
                DocumentInsets.zero(),
                DocumentInsets.zero());
    }

    private static DocumentNode verticalSpacer(String name, double height) {
        return new SpacerNode(
                name,
                /* width  */ 0.0,
                /* height */ height,
                DocumentInsets.zero(),
                DocumentInsets.zero());
    }
}
