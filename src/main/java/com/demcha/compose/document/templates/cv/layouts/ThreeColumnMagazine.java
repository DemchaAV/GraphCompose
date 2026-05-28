package com.demcha.compose.document.templates.cv.layouts;

import com.demcha.compose.document.node.ContainerNode;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.RowNode;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.templates.api.SlotMap;

import java.util.List;
import java.util.Objects;

/**
 * Three-column CV layout with three equal-weighted content columns
 * arranged in a magazine-style row beneath the header.
 *
 * <p>Header sits across the full width at the top; below it a
 * {@link RowNode} carries three weighted columns:</p>
 *
 * <ul>
 *   <li>{@link #COL_1} — first column (typically Summary, Skills).</li>
 *   <li>{@link #COL_2} — second column (typically Experience).</li>
 *   <li>{@link #COL_3} — third column (typically Education, Projects).</li>
 * </ul>
 *
 * <p>Column weights default to equal thirds and are configurable via
 * {@link #weights(double, double, double)}. Inter-column gap and
 * inter-module gap are also tunable.</p>
 *
 * @deprecated Superseded by the layered <code>…v2…</code> surface (the current
 *             standard) — the layered model
 *             {@link com.demcha.compose.document.templates.cv.v2.data.CvDocument}
 *             plus the {@code cv.v2} presets. Kept for backward compatibility;
 *             scheduled for removal in a future major. See
 *             {@code docs/templates/v2-layered/}.
 */
@Deprecated(since = "1.7.0", forRemoval = true)
public final class ThreeColumnMagazine implements CvLayout {

    /** Stable slot name for the first column. */
    public static final String COL_1 = "col-1";

    /** Stable slot name for the second column. */
    public static final String COL_2 = "col-2";

    /** Stable slot name for the third column. */
    public static final String COL_3 = "col-3";

    private static final List<String> SLOT_NAMES = List.of(COL_1, COL_2, COL_3);
    private static final String LAYOUT_NAME = "layout.threeColumnMagazine";

    private double weight1 = 1.0;
    private double weight2 = 1.0;
    private double weight3 = 1.0;
    private double columnGap = 0.0;
    private double moduleGap = 0.0;

    private ThreeColumnMagazine() {
    }

    /**
     * Returns a new three-column layout with equal column weights.
     *
     * @return new layout instance
     */
    public static ThreeColumnMagazine layout() {
        return new ThreeColumnMagazine();
    }

    /**
     * Sets explicit weights for the three columns.
     *
     * @param w1 positive finite weight for {@link #COL_1}
     * @param w2 positive finite weight for {@link #COL_2}
     * @param w3 positive finite weight for {@link #COL_3}
     * @return this layout (for chaining)
     * @throws IllegalArgumentException if any weight is non-positive
     */
    public ThreeColumnMagazine weights(double w1, double w2, double w3) {
        validatePositive(w1, "weight1");
        validatePositive(w2, "weight2");
        validatePositive(w3, "weight3");
        this.weight1 = w1;
        this.weight2 = w2;
        this.weight3 = w3;
        return this;
    }

    /**
     * Sets the horizontal gap between adjacent columns.
     *
     * @param value non-negative finite gap in points
     * @return this layout (for chaining)
     * @throws IllegalArgumentException if {@code value} is negative,
     *         {@code NaN}, or infinite
     */
    public ThreeColumnMagazine columnGap(double value) {
        validateNonNegative(value, "columnGap");
        this.columnGap = value;
        return this;
    }

    /**
     * Sets the vertical gap between modules inside each column.
     *
     * @param value non-negative finite gap in points
     * @return this layout (for chaining)
     * @throws IllegalArgumentException if {@code value} is negative,
     *         {@code NaN}, or infinite
     */
    public ThreeColumnMagazine moduleGap(double value) {
        validateNonNegative(value, "moduleGap");
        this.moduleGap = value;
        return this;
    }

    @Override
    public List<String> slotNames() {
        return SLOT_NAMES;
    }

    @Override
    public DocumentNode compose(DocumentNode header, SlotMap slots) {
        Objects.requireNonNull(header, "header");
        Objects.requireNonNull(slots, "slots");

        ContainerNode col1 = column(LAYOUT_NAME + "." + COL_1, slots.get(COL_1));
        ContainerNode col2 = column(LAYOUT_NAME + "." + COL_2, slots.get(COL_2));
        ContainerNode col3 = column(LAYOUT_NAME + "." + COL_3, slots.get(COL_3));

        RowNode columnsRow = new RowNode(
                LAYOUT_NAME + ".columns",
                List.of(col1, col2, col3),
                List.of(weight1, weight2, weight3),
                columnGap,
                DocumentInsets.zero(),
                DocumentInsets.zero(),
                null, null, null, null);

        return new ContainerNode(
                LAYOUT_NAME,
                List.of(header, columnsRow),
                /* spacing */ 0.0,
                DocumentInsets.zero(),
                DocumentInsets.zero(),
                null, null, null, null);
    }

    private ContainerNode column(String name, List<DocumentNode> children) {
        return new ContainerNode(
                name,
                children,
                moduleGap,
                DocumentInsets.zero(),
                DocumentInsets.zero(),
                null, null, null, null);
    }

    private static void validatePositive(double value, String name) {
        if (Double.isNaN(value) || Double.isInfinite(value) || value <= 0) {
            throw new IllegalArgumentException(name + " must be positive: " + value);
        }
    }

    private static void validateNonNegative(double value, String name) {
        if (Double.isNaN(value) || Double.isInfinite(value) || value < 0) {
            throw new IllegalArgumentException(name + " must be finite and non-negative: " + value);
        }
    }
}
