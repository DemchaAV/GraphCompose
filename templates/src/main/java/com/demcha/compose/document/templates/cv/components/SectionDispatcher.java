package com.demcha.compose.document.templates.cv.components;

import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.templates.cv.api.CvConstructor;
import com.demcha.compose.document.templates.cv.data.*;
import com.demcha.compose.document.templates.core.theme.BrandTheme;

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
    public static void renderBody(SectionBuilder host, CvSection section, BrandTheme theme) {
        renderBody(host, section, theme, CvRenderKit.defaults());
    }

    /**
     * Renders the section body, sending a runtime module through
     * {@code constructor} so each kind lands on the method the template
     * implemented for it.
     *
     * <p>Typed sections still take the canonical path: the constructor
     * is the module contract, not a second dispatcher for
     * {@code EntriesSection}.</p>
     *
     * @param host         host section receiving the body
     * @param section      the section whose subtype selects the renderer
     * @param theme        the active theme supplying palette, typography, and spacing
     * @param constructor  the template's kind methods
     * @throws IllegalStateException if the section subtype is unhandled
     * @since 2.3.0
     */
    public static void renderBody(SectionBuilder host, CvSection section, BrandTheme theme,
                                  CvConstructor constructor) {
        if (section instanceof ModuleSection module) {
            host.spacing(theme.spacing().sectionBodySpacing())
                    .padding(theme.spacing().sectionBodyPadding());
            constructor.render(host, module, theme);
            return;
        }
        renderBody(host, section, theme);
    }

    /**
     * Renders the section body, drawing through {@code kit}.
     *
     * <p>The routing is identical to the three-argument form; only who draws
     * differs. Prefer the {@link CvConstructor} overload for a modular
     * template: that is the kind contract. This overload remains for a
     * preset that is not yet on that contract and restyles the three
     * drawing primitives.</p>
     *
     * @param host    host section receiving the body
     * @param section the section whose subtype selects the renderer
     * @param theme   the active theme supplying palette, typography, and spacing
     * @param kit     how this template draws paragraphs, rows, and entries
     * @throws IllegalStateException if the section subtype is unhandled
     * @since 2.3.0
     */
    public static void renderBody(SectionBuilder host, CvSection section, BrandTheme theme,
                                  CvRenderKit kit) {
        host.spacing(theme.spacing().sectionBodySpacing())
                .padding(theme.spacing().sectionBodyPadding());

        if (section instanceof ParagraphSection p) {
            kit.paragraph(host, p.body(), theme);
        } else if (section instanceof SkillsSection s) {
            SkillsRenderer.render(host, s, theme);
        } else if (section instanceof RowsSection r) {
            // Multi-line stacked rows (Projects-style) get a spacer
            // between items so consecutive entries don't visually
            // collapse into a wall of text. Single-line styles (PLAIN,
            // BULLETED) already breathe via paragraphMarginTop.
            boolean stackedNeedsSeparator =
                    r.style() == com.demcha.compose.document.templates.cv.data.RowStyle.BULLETED_STACKED;
            for (int i = 0; i < r.rows().size(); i++) {
                if (i > 0 && stackedNeedsSeparator) {
                    host.spacer(0, theme.spacing().entrySeparation());
                }
                kit.row(host, r.rows().get(i), r.style(), theme);
            }
        } else if (section instanceof ModuleSection m) {
            // Runtime-assembled module. The kind decides which of the
            // renderers above each item lands on, so this branch draws
            // nothing of its own — see ModuleRenderer.
            ModuleRenderer.render(host, m, theme, kit);
        } else if (section instanceof EntriesSection e) {
            // Timeline entries (Education, Experience) get a spacer
            // between items — each entry is a multi-line block
            // (title + subtitle + body) and without a gap the
            // boundary between consecutive entries becomes invisible.
            for (int i = 0; i < e.entries().size(); i++) {
                if (i > 0) {
                    host.spacer(0, theme.spacing().entrySeparation());
                }
                kit.entry(host, e.entries().get(i), theme);
            }
        } else {
            throw new IllegalStateException(
                    "Unknown CvSection subtype: " + section.getClass().getName());
        }
    }
}
