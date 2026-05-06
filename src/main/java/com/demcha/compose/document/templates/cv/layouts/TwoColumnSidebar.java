package com.demcha.compose.document.templates.cv.layouts;

import com.demcha.compose.document.node.ContainerNode;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.RowNode;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.templates.api.SlotMap;

import java.util.List;
import java.util.Objects;

/**
 * Two-column CV layout with a primary main column and a narrower
 * sidebar column.
 *
 * <p>Header sits across the full width at the top; below it a
 * {@link RowNode} carries two weighted columns:</p>
 *
 * <ul>
 *   <li>{@link #MAIN} — primary content (Summary, Experience, Projects).</li>
 *   <li>{@link #SIDEBAR} — secondary content (Education, Skills, Languages).</li>
 * </ul>
 *
 * <p>Column weights default to 0.65 / 0.35 (Modern Professional ratio)
 * and are configurable via {@link #mainWeight(double)} /
 * {@link #sidebarWeight(double)}. Inter-column gap and inter-module
 * gap are also tunable.</p>
 */
public final class TwoColumnSidebar implements CvLayout {

    /** Stable slot name for the primary (wider) content column. */
    public static final String MAIN = "main";

    /** Stable slot name for the sidebar (narrower) content column. */
    public static final String SIDEBAR = "sidebar";

    private static final List<String> SLOT_NAMES = List.of(MAIN, SIDEBAR);
    private static final String LAYOUT_NAME = "layout.twoColumnSidebar";

    private double mainWeight = 0.65;
    private double sidebarWeight = 0.35;
    private double columnGap = 0.0;
    private double moduleGap = 0.0;

    private TwoColumnSidebar() {
    }

    /**
     * Returns a new two-column layout with the default 0.65 / 0.35
     * weight split and zero gaps.
     *
     * @return new layout instance
     */
    public static TwoColumnSidebar layout() {
        return new TwoColumnSidebar();
    }

    /**
     * Sets the main column's weight (relative share of the row width).
     *
     * @param value positive finite weight; usually between 0 and 1
     * @return this layout (for chaining)
     * @throws IllegalArgumentException if {@code value} is non-positive
     */
    public TwoColumnSidebar mainWeight(double value) {
        validatePositive(value, "mainWeight");
        this.mainWeight = value;
        return this;
    }

    /**
     * Sets the sidebar column's weight (relative share of the row
     * width).
     *
     * @param value positive finite weight; usually between 0 and 1
     * @return this layout (for chaining)
     * @throws IllegalArgumentException if {@code value} is non-positive
     */
    public TwoColumnSidebar sidebarWeight(double value) {
        validatePositive(value, "sidebarWeight");
        this.sidebarWeight = value;
        return this;
    }

    /**
     * Sets the horizontal gap between the main and sidebar columns.
     *
     * @param value non-negative finite gap in points
     * @return this layout (for chaining)
     * @throws IllegalArgumentException if {@code value} is negative,
     *         {@code NaN}, or infinite
     */
    public TwoColumnSidebar columnGap(double value) {
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
    public TwoColumnSidebar moduleGap(double value) {
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

        ContainerNode mainColumn = new ContainerNode(
                LAYOUT_NAME + ".main",
                slots.get(MAIN),
                moduleGap,
                DocumentInsets.zero(),
                DocumentInsets.zero(),
                null, null, null, null);

        ContainerNode sidebarColumn = new ContainerNode(
                LAYOUT_NAME + ".sidebar",
                slots.get(SIDEBAR),
                moduleGap,
                DocumentInsets.zero(),
                DocumentInsets.zero(),
                null, null, null, null);

        RowNode columnsRow = new RowNode(
                LAYOUT_NAME + ".columns",
                List.of(mainColumn, sidebarColumn),
                List.of(mainWeight, sidebarWeight),
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
