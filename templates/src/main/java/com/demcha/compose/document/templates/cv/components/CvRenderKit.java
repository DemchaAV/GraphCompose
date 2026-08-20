package com.demcha.compose.document.templates.cv.components;

import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.templates.core.theme.BrandTheme;
import com.demcha.compose.document.templates.cv.data.CvEntry;
import com.demcha.compose.document.templates.cv.data.CvItem;
import com.demcha.compose.document.templates.cv.data.CvKind;
import com.demcha.compose.document.templates.cv.data.CvRow;
import com.demcha.compose.document.templates.cv.data.RowStyle;

/**
 * How one template draws the three shapes a CV section body reduces to:
 * a paragraph of prose, a label/value row, a timeline entry.
 *
 * <p>A preset that wants runtime {@code ModuleSection}s to look like the
 * rest of its own document implements this and hands it back through
 * {@link com.demcha.compose.document.templates.cv.api.ModularCvTemplate};
 * {@link #defaults()} draws them the canonical way, and every method has a
 * default, so a preset overrides only the shapes it actually styles
 * differently.</p>
 *
 * <p><strong>Why the primitives and not the kinds.</strong> The obvious
 * alternative is a function per {@link CvKind}. It puts the wrong work on
 * the preset: turning a {@link CvItem} into an entry or a row means
 * deciding what a linked title looks like, how a subtitle and a location
 * join, which fields the kind ignores, what an empty description does to a
 * trailing colon — rules that belong to the model and must not be
 * re-decided sixteen times. {@link ModuleRenderer} keeps that lowering and
 * asks the kit only to draw what came out of it, which is exactly the part
 * a preset has an opinion about. It is also the shape the presets already
 * have: their private renderers take a {@code CvEntry} or a {@code CvRow}
 * today.</p>
 *
 * <p>Implementations draw into the host and return; they do not set the
 * host's spacing or padding, which the caller has already settled, and
 * they do not insert separators between items — {@code ModuleRenderer}
 * owns the gaps so that spacing stays uniform whoever is drawing.</p>
 *
 * @since 2.3.0
 */
public interface CvRenderKit {

    /**
     * The canonical kit: every shape drawn by the shared components, which
     * is what a section rendered through
     * {@link SectionDispatcher#renderBody(SectionBuilder, com.demcha.compose.document.templates.cv.data.CvSection, BrandTheme)}
     * has always produced.
     *
     * @return a kit that draws every shape the canonical way
     */
    static CvRenderKit defaults() {
        return DEFAULTS;
    }

    /** The canonical kit. Stateless, so one instance serves every caller. */
    CvRenderKit DEFAULTS = new CvRenderKit() {
    };

    /**
     * Draws one paragraph of prose. Blank text draws nothing.
     *
     * @param host  host section receiving the paragraph
     * @param text  the prose; may carry inline markdown
     * @param theme the active theme
     */
    default void paragraph(SectionBuilder host, String text, BrandTheme theme) {
        ParagraphRenderer.render(host, text, theme);
    }

    /**
     * Draws one label/value row with the given decoration.
     *
     * @param host  host section receiving the row
     * @param row   label and body
     * @param style plain, bulleted, or bulleted with the body stacked under
     *              the label
     * @param theme the active theme
     */
    default void row(SectionBuilder host, CvRow row, RowStyle style, BrandTheme theme) {
        RowRenderer.render(host, row, style, theme);
    }

    /**
     * Draws one timeline entry. A blank {@code date} collapses the date
     * column rather than reserving an empty one.
     *
     * @param host  host section receiving the entry
     * @param entry title, subtitle, date, and body
     * @param theme the active theme
     */
    default void entry(SectionBuilder host, CvEntry entry, BrandTheme theme) {
        EntryRenderer.render(host, entry, theme);
    }
}
