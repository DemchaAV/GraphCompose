package com.demcha.compose.document.dsl;

import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentRowColumn;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.font.FontName;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Builds a vertical timeline: a sequence of entries, each a {@link TimelineMarker}
 * sitting in a continuous connector rail paired with its content (title, meta,
 * body). Pairing the marker with its entry — instead of hand-placing a bullet
 * plus a left margin per row — is the semantic win this builder provides.
 *
 * <p>Authored through {@link AbstractFlowBuilder#addTimeline}:</p>
 * <pre>{@code
 * section.addTimeline(timeline -> timeline
 *     .connector(rule, 1.0)
 *     .entry(TimelineMarker.dot(8, accent), e -> e
 *         .title("Senior Engineer").meta("2021 - present").body("Led ..."))
 *     .entry(TimelineMarker.numbered(2, 16, accent, white), e -> e
 *         .title("Engineer").meta("2019 - 2021").body("Built ...")));
 * }</pre>
 *
 * <p>The rail is a left border on each entry that auto-stretches to the entry's
 * height, so it spans variable-length content without any fixed sizing; entries
 * stack flush so the rail reads as one continuous line. The timeline paginates
 * between entries, and a tall entry splits within itself — between its marker row
 * and its body, and within the body — with the rail continuing across the page
 * break. Only the single marker-plus-title row of an entry is atomic, so it would
 * throw {@code AtomicNodeTooLargeException} only in the degenerate case of one
 * marker row taller than a whole page.</p>
 *
 * @author Artem Demchyshyn
 * @since 1.7.0
 */
public final class TimelineBuilder {

    private static final DocumentColor DEFAULT_RAIL = DocumentColor.rgb(150, 158, 172);
    private static final DocumentColor DEFAULT_INK = DocumentColor.rgb(34, 38, 50);
    private static final DocumentColor DEFAULT_MUTED = DocumentColor.rgb(120, 124, 136);
    private final List<TimelineEntryBuilder> entries = new ArrayList<>();
    private DocumentColor connectorColor = DEFAULT_RAIL;
    private double connectorWidth = 1.5;
    private double gutter = 8.0;
    private double markerGap = 8.0;
    private TimelineAxisSize axis = new TimelineAxisSize.Weight(0.10);
    private String axisDeclaredBy;
    private DocumentRowColumn leadingColumn;
    private double entrySpacing = 14.0;
    private DocumentTextStyle titleStyle;
    private DocumentTextStyle metaStyle;
    private DocumentTextStyle bodyStyle;
    private boolean keepTogether = false;
    private boolean keepEntriesTogether = false;

    TimelineBuilder() {
    }

    private static DocumentTextStyle defaultTitleStyle() {
        return DocumentTextStyle.builder()
                .fontName(FontName.HELVETICA)
                .decoration(DocumentTextDecoration.BOLD)
                .size(11)
                .color(DEFAULT_INK)
                .build();
    }

    private static DocumentTextStyle defaultMetaStyle() {
        return DocumentTextStyle.builder()
                .fontName(FontName.HELVETICA)
                .size(8.5)
                .color(DEFAULT_MUTED)
                .build();
    }

    private static DocumentTextStyle defaultBodyStyle() {
        return DocumentTextStyle.builder()
                .fontName(FontName.HELVETICA)
                .size(9.5)
                .color(DEFAULT_INK)
                .build();
    }

    /**
     * Sets the connector rail colour and width.
     *
     * @param color rail colour; ignored when {@code null}
     * @param width rail width in points; ignored when not positive
     * @return this builder
     */
    public TimelineBuilder connector(DocumentColor color, double width) {
        if (color != null) {
            this.connectorColor = color;
        }
        if (width > 0) {
            this.connectorWidth = width;
        }
        return this;
    }

    /**
     * Sets the gutter between the rail and the marker / content.
     *
     * @param gutter gutter width in points
     * @return this builder
     */
    public TimelineBuilder gutter(double gutter) {
        if (gutter >= 0) {
            this.gutter = gutter;
        }
        return this;
    }

    /**
     * Sets the horizontal gap between the marker and the entry title.
     *
     * @param gap gap in points
     * @return this builder
     */
    public TimelineBuilder markerGap(double gap) {
        if (gap >= 0) {
            this.markerGap = gap;
        }
        return this;
    }

    /**
     * Sets the relative width of the marker column (its weight against a content
     * weight of 1.0). Increase it for large numbered discs on narrow timelines.
     *
     * <p>See {@link #axisWidth(double)} for the same column in points. Declare one or the
     * other, not both.</p>
     *
     * @param weight marker column weight; ignored when not positive
     * @return this builder
     * @throws IllegalStateException if the axis width is already declared
     */
    public TimelineBuilder markerColumnWeight(double weight) {
        if (weight > 0) {
            declareAxisOnce("markerColumnWeight");
            this.axis = new TimelineAxisSize.Weight(weight);
        }
        return this;
    }

    /**
     * Sets the axis column — the one the markers sit in — to a fixed width in points.
     *
     * <p>The peer of {@link #markerColumnWeight(double)}, and the one to reach for when the
     * markers should sit the same distance from the edge whatever the page width: a weight
     * is a share of what the row has left, so it moves when the page or the columns beside
     * it do.</p>
     *
     * <p>Declare one or the other, not both. They are two answers to the same question, and
     * there is no conversion between them that does not need a row width neither the
     * builder nor the caller has.</p>
     *
     * @param points axis width in points
     * @return this builder
     * @throws IllegalArgumentException if {@code points} is not positive and finite
     * @throws IllegalStateException    if the axis width is already declared
     * @since 2.4.0
     */
    public TimelineBuilder axisWidth(double points) {
        if (!(points > 0) || Double.isInfinite(points)) {
            throw new IllegalArgumentException(
                    "A timeline's axis width must be a positive finite number of points, got: " + points);
        }
        declareAxisOnce("axisWidth");
        this.axis = new TimelineAxisSize.Fixed(points);
        return this;
    }

    private void declareAxisOnce(String call) {
        if (axisDeclaredBy != null) {
            throw new IllegalStateException(
                    "A timeline's axis column has one width, declared once: this one calls "
                    + axisDeclaredBy + "(...) and " + call + "(...). A weight is a share of the "
                    + "row and a fixed width is points, so neither can stand in for the other.");
        }
        axisDeclaredBy = call;
    }

    /**
     * Gives every entry a column before its marker, for the {@code DATE} of a
     * {@code DATE | ● | CONTENT} timeline.
     *
     * <p>The width is declared once, for the whole timeline, and every entry gets it —
     * including entries that put nothing in it, so they stay aligned with the ones that
     * do. Fill it per entry with {@link TimelineEntryBuilder#leading(Consumer)}.</p>
     *
     * <p>{@link DocumentRowColumn#auto()} is rejected: an auto column is measured from its
     * own row's content, so entries with leading text of different lengths would place
     * their markers at different x and the rail would not be straight. A fixed width or a
     * weight is decided by the row, which is what makes it the same in every entry.</p>
     *
     * @param column the leading column's width
     * @return this builder
     * @throws NullPointerException     if {@code column} is null
     * @throws IllegalArgumentException if {@code column} is {@link DocumentRowColumn#auto()}
     * @since 2.4.0
     */
    public TimelineBuilder leadingColumn(DocumentRowColumn column) {
        Objects.requireNonNull(column, "column");
        if (column.type() == DocumentRowColumn.Type.AUTO) {
            throw new IllegalArgumentException(
                    "A timeline's leading column cannot be auto(): an auto column is measured "
                    + "from its own row's content, so entries with leading text of different "
                    + "lengths would put their markers at different x and the rail would not be "
                    + "straight. Use fixed(points) or weight(share), which the row decides.");
        }
        this.leadingColumn = column;
        return this;
    }

    /**
     * Sets the vertical spacing between entries (the rail spans the gap).
     *
     * @param spacing spacing in points
     * @return this builder
     */
    public TimelineBuilder spacing(double spacing) {
        if (spacing >= 0) {
            this.entrySpacing = spacing;
        }
        return this;
    }

    /**
     * Overrides the default title text style for every entry.
     *
     * @param style title style
     * @return this builder
     */
    public TimelineBuilder titleStyle(DocumentTextStyle style) {
        this.titleStyle = style;
        return this;
    }

    /**
     * Overrides the default meta text style for every entry.
     *
     * @param style meta style
     * @return this builder
     */
    public TimelineBuilder metaStyle(DocumentTextStyle style) {
        this.metaStyle = style;
        return this;
    }

    /**
     * Overrides the default body text style for every entry.
     *
     * @param style body style
     * @return this builder
     */
    public TimelineBuilder bodyStyle(DocumentTextStyle style) {
        this.bodyStyle = style;
        return this;
    }

    /**
     * Adds one timeline entry — a marker paired with its content.
     *
     * @param marker  the marker drawn in the rail for this entry
     * @param content callback configuring the entry's title, meta and body
     * @return this builder
     * @throws NullPointerException if {@code marker} is {@code null}
     */
    public TimelineBuilder entry(TimelineMarker marker, Consumer<TimelineEntryBuilder> content) {
        Objects.requireNonNull(marker, "marker");
        TimelineEntryBuilder entry = new TimelineEntryBuilder();
        entry.markerFromShorthand(marker);
        if (content != null) {
            content.accept(entry);
        }
        entries.add(entry);
        return this;
    }

    /**
     * Adds one timeline entry, marker included.
     *
     * <p>The longer form of {@link #entry(TimelineMarker, Consumer)}, for entries that
     * describe their own content rather than filling in a title, a meta line and a body:</p>
     * <pre>{@code
     * timeline.entry(e -> e
     *     .marker(TimelineMarker.dot(8, accent))
     *     .content(column -> column.addParagraph("Anything at all")));
     * }</pre>
     *
     * @param entry callback configuring the entry, which must set a marker
     * @return this builder
     * @throws NullPointerException if {@code entry} is {@code null}
     * @since 2.4.0
     */
    public TimelineBuilder entry(Consumer<TimelineEntryBuilder> entry) {
        Objects.requireNonNull(entry, "entry");
        TimelineEntryBuilder built = new TimelineEntryBuilder();
        entry.accept(built);
        entries.add(built);
        return this;
    }

    /**
     * Keeps the whole timeline on one page: when it does not fit in the
     * remaining page space but fits on a fresh page, it relocates whole instead
     * of splitting between entries. Timelines taller than a page still flow.
     *
     * @return this builder
     * @since 1.8.0
     */
    public TimelineBuilder keepTogether() {
        this.keepTogether = true;
        return this;
    }

    /**
     * Keeps each timeline entry whole: an entry that does not fit in the
     * remaining page space moves to the next page instead of splitting its
     * marker, title, and body across the boundary. The timeline as a whole may
     * still break <em>between</em> entries.
     *
     * @return this builder
     * @since 1.8.0
     */
    public TimelineBuilder keepEntriesTogether() {
        this.keepEntriesTogether = true;
        return this;
    }

    void buildInto(SectionBuilder timeline) {
        layout(normalize(), timeline);
    }

    /**
     * Resolves this builder's defaults and per-entry overrides into the internal model.
     *
     * <p>Everything the authoring API knows and the layout does not — which style a slot
     * fell back to, whether a body was given at all — is settled here.</p>
     *
     * @return the normalized timeline
     */
    private TimelineSpec normalize() {
        DocumentTextStyle resolvedTitle = titleStyle != null ? titleStyle : defaultTitleStyle();
        DocumentTextStyle resolvedMeta = metaStyle != null ? metaStyle : defaultMetaStyle();
        DocumentTextStyle resolvedBody = bodyStyle != null ? bodyStyle : defaultBodyStyle();
        List<TimelineEntrySpec> specs = new ArrayList<>(entries.size());
        for (TimelineEntryBuilder entry : entries) {
            if (leadingColumn == null && entry.hasLeading()) {
                // Caught here rather than dropped: without a declared width there is no
                // column to put it in, and inventing one per entry is exactly what would
                // stop the markers lining up.
                throw new IllegalStateException(
                        "An entry has leading(...) content but the timeline has no leading column. "
                        + "Call leadingColumn(...) on the timeline, so every entry's leading is the "
                        + "same width and the markers line up.");
            }
            specs.add(entry.normalize(resolvedTitle, resolvedMeta, resolvedBody));
        }
        return new TimelineSpec(new TimelineRailSpec(connectorColor, connectorWidth),
                leadingColumn, gutter, markerGap, axis, entrySpacing,
                keepTogether, keepEntriesTogether, List.copyOf(specs));
    }

    /**
     * Lays a normalized timeline out. It reads nothing but the spec, which is what will
     * let a second authoring API reach this same code without it learning of that API.
     *
     * @param spec     the normalized timeline
     * @param timeline the section the timeline is built into
     */
    private static void layout(TimelineSpec spec, SectionBuilder timeline) {
        timeline.spacing(0);
        timeline.keepTogether(spec.keepTogether());
        List<TimelineEntrySpec> entries = spec.entries();
        for (int i = 0; i < entries.size(); i++) {
            TimelineEntrySpec entry = entries.get(i);
            boolean last = i == entries.size() - 1;
            double bottom = last ? 0.0 : spec.entrySpacing();
            timeline.addSection(section -> {
                section.keepTogether(spec.keepEntriesTogether())
                        .accentLeft(spec.rail().color(), spec.rail().width())
                        .padding(new DocumentInsets(0, 0, bottom, spec.gutter()))
                        .spacing(4);
                section.addRow(header -> {
                    header.spacing(spec.markerGap());
                    DocumentRowColumn axis = column(spec.axis());
                    if (spec.leadingColumn() == null && spec.axis() instanceof TimelineAxisSize.Weight weight) {
                        // The same two columns either way — columns(weight, weight) resolves
                        // exactly as weights(...) does, confirmed by the snapshots. But
                        // weights(...) is what a timeline has always put on its RowNode, and
                        // RowNode.weights() is public; spelling it the other way empties that
                        // list for every timeline that exists. Sugar where the sugar applies.
                        header.weights(weight.weight(), 1.0);
                    } else if (spec.leadingColumn() == null) {
                        header.columns(axis, DocumentRowColumn.weight(1.0));
                    } else {
                        header.columns(spec.leadingColumn(), axis, DocumentRowColumn.weight(1.0));
                        // Present even when this entry put nothing in it: the column is the
                        // timeline's, not the entry's, and an entry that skipped it must
                        // still start its marker where every other entry starts one.
                        header.addSection(entry.leading() == null ? column -> { } : entry.leading());
                    }
                    header.addSection(markerColumn -> {
                        markerColumn.spacing(0);
                        entry.marker().renderInto(markerColumn);
                    });
                    header.addSection(entry.beside());
                });
                entry.below().accept(section);
            });
        }
    }

    /**
     * Hands the axis size to the row in the row's own vocabulary.
     *
     * <p>The only place the two strategies meet, and neither is converted into the other:
     * the row resolves a weight against its own width, which is the number nobody upstream
     * of it has.</p>
     *
     * @param axis the axis size
     * @return the column to give the row
     */
    private static DocumentRowColumn column(TimelineAxisSize axis) {
        if (axis instanceof TimelineAxisSize.Fixed fixed) {
            return DocumentRowColumn.fixed(fixed.points());
        }
        return DocumentRowColumn.weight(((TimelineAxisSize.Weight) axis).weight());
    }
}
