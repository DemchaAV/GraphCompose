package com.demcha.compose.document.dsl;

import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextStyle;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Collects the content shown beside a timeline marker: an optional title, a
 * meta line (date / subtitle), a body, and arbitrary extra blocks. Configured
 * inside the {@code entry(marker, e -> ...)} lambda of {@link TimelineBuilder}.
 *
 * <p>Each text slot has a no-style setter (the timeline's default style is
 * applied) and a per-entry style override.</p>
 *
 * <p>An entry describes its content one of two ways, never both. The
 * <em>semantic</em> way is {@link #title(String)}, {@link #meta(String)},
 * {@link #body(String)} and {@link #add(Consumer)}, which the timeline styles and
 * arranges for you. The <em>custom</em> way is {@link #content(Consumer)}, which hands you
 * the entry's content column to fill however you like. Mixing them throws, because the
 * two disagree about what the entry's shape is rather than composing.</p>
 *
 * @author Artem Demchyshyn
 * @since 1.7.0
 */
public final class TimelineEntryBuilder {

    /** Which vocabulary an entry has committed to; null until it commits. */
    private enum Mode { SEMANTIC, CUSTOM }

    private Mode mode;
    private TimelineMarker marker;
    private boolean markerGivenByShorthand;
    private String title;
    private DocumentTextStyle titleStyle;
    private String meta;
    private DocumentTextStyle metaStyle;
    private String body;
    private DocumentTextStyle bodyStyle;
    private Consumer<SectionBuilder> extra;
    private Consumer<SectionBuilder> content;

    TimelineEntryBuilder() {
    }

    /**
     * Sets the marker drawn in the rail for this entry.
     *
     * <p>Only for {@code entry(e -> ...)}. The {@code entry(marker, e -> ...)} shorthand
     * already carries one, and declaring a second there throws rather than quietly letting
     * one win.</p>
     *
     * @param marker the marker
     * @return this builder
     * @throws NullPointerException  if {@code marker} is null
     * @throws IllegalStateException if the entry already has a marker
     * @since 2.4.0
     */
    public TimelineEntryBuilder marker(TimelineMarker marker) {
        Objects.requireNonNull(marker, "marker");
        if (markerGivenByShorthand) {
            throw new IllegalStateException(
                    "This entry already has a marker from entry(marker, ...). Call marker(...) "
                    + "only inside entry(entry -> ...).");
        }
        this.marker = marker;
        return this;
    }

    /**
     * Fills the entry's content column yourself, instead of describing it as a title, a
     * meta line and a body.
     *
     * <p>Nothing is styled for you here — the timeline's title, meta and body styles
     * describe slots this entry no longer has.</p>
     *
     * @param content callback receiving the entry's content column
     * @return this builder
     * @throws NullPointerException  if {@code content} is null
     * @throws IllegalStateException if the entry already uses the semantic content API
     * @since 2.4.0
     */
    public TimelineEntryBuilder content(Consumer<SectionBuilder> content) {
        Objects.requireNonNull(content, "content");
        enter(Mode.CUSTOM);
        this.content = content;
        return this;
    }

    /**
     * Sets the entry title (drawn beside the marker).
     *
     * @param title title text
     * @return this builder
     */
    public TimelineEntryBuilder title(String title) {
        enter(Mode.SEMANTIC);
        this.title = title;
        return this;
    }

    /**
     * Sets the entry title with a per-entry style override.
     *
     * @param title title text
     * @param style title text style
     * @return this builder
     */
    public TimelineEntryBuilder title(String title, DocumentTextStyle style) {
        enter(Mode.SEMANTIC);
        this.title = title;
        this.titleStyle = style;
        return this;
    }

    /**
     * Overrides the title text style for this entry.
     *
     * @param style title text style
     * @return this builder
     */
    public TimelineEntryBuilder titleStyle(DocumentTextStyle style) {
        enter(Mode.SEMANTIC);
        this.titleStyle = style;
        return this;
    }

    /**
     * Sets the meta line (for example a date range or subtitle) shown under the
     * title.
     *
     * @param meta meta text
     * @return this builder
     */
    public TimelineEntryBuilder meta(String meta) {
        enter(Mode.SEMANTIC);
        this.meta = meta;
        return this;
    }

    /**
     * Sets the meta line with a per-entry style override.
     *
     * @param meta  meta text
     * @param style meta text style
     * @return this builder
     */
    public TimelineEntryBuilder meta(String meta, DocumentTextStyle style) {
        enter(Mode.SEMANTIC);
        this.meta = meta;
        this.metaStyle = style;
        return this;
    }

    /**
     * Overrides the meta text style for this entry.
     *
     * @param style meta text style
     * @return this builder
     */
    public TimelineEntryBuilder metaStyle(DocumentTextStyle style) {
        enter(Mode.SEMANTIC);
        this.metaStyle = style;
        return this;
    }

    /**
     * Sets the entry body paragraph.
     *
     * @param body body text
     * @return this builder
     */
    public TimelineEntryBuilder body(String body) {
        enter(Mode.SEMANTIC);
        this.body = body;
        return this;
    }

    /**
     * Sets the entry body with a per-entry style override.
     *
     * @param body  body text
     * @param style body text style
     * @return this builder
     */
    public TimelineEntryBuilder body(String body, DocumentTextStyle style) {
        enter(Mode.SEMANTIC);
        this.body = body;
        this.bodyStyle = style;
        return this;
    }

    /**
     * Overrides the body text style for this entry.
     *
     * @param style body text style
     * @return this builder
     */
    public TimelineEntryBuilder bodyStyle(DocumentTextStyle style) {
        enter(Mode.SEMANTIC);
        this.bodyStyle = style;
        return this;
    }

    /**
     * Adds arbitrary extra content below the body, configured against the
     * entry's content section (for chips, nested rows, lists, and so on).
     *
     * @param extra callback receiving the entry's content section
     * @return this builder
     */
    public TimelineEntryBuilder add(Consumer<SectionBuilder> extra) {
        enter(Mode.SEMANTIC);
        this.extra = extra;
        return this;
    }

    /** Records the marker the {@code entry(marker, ...)} shorthand supplied. */
    void markerFromShorthand(TimelineMarker marker) {
        this.marker = marker;
        this.markerGivenByShorthand = true;
    }

    /**
     * Commits this entry to one content vocabulary, or rejects the second one.
     *
     * @param wanted the vocabulary the calling setter belongs to
     * @throws IllegalStateException if the entry already committed to the other one
     */
    private void enter(Mode wanted) {
        if (mode != null && mode != wanted) {
            throw new IllegalStateException(
                    "Cannot combine title/meta/body entry content with custom content(). "
                    + "Use either the semantic entry API or content().");
        }
        mode = wanted;
    }

    /**
     * Resolves this entry into the form the layout consumes.
     *
     * <p>Style resolution happens here and only here: a per-entry override wins, otherwise
     * the timeline's default for that slot. Downstream there is no title, meta or body
     * left — only content that goes beside the marker and content that goes below it, and
     * no trace of which of the two authoring APIs described it.</p>
     *
     * @param defaultTitleStyle the timeline's title style
     * @param defaultMetaStyle  the timeline's meta style
     * @param defaultBodyStyle  the timeline's body style
     * @return the normalized entry
     * @throws IllegalStateException if the entry has no marker
     */
    TimelineEntrySpec normalize(DocumentTextStyle defaultTitleStyle,
                                DocumentTextStyle defaultMetaStyle,
                                DocumentTextStyle defaultBodyStyle) {
        if (marker == null) {
            throw new IllegalStateException(
                    "A timeline entry needs a marker: call marker(...) inside entry(entry -> ...), "
                    + "or use the entry(marker, ...) shorthand.");
        }
        if (mode == Mode.CUSTOM) {
            // The content column, handed over whole. Nothing is styled or spaced for the
            // caller here — the slots those defaults describe are the ones they declined.
            return new TimelineEntrySpec(marker, content, section -> { });
        }
        String entryTitle = title;
        String entryMeta = meta;
        String entryBody = body;
        DocumentTextStyle resolvedTitle = titleStyle != null ? titleStyle : defaultTitleStyle;
        DocumentTextStyle resolvedMeta = metaStyle != null ? metaStyle : defaultMetaStyle;
        DocumentTextStyle resolvedBody = bodyStyle != null ? bodyStyle : defaultBodyStyle;
        Consumer<SectionBuilder> entryExtra = extra;

        Consumer<SectionBuilder> beside = column -> {
            column.spacing(2);
            if (notBlank(entryTitle)) {
                column.addParagraph(p -> p
                        .text(entryTitle)
                        .textStyle(resolvedTitle)
                        .margin(DocumentInsets.zero()));
            }
            if (notBlank(entryMeta)) {
                column.addParagraph(p -> p
                        .text(entryMeta)
                        .textStyle(resolvedMeta)
                        .margin(DocumentInsets.zero()));
            }
        };
        Consumer<SectionBuilder> below = section -> {
            if (notBlank(entryBody)) {
                section.addParagraph(p -> p
                        .text(entryBody)
                        .textStyle(resolvedBody)
                        .lineSpacing(1.3)
                        .margin(DocumentInsets.zero()));
            }
            if (entryExtra != null) {
                entryExtra.accept(section);
            }
        };
        return new TimelineEntrySpec(marker, beside, below);
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }
}
