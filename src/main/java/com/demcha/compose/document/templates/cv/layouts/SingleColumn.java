package com.demcha.compose.document.templates.cv.layouts;

import com.demcha.compose.document.node.ContainerNode;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.templates.api.SlotMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Single-column CV layout.
 *
 * <p>Header sits at the top of the page; all module content stacks
 * vertically beneath it inside one main slot. The module gap is
 * controlled by the {@link #moduleGap(double)} setter.</p>
 *
 * <p>Used by presets such as Modern Professional and Classic Serif
 * where a tight, focused single-page layout is the goal.</p>
 *
 * @deprecated Superseded by the layered <code>…v2…</code> surface (the current
 *             standard) — the layered model
 *             {@link com.demcha.compose.document.templates.cv.v2.data.CvDocument}
 *             plus the {@code cv.v2} presets. Kept for backward compatibility;
 *             scheduled for removal in a future major. See
 *             {@code docs/templates/v2-layered/}.
 */
@Deprecated(since = "1.7.0", forRemoval = true)
public final class SingleColumn implements CvLayout {

    /** Stable slot name that holds all module content. */
    public static final String MAIN = "main";

    private static final List<String> SLOT_NAMES = List.of(MAIN);
    private static final String LAYOUT_NAME = "layout.singleColumn";

    private double moduleGap = 0.0;

    private SingleColumn() {
    }

    /**
     * Returns a new single-column layout with zero module gap (caller
     * provides spacing via module margins).
     *
     * @return new layout instance
     */
    public static SingleColumn layout() {
        return new SingleColumn();
    }

    /**
     * Sets the vertical gap between top-level modules.
     *
     * @param value non-negative finite gap in points
     * @return this layout (for chaining)
     * @throws IllegalArgumentException if {@code value} is negative,
     *         {@code NaN}, or infinite
     */
    public SingleColumn moduleGap(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value) || value < 0) {
            throw new IllegalArgumentException("moduleGap must be finite and non-negative: " + value);
        }
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

        List<DocumentNode> children = new ArrayList<>();
        children.add(header);
        children.addAll(slots.get(MAIN));

        return new ContainerNode(
                LAYOUT_NAME,
                children,
                moduleGap,
                DocumentInsets.zero(),
                DocumentInsets.zero(),
                /* fillColor    */ null,
                /* stroke       */ null,
                /* cornerRadius */ null,
                /* borders      */ null);
    }
}
