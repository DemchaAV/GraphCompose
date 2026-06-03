package com.demcha.compose.document.templates.cv.v2.components;

import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.templates.cv.v2.data.SkillGroup;
import com.demcha.compose.document.templates.cv.v2.data.SkillsSection;
import com.demcha.compose.document.templates.cv.v2.theme.CvTheme;

/**
 * Default renderer for a grouped {@link SkillsSection}.
 *
 * <p>The data model says "these are labelled skill groups"; this
 * renderer chooses the conservative shared visual: one bulleted
 * group per paragraph with the category bolded and skills joined
 * inline. Presets with a stronger visual signature (for example a
 * compact grid/table) can branch on {@link SkillsSection} and render
 * the same data differently without parsing generic rows.</p>
 */
public final class SkillsRenderer {

    private SkillsRenderer() {
    }

    /**
     * Renders each skill group as one bulleted paragraph with the
     * category bolded and its skills joined inline.
     *
     * @param section the section builder being populated
     * @param skills  the grouped skills to render
     * @param theme   the active theme supplying palette, typography, and spacing
     */
    public static void render(SectionBuilder section,
                              SkillsSection skills,
                              CvTheme theme) {
        DocumentTextStyle style = theme.bodyStyle();
        DocumentInsets margin = DocumentInsets.top(
                (float) theme.spacing().paragraphMarginTop());
        for (SkillGroup group : skills.groups()) {
            String text = "**" + group.category() + ":** " + group.skillsInline();
            ParagraphPrimitive.writeBulleted(section, text, style,
                    theme.decoration().bulletGlyph(), margin, theme);
        }
    }
}
