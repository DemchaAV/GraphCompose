package com.demcha.compose.document.dsl;

import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
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
    private final List<Entry> entries = new ArrayList<>();
    private DocumentColor connectorColor = DEFAULT_RAIL;
    private double connectorWidth = 1.5;
    private double gutter = 8.0;
    private double markerGap = 8.0;
    private double markerColumnWeight = 0.10;
    private double entrySpacing = 14.0;
    private DocumentTextStyle titleStyle;
    private DocumentTextStyle metaStyle;
    private DocumentTextStyle bodyStyle;
    private boolean keepTogether = false;
    private boolean keepEntriesTogether = false;

    TimelineBuilder() {
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    private static DocumentTextStyle defaultTitleStyle() {
        return DocumentTextStyle.builder()
                .fontName(FontName.HELVETICA_BOLD)
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
     * @param weight marker column weight; ignored when not positive
     * @return this builder
     */
    public TimelineBuilder markerColumnWeight(double weight) {
        if (weight > 0) {
            this.markerColumnWeight = weight;
        }
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
        if (content != null) {
            content.accept(entry);
        }
        entries.add(new Entry(marker, entry));
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
        timeline.spacing(0);
        timeline.keepTogether(keepTogether);
        DocumentTextStyle resolvedTitle = titleStyle != null ? titleStyle : defaultTitleStyle();
        DocumentTextStyle resolvedMeta = metaStyle != null ? metaStyle : defaultMetaStyle();
        DocumentTextStyle resolvedBody = bodyStyle != null ? bodyStyle : defaultBodyStyle();
        for (int i = 0; i < entries.size(); i++) {
            Entry entry = entries.get(i);
            boolean last = i == entries.size() - 1;
            double bottom = last ? 0.0 : entrySpacing;
            timeline.addSection(section -> {
                section.keepTogether(keepEntriesTogether)
                        .accentLeft(connectorColor, connectorWidth)
                        .padding(new DocumentInsets(0, 0, bottom, gutter))
                        .spacing(4);
                section.addRow(header -> {
                    header.spacing(markerGap).weights(markerColumnWeight, 1.0);
                    header.addSection(markerColumn -> {
                        markerColumn.spacing(0);
                        entry.marker().renderInto(markerColumn);
                    });
                    header.addSection(titleColumn -> {
                        titleColumn.spacing(2);
                        if (notBlank(entry.entry().title())) {
                            DocumentTextStyle style = entry.entry().titleStyle() != null
                                    ? entry.entry().titleStyle() : resolvedTitle;
                            titleColumn.addParagraph(p -> p
                                    .text(entry.entry().title())
                                    .textStyle(style)
                                    .margin(DocumentInsets.zero()));
                        }
                        if (notBlank(entry.entry().meta())) {
                            DocumentTextStyle style = entry.entry().metaStyle() != null
                                    ? entry.entry().metaStyle() : resolvedMeta;
                            titleColumn.addParagraph(p -> p
                                    .text(entry.entry().meta())
                                    .textStyle(style)
                                    .margin(DocumentInsets.zero()));
                        }
                    });
                });
                if (notBlank(entry.entry().body())) {
                    DocumentTextStyle style = entry.entry().bodyStyle() != null
                            ? entry.entry().bodyStyle() : resolvedBody;
                    section.addParagraph(p -> p
                            .text(entry.entry().body())
                            .textStyle(style)
                            .lineSpacing(1.3)
                            .margin(DocumentInsets.zero()));
                }
                if (entry.entry().extra() != null) {
                    entry.entry().extra().accept(section);
                }
            });
        }
    }

    private record Entry(TimelineMarker marker, TimelineEntryBuilder entry) {
    }
}
