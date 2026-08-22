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
 * A runtime module reaches the page on every modular template, whatever
 * language its heading is written in — because it is a shape, not a CV
 * meaning.
 *
 * <p>Slots that still ask for Experience or Skills only see the four
 * hand-written section types. A {@link ModuleSection} is not a slot claim:
 * it stays in document order (or the leftover tail) and draws through
 * {@link com.demcha.compose.document.templates.cv.api.CvConstructor}. This
 * suite holds that promise for headings no English keyword list contains.</p>
 */
class RoleRoutingTest {

    private static Stream<Named<DocumentTemplate<CvDocument>>> modularPresets() {
        return CvTemplates.modular().stream().map(t -> Named.of(t.id(), t));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("modularPresets")
    void aCvWrittenInAnotherLanguageRendersOnEveryModularPreset(
            DocumentTemplate<CvDocument> preset) {
        String text = CvComposedText.squashedNodes(preset, foreignLanguageCv());

        assertRendered(text, "Ведущий инженер", preset, "entries-dated");
        assertRendered(text, "Информатика", preset, "entries-dated");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("modularPresets")
    void aModuleRendersWhateverItsKind(DocumentTemplate<CvDocument> preset) {
        CvDocument doc = CvDocument.builder()
                .identity(identity())
                .section(ModuleSection.builder("Berufserfahrung", SectionRole.OTHER,
                                CvKind.BULLETS)
                        .item(CvItem.of("Senior Engineer").paragraphs("Acme GmbH, 2021-2025"))
                        .build())
                .section(ModuleSection.builder("Kenntnisse", SectionRole.OTHER,
                                CvKind.INLINE_LIST)
                        .item(CvItem.of("Sprachen").paragraphs("Java 21", "Kotlin"))
                        .build())
                .build();

        String text = CvComposedText.squashedNodes(preset, doc);

        assertRendered(text, "Senior Engineer", preset, "bullets");
        assertRendered(text, "Java 21", preset, "inline-list");
    }

    @Test
    void aModuleIsNotClaimedByRoleOrHeading() {
        List<com.demcha.compose.document.templates.cv.data.CvSection> sections = List.of(
                ModuleSection.builder("Projects", SectionRole.EXPERIENCE, CvKind.ENTRIES_DATED)
                        .item(CvItem.of("Senior Engineer").period("2021")).build());

        assertThat(SectionRouter.find(sections, SectionRole.EXPERIENCE, List.of("experience")))
                .as("a module is not a CV meaning")
                .isNull();
        assertThat(SectionRouter.find(sections, SectionRole.PROJECTS, List.of("projects")))
                .as("nor a heading the slot happens to recognise")
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
                .as("a runtime module is not claimed by heading either")
                .isNull();
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
                .as("%s must render the %s module", preset.id(), slot)
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
