package com.demcha.compose.document.templates.receipt.widgets;

import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.templates.core.text.TextOrnaments;
import com.demcha.compose.document.templates.core.theme.BrandTheme;
import com.demcha.compose.document.templates.data.receipt.ReceiptFieldGroup;
import com.demcha.compose.document.templates.receipt.components.FieldRowRenderer;
import com.demcha.compose.document.templates.receipt.components.ReceiptStyles;

import java.util.Objects;

/**
 * A titled block of field rows — {@code TRANSFER DETAILS} over a hairline
 * table of labels and values.
 *
 * <p>The block runs the full content width rather than sitting in a narrow
 * left column: the values right-align to the page edge, which is what turns
 * a stack of unrelated rows into a column a reader can run an eye down.</p>
 *
 * <p>Each group keeps together, so a group never splits across a page
 * boundary with its heading stranded on the previous one.</p>
 */
public final class DetailGroup {

    /** Gap between the spaced-caps heading and the first row, in points. */
    private static final double TITLE_GAP = 7.0;

    private DetailGroup() {
    }

    /**
     * Renders one group; a group with no rows renders nothing.
     *
     * @param host  section the group is appended to
     * @param group the titled rows
     * @param theme active theme
     */
    public static void render(SectionBuilder host, ReceiptFieldGroup group, BrandTheme theme) {
        Objects.requireNonNull(host, "host");
        Objects.requireNonNull(theme, "theme");
        if (group == null || group.fields().isEmpty()) {
            return;
        }
        host.addSection("ReceiptDetailGroup", block -> {
            block.keepTogether();
            if (!group.title().isBlank()) {
                block.addParagraph(p -> p
                        .text(TextOrnaments.spacedUpper(group.title()))
                        .textStyle(ReceiptStyles.groupTitle(theme))
                        .margin(new DocumentInsets(0, 0, TITLE_GAP, 0)));
            }
            FieldRowRenderer.renderAll(block, group.fields(), theme);
        });
    }
}
