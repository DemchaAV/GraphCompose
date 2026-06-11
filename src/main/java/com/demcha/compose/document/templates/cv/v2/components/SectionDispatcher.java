package com.demcha.compose.document.templates.cv.v2.components;

import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.templates.cv.v2.data.*;
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

    /**
     * Renders the section body into the host, dispatching on the
     * sealed {@link CvSection} subtype.
     *
     * @param host    host section receiving the body
     * @param section the section whose subtype selects the renderer
     * @param theme   the active theme supplying palette, typography, and spacing
     * @throws IllegalStateException if the section subtype is unhandled
     */
    public static void renderBody(SectionBuilder host, CvSection section, CvTheme theme) {
        host.spacing(theme.spacing().sectionBodySpacing())
                .padding(theme.spacing().sectionBodyPadding());

        if (section instanceof ParagraphSection p) {
            ParagraphRenderer.render(host, p.body(), theme);
        } else if (section instanceof SkillsSection s) {
            SkillsRenderer.render(host, s, theme);
        } else if (section instanceof RowsSection r) {
            // Multi-line stacked rows (Projects-style) get a spacer
            // between items so consecutive entries don't visually
            // collapse into a wall of text. Single-line styles (PLAIN,
            // BULLETED) already breathe via paragraphMarginTop.
            boolean stackedNeedsSeparator =
                    r.style() == com.demcha.compose.document.templates.cv.v2.data.RowStyle.BULLETED_STACKED;
            for (int i = 0; i < r.rows().size(); i++) {
                if (i > 0 && stackedNeedsSeparator) {
                    host.spacer(0, theme.spacing().entrySeparation());
                }
                RowRenderer.render(host, r.rows().get(i), r.style(), theme);
            }
        } else if (section instanceof EntriesSection e) {
            // Timeline entries (Education, Experience) get a spacer
            // between items — each entry is a multi-line block
            // (title + subtitle + body) and without a gap the
            // boundary between consecutive entries becomes invisible.
            for (int i = 0; i < e.entries().size(); i++) {
                if (i > 0) {
                    host.spacer(0, theme.spacing().entrySeparation());
                }
                EntryRenderer.render(host, e.entries().get(i), theme);
            }
        } else {
            throw new IllegalStateException(
                    "Unknown CvSection subtype: " + section.getClass().getName());
        }
    }
}
