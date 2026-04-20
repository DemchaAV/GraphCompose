package com.demcha.compose.document.templates.data;

import com.demcha.compose.document.model.node.ListMarker;
import com.demcha.compose.document.templates.support.TemplateComposeTarget;
import com.demcha.compose.document.templates.support.TemplateDividerSpec;
import com.demcha.compose.document.templates.support.TemplateTableSpec;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Public semantic CV section made of one title plus ordered body blocks.
 *
 * <p><b>Authoring role:</b> keeps CV composition simple for applications:
 * callers describe one section as a paragraph, a list, aligned rows, a table,
 * a divider, a page break, or a small custom block without writing raw
 * template-scene code.</p>
 *
 * @param name stable semantic module name used for snapshot/debug naming
 * @param title human-readable module title rendered as the section heading
 * @param bodyBlocks ordered body blocks rendered below the heading
 * @author Artem Demchyshyn
 */
public record CvModule(
        String name,
        String title,
        List<BodyBlock> bodyBlocks
) {
    private static final String DEFAULT_PARAGRAPH_INDENT = "    ";
    private static final String DEFAULT_ROW_CONTINUATION_INDENT = "  ";

    /**
     * Creates a normalized module.
     */
    public CvModule {
        title = title == null ? "" : title;
        name = normalizeName(name == null || name.isBlank() ? title : name);
        bodyBlocks = bodyBlocks == null ? List.of() : List.copyOf(bodyBlocks);
    }

    /**
     * Creates a one-paragraph module.
     *
     * @param title visible module title
     * @param text paragraph body
     * @return module spec
     */
    public static CvModule paragraph(String title, String text) {
        return builder(title)
                .paragraph(text)
                .build();
    }

    /**
     * Creates a bullet-list module.
     *
     * @param title visible module title
     * @param items ordered list items
     * @return module spec
     */
    public static CvModule list(String title, List<String> items) {
        return builder(title)
                .list(items)
                .build();
    }

    /**
     * Creates a bullet-list module.
     *
     * @param title visible module title
     * @param items ordered list items
     * @return module spec
     */
    public static CvModule list(String title, String... items) {
        return list(title, items == null ? List.of() : Arrays.asList(items));
    }

    /**
     * Creates a markerless row module with aligned wrapped continuations.
     *
     * @param title visible module title
     * @param rows ordered row texts
     * @return module spec
     */
    public static CvModule rows(String title, List<String> rows) {
        return builder(title)
                .rows(rows)
                .build();
    }

    /**
     * Creates a markerless row module with aligned wrapped continuations.
     *
     * @param title visible module title
     * @param rows ordered row texts
     * @return module spec
     */
    public static CvModule rows(String title, String... rows) {
        return rows(title, rows == null ? List.of() : Arrays.asList(rows));
    }

    /**
     * Starts a fluent module builder.
     *
     * @param title visible module title
     * @return module builder
     */
    public static Builder builder(String title) {
        return new Builder(title);
    }

    /**
     * Semantic block kinds supported by the public CV module builder.
     */
    public enum BodyKind {
        PARAGRAPH,
        LIST,
        TABLE,
        DIVIDER,
        PAGE_BREAK,
        CUSTOM
    }

    /**
     * Immutable semantic body block used inside one CV module.
     */
    public static final class BodyBlock {
        private final BodyKind kind;
        private final String name;
        private final String text;
        private final List<String> items;
        private final ListMarker marker;
        private final String continuationIndent;
        private final boolean normalizeMarkers;
        private final String firstLineIndent;
        private final TemplateTableSpec table;
        private final TemplateDividerSpec divider;
        private final String pageBreakName;
        private final Consumer<TemplateComposeTarget> customRenderer;

        private BodyBlock(BodyKind kind,
                          String name,
                          String text,
                          List<String> items,
                          ListMarker marker,
                          String continuationIndent,
                          boolean normalizeMarkers,
                          String firstLineIndent,
                          TemplateTableSpec table,
                          TemplateDividerSpec divider,
                          String pageBreakName,
                          Consumer<TemplateComposeTarget> customRenderer) {
            this.kind = Objects.requireNonNull(kind, "kind");
            this.name = name == null ? "" : name;
            this.text = text == null ? "" : text;
            this.items = items == null ? List.of() : List.copyOf(items);
            this.marker = marker == null ? ListMarker.bullet() : marker;
            this.continuationIndent = continuationIndent == null ? "" : continuationIndent;
            this.normalizeMarkers = normalizeMarkers;
            this.firstLineIndent = firstLineIndent == null ? "" : firstLineIndent;
            this.table = table;
            this.divider = divider;
            this.pageBreakName = pageBreakName == null ? "" : pageBreakName;
            this.customRenderer = customRenderer;
        }

        /**
         * Returns the block kind.
         *
         * @return semantic block kind
         */
        public BodyKind kind() {
            return kind;
        }

        /**
         * Returns the optional explicit block name override.
         *
         * @return block name override, or an empty string
         */
        public String name() {
            return name;
        }

        /**
         * Returns paragraph text.
         *
         * @return paragraph text
         */
        public String text() {
            return text;
        }

        /**
         * Returns list items.
         *
         * @return ordered item texts
         */
        public List<String> items() {
            return items;
        }

        /**
         * Returns the list marker.
         *
         * @return marker
         */
        public ListMarker marker() {
            return marker;
        }

        /**
         * Returns the continuation indent used by markerless rows.
         *
         * @return continuation indent string
         */
        public String continuationIndent() {
            return continuationIndent;
        }

        /**
         * Returns whether visible markers should be normalized away from raw
         * input items.
         *
         * @return whether raw markers are normalized
         */
        public boolean normalizeMarkers() {
            return normalizeMarkers;
        }

        /**
         * Returns the first-line indent used by paragraph blocks.
         *
         * @return first-line indent string
         */
        public String firstLineIndent() {
            return firstLineIndent;
        }

        /**
         * Returns the table payload when this is a table block.
         *
         * @return table payload, or {@code null}
         */
        public TemplateTableSpec table() {
            return table;
        }

        /**
         * Returns the divider payload when this is a divider block.
         *
         * @return divider payload, or {@code null}
         */
        public TemplateDividerSpec divider() {
            return divider;
        }

        /**
         * Returns the page-break name when this is a page-break block.
         *
         * @return page-break name
         */
        public String pageBreakName() {
            return pageBreakName;
        }

        /**
         * Returns the advanced custom renderer when this is a custom block.
         *
         * @return custom renderer, or {@code null}
         */
        public Consumer<TemplateComposeTarget> customRenderer() {
            return customRenderer;
        }

        private static BodyBlock paragraph(String name, String text, String firstLineIndent) {
            return new BodyBlock(
                    BodyKind.PARAGRAPH,
                    name,
                    text,
                    List.of(),
                    ListMarker.none(),
                    "",
                    true,
                    firstLineIndent,
                    null,
                    null,
                    "",
                    null);
        }

        private static BodyBlock list(String name,
                                      List<String> items,
                                      ListMarker marker,
                                      String continuationIndent,
                                      boolean normalizeMarkers) {
            return new BodyBlock(
                    BodyKind.LIST,
                    name,
                    "",
                    items,
                    marker,
                    continuationIndent,
                    normalizeMarkers,
                    "",
                    null,
                    null,
                    "",
                    null);
        }

        private static BodyBlock table(TemplateTableSpec table) {
            return new BodyBlock(
                    BodyKind.TABLE,
                    table == null ? "" : table.name(),
                    "",
                    List.of(),
                    ListMarker.none(),
                    "",
                    true,
                    "",
                    Objects.requireNonNull(table, "table"),
                    null,
                    "",
                    null);
        }

        private static BodyBlock divider(TemplateDividerSpec divider) {
            return new BodyBlock(
                    BodyKind.DIVIDER,
                    divider == null ? "" : divider.name(),
                    "",
                    List.of(),
                    ListMarker.none(),
                    "",
                    true,
                    "",
                    null,
                    Objects.requireNonNull(divider, "divider"),
                    "",
                    null);
        }

        private static BodyBlock pageBreak(String pageBreakName) {
            return new BodyBlock(
                    BodyKind.PAGE_BREAK,
                    pageBreakName,
                    "",
                    List.of(),
                    ListMarker.none(),
                    "",
                    true,
                    "",
                    null,
                    null,
                    pageBreakName,
                    null);
        }

        private static BodyBlock custom(Consumer<TemplateComposeTarget> renderer) {
            return new BodyBlock(
                    BodyKind.CUSTOM,
                    "",
                    "",
                    List.of(),
                    ListMarker.none(),
                    "",
                    true,
                    "",
                    null,
                    null,
                    "",
                    Objects.requireNonNull(renderer, "renderer"));
        }
    }

    /**
     * Builder for one semantic CV module.
     */
    public static final class Builder {
        private String name;
        private String title;
        private final List<BodyBlock> bodyBlocks = new ArrayList<>();

        private Builder(String title) {
            this.title = title == null ? "" : title;
            this.name = normalizeName(this.title);
        }

        /**
         * Overrides the semantic module name.
         *
         * @param name stable module name
         * @return this builder
         */
        public Builder name(String name) {
            this.name = normalizeName(name);
            return this;
        }

        /**
         * Replaces the visible module title.
         *
         * @param title visible title
         * @return this builder
         */
        public Builder title(String title) {
            this.title = title == null ? "" : title;
            if (this.name.isBlank() || "Module".equals(this.name)) {
                this.name = normalizeName(this.title);
            }
            return this;
        }

        /**
         * Appends a standard paragraph block.
         *
         * @param text paragraph text
         * @return this builder
         */
        public Builder paragraph(String text) {
            bodyBlocks.add(BodyBlock.paragraph("", text, DEFAULT_PARAGRAPH_INDENT));
            return this;
        }

        /**
         * Appends a paragraph block with an explicit first-line indent.
         *
         * @param text paragraph text
         * @param firstLineIndent first-line indent prefix
         * @return this builder
         */
        public Builder paragraph(String text, String firstLineIndent) {
            bodyBlocks.add(BodyBlock.paragraph("", text, firstLineIndent));
            return this;
        }

        /**
         * Appends a default bullet list block.
         *
         * @param items ordered item texts
         * @return this builder
         */
        public Builder list(List<String> items) {
            return list(items, spec -> {
            });
        }

        /**
         * Appends a configurable list block.
         *
         * @param items ordered item texts
         * @param spec optional list configuration
         * @return this builder
         */
        public Builder list(List<String> items, Consumer<ListBlockBuilder> spec) {
            ListBlockBuilder builder = new ListBlockBuilder().items(items);
            if (spec != null) {
                spec.accept(builder);
            }
            bodyBlocks.add(builder.build());
            return this;
        }

        /**
         * Appends a default bullet list block.
         *
         * @param items ordered item texts
         * @return this builder
         */
        public Builder list(String... items) {
            return list(items == null ? List.of() : Arrays.asList(items));
        }

        /**
         * Appends a markerless row block with aligned wrapped continuations.
         *
         * @param rows ordered row texts
         * @return this builder
         */
        public Builder rows(List<String> rows) {
            return list(rows, spec -> spec
                    .noMarker()
                    .continuationIndent(DEFAULT_ROW_CONTINUATION_INDENT));
        }

        /**
         * Appends a markerless row block with aligned wrapped continuations.
         *
         * @param rows ordered row texts
         * @return this builder
         */
        public Builder rows(String... rows) {
            return rows(rows == null ? List.of() : Arrays.asList(rows));
        }

        /**
         * Appends a table block.
         *
         * @param table table payload
         * @return this builder
         */
        public Builder table(TemplateTableSpec table) {
            bodyBlocks.add(BodyBlock.table(table));
            return this;
        }

        /**
         * Appends a divider block.
         *
         * @param divider divider payload
         * @return this builder
         */
        public Builder divider(TemplateDividerSpec divider) {
            bodyBlocks.add(BodyBlock.divider(divider));
            return this;
        }

        /**
         * Appends an explicit page break.
         *
         * @param name semantic break name
         * @return this builder
         */
        public Builder pageBreak(String name) {
            bodyBlocks.add(BodyBlock.pageBreak(name));
            return this;
        }

        /**
         * Appends an advanced callback-backed block.
         *
         * @param renderer custom emission callback
         * @return this builder
         */
        public Builder custom(Consumer<TemplateComposeTarget> renderer) {
            bodyBlocks.add(BodyBlock.custom(renderer));
            return this;
        }

        /**
         * Builds the immutable module.
         *
         * @return immutable module spec
         */
        public CvModule build() {
            return new CvModule(name, title, bodyBlocks);
        }
    }

    /**
     * Builder for one semantic list block inside a CV module.
     */
    public static final class ListBlockBuilder {
        private String name = "";
        private final List<String> items = new ArrayList<>();
        private ListMarker marker = ListMarker.bullet();
        private String continuationIndent = "";
        private boolean normalizeMarkers = true;

        /**
         * Overrides the semantic block name.
         *
         * @param name block name
         * @return this builder
         */
        public ListBlockBuilder name(String name) {
            this.name = name == null ? "" : name;
            return this;
        }

        /**
         * Replaces the full item collection.
         *
         * @param items ordered item texts
         * @return this builder
         */
        public ListBlockBuilder items(List<String> items) {
            this.items.clear();
            if (items != null) {
                this.items.addAll(items);
            }
            return this;
        }

        /**
         * Replaces the full item collection.
         *
         * @param items ordered item texts
         * @return this builder
         */
        public ListBlockBuilder items(String... items) {
            return items(items == null ? List.of() : Arrays.asList(items));
        }

        /**
         * Appends one item.
         *
         * @param item item text
         * @return this builder
         */
        public ListBlockBuilder addItem(String item) {
            items.add(item);
            return this;
        }

        /**
         * Sets an explicit marker.
         *
         * @param marker list marker
         * @return this builder
         */
        public ListBlockBuilder marker(ListMarker marker) {
            this.marker = marker == null ? ListMarker.bullet() : marker;
            return this;
        }

        /**
         * Sets an explicit custom marker.
         *
         * @param marker marker text
         * @return this builder
         */
        public ListBlockBuilder marker(String marker) {
            return marker(ListMarker.custom(marker));
        }

        /**
         * Uses bullet markers.
         *
         * @return this builder
         */
        public ListBlockBuilder bullet() {
            return marker(ListMarker.bullet());
        }

        /**
         * Uses dash markers.
         *
         * @return this builder
         */
        public ListBlockBuilder dash() {
            return marker(ListMarker.dash());
        }

        /**
         * Uses no visible marker.
         *
         * @return this builder
         */
        public ListBlockBuilder noMarker() {
            return marker(ListMarker.none());
        }

        /**
         * Sets the continuation indent used by markerless wrapped lines.
         *
         * @param continuationIndent continuation prefix
         * @return this builder
         */
        public ListBlockBuilder continuationIndent(String continuationIndent) {
            this.continuationIndent = continuationIndent == null ? "" : continuationIndent;
            return this;
        }

        /**
         * Sets whether raw leading markers should be normalized.
         *
         * @param normalizeMarkers whether raw markers are normalized
         * @return this builder
         */
        public ListBlockBuilder normalizeMarkers(boolean normalizeMarkers) {
            this.normalizeMarkers = normalizeMarkers;
            return this;
        }

        private BodyBlock build() {
            return BodyBlock.list(name, items, marker, continuationIndent, normalizeMarkers);
        }
    }

    private static String normalizeName(String raw) {
        String safe = raw == null ? "" : raw.strip();
        if (safe.isEmpty()) {
            return "Module";
        }

        StringBuilder normalized = new StringBuilder();
        boolean capitalize = true;
        for (int index = 0; index < safe.length(); index++) {
            char current = safe.charAt(index);
            if (!Character.isLetterOrDigit(current)) {
                capitalize = true;
                continue;
            }
            normalized.append(capitalize ? Character.toUpperCase(current) : current);
            capitalize = false;
        }

        if (normalized.isEmpty()) {
            return "Module";
        }
        if (!Character.isLetter(normalized.charAt(0))) {
            normalized.insert(0, "Module");
        }
        return normalized.toString();
    }
}
