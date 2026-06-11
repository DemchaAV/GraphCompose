package com.demcha.compose.document.style;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Geometric outline of a shape container. Sealed so layout, render, and
 * snapshot code can pattern-match exhaustively against the supported kinds.
 *
 * <p>Outlines are always axis-aligned and described in their own local
 * coordinate space. Their {@link #width()} / {@link #height()} are the
 * intrinsic outer size; container layout adds {@link com.demcha.compose.document.style.DocumentInsets}
 * around them for padding / margin.</p>
 *
 * @author Artem Demchyshyn
 */
public sealed interface ShapeOutline permits
        ShapeOutline.Rectangle,
        ShapeOutline.RoundedRectangle,
        ShapeOutline.RoundedRectanglePerCorner,
        ShapeOutline.Ellipse,
        ShapeOutline.Polygon {

    /**
     * Returns the outline outer width.
     *
     * @return outline outer width in points
     */
    double width();

    /**
     * Returns the outline outer height.
     *
     * @return outline outer height in points
     */
    double height();

    /**
     * Plain axis-aligned rectangle outline.
     *
     * @param width  outer width in points
     * @param height outer height in points
     */
    record Rectangle(double width, double height) implements ShapeOutline {
        /**
         * Validates that both dimensions are finite and positive.
         */
        public Rectangle {
            requirePositive("width", width);
            requirePositive("height", height);
        }
    }

    /**
     * Rectangle with a uniform corner radius. Render code may further clamp
     * {@code cornerRadius} to half of the smaller side at draw time.
     *
     * @param width        outer width in points
     * @param height       outer height in points
     * @param cornerRadius corner radius in points (0 means square corners)
     */
    record RoundedRectangle(double width, double height, double cornerRadius) implements ShapeOutline {
        /**
         * Validates dimensions and that {@code cornerRadius} is non-negative.
         */
        public RoundedRectangle {
            requirePositive("width", width);
            requirePositive("height", height);
            if (cornerRadius < 0 || Double.isNaN(cornerRadius) || Double.isInfinite(cornerRadius)) {
                throw new IllegalArgumentException("cornerRadius must be finite and non-negative: " + cornerRadius);
            }
        }
    }

    /**
     * Rectangle with independent per-corner radii. Render code clamps each
     * radius to half the smaller side at draw time. Use it for a container
     * rounded on only one side — e.g.
     * {@link DocumentCornerRadius#right(double)} for a card that sits flush
     * against a left edge — without a CLIP_PATH parent workaround.
     *
     * @param width   outer width in points
     * @param height  outer height in points
     * @param corners per-corner radii
     * @since 1.7.0
     */
    record RoundedRectanglePerCorner(double width, double height, DocumentCornerRadius corners)
            implements ShapeOutline {
        /**
         * Validates dimensions and that {@code corners} is non-null.
         */
        public RoundedRectanglePerCorner {
            requirePositive("width", width);
            requirePositive("height", height);
            Objects.requireNonNull(corners, "corners");
        }
    }

    /**
     * Ellipse outline. A circle is just an ellipse with {@code width == height}.
     *
     * @param width  outer width in points
     * @param height outer height in points
     */
    record Ellipse(double width, double height) implements ShapeOutline {
        /**
         * Validates that both dimensions are finite and positive.
         */
        public Ellipse {
            requirePositive("width", width);
            requirePositive("height", height);
        }
    }

    /**
     * Closed polygon outline described by a ring of normalized vertices. The
     * vertices live in a unit box (see {@link ShapePoint}) and are scaled to
     * {@code width × height} at render time, so one vertex ring renders at any
     * size. Diamonds, triangles, stars and arbitrary convex/concave polygons
     * are all expressed through this single kind via the factories below.
     *
     * @param width  outer width in points
     * @param height outer height in points
     * @param points ring of at least three normalized vertices, in draw order
     * @since 1.7.0
     */
    record Polygon(double width, double height, List<ShapePoint> points) implements ShapeOutline {
        /**
         * Validates dimensions and copies the vertex ring defensively.
         */
        public Polygon {
            requirePositive("width", width);
            requirePositive("height", height);
            Objects.requireNonNull(points, "points");
            points = List.copyOf(points);
            if (points.size() < 3) {
                throw new IllegalArgumentException("polygon needs at least 3 points: " + points.size());
            }
        }
    }

    /**
     * Cardinal direction for directional figures (arrows, chevrons).
     *
     * @since 1.7.0
     */
    enum Direction {
        /**
         * Pointing right.
         */
        RIGHT,
        /**
         * Pointing left.
         */
        LEFT,
        /**
         * Pointing up.
         */
        UP,
        /**
         * Pointing down.
         */
        DOWN
    }

    /**
     * Selectable design of a checkmark ("✓") figure — the swappable "tick"
     * variant used by {@link #checkmark(double, double, CheckmarkStyle)} and by
     * inline checkbox factories. Adding a new look is one enum constant plus its
     * vertex ring, so callers (and a future "pick your tick" UI) choose a style
     * by name rather than hand-building geometry.
     *
     * @since 1.7.0
     */
    enum CheckmarkStyle {
        /**
         * The default tick: a slim six-vertex checkmark band.
         */
        CLASSIC,
        /**
         * A bolder tick with a visibly thicker band.
         */
        HEAVY
    }

    /**
     * Selectable design of an arrow figure — the swappable "arrow" variant used
     * by {@link #arrow(double, double, Direction, ArrowStyle)}. Each style is a
     * normalized vertex ring pointed right and rotated to {@link Direction} at
     * build time, so a new arrow look is one enum constant plus its ring.
     *
     * @since 1.7.0
     */
    enum ArrowStyle {
        /**
         * The default arrow: a seven-vertex block arrow with a shaft.
         */
        BLOCK,
        /**
         * A solid triangular arrowhead ("▶") with no shaft.
         */
        TRIANGLE
    }

    /**
     * Convenience factory for a circular {@link Ellipse}.
     *
     * @param diameter circle diameter in points
     * @return ellipse with {@code width == height == diameter}
     */
    static Ellipse circle(double diameter) {
        return new Ellipse(diameter, diameter);
    }

    /**
     * Creates a {@link Polygon} from an explicit ring of normalized vertices.
     *
     * @param width  outer width in points
     * @param height outer height in points
     * @param points ring of at least three normalized vertices, in draw order
     * @return polygon outline
     */
    static Polygon polygon(double width, double height, List<ShapePoint> points) {
        return new Polygon(width, height, points);
    }

    /**
     * Creates a four-point diamond (rhombus) inscribed in the box.
     *
     * @param width  outer width in points
     * @param height outer height in points
     * @return diamond polygon outline
     */
    static Polygon diamond(double width, double height) {
        return new Polygon(width, height, List.of(
                new ShapePoint(0.5, 1.0),
                new ShapePoint(1.0, 0.5),
                new ShapePoint(0.5, 0.0),
                new ShapePoint(0.0, 0.5)));
    }

    /**
     * Creates an upward-pointing triangle inscribed in the box.
     *
     * @param width  outer width in points
     * @param height outer height in points
     * @return triangle polygon outline
     */
    static Polygon triangle(double width, double height) {
        return new Polygon(width, height, List.of(
                new ShapePoint(0.5, 1.0),
                new ShapePoint(1.0, 0.0),
                new ShapePoint(0.0, 0.0)));
    }

    /**
     * Creates a five-pointed star inscribed in the box.
     *
     * @param width  outer width in points
     * @param height outer height in points
     * @return five-pointed star polygon outline
     */
    static Polygon star(double width, double height) {
        return star(width, height, 5);
    }

    /**
     * Creates an {@code n}-pointed star inscribed in the box, with the first
     * point facing up.
     *
     * @param width  outer width in points
     * @param height outer height in points
     * @param points number of outer points (at least 3)
     * @return star polygon outline
     */
    static Polygon star(double width, double height, int points) {
        if (points < 3) {
            throw new IllegalArgumentException("star needs at least 3 points: " + points);
        }
        double outerRadius = 0.5;
        // Inner/outer ratio of a true star polygon (the inner ring sits on the
        // chords between outer points); it tends to 1 as the point count grows
        // and equals the classic 0.382 at five points. Below five points the
        // formula degenerates, so fall back to a fixed spiky ratio.
        double innerRatio = points >= 5
                ? Math.cos(2 * Math.PI / points) / Math.cos(Math.PI / points)
                : 0.38;
        double innerRadius = 0.5 * innerRatio;
        double start = Math.PI / 2.0;     // first outer vertex faces up
        List<ShapePoint> vertices = new ArrayList<>(points * 2);
        for (int i = 0; i < points * 2; i++) {
            double radius = (i % 2 == 0) ? outerRadius : innerRadius;
            double angle = start + i * Math.PI / points;
            double x = clampUnit(0.5 + radius * Math.cos(angle));
            double y = clampUnit(0.5 + radius * Math.sin(angle));
            vertices.add(new ShapePoint(x, y));
        }
        return new Polygon(width, height, vertices);
    }

    /**
     * Creates a block arrow pointing in {@code direction} — a list bullet or an
     * inline marker between text ("Step 1 → Step 2").
     *
     * @param width     outer width in points
     * @param height    outer height in points
     * @param direction the way the arrow points
     * @return arrow polygon outline
     */
    static Polygon arrow(double width, double height, Direction direction) {
        return arrow(width, height, direction, ArrowStyle.BLOCK);
    }

    /**
     * Creates an arrow of the given {@link ArrowStyle} pointing in
     * {@code direction} — the swappable-design overload. {@link ArrowStyle#BLOCK}
     * reproduces {@link #arrow(double, double, Direction)} exactly.
     *
     * @param width     outer width in points
     * @param height    outer height in points
     * @param direction the way the arrow points
     * @param style     the arrow design
     * @return arrow polygon outline
     * @since 1.7.0
     */
    static Polygon arrow(double width, double height, Direction direction, ArrowStyle style) {
        Objects.requireNonNull(direction, "direction");
        Objects.requireNonNull(style, "style");
        double[][] base = switch (style) {
            case BLOCK -> new double[][]{
                    {0.00, 0.65}, {0.55, 0.65}, {0.55, 0.88},
                    {1.00, 0.50}, {0.55, 0.12}, {0.55, 0.35}, {0.00, 0.35}
            };
            case TRIANGLE -> new double[][]{
                    {0.00, 0.00}, {1.00, 0.50}, {0.00, 1.00}
            };
        };
        return new Polygon(width, height, directional(base, direction));
    }

    /**
     * Creates a right-pointing block arrow.
     *
     * @param width  outer width in points
     * @param height outer height in points
     * @return right arrow polygon outline
     */
    static Polygon arrowRight(double width, double height) {
        return arrow(width, height, Direction.RIGHT);
    }

    /**
     * Creates a left-pointing block arrow.
     *
     * @param width  outer width in points
     * @param height outer height in points
     * @return left arrow polygon outline
     */
    static Polygon arrowLeft(double width, double height) {
        return arrow(width, height, Direction.LEFT);
    }

    /**
     * Creates a chevron ("›") pointing in {@code direction} — a lighter
     * directional marker for breadcrumbs and step lists.
     *
     * @param width     outer width in points
     * @param height    outer height in points
     * @param direction the way the chevron points
     * @return chevron polygon outline
     */
    static Polygon chevron(double width, double height, Direction direction) {
        Objects.requireNonNull(direction, "direction");
        double thickness = 0.45;
        double[][] base = {
                {0.00, 1.00}, {1.00, 0.50}, {0.00, 0.00},
                {thickness, 0.00}, {1.00 - thickness, 0.50}, {thickness, 1.00}
        };
        return new Polygon(width, height, directional(base, direction));
    }

    /**
     * Creates a checkmark ("✓") figure for "done" items in checklists.
     *
     * @param width  outer width in points
     * @param height outer height in points
     * @return checkmark polygon outline
     */
    static Polygon checkmark(double width, double height) {
        return checkmark(width, height, CheckmarkStyle.CLASSIC);
    }

    /**
     * Creates a checkmark ("✓") of the given {@link CheckmarkStyle} — the
     * swappable-design overload. {@link CheckmarkStyle#CLASSIC} reproduces
     * {@link #checkmark(double, double)} exactly.
     *
     * @param width  outer width in points
     * @param height outer height in points
     * @param style  the checkmark design
     * @return checkmark polygon outline
     * @since 1.7.0
     */
    static Polygon checkmark(double width, double height, CheckmarkStyle style) {
        Objects.requireNonNull(style, "style");
        List<ShapePoint> ring = switch (style) {
            case CLASSIC -> toPoints(new double[][]{
                    {0.45, 0.00}, {1.00, 0.72}, {0.86, 0.92},
                    {0.42, 0.34}, {0.16, 0.58}, {0.04, 0.44}
            });
            case HEAVY -> toPoints(ShapeRings.checkmarkBand(0.13));
        };
        return new Polygon(width, height, ring);
    }

    /**
     * Creates a plus ("+") figure for "add" affordances or checklist markers.
     *
     * @param width  outer width in points
     * @param height outer height in points
     * @return plus polygon outline
     */
    static Polygon plus(double width, double height) {
        double low = 0.34;
        double high = 0.66;
        double[][] points = {
                {low, 0.00}, {high, 0.00}, {high, low}, {1.00, low},
                {1.00, high}, {high, high}, {high, 1.00}, {low, 1.00},
                {low, high}, {0.00, high}, {0.00, low}, {low, low}
        };
        return new Polygon(width, height, toPoints(points));
    }

    /**
     * Creates a regular {@code sides}-gon (pentagon, hexagon, …) inscribed in
     * the box, with the first vertex facing up.
     *
     * @param width  outer width in points
     * @param height outer height in points
     * @param sides  number of sides (at least 3)
     * @return regular polygon outline
     */
    static Polygon regularPolygon(double width, double height, int sides) {
        if (sides < 3) {
            throw new IllegalArgumentException("regular polygon needs at least 3 sides: " + sides);
        }
        double start = Math.PI / 2.0;
        List<ShapePoint> vertices = new ArrayList<>(sides);
        for (int i = 0; i < sides; i++) {
            double angle = start + i * 2.0 * Math.PI / sides;
            vertices.add(new ShapePoint(
                    clampUnit(0.5 + 0.5 * Math.cos(angle)),
                    clampUnit(0.5 + 0.5 * Math.sin(angle))));
        }
        return new Polygon(width, height, vertices);
    }

    private static List<ShapePoint> directional(double[][] base, Direction direction) {
        Direction resolved = direction == null ? Direction.RIGHT : direction;
        List<ShapePoint> points = new ArrayList<>(base.length);
        for (double[] vertex : base) {
            double x = vertex[0];
            double y = vertex[1];
            double tx;
            double ty;
            switch (resolved) {
                case LEFT -> {
                    tx = 1.0 - x;
                    ty = y;
                }
                case UP -> {
                    tx = y;
                    ty = x;
                }
                case DOWN -> {
                    tx = y;
                    ty = 1.0 - x;
                }
                default -> {
                    tx = x;
                    ty = y;
                }
            }
            points.add(new ShapePoint(clampUnit(tx), clampUnit(ty)));
        }
        return points;
    }

    private static List<ShapePoint> toPoints(double[][] raw) {
        List<ShapePoint> points = new ArrayList<>(raw.length);
        for (double[] vertex : raw) {
            points.add(new ShapePoint(clampUnit(vertex[0]), clampUnit(vertex[1])));
        }
        return points;
    }

    private static double clampUnit(double value) {
        return value < 0.0 ? 0.0 : (value > 1.0 ? 1.0 : value);
    }

    private static void requirePositive(String label, double value) {
        if (value <= 0 || Double.isNaN(value) || Double.isInfinite(value)) {
            throw new IllegalArgumentException(label + " must be finite and positive: " + value);
        }
    }
}
