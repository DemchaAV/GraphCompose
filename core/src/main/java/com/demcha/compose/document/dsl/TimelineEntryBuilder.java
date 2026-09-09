package com.demcha.compose.document.dsl;

import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextStyle;

import java.util.function.Consumer;

/**
 * Collects the content shown beside a timeline marker: an optional title, a
 * meta line (date / subtitle), a body, and arbitrary extra blocks. Configured
 * inside the {@code entry(marker, e -> ...)} lambda of {@link TimelineBuilder}.
 *
 * <p>Each text slot has a no-style setter (the timeline's default style is
 * applied) and a per-entry style override.</p>
 *
 * @author Artem Demchyshyn
 * @since 1.7.0
 */
public final class TimelineEntryBuilder {

    private String title;
    private DocumentTextStyle titleStyle;
    private String meta;
    private DocumentTextStyle metaStyle;
    private String body;
    private DocumentTextStyle bodyStyle;
    private Consumer<SectionBuilder> extra;

    TimelineEntryBuilder() {
    }

    /**
     * Sets the entry title (drawn beside the marker).
     *
     * @param title title text
     * @return this builder
     */
    public TimelineEntryBuilder title(String title) {
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
        this.extra = extra;
        return this;
    }

    /**
     * Resolves this entry into the form the layout consumes.
     *
     * <p>Style resolution happens here and only here: a per-entry override wins, otherwise
     * the timeline's default for that slot. Downstream there is no title, meta or body
     * left — only content that goes beside the marker and content that goes below it.</p>
     *
     * @param marker            the marker this entry was declared with
     * @param defaultTitleStyle the timeline's title style
     * @param defaultMetaStyle  the timeline's meta style
     * @param defaultBodyStyle  the timeline's body style
     * @return the normalized entry
     */
    TimelineEntrySpec normalize(TimelineMarker marker,
                                DocumentTextStyle defaultTitleStyle,
                                DocumentTextStyle defaultMetaStyle,
                                DocumentTextStyle defaultBodyStyle) {
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
