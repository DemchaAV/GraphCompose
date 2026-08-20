package com.demcha.compose.document.templates.cv.components;

import com.demcha.compose.document.templates.cv.data.CvEntry;
import com.demcha.compose.document.templates.cv.data.CvItem;
import com.demcha.compose.document.templates.cv.data.CvKind;
import com.demcha.compose.document.templates.cv.data.CvSection;
import com.demcha.compose.document.templates.cv.data.EntriesSection;
import com.demcha.compose.document.templates.cv.data.ModuleSection;
import com.demcha.compose.document.templates.cv.data.ParagraphSection;
import com.demcha.compose.document.templates.cv.data.RowStyle;
import com.demcha.compose.document.templates.cv.data.RowsSection;
import com.demcha.compose.document.templates.cv.data.SectionRole;
import com.demcha.compose.document.templates.cv.data.SkillGroup;
import com.demcha.compose.document.templates.cv.data.SkillsSection;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.type;

/**
 * What each slot receives, and what the lowering costs.
 *
 * <p>The routing half is checked end to end by {@code RoleRoutingTest}, which
 * renders a foreign-language CV through every preset. What that cannot see is
 * the text itself: a preset draws whatever it is handed, so a module lowered
 * with the wrong separator or a doubled label renders perfectly and reads
 * wrong. These cases pin the strings.</p>
 */
class SectionRouterTest {

    private static List<CvSection> only(CvSection section) {
        return List.of(section);
    }

    // -- role beats heading, heading still works ------------------------

    @Test
    void aModuleIsFoundByItsRoleWhateverItsHeadingSays() {
        CvSection module = ModuleSection.builder("Berufserfahrung", SectionRole.EXPERIENCE,
                        CvKind.ENTRIES_DATED)
                .item(CvItem.of("Senior Engineer").period("2021"))
                .build();

        assertThat(SectionRouter.find(only(module), SectionRole.EXPERIENCE, List.of("experience")))
                .isSameAs(module);
    }

    @Test
    void aHeadingNeverOverrulesADeclaredRole() {
        // Both slots would otherwise claim it — the experience slot by role and
        // the projects slot by heading — and the module would render twice.
        CvSection module = ModuleSection.builder("Projects", SectionRole.EXPERIENCE,
                        CvKind.ENTRIES_DATED)
                .item(CvItem.of("Senior Engineer").period("2021"))
                .build();

        assertThat(SectionRouter.find(only(module), SectionRole.PROJECTS, List.of("projects")))
                .isNull();
    }

    @Test
    void aSectionWithNoRoleIsStillFoundByItsHeading() {
        CvSection legacy = new ParagraphSection("Professional Summary", "Backend engineer.");

        assertThat(SectionRouter.find(only(legacy), SectionRole.SUMMARY, List.of("summary")))
                .isSameAs(legacy);
    }

    @Test
    void anEmptyModuleDoesNotShadowASectionThatHasContent() {
        CvSection empty = ModuleSection.of("Experience", SectionRole.EXPERIENCE,
                CvKind.ENTRIES_DATED);
        CvSection populated = EntriesSection.builder("Experience")
                .entry("Senior Engineer", "Acme", "2021", "")
                .build();

        assertThat(SectionRouter.find(List.of(empty, populated), SectionRole.EXPERIENCE,
                List.of("experience"))).isSameAs(populated);
    }

    // -- what each lowering produces -----------------------------------

    @Test
    void datedEntriesKeepThePeriodAndUndatedOnesDropIt() {
        CvItem item = CvItem.of("Senior Engineer").at("Acme GmbH").in("Berlin")
                .period("2021 - Present").paragraphs("Owned payments.");

        assertThat(entriesOf(CvKind.ENTRIES_DATED, item))
                .singleElement()
                .satisfies(entry -> {
                    assertThat(entry.title()).isEqualTo("Senior Engineer");
                    assertThat(entry.subtitle()).isEqualTo("Acme GmbH · Berlin");
                    assertThat(entry.date()).isEqualTo("2021 - Present");
                    assertThat(entry.body()).isEqualTo("Owned payments.");
                });
        assertThat(entriesOf(CvKind.ENTRIES, item))
                .singleElement()
                .extracting(CvEntry::date)
                .as("ENTRIES ignores the period, so the slot draws no date column")
                .isEqualTo("");
    }

    @Test
    void aRowJoinsProseWithSpacesAndDiscretePointsWithCommas() {
        CvSection prose = rowsOf(RowStyle.PLAIN,
                CvItem.of("Languages").paragraphs("English (Fluent).", "German (B2)."));
        CvSection points = rowsOf(RowStyle.PLAIN,
                CvItem.of("Highlights").bullets("Doubled throughput", "Cut onboarding"));

        assertThat(rowBody(prose)).isEqualTo("English (Fluent). German (B2).");
        assertThat(rowBody(points))
                .as("bulleted points are discrete: joined with a space they read as one sentence")
                .isEqualTo("Doubled throughput, Cut onboarding");
    }

    @Test
    void onlyTheStackedRowStyleCarriesALinkedTitle() {
        CvItem linked = CvItem.of("GraphCompose").linkedTo("https://example.dev/gc")
                .paragraphs("A layout engine.");

        assertThat(rowLabel(rowsOf(RowStyle.BULLETED_STACKED, linked)))
                .as("the stacked row bolds through the text style, so the link survives")
                .isEqualTo("[GraphCompose](https://example.dev/gc)");
        assertThat(rowLabel(rowsOf(RowStyle.PLAIN, linked)))
                .as("an inline row bolds by wrapping in markdown, which would nest "
                        + "around the link and print literal asterisks")
                .isEqualTo("GraphCompose");
    }

    @Test
    void aPlainListOfSkillsArrivesAsSkillsNotAsCategoriesHoldingThemselves() {
        CvSection lowered = SectionRouter.skills(only(ModuleSection
                .builder("Kenntnisse", SectionRole.SKILLS, CvKind.BULLETS)
                .item("Java 21")
                .item("Kotlin")
                .build()), SectionRole.SKILLS, List.of("skills"));

        assertThat(lowered).asInstanceOf(type(SkillsSection.class))
                .extracting(SkillsSection::groups, org.assertj.core.api.InstanceOfAssertFactories.LIST)
                .singleElement()
                .satisfies(group -> {
                    SkillGroup skillGroup = (SkillGroup) group;
                    assertThat(skillGroup.category()).isEqualTo("Kenntnisse");
                    assertThat(skillGroup.skills()).containsExactly("Java 21", "Kotlin");
                });
    }

    @Test
    void anItemWithADescriptionBecomesItsOwnSkillCategory() {
        CvSection lowered = SectionRouter.skills(only(ModuleSection
                .builder("Technical Skills", SectionRole.SKILLS, CvKind.INLINE_LIST)
                .item(CvItem.of("Languages").paragraphs("Java 21", "Kotlin"))
                .item("Docker")
                .build()), SectionRole.SKILLS, List.of("skills"));

        SkillsSection skills = (SkillsSection) lowered;
        assertThat(skills.groups()).extracting(SkillGroup::category)
                .containsExactly("Languages", "Technical Skills");
        assertThat(skills.groups().get(0).skills()).containsExactly("Java 21", "Kotlin");
        assertThat(skills.groups().get(1).skills()).containsExactly("Docker");
    }

    @Test
    void proseJoinsEveryItemsDescriptionIntoOneBlock() {
        CvSection lowered = SectionRouter.paragraph(only(ModuleSection
                .builder("Profile", SectionRole.SUMMARY, CvKind.PARAGRAPH)
                .item(CvItem.of("first").paragraphs("Backend engineer.", "Ten years of it."))
                .build()), SectionRole.SUMMARY, List.of("summary"));

        assertThat(lowered).asInstanceOf(type(ParagraphSection.class))
                .extracting(ParagraphSection::body)
                .isEqualTo("Backend engineer. Ten years of it.");
    }

    @Test
    void aSectionThatIsAlreadyTheRightShapePassesThroughUntouched() {
        CvSection legacy = EntriesSection.builder("Experience")
                .entry("Senior Engineer", "Acme", "2021", "").build();

        assertThat(SectionRouter.entries(only(legacy), SectionRole.EXPERIENCE,
                List.of("experience")))
                .as("lowering must not rebuild what a preset already handles")
                .isSameAs(legacy);
    }

    @Test
    void nothingMatchingYieldsNullFromEveryFinder() {
        List<CvSection> none = List.of(new ParagraphSection("Awards", "Employee of the year"));

        assertThat(SectionRouter.entries(none, SectionRole.EXPERIENCE, List.of("experience"))).isNull();
        assertThat(SectionRouter.rows(none, SectionRole.PROJECTS, List.of("projects"),
                RowStyle.PLAIN)).isNull();
        assertThat(SectionRouter.paragraph(none, SectionRole.SUMMARY, List.of("summary"))).isNull();
        assertThat(SectionRouter.skills(none, SectionRole.SKILLS, List.of("skills"))).isNull();
        assertThat(SectionRouter.find(null, SectionRole.SKILLS, List.of("skills"))).isNull();
    }

    // -- helpers ---------------------------------------------------------

    private static List<CvEntry> entriesOf(CvKind kind, CvItem item) {
        CvSection lowered = SectionRouter.entries(only(ModuleSection
                        .of("Experience", SectionRole.EXPERIENCE, kind, item)),
                SectionRole.EXPERIENCE, List.of("experience"));
        return ((EntriesSection) lowered).entries();
    }

    private static CvSection rowsOf(RowStyle style, CvItem item) {
        return SectionRouter.rows(only(ModuleSection
                        .of("Section", SectionRole.OTHER, CvKind.BULLETS, item)),
                SectionRole.OTHER, List.of("section"), style);
    }

    private static String rowBody(CvSection section) {
        return ((RowsSection) section).rows().get(0).body();
    }

    private static String rowLabel(CvSection section) {
        return ((RowsSection) section).rows().get(0).label();
    }
}
