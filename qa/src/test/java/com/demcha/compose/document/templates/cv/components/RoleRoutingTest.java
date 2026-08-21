package com.demcha.compose.document.templates.cv.components;

import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.cv.CvComposedText;
import com.demcha.compose.document.templates.cv.data.CvDocument;
import com.demcha.compose.document.templates.cv.data.CvIdentity;
import com.demcha.compose.document.templates.cv.data.CvItem;
import com.demcha.compose.document.templates.cv.data.CvKind;
import com.demcha.compose.document.templates.cv.data.ModuleSection;
import com.demcha.compose.document.templates.cv.data.SectionRole;
import com.demcha.compose.document.templates.cv.presets.CvTemplates;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A CV whose headings are in the author's own language reaches the page on
 * every preset.
 *
 * <p>Presets with a designed layout place sections into fixed slots, and they
 * chose what goes where by matching the heading against a list of English
 * words each kept privately. A CV headed {@code Berufserfahrung} or
 * {@code Опыт работы} matched nothing: the section was dropped and the slot
 * that wanted it rendered empty. Nothing failed — the CV came out looking
 * finished, one job short.</p>
 *
 * <p>A module states its {@link SectionRole}, so the routing has an answer
 * that does not depend on the language the CV is written in. Every heading
 * here is deliberately in Russian and German: if any preset still routes by
 * keyword, its slot stays empty and this goes red.</p>
 */
class RoleRoutingTest {

    private static Stream<Named<DocumentTemplate<CvDocument>>> everyPreset() {
        return CvTemplates.all().stream().map(t -> Named.of(t.id(), t));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("everyPreset")
    void aCvWrittenInAnotherLanguageRendersOnEveryPreset(DocumentTemplate<CvDocument> preset) {
        String text = CvComposedText.squashedNodes(preset, foreignLanguageCv());

        assertRendered(text, "Ведущий инженер", preset, "experience");
        assertRendered(text, "Информатика", preset, "education");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("everyPreset")
    void aRoleRoutedModuleRendersWhateverItsKind(DocumentTemplate<CvDocument> preset) {
        // The slots were guarded on the section's Java type as well as its
        // heading, so a module routed correctly was dropped anyway. Kinds here
        // are deliberately the "wrong" shape for the slot each role names —
        // experience as bullets, education as an inline list — because the
        // author picks the kind and the preset does not get a veto.
        CvDocument doc = CvDocument.builder()
                .identity(identity())
                .section(ModuleSection.builder("Berufserfahrung", SectionRole.EXPERIENCE,
                                CvKind.BULLETS)
                        .item(CvItem.of("Senior Engineer").paragraphs("Acme GmbH, 2021-2025"))
                        .build())
                .section(ModuleSection.builder("Kenntnisse", SectionRole.SKILLS,
                                CvKind.INLINE_LIST)
                        .item(CvItem.of("Sprachen").paragraphs("Java 21", "Kotlin"))
                        .build())
                .build();

        String text = CvComposedText.squashedNodes(preset, doc);

        assertRendered(text, "Senior Engineer", preset, "EXPERIENCE");
        assertRendered(text, "Java 21", preset, "SKILLS");
    }

    @Test
    void theRoleWinsOverAHeadingThatMatchesADifferentSlot() {
        // A module titled "Projects" but declared EXPERIENCE belongs where its
        // author said, not where its heading reads.
        List<com.demcha.compose.document.templates.cv.data.CvSection> sections = List.of(
                ModuleSection.builder("Projects", SectionRole.EXPERIENCE, CvKind.ENTRIES_DATED)
                        .item(CvItem.of("Senior Engineer").period("2021")).build());

        assertThat(SectionRouter.find(sections, SectionRole.EXPERIENCE, List.of("experience")))
                .as("the role names the slot")
                .isNotNull();
        assertThat(SectionRouter.find(sections, SectionRole.PROJECTS, List.of("projects")))
                .as("...and the heading no longer claims a slot the role did not name")
                .isNull();
    }

    @Test
    void aSectionWithoutARoleStillRoutesByItsHeading() {
        // The four hand-written section types carry no role, and neither does a
        // module the catalogue has no name for. Keywords remain the answer for
        // them, so nothing that worked before stops working.
        List<com.demcha.compose.document.templates.cv.data.CvSection> sections = List.of(
                new com.demcha.compose.document.templates.cv.data.ParagraphSection(
                        "Professional Summary", "Backend engineer."),
                ModuleSection.builder("Awards", SectionRole.OTHER, CvKind.BULLETS)
                        .item("Employee of the year").build());

        assertThat(SectionRouter.find(sections, SectionRole.SUMMARY, List.of("summary")))
                .as("a hand-written section still matches by heading")
                .isNotNull();
        assertThat(SectionRouter.find(sections, SectionRole.OTHER, List.of("awards")))
                .as("SectionRole.OTHER claims no slot and falls through to the heading")
                .isNotNull();
    }

    /**
     * Asserts the words reached the page, ignoring how the preset set them:
     * several upper-case entry titles and several letter-space them, so
     * "Senior Engineer" arrives as "S E N I O R   E N G I N E E R". Typography
     * is the preset's to choose; the words are the author's to keep.
     */
    private static void assertRendered(String text, String words,
                                       DocumentTemplate<CvDocument> preset, String slot) {
        assertThat(text)
                .as("%s must render the %s module routed by role", preset.id(), slot)
                .contains(CvComposedText.squash(words));
    }

    // -- fixtures --------------------------------------------------------

    private static CvDocument foreignLanguageCv() {
        return CvDocument.builder()
                .identity(identity())
                .section(ModuleSection.builder("О себе", SectionRole.SUMMARY, CvKind.PARAGRAPH)
                        .item(CvItem.of("summary").paragraphs("Backend engineer."))
                        .build())
                .section(ModuleSection.builder("Опыт работы", SectionRole.EXPERIENCE,
                                CvKind.ENTRIES_DATED)
                        .item(CvItem.of("Ведущий инженер").at("Acme GmbH")
                                .period("2021 - 2025").paragraphs("Payments."))
                        .build())
                .section(ModuleSection.builder("Образование", SectionRole.EDUCATION,
                                CvKind.ENTRIES_DATED)
                        .item(CvItem.of("Информатика").at("МГУ").period("2014 - 2018"))
                        .build())
                .build();
    }

    private static CvIdentity identity() {
        return CvIdentity.builder()
                .name("Jordan", "Rivera")
                .jobTitle("Backend Engineer")
                .contact("+1 555 0100", "jordan@example.com", "Berlin, DE")
                .build();
    }

}
