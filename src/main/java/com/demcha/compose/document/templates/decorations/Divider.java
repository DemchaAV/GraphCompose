package com.demcha.compose.document.templates.decorations;

import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.ShapeNode;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentCornerRadius;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTransform;

import java.util.Objects;

/**
 * Templates v2 horizontal divider decoration.
 *
 * <p>Produces a thin filled {@link ShapeNode} that visually separates
 * adjacent content. Width is supplied by the caller (typically the
 * preset, which knows its column or page width); thickness, colour, and
 * style are chosen via the factory methods.</p>
 *
 * <p>Five named styles cover the common cases:</p>
 *
 * <ul>
 *   <li>{@link #thin(DocumentColor, double)} — 0.5 pt rule, the default
 *       inter-section divider.</li>
 *   <li>{@link #thick(DocumentColor, double)} — 1.5 pt rule, used for
 *       stronger visual breaks (e.g. above a totals row).</li>
 *   <li>{@link #dashed(DocumentColor, double)} — placeholder; same as
 *       {@code thin(...)} until dashed-fill becomes available.</li>
 *   <li>{@link #dottedAccent(DocumentColor, double)} — placeholder; same
 *       as {@code thin(...)} until dotted-fill becomes available.</li>
 *   <li>{@link #custom(DocumentColor, double, double)} — fully custom
 *       width and thickness.</li>
 * </ul>
 *
 * <p>This class is a stateless utility — all factories are static and
 * the produced {@link ShapeNode} is immutable.</p>
 */
public final class Divider {

    /**
     * Default thin rule thickness in points.
     */
    public static final double THIN_THICKNESS = 0.5;

    /**
     * Default thick rule thickness in points.
     */
    public static final double THICK_THICKNESS = 1.5;

    private Divider() {
        // utility class — not instantiable
    }

    /**
     * Returns a thin (0.5 pt) horizontal rule of the given width and
     * colour.
     *
     * @param color non-null fill colour
     * @param width positive finite rule width in points
     * @return shape node representing the rule
     * @throws NullPointerException     if {@code color} is null
     * @throws IllegalArgumentException if {@code width} is non-positive
     */
    public static DocumentNode thin(DocumentColor color, double width) {
        return rule("Divider.thin", color, width, THIN_THICKNESS);
    }

    /**
     * Returns a thick (1.5 pt) horizontal rule of the given width and
     * colour.
     *
     * @param color non-null fill colour
     * @param width positive finite rule width in points
     * @return shape node representing the rule
     * @throws NullPointerException     if {@code color} is null
     * @throws IllegalArgumentException if {@code width} is non-positive
     */
    public static DocumentNode thick(DocumentColor color, double width) {
        return rule("Divider.thick", color, width, THICK_THICKNESS);
    }

    /**
     * Returns a dashed-style rule. Currently rendered identically to
     * {@link #thin(DocumentColor, double)}; the dashed fill style is
     * reserved for a future engine extension.
     *
     * @param color non-null fill colour
     * @param width positive finite rule width in points
     * @return shape node representing the rule
     * @throws NullPointerException     if {@code color} is null
     * @throws IllegalArgumentException if {@code width} is non-positive
     */
    public static DocumentNode dashed(DocumentColor color, double width) {
        return rule("Divider.dashed", color, width, THIN_THICKNESS);
    }

    /**
     * Returns a dotted accent rule. Currently rendered identically to
     * {@link #thin(DocumentColor, double)}; the dotted fill style is
     * reserved for a future engine extension.
     *
     * @param color non-null fill colour
     * @param width positive finite rule width in points
     * @return shape node representing the rule
     * @throws NullPointerException     if {@code color} is null
     * @throws IllegalArgumentException if {@code width} is non-positive
     */
    public static DocumentNode dottedAccent(DocumentColor color, double width) {
        return rule("Divider.dottedAccent", color, width, THIN_THICKNESS);
    }

    /**
     * Returns a fully customised rule.
     *
     * @param color     non-null fill colour
     * @param width     positive finite rule width in points
     * @param thickness positive finite rule thickness in points
     * @return shape node representing the rule
     * @throws NullPointerException     if {@code color} is null
     * @throws IllegalArgumentException if {@code width} or
     *                                  {@code thickness} is non-positive
     */
    public static DocumentNode custom(DocumentColor color, double width, double thickness) {
        if (Double.isNaN(thickness) || Double.isInfinite(thickness) || thickness <= 0) {
            throw new IllegalArgumentException("thickness must be positive: " + thickness);
        }
        return rule("Divider.custom", color, width, thickness);
    }

    private static DocumentNode rule(String name, DocumentColor color, double width, double thickness) {
        Objects.requireNonNull(color, "color");
        return new ShapeNode(
                name,
                width,
                thickness,
                /* fillColor    */ color,
                /* stroke       */ null,
                /* cornerRadius */ DocumentCornerRadius.ZERO,
                /* link         */ null,
                /* bookmark     */ null,
                /* padding      */ DocumentInsets.zero(),
                /* margin       */ DocumentInsets.zero(),
                /* transform    */ DocumentTransform.NONE);
    }
}
