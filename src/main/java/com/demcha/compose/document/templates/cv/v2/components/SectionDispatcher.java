package com.demcha.compose.document.templates.cv.v2.components;

import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.templates.cv.v2.data.CvEntry;
import com.demcha.compose.document.templates.cv.v2.data.CvRow;
import com.demcha.compose.document.templates.cv.v2.data.CvSection;
import com.demcha.compose.document.templates.cv.v2.data.EntriesSection;
import com.demcha.compose.document.templates.cv.v2.data.ParagraphSection;
import com.demcha.compose.document.templates.cv.v2.data.RowsSection;
import com.demcha.compose.document.templates.cv.v2.theme.CvTheme;

/**
 * Single entry point that renders any {@link CvSection} body into a
 * host {@link SectionBuilder}, dispatching on the sealed subtype.
 *
 * <p>Adding a new {@code CvSection} variant means adding a branch
 * here — the final {@code else} throws so that an unhandled subtype
 * fails loudly at runtime. On Java 21+ this becomes a pattern-match
 * switch with compiler-enforced exhaustiveness; we target Java 17 so
 * the if/else cascade is the cleanest form available.</p>
 *
 * <p>Presets call this from their page-flow loop — that is the
 * <em>only</em> place a preset reasons about section subtypes. Every
 * other rendering decision (palette, typography, layout weights) is
 * settled by the theme.</p>
 */
public final class SectionDispatcher {

    private SectionDispatcher() {
    }

    public static void renderBody(SectionBuilder host, CvSection section, CvTheme theme) {
        host.spacing(theme.spacing().sectionBodySpacing())
                .padding(theme.spacing().sectionBodyPadding());

        if (section instanceof ParagraphSection p) {
            ParagraphRenderer.render(host, p.body(), theme);
        } else if (section instanceof RowsSection r) {
            for (CvRow row : r.rows()) {
                RowRenderer.render(host, row, r.style(), theme);
            }
        } else if (section instanceof EntriesSection e) {
            for (CvEntry entry : e.entries()) {
                EntryRenderer.render(host, entry, theme);
            }
        } else {
            throw new IllegalStateException(
                    "Unknown CvSection subtype: " + section.getClass().getName());
        }
    }
}
