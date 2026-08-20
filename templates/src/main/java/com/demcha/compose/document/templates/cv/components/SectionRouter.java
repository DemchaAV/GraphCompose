package com.demcha.compose.document.templates.cv.components;

import com.demcha.compose.document.templates.core.identity.Link;
import com.demcha.compose.document.templates.cv.data.BodyStyle;
import com.demcha.compose.document.templates.cv.data.CvEntry;
import com.demcha.compose.document.templates.cv.data.CvItem;
import com.demcha.compose.document.templates.cv.data.CvKind;
import com.demcha.compose.document.templates.cv.data.CvRow;
import com.demcha.compose.document.templates.cv.data.CvSection;
import com.demcha.compose.document.templates.cv.data.EntriesSection;
import com.demcha.compose.document.templates.cv.data.ModuleSection;
import com.demcha.compose.document.templates.cv.data.ParagraphSection;
import com.demcha.compose.document.templates.cv.data.RowStyle;
import com.demcha.compose.document.templates.cv.data.RowsSection;
import com.demcha.compose.document.templates.cv.data.SectionRole;
import com.demcha.compose.document.templates.cv.data.SkillGroup;
import com.demcha.compose.document.templates.cv.data.SkillsSection;

import java.util.ArrayList;
import java.util.List;

/**
 * Finds the section a preset's slot should hold, by what it means rather
 * than by what it is called — and hands it back in the shape that slot
 * knows how to draw.
 *
 * <p>Presets with a designed layout place their sections into fixed slots,
 * and they have been choosing what goes where by matching the section's
 * heading against a list of English words each preset keeps privately. That
 * works for a CV written in English by someone who used the expected
 * headings. A CV headed {@code Ausbildung} or {@code Навыки} matches
 * nothing and loses the section; so does {@code "Volunteering"}, and so does
 * a second section whose heading matched a word the first one took.</p>
 *
 * <p>A {@link ModuleSection} carries a {@link SectionRole} because the
 * author already knew the answer, so the role is asked first and the
 * keywords are the fallback for the sections that have no role to give.</p>
 *
 * <p>The second half is the shape. These slots are written against a
 * particular section type — {@code if (!(section instanceof EntriesSection
 * entries)) return;} — because each draws its content its own way, and a
 * module reaching one would be dropped by that guard however well it was
 * routed. Each finder below therefore lowers a matched module to the type
 * its slot expects, so the preset draws it exactly as it draws everything
 * else. What that costs is stated per method: a module's description lines
 * are joined where the target type holds one string, and a bulleted
 * description reads as prose.</p>
 *
 * <p>A section that matches no slot at all is a different question, and one
 * this class cannot answer: it keeps no memory of what a previous slot took.
 * {@link SectionAllocation} does, so a preset that wants to draw the leftovers
 * claims through it and reads {@link SectionAllocation#remaining()} — with
 * {@link #naturalShape(CvSection)} for the shape those sections have no slot
 * to give them.</p>
 *
 * @since 2.3.0
 */
public final class SectionRouter {

    private SectionRouter() {
    }

    /**
     * The section for a timeline slot — education, experience, anything the
     * preset draws as dated entries.
     *
     * <p>A matched module becomes an {@link EntriesSection}: each item's
     * title, its subtitle and location joined, its period (blank when the
     * module's kind does not read one), and its description lines joined
     * into the single body string a {@link CvEntry} holds. A description the
     * author asked to bullet reads as prose here — the slot draws one
     * paragraph.</p>
     *
     * @param sections the document's sections for this slot's column
     * @param role     the role this slot holds
     * @param keys     heading fragments to fall back on
     * @return an {@code EntriesSection}, or {@code null} when nothing matches
     */
    public static CvSection entries(List<CvSection> sections, SectionRole role,
                                    List<String> keys) {
        return asEntries(find(sections, role, keys));
    }

    /**
     * The entries shape of an already-chosen section.
     *
     * <p>The lowering half of {@link #entries(List, SectionRole, List)}, split
     * out so a caller that picked the section itself — {@link SectionAllocation},
     * which has to record the claim to know what is left over — draws it the
     * same way rather than routing twice.</p>
     *
     * @param found the section a slot claimed, or {@code null}
     * @return the section as entries, or {@code null}
     * @since 2.3.0
     */
    public static CvSection asEntries(CvSection found) {
        if (!(found instanceof ModuleSection module)) {
            return found;
        }
        List<CvEntry> entries = new ArrayList<>(module.items().size());
        boolean dated = module.kind() == CvKind.ENTRIES_DATED;
        for (CvItem item : module.items()) {
            entries.add(new CvEntry(title(item), subtitleWithLocation(item),
                    dated ? item.period() : "", String.join(" ", item.body())));
        }
        return new EntriesSection(module.title(), entries);
    }

    /**
     * The section for a label/value slot — projects, languages, additional
     * information.
     *
     * <p>A matched module becomes a {@link RowsSection} in the caller's
     * {@link RowStyle}: one row per item, its title the label and its
     * description lines joined into the body.</p>
     *
     * @param sections the document's sections for this slot's column
     * @param role     the role this slot holds
     * @param keys     heading fragments to fall back on
     * @param style    the decoration this slot draws rows with
     * @return a {@code RowsSection}, or {@code null} when nothing matches
     */
    public static CvSection rows(List<CvSection> sections, SectionRole role,
                                 List<String> keys, RowStyle style) {
        return asRows(find(sections, role, keys), style);
    }

    /**
     * The rows shape of an already-chosen section.
     *
     * @param found the section a slot claimed, or {@code null}
     * @param style the decoration this slot draws rows with
     * @return the section as rows, or {@code null}
     * @since 2.3.0
     */
    public static CvSection asRows(CvSection found, RowStyle style) {
        if (!(found instanceof ModuleSection module)) {
            return found;
        }
        List<CvRow> rows = new ArrayList<>(module.items().size());
        for (CvItem item : module.items()) {
            // Discrete points are joined with a comma and prose with a space:
            // a row holds one string, and "Rebuilt the ledger Cut p99 40%" reads
            // as one garbled sentence while "Shipped it., Measured it." puts a
            // comma after a full stop.
            String separator = item.bodyStyle() == BodyStyle.BULLETS ? ", " : " ";
            // Only the stacked style keeps a linked title: the inline ones bold
            // their label by wrapping it in markdown markers, which would nest
            // around the link and reach the page as literal asterisks.
            String label = style == RowStyle.BULLETED_STACKED ? title(item) : item.title();
            rows.add(new CvRow(label, String.join(separator, item.body())));
        }
        return new RowsSection(module.title(), rows, style);
    }

    /**
     * The section for a prose slot — a profile, a summary, an objective.
     *
     * <p>A matched module becomes a {@link ParagraphSection} whose body is
     * every item's description, joined. The slot holds one block of prose,
     * so a module with several items reads as one.</p>
     *
     * @param sections the document's sections for this slot's column
     * @param role     the role this slot holds
     * @param keys     heading fragments to fall back on
     * @return a {@code ParagraphSection}, or {@code null} when nothing matches
     */
    public static CvSection paragraph(List<CvSection> sections, SectionRole role,
                                      List<String> keys) {
        return asParagraph(find(sections, role, keys));
    }

    /**
     * The prose shape of an already-chosen section.
     *
     * @param found the section a slot claimed, or {@code null}
     * @return the section as one paragraph, or {@code null}
     * @since 2.3.0
     */
    public static CvSection asParagraph(CvSection found) {
        if (!(found instanceof ModuleSection module)) {
            return found;
        }
        List<String> lines = new ArrayList<>();
        for (CvItem item : module.items()) {
            lines.addAll(item.body());
        }
        return new ParagraphSection(module.title(), String.join(" ", lines));
    }

    /**
     * The section for a skills slot — the one preset slot that wants
     * categories rather than lines, because it may draw a table, chips, or
     * proficiency bars.
     *
     * <p>A matched module becomes a {@link SkillsSection}: an item with a
     * description is a category whose skills are its lines, and the items
     * with none are collected into one group under the module's own heading —
     * a plain list of skills is a list of skills, not a set of categories
     * each holding itself.</p>
     *
     * @param sections the document's sections for this slot's column
     * @param role     the role this slot holds
     * @param keys     heading fragments to fall back on
     * @return a {@code SkillsSection}, or {@code null} when nothing matches
     */
    public static CvSection skills(List<CvSection> sections, SectionRole role,
                                   List<String> keys) {
        return asSkills(find(sections, role, keys));
    }

    /**
     * The grouped-skills shape of an already-chosen section.
     *
     * @param found the section a slot claimed, or {@code null}
     * @return the section as skill groups, or {@code null}
     * @since 2.3.0
     */
    public static CvSection asSkills(CvSection found) {
        if (!(found instanceof ModuleSection module)) {
            return found;
        }
        List<SkillGroup> groups = new ArrayList<>(module.items().size());
        List<String> loose = new ArrayList<>();
        for (CvItem item : module.items()) {
            if (item.body().isEmpty()) {
                // A skill with nothing under it is a skill, not a category of
                // one: making it its own group prints "Java 21: Java 21".
                loose.add(item.title());
                continue;
            }
            groups.add(SkillGroup.ofNames(item.title(), item.body()));
        }
        if (!loose.isEmpty()) {
            groups.add(SkillGroup.ofNames(module.title(), loose));
        }
        return new SkillsSection(module.title(), groups);
    }

    /**
     * The shape the section's own author asked for.
     *
     * <p>A slot lowers a module to the shape <em>it</em> draws. A section no
     * slot claimed has no such answer, so this asks the module instead: its
     * {@link CvKind} is the shape it was built as, which is the only reading
     * that cannot be wrong for a category the preset does not know about.
     * Anything that is not a module is already a shape and comes back
     * unchanged.</p>
     *
     * @param section a section, typically one from {@link SectionAllocation#remaining()}
     * @return the section in the shape its kind names
     * @since 2.3.0
     */
    public static CvSection naturalShape(CvSection section) {
        if (!(section instanceof ModuleSection module)) {
            return section;
        }
        return switch (module.kind()) {
            case PARAGRAPH -> asParagraph(section);
            case ENTRIES, ENTRIES_DATED -> asEntries(section);
            case BULLETS -> asRows(section, RowStyle.BULLETED);
            case BULLETS_STACKED -> asRows(section, RowStyle.BULLETED_STACKED);
            case INLINE_LIST -> asRows(section, RowStyle.PLAIN);
        };
    }

    /**
     * The section this slot should hold, or {@code null} when the document
     * has none: the first module whose role is the slot's, else the first
     * section whose heading matches one of the keys.
     *
     * <p>Role first, and only a role the author actually chose —
     * {@link SectionRole#OTHER} is what a module carries when the catalogue
     * has no name for it, so it never claims a slot and falls through to the
     * headings like any other section.</p>
     *
     * @param sections the document's sections for this slot's column
     * @param role     the role this slot holds
     * @param keys     heading fragments to fall back on
     * @return the section, or {@code null} when nothing matches
     */
    public static CvSection find(List<CvSection> sections, SectionRole role,
                                 List<String> keys) {
        if (sections == null) {
            return null;
        }
        if (role != null && role != SectionRole.OTHER) {
            for (CvSection section : sections) {
                if (section instanceof ModuleSection module && module.role() == role
                        && SectionLookup.hasContent(section)) {
                    return section;
                }
            }
        }
        // The heading is the fallback, and it may not overrule a role. A module
        // declared EXPERIENCE and headed "Projects" belongs where its author put
        // it; letting the projects slot claim it by heading would render it in
        // both places, which is worse than the drop this routing exists to fix.
        return SectionLookup.firstMatching(spokenFor(sections), keys);
    }

    /** The sections a keyword slot may still claim: everything but a module that named its own role. */
    private static List<CvSection> spokenFor(List<CvSection> sections) {
        List<CvSection> open = new ArrayList<>(sections.size());
        for (CvSection section : sections) {
            if (section instanceof ModuleSection module && module.role() != SectionRole.OTHER) {
                continue;
            }
            open.add(section);
        }
        return open;
    }

    /** The title, as markdown link syntax when the item carries a link. */
    private static String title(CvItem item) {
        Link link = item.link();
        if (link == null || item.title().indexOf('[') >= 0 || item.title().indexOf(']') >= 0) {
            return item.title();
        }
        return "[" + item.title() + "](" + link.url() + ")";
    }

    /** Subtitle and location joined, or whichever exists, or blank. */
    private static String subtitleWithLocation(CvItem item) {
        if (item.subtitle().isBlank()) {
            return item.location();
        }
        if (item.location().isBlank()) {
            return item.subtitle();
        }
        return item.subtitle() + " · " + item.location();
    }
}
