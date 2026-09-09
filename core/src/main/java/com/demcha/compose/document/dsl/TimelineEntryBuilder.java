package com.demcha.compose.document.dsl;

import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentRowColumn;
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
    private Consumer<SectionBuilder> leading;

    TimelineEntryBuilder() {
    }

    /**
     * Sets the marker drawn in the rail for this entry.
     *
     * <p>An entry has exactly one marker. Declaring a second throws, whether the first came
     * from the {@code entry(marker, e -> ...)} shorthand or from an earlier call to this
     * method — quietly letting one of them win is a difference that would show up only in
     * the rendered document.</p>
     *
     * @param marker the marker
     * @return this builder
     * @throws NullPointerException  if {@code marker} is null
     * @throws IllegalStateException if the entry already has a marker
     * @since 2.4.0
     */
    public TimelineEntryBuilder marker(TimelineMarker marker) {
        Objects.requireNonNull(marker, "marker");
        if (this.marker != null) {
            // Keyed on the marker itself, not on where the first one came from: two
            // marker(...) calls in the advanced form are the same mistake as one beside the
            // shorthand, and letting the second win would only show in the rendered page.
            throw new IllegalStateException(
                    "A timeline entry has exactly one marker, and this entry already has "
                    + (markerGivenByShorthand
                            ? "the one from entry(marker, ...). Call marker(...) only inside "
                              + "entry(entry -> ...)."
                            : "one. Call marker(...) once."));
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
        declareOnce(this.content != null, "content");
        this.content = content;
        return this;
    }

    /**
     * Fills the column before the marker — the {@code DATE} of a
     * {@code DATE | ● | CONTENT} timeline.
     *
     * <p>Legal with either way of describing the entry's content, because it describes a
     * different column. The timeline must declare how wide that column is, with
     * {@link TimelineBuilder#leadingColumn(DocumentRowColumn)}: the width has to be the
     * same for every entry, or the markers do not line up and the rail is not straight.</p>
     *
     * @param leading callback receiving the entry's leading column
     * @return this builder
     * @throws NullPointerException  if {@code leading} is null
     * @throws IllegalStateException if the entry already has leading content
     * @since 2.4.0
     */
    public TimelineEntryBuilder leading(Consumer<SectionBuilder> leading) {
        Objects.requireNonNull(leading, "leading");
        declareOnce(this.leading != null, "leading");
        this.leading = leading;
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

    /** Whether this entry was given leading content, for the timeline to check. */
    boolean hasLeading() {
        return leading != null;
    }

    /**
     * Rejects a second declaration of one of the entry's structural slots.
     *
     * <p>A slot that takes a whole column — {@code marker}, {@code leading},
     * {@code content} — is declared, not assigned. Two of them is a mistake rather than an
     * override, and the shape of an entry should not depend on which call came last.
     * Ordinary values like {@code title(...)} do replace, as builder setters normally
     * do.</p>
     *
     * @param alreadyDeclared whether the slot is already filled
     * @param slot            the slot's name, for the message
     * @throws IllegalStateException if it is
     */
    private static void declareOnce(boolean alreadyDeclared, String slot) {
        if (alreadyDeclared) {
            throw new IllegalStateException(
                    "A timeline entry declares " + slot + "(...) once; this entry declares it twice.");
        }
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
            return new TimelineEntrySpec(leading, marker, content, section -> { });
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
        return new TimelineEntrySpec(leading, marker, beside, below);
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }
}
