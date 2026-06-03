package com.demcha.compose.document.templates.cv.v2.components;

import com.demcha.compose.document.templates.cv.v2.data.CvSection;
import com.demcha.compose.document.templates.cv.v2.data.EntriesSection;
import com.demcha.compose.document.templates.cv.v2.data.ParagraphSection;
import com.demcha.compose.document.templates.cv.v2.data.RowsSection;
import com.demcha.compose.document.templates.cv.v2.data.SkillsSection;

import java.util.List;

/**
 * Shared section title matching for CV presets that still receive free-form section names.
 */
public final class SectionLookup {
    private SectionLookup() {
    }

    /**
     * Returns the first section whose normalised title contains any of
     * the normalised keys.
     *
     * @param sections the sections to scan; null yields {@code null}
     * @param keys     the candidate title fragments; null yields {@code null}
     * @return the first matching section, or {@code null} if none match
     */
    public static CvSection firstMatching(List<CvSection> sections,
                                          List<String> keys) {
        if (sections == null || keys == null) {
            return null;
        }

        for (CvSection section : sections) {
            if (section == null) {
                continue;
            }
            String normalizedTitle = normalize(section.title());
            for (String key : keys) {
                if (normalizedTitle.contains(normalize(key))) {
                    return section;
                }
            }
        }
        return null;
    }

    /**
     * Reports whether the section carries any renderable content.
     *
     * @param section the section to inspect
     * @return {@code true} if the section has non-empty body, entries,
     *         rows, or skill groups
     */
    public static boolean hasContent(CvSection section) {
        if (section instanceof ParagraphSection paragraph) {
            return paragraph.body() != null && !paragraph.body().isBlank();
        }
        if (section instanceof EntriesSection entries) {
            return entries.entries() != null && !entries.entries().isEmpty();
        }
        if (section instanceof RowsSection rows) {
            return rows.rows() != null && !rows.rows().isEmpty();
        }
        if (section instanceof SkillsSection skills) {
            return skills.groups() != null && !skills.groups().isEmpty();
        }
        return false;
    }

    /**
     * Reports whether the normalised title contains the normalised key.
     *
     * @param title the title to test
     * @param key   the fragment to look for
     * @return {@code true} if the normalised title contains the normalised key
     */
    public static boolean titleContains(String title, String key) {
        return normalize(title).contains(normalize(key));
    }

    /**
     * Normalises a value to lower-case alphanumerics for loose matching.
     *
     * @param value the value to normalise; null yields an empty string
     * @return the lower-cased value with non-alphanumeric characters removed
     */
    public static String normalize(String value) {
        return value == null ? "" : value.toLowerCase().replaceAll("[^a-z0-9]+", "");
    }
}
