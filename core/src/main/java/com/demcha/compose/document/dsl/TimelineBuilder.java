package com.demcha.compose.document.dsl;

import com.demcha.compose.document.layout.HorizontalBandContentNode;
import com.demcha.compose.document.layout.HorizontalBandsNode;
import com.demcha.compose.document.layout.LayoutAnchorId;
import com.demcha.compose.document.layout.LayoutAnchorNode;
import com.demcha.compose.document.node.AlignNode;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.HorizontalAlign;
import com.demcha.compose.document.node.SectionNode;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentRowColumn;
import com.demcha.compose.document.style.DocumentStroke;
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
 * <p>The rail is one logical line, computed after layout from where the markers and
 * entries actually landed and drawn as one fragment per page it crosses. How far it
 * runs is a {@link TimelineRailExtent}; where it runs comes from the marker anchor,
 * a gutter to the left of the markers by default or through them after
 * {@link #markerOnRail()}. It is drawn beneath the markers, so a filled marker
 * covers the line passing under it.</p>
 *
 * <p>The timeline paginates between entries, and a tall entry splits within itself —
 * between its marker row and its body, and within the body — with the rail
 * continuing across the page break. Only the single marker-plus-title row of an
 * entry is atomic, so it would throw {@code AtomicNodeTooLargeException} only in the
 * degenerate case of one marker row taller than a whole page.</p>
 *
 * @author Artem Demchyshyn
 * @since 1.7.0
 */
public final class TimelineBuilder {

    private static final DocumentColor DEFAULT_RAIL = DocumentColor.rgb(150, 158, 172);
    private static final DocumentColor DEFAULT_INK = DocumentColor.rgb(34, 38, 50);
    private static final DocumentColor DEFAULT_MUTED = DocumentColor.rgb(120, 124, 136);
    private final List<TimelineEntryBuilder> entries = new ArrayList<>();
    private DocumentStroke railStroke = DocumentStroke.of(DEFAULT_RAIL, 1.5);
    private String railDeclaredBy;
    private TimelineRailExtent railExtent = TimelineRailExtent.ENTRY_BOUNDS;
    private TimelineMarkerAnchor markerAnchor;
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
     * <p>The shorthand for {@link #rail(Consumer)}: both describe the same rail, and a
     * timeline that uses both throws rather than letting one of them win. A call that
     * changes nothing — a null colour and a non-positive width — is not a use.</p>
     *
     * @param color rail colour; ignored when {@code null}
     * @param width rail width in points; ignored when not positive
     * @return this builder
     * @throws IllegalStateException if the rail is already configured
     */
    public TimelineBuilder connector(DocumentColor color, double width) {
        if (color == null && !(width > 0)) {
            return this;
        }
        declareRailOnce("connector");
        this.railStroke = DocumentStroke.of(
                color == null ? railStroke.color() : color,
                width > 0 ? width : railStroke.width());
        return this;
    }

    /**
     * Configures the connector rail.
     *
     * <p>{@link #connector(DocumentColor, double)} is the shorthand for this and produces
     * the same rail — one configuration, not an old one and a new one. Setting the rail
     * both ways throws rather than letting one of them win.</p>
     *
     * @param spec rail builder callback
     * @return this builder
     * @throws NullPointerException  if {@code spec} is null
     * @throws IllegalStateException if the rail is already configured
     * @since 2.4.0
     */
    public TimelineBuilder rail(Consumer<TimelineRailBuilder> spec) {
        Objects.requireNonNull(spec, "spec");
        TimelineRailBuilder builder = new TimelineRailBuilder();
        spec.accept(builder);
        if (builder.extent() != null) {
            this.railExtent = builder.extent();
        }
        if (builder.stroke() == null) {
            return this;
        }
        declareRailOnce("rail");
        this.railStroke = builder.stroke();
        return this;
    }

    /**
     * Puts the markers on the rail, rather than beside it.
     *
     * <p>A timeline draws its rail at the marker's left edge, pulled back by the gutter —
     * where it has been since before there was a choice, and a distance that grows with the
     * marker, so leaving it alone is what "renders unchanged" means. This opts a timeline
     * into the other anchor: the rail passes through the marker's centre, at every marker
     * size.</p>
     *
     * <p>It moves the markers too, because that is what putting them on the rail means:
     * each is placed inside the axis column so that its anchor point lands on the axis,
     * which for the centre anchor is the middle of that column. Markers of different sizes
     * therefore share one line instead of one left edge.</p>
     *
     * <p>An entry's body moves as well, into the content column beside the marker. With the
     * rail beside the axis a body spanning the entry clears the line by the gutter; with the
     * rail inside the axis that same body would be drawn through, so the body starts where
     * the title starts. It stays a vertical block — an entry longer than a page still splits,
     * with its text at the same x on every page — and the column it uses is the one the
     * entry's own header row resolved, so a fixed axis and a weighted one behave alike.</p>
     *
     * <p>A timeline that does not call this keeps the left-edge anchor and the placement it
     * has always had: markers packed to the left of the axis column, rail one gutter
     * further left.</p>
     *
     * @return this builder
     * @since 2.4.0
     */
    public TimelineBuilder markerOnRail() {
        this.markerAnchor = TimelineMarkerAnchor.onTheRail();
        return this;
    }

    /**
     * Rejects the rail being configured through both spellings.
     *
     * <p>Calling the <em>same</em> one twice is ordinary setter accumulation and stays
     * legal — {@code connector(colour, 0)} then {@code connector(null, width)} has always
     * been a way to set the two halves separately, and code doing that must not start
     * throwing. What is rejected is a timeline that says it both ways.</p>
     *
     * @param call the spelling being used
     * @throws IllegalStateException if the other spelling already configured the rail
     */
    private void declareRailOnce(String call) {
        if (railDeclaredBy != null && !railDeclaredBy.equals(call)) {
            throw new IllegalStateException(
                    "A timeline has one rail, configured once: this one calls " + railDeclaredBy
                    + "(...) and " + call + "(...). connector(colour, width) is the shorthand for "
                    + "rail(r -> r.stroke(...)), so either says the whole thing.");
        }
        railDeclaredBy = call;
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
        // One owner per timeline, allocated here. Every marker below anchors on this
        // instance, so the pass that draws the rail asks for it and gets these markers and
        // nobody else's — two timelines on a page never merge.
        if (railExtent == TimelineRailExtent.TIMELINE_BOUNDS) {
            throw new IllegalArgumentException(
                    "TimelineRailExtent.TIMELINE_BOUNDS is not implemented. On one page it is the "
                    + "same line as ENTRY_BOUNDS, and across pages there is nothing to measure it "
                    + "against — a timeline's own box draws nothing. Use ENTRY_BOUNDS or "
                    + "MARKER_TO_MARKER.");
        }
        TimelineRailSpec railSpec = new TimelineRailSpec(railStroke);
        // The gutter is only knowable here, so the default anchor is resolved here too —
        // and it is the same model the opted-in one uses, not a branch beside it.
        TimelineMarkerAnchor anchor =
                markerAnchor == null ? TimelineMarkerAnchor.atLeftEdge(gutter) : markerAnchor;
        return new TimelineSpec(new TimelineRailOwner(railSpec, railExtent, anchor),
                railSpec, leadingColumn, gutter, markerGap, axis, anchor, entrySpacing,
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
        // Which column the body belongs in, and whether the question arises at all. It does
        // only when the rail moved into the axis; with the rail beside it the body spans the
        // entry as it always has, and the header row publishes nothing.
        boolean bodyClearsTheAxis = spec.markerAnchor().railRunsThroughTheAxis();
        int contentColumn = spec.leadingColumn() == null ? 1 : 2;
        List<TimelineEntrySpec> entries = spec.entries();
        for (int i = 0; i < entries.size(); i++) {
            TimelineEntrySpec entry = entries.get(i);
            int index = i;
            boolean last = i == entries.size() - 1;
            double bottom = last ? 0.0 : spec.entrySpacing();
            // One identity per entry, because one row resolves one set of columns. Nothing
            // reads it but the body immediately below, and nothing else can: it is compared
            // by reference and never leaves this loop.
            Object bandKey = bodyClearsTheAxis ? new Object() : null;
            SectionBuilder entrySection = new SectionBuilder();
            {
                SectionBuilder section = entrySection;
                // No accentLeft. The rail is one logical line drawn from the resolved
                // anchors below, not a border repeated per entry — which is why it can
                // start and stop at the markers, and why it holds its x under markers of
                // different sizes.
                section.keepTogether(spec.keepEntriesTogether())
                        .padding(new DocumentInsets(0, 0, bottom, spec.gutter()))
                        .spacing(4);
                Consumer<RowBuilder> headerSpec = header -> {
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
                    header.addSection(anchoredMarker(spec, entry, index));
                    header.addSection(entry.beside());
                };

                if (bandKey == null) {
                    // The rail is beside the axis, so a body spanning the entry clears it by
                    // the gutter. This is the layout every timeline written before the choice
                    // already has, and it is left exactly as it was.
                    section.addRow(headerSpec);
                    entry.below().accept(section);
                } else {
                    // The rail runs through the axis, so a body spanning the entry would be
                    // crossed by it. The header publishes its columns; the body lays itself
                    // out in the content one and stays a vertical sibling, which is what lets
                    // it be longer than a page — a row cannot cross one.
                    RowBuilder header = new RowBuilder();
                    headerSpec.accept(header);
                    section.add(new HorizontalBandsNode("", bandKey, header.build()));

                    SectionBuilder body = new SectionBuilder();
                    body.spacing(4);
                    entry.below().accept(body);
                    SectionNode built = body.build();
                    if (!built.children().isEmpty()) {
                        section.add(new HorizontalBandContentNode("", bandKey, contentColumn, built));
                    }
                }
            }
            // The entry, anchored. Its slices are where the rail starts and stops on each
            // page — and only its slices carry that, because an entry is the one thing
            // here that can cross a page boundary.
            timeline.add(new LayoutAnchorNode("",
                    new LayoutAnchorId(spec.owner(), TimelineAnchorKind.ENTRY, index),
                    entrySection.build()));
        }
    }


    /**
     * The marker's column, wrapped so the finished layout reports where the marker landed.
     *
     * <p>The wrapper is what makes a marker one box however many fragments it drew: the
     * anchor reports the wrapped node's own box, so a ring-disc-pip marker and a plain dot
     * of the same size resolve identically. It adds a level to the layout paths — anything
     * keying on those, a snapshot for one, sees it — and no geometry: the wrapper measures
     * to its child and adds no spacing.</p>
     *
     * <p>The anchor sits <em>inside</em> the row's column rather than being the column. A
     * row hosts a fixed set of child types and an anchor is not one of them, and widening
     * that list — a public builder's contract — for an internal wrapper would be the wrong
     * trade. Wrapping the marker rather than its column is also the truer statement of what
     * is being anchored, and it is what a recipe that draws several nodes needs: they
     * become one box here.</p>
     *
     * @param spec  the timeline, for its owner
     * @param entry the entry whose marker this is
     * @param index the entry's position, which becomes the anchor's index
     * @return the marker column, with the marker anchored inside it
     */
    private static Consumer<SectionBuilder> anchoredMarker(TimelineSpec spec,
                                                           TimelineEntrySpec entry,
                                                           int index) {
        DocumentNode anchored = new LayoutAnchorNode("",
                new LayoutAnchorId(spec.owner(), TimelineAnchorKind.MARKER, index),
                entry.marker().node());
        // Where in the axis column the marker sits comes from the anchor, not from a mode:
        // an anchor on the marker's left edge wants the marker at the column's left edge,
        // one on its centre wants it at the column's centre. That is what puts markers of
        // 6, 14 and 24pt on one line — each is centred in the same column, so each centre
        // is the column's centre — and it works with a weight axis, whose width nobody
        // knows until layout.
        //
        // The align wraps the anchor and not the other way round. The anchor has to stay
        // around the marker itself or it would report the column's box, which is the whole
        // thing the marker anchor exists not to be.
        DocumentNode placed = new AlignNode(anchored, spec.markerAnchor().horizontalAlign());
        return markerColumn -> {
            markerColumn.spacing(0);
            markerColumn.add(placed);
        };
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
