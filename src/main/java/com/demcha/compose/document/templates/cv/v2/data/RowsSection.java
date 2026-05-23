package com.demcha.compose.document.templates.cv.v2.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Section whose body is a list of {@link CvRow}s rendered with a
 * uniform {@link RowStyle} decoration.
 *
 * <p>One record covers three common shapes:</p>
 *
 * <ul>
 *   <li>Technical Skills: {@code style = BULLETED}.</li>
 *   <li>Projects: {@code style = BULLETED_STACKED}.</li>
 *   <li>Additional Information: {@code style = PLAIN}.</li>
 * </ul>
 *
 * <p>If a future shape needs a different decoration (numbered list,
 * coloured chips, etc.) the only change is a new constant in
 * {@link RowStyle} plus a branch in
 * {@code components.RowRenderer} — the data model stays untouched.</p>
 *
 * @param title non-blank banner heading
 * @param rows  ordered list of two-field rows
 * @param style decoration applied uniformly to every row
 */
public record RowsSection(String title, List<CvRow> rows, RowStyle style)
        implements CvSection {

    public RowsSection {
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(rows, "rows");
        Objects.requireNonNull(style, "style");
        if (title.isBlank()) {
            throw new IllegalArgumentException("title must not be blank");
        }
        rows = List.copyOf(rows);
    }

    /**
     * Fluent builder seeded with the row decoration.
     *
     * @param title non-blank section heading
     * @param style decoration applied to every row
     * @return new builder
     */
    public static Builder builder(String title, RowStyle style) {
        return new Builder(title, style);
    }

    /**
     * Mutable builder.
     */
    public static final class Builder {
        private final String title;
        private final RowStyle style;
        private final List<CvRow> rows = new ArrayList<>();

        private Builder(String title, RowStyle style) {
            this.title = title;
            this.style = style;
        }

        public Builder row(String label, String body) {
            this.rows.add(new CvRow(label, body));
            return this;
        }

        public Builder row(CvRow row) {
            this.rows.add(Objects.requireNonNull(row, "row"));
            return this;
        }

        public RowsSection build() {
            return new RowsSection(title, rows, style);
        }
    }
}
