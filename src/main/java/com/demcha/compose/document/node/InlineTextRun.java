package com.demcha.compose.document.node;

import com.demcha.compose.document.style.DocumentTextStyle;

/**
 * One styled inline text run inside a semantic paragraph.
 *
 * @param text       visible text for the run
 * @param textStyle  style for this run; falls back to the paragraph style when null
 * @param linkTarget optional link target (external URI or internal anchor) scoped
 *                   only to this run
 * @author Artem Demchyshyn
 */
public record InlineTextRun(
        String text,
        DocumentTextStyle textStyle,
        DocumentLinkTarget linkTarget
) implements InlineRun {
    /**
     * Normalizes null text to an empty run.
     */
    public InlineTextRun {
        text = text == null ? "" : text;
    }

    /**
     * Creates an inline run with external link metadata.
     *
     * @param text        visible text
     * @param textStyle   style for this run
     * @param linkOptions external link metadata, wrapped into an
     *                    {@link ExternalLinkTarget}
     */
    public InlineTextRun(String text, DocumentTextStyle textStyle, DocumentLinkOptions linkOptions) {
        this(text, textStyle, linkOptions == null ? null : new ExternalLinkTarget(linkOptions));
    }

    /**
     * Creates a styled inline run without link metadata.
     *
     * @param text      visible text
     * @param textStyle style for this run
     */
    public InlineTextRun(String text, DocumentTextStyle textStyle) {
        this(text, textStyle, (DocumentLinkTarget) null);
    }

    /**
     * Creates an unstyled inline run.
     *
     * @param text visible text
     */
    public InlineTextRun(String text) {
        this(text, (DocumentTextStyle) null, (DocumentLinkTarget) null);
    }

    /**
     * Returns the external link options of this run, or {@code null} when the run
     * has no link or targets an internal anchor.
     *
     * @return external link metadata, or {@code null}
     * @deprecated use {@link #linkTarget()}; this bridge only exposes external links
     */
    @Deprecated(since = "1.9.0")
    public DocumentLinkOptions linkOptions() {
        return linkTarget instanceof ExternalLinkTarget external ? external.options() : null;
    }
}
