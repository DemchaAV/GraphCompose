package com.demcha.compose.document.output;

import com.demcha.compose.document.style.DocumentPageLayout;
import com.demcha.compose.document.style.DocumentPageMode;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * Backend-neutral PDF viewer preferences — how a reader should present the
 * document when it opens (page mode, page layout, and window-chrome flags),
 * written to the PDF document catalog.
 *
 * <p>Every field is optional: a {@code null} leaves the reader's own default in
 * place. Non-PDF backends ignore viewer preferences (a documented part of the
 * {@code DocumentOutputOptions} contract). Set via
 * {@code chrome().viewerPreferences(...)}.</p>
 *
 * @author Artem Demchyshyn
 * @since 1.9.0
 */
@Getter
@Builder(toBuilder = true)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class DocumentViewerPreferences {

    /** Page mode (e.g. open the bookmark panel), or {@code null} for the reader default. */
    private final DocumentPageMode pageMode;

    /** Page layout (e.g. two-column), or {@code null} for the reader default. */
    private final DocumentPageLayout pageLayout;

    /** Show the document title in the window title bar instead of the file name. */
    private final Boolean displayDocTitle;

    /** Hide the reader's toolbar. */
    private final Boolean hideToolbar;

    /** Hide the reader's menu bar. */
    private final Boolean hideMenubar;

    /** Resize the window to fit the first page. */
    private final Boolean fitWindow;

    /** Centre the window on screen. */
    private final Boolean centerWindow;

    /**
     * Preset that opens the reader with the bookmark (outline) panel visible —
     * pairs with {@code bookmark(...)} on sections and containers.
     *
     * @return viewer preferences with {@link DocumentPageMode#USE_OUTLINES}
     */
    public static DocumentViewerPreferences openOutline() {
        return builder().pageMode(DocumentPageMode.USE_OUTLINES).build();
    }
}
