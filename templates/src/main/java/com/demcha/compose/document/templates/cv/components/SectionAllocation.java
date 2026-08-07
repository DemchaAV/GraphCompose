package com.demcha.compose.document.templates.cv.components;

import com.demcha.compose.document.templates.cv.data.CvSection;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Assigns a CV's sections to a preset's fixed modules and keeps whatever is
 * left over.
 *
 * <p>{@link SectionLookup#firstMatching} answers "which section is the
 * Education one?" and nothing else. A preset built only from those calls
 * renders exactly the categories it thought to ask about: a second section
 * matching the same keywords is shadowed by the first, and a section whose
 * title matches no list at all — "Awards", "Publications", anything a user
 * invented — is never looked at. Neither loss is visible in the output. The
 * page renders, it looks finished, and the reader has no way to tell that a
 * section was dropped.</p>
 *
 * <p>This narrows that to an allocation: {@link #claim(List)} hands out each
 * section <em>once</em>, and {@link #remaining()} returns everything no module
 * asked for, in document order, so the preset can render it rather than
 * discard it.</p>
 *
 * <p>Not thread-safe, and not meant to be: a preset builds one of these inside
 * a single {@code compose} call and drops it.</p>
 *
 * @since 2.1.2
 */
public final class SectionAllocation {

    private final List<CvSection> sections;
    private final Map<CvSection, Boolean> claimed = new IdentityHashMap<>();

    private SectionAllocation(List<CvSection> sections) {
        this.sections = sections;
    }

    /**
     * Starts an allocation over a document's sections.
     *
     * @param sections the sections to allocate; {@code null} is treated as empty
     * @return a fresh allocation with nothing claimed yet
     */
    public static SectionAllocation of(List<CvSection> sections) {
        List<CvSection> copy = new ArrayList<>();
        if (sections != null) {
            for (CvSection section : sections) {
                if (section != null) {
                    copy.add(section);
                }
            }
        }
        return new SectionAllocation(List.copyOf(copy));
    }

    /**
     * Claims the first not-yet-claimed section whose normalised title contains
     * any of the keys.
     *
     * <p>Claiming is what makes a second call with overlapping keys return a
     * <em>different</em> section instead of the same one twice, and what moves
     * the section out of {@link #remaining()}.</p>
     *
     * @param keys candidate title fragments; {@code null} claims nothing
     * @return the claimed section, or {@code null} when nothing matches
     */
    public CvSection claim(List<String> keys) {
        if (keys == null) {
            return null;
        }
        for (CvSection section : sections) {
            if (claimed.containsKey(section)) {
                continue;
            }
            String title = SectionLookup.normalize(section.title());
            for (String key : keys) {
                if (title.contains(SectionLookup.normalize(key))) {
                    claimed.put(section, Boolean.TRUE);
                    return section;
                }
            }
        }
        return null;
    }

    /**
     * The sections no module claimed, in document order.
     *
     * <p>These are the ones a keyword-only preset loses silently. Render them
     * under {@link CvSection#title()} — the user wrote that title, and it is
     * the only label that can be correct for a category the preset does not
     * know about.</p>
     *
     * @return unclaimed sections carrying content, in document order
     */
    public List<CvSection> remaining() {
        List<CvSection> rest = new ArrayList<>();
        for (CvSection section : sections) {
            if (!claimed.containsKey(section) && SectionLookup.hasContent(section)) {
                rest.add(section);
            }
        }
        return List.copyOf(rest);
    }

    /**
     * The section's own title, or the preset's label when there is no section.
     *
     * <p>A preset that hardcodes its module labels renames the user's content:
     * a section the author called "Projects" prints as "EXPERTISE", and
     * "Additional Information" prints as "LANGUAGES". The author's title wins.
     * There is no blank-title case to weigh against it — every
     * {@link CvSection} implementation rejects a blank title at construction —
     * so the label is reached only when the module found no section at all,
     * which is also when it has nothing to render.</p>
     *
     * @param section  the claimed section, or {@code null} when none matched
     * @param fallback the preset's own label for the module
     * @return the title to print
     * @throws NullPointerException if {@code fallback} is {@code null}
     */
    public static String titleOr(CvSection section, String fallback) {
        Objects.requireNonNull(fallback, "fallback");
        return section == null ? fallback : section.title();
    }
}
