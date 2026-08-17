package com.demcha.compose.document.templates.cv.api;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.ParagraphNode;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.cv.data.CvDocument;
import com.demcha.compose.document.templates.cv.data.CvIdentity;
import com.demcha.compose.document.templates.cv.data.CvItem;
import com.demcha.compose.document.templates.cv.data.CvKind;
import com.demcha.compose.document.templates.cv.data.ModuleSection;
import com.demcha.compose.document.templates.cv.data.SectionRole;
import com.demcha.compose.document.templates.cv.data.Slot;
import com.demcha.compose.document.templates.cv.presets.CvTemplates;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Every template that declares {@link ModularCvTemplate} renders every kind
 * of module, under whatever heading the author wrote.
 *
 * <p>The interface is a promise made to a caller who cannot check it: a CV
 * builder offers the modular templates and trusts that whatever the user
 * assembled comes out the other side. A template that quietly dropped a
 * section would produce a CV that still looks finished — the failure has no
 * symptom at the point it happens, only a missing job three weeks later.
 * This is where the promise is checked, so wearing the interface costs
 * something.</p>
 *
 * <p>It enumerates {@link CvTemplates#modular()} rather than a list of its
 * own, and every {@link CvKind} rather than the kinds in use, so a template
 * or a kind added later is covered the day it lands — the coverage cannot
 * be forgotten, only made to pass.</p>
 *
 * <p>Text is read from the composed layout, not the PDF text layer: the CV
 * themes draw with the standard-14 Helvetica, whose encoding has no
 * Cyrillic, and the non-Latin heading below is the case that matters most.
 * What the model owes is that the section is placed carrying its own words;
 * which glyphs a font can draw is the caller's font choice.</p>
 */
class ModularCvTemplateFidelityTest {

    private static Stream<Named<ModularCvTemplate>> modularTemplates() {
        return CvTemplates.modular().stream()
                .map(template -> Named.of(template.id(), template));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("modularTemplates")
    void everyModularTemplateRendersEveryKind(ModularCvTemplate template) {
        String text = composedText(template, everyKindDocument());

        for (CvKind kind : CvKind.values()) {
            // Case-insensitively: a preset that upper-cases its entry titles is
            // styling them, not losing them.
            assertThat(text)
                    .as("%s must render the %s module's item", template.id(), kind)
                    .containsIgnoringCase(itemTitle(kind));
            assertThat(text)
                    .as("%s must render the %s module's description", template.id(), kind)
                    .contains(bodyLine(kind));
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("modularTemplates")
    void everyModularTemplateRendersAnAdHocSection(ModularCvTemplate template) {
        // No keyword list contains "Volunteering", and no preset was written
        // with it in mind. A section the author invented is the whole point of
        // assembling a CV at runtime, so it is the minimum this promise means.
        CvDocument doc = document(ModuleSection.builder("Volunteering", SectionRole.OTHER,
                        CvKind.ENTRIES_DATED)
                .item(CvItem.of("Mentor, Rails Girls").at("Rails Girls Berlin")
                        .period("2019 - 2021").bullets("Ran three weekend workshops"))
                .build());

        String text = composedText(template, doc);

        assertThatHeading(text, "Volunteering", template);
        assertThat(text)
                .as("%s must render the invented section's entry", template.id())
                .containsIgnoringCase("Mentor, Rails Girls");
        assertThat(text)
                .as("%s must render the invented section's description", template.id())
                .contains("Ran three weekend workshops");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("modularTemplates")
    void everyModularTemplateRendersANonLatinHeading(ModularCvTemplate template) {
        CvDocument doc = document(ModuleSection.builder("Навыки", SectionRole.SKILLS,
                        CvKind.INLINE_LIST)
                .item(CvItem.of("Языки").paragraphs("Java 21", "Kotlin"))
                .build());

        String text = composedText(template, doc);

        assertThatHeading(text, "Навыки", template);
        assertThat(text)
                .as("%s must render the section's items", template.id())
                .contains("Языки", "Java 21, Kotlin");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("modularTemplates")
    void aHeadingThePresetHasAWordForIsStillTheAuthorsHeading(ModularCvTemplate template) {
        // The ad-hoc cases above use headings no keyword list contains, which is
        // the easy half. A preset with an editorial vocabulary of its own is the
        // one that rewrites: one of these renamed any heading matching
        // "certification" to EDUCATION, so "Certifications & Awards" reached the
        // page as a word the author never wrote and the awards lost their title.
        CvDocument doc = document(ModuleSection.builder("Certifications & Awards",
                        SectionRole.OTHER, CvKind.BULLETS)
                .item(CvItem.of("AWS Solutions Architect").paragraphs("2024"))
                .build());

        String text = composedText(template, doc);

        assertThatHeading(text, "Certifications & Awards", template);
        assertThat(text)
                .as("%s must render the section's item", template.id())
                .containsIgnoringCase("AWS Solutions Architect");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("modularTemplates")
    void aSidebarSectionIsNotRenderedAndTheContractSaysSo(ModularCvTemplate template) {
        // Pinning a limitation, not a feature. Every shipped preset composes one
        // main column and reads Slot.MAIN, so a section placed in the sidebar is
        // dropped — which ModularCvTemplate's contract states rather than leaving
        // "renders whatever it is handed" to be read generously. When slots go
        // live this test goes red, which is the point: the promise and the code
        // change together.
        CvDocument doc = CvDocument.builder()
                .identity(identity())
                .section(Slot.SIDEBAR, ModuleSection.builder("Languages",
                                SectionRole.LANGUAGES, CvKind.INLINE_LIST)
                        .item(CvItem.of("Spoken").paragraphs("English", "German"))
                        .build())
                .build();

        assertThat(composedText(template, doc))
                .as("%s reads Slot.MAIN, as its contract says", template.id())
                .doesNotContain("Spoken");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("modularTemplates")
    void everyModularTemplateDeclaresAKit(ModularCvTemplate template) {
        assertThat(template.kit())
                .as("%s must hand back a kit — the drawing half of the promise", template.id())
                .isNotNull();
    }

    @Test
    void theModularListIsASubsetOfTheCatalogueAndNotEmpty() {
        List<String> modularIds = CvTemplates.modular().stream()
                .map(DocumentTemplate::id).toList();

        assertThat(modularIds)
                .as("a promise nobody makes is a promise nobody keeps")
                .isNotEmpty();
        assertThat(CvTemplates.ids()).containsAll(modularIds);
        assertThat(modularIds).doesNotHaveDuplicates();
    }

    /**
     * Asserts the heading reached the page, ignoring how the preset styled
     * it: several letter-space and upper-case their headings, so
     * "Volunteering" arrives as "V O L U N T E E R I N G". The words are the
     * template's to keep; the typography is the template's to choose.
     */
    private static void assertThatHeading(String text, String heading,
                                          ModularCvTemplate template) {
        assertThat(text.replace(" ", ""))
                .as("%s must render the section under its own heading (%s)",
                        template.id(), heading)
                .containsIgnoringCase(heading.replace(" ", ""));
    }

    // -- fixtures --------------------------------------------------------

    /**
     * One module per kind, each carrying an item whose title and description
     * name the kind — so a failure says which kind was dropped rather than
     * that something was missing.
     */
    private static CvDocument everyKindDocument() {
        CvDocument.Builder builder = CvDocument.builder().identity(identity());
        for (CvKind kind : CvKind.values()) {
            builder.section(ModuleSection.builder(sectionTitle(kind), SectionRole.OTHER, kind)
                    .item(CvItem.of(itemTitle(kind))
                            .at("Acme GmbH").in("Berlin").period("2021 - Present")
                            .paragraphs(bodyLine(kind)))
                    .build());
        }
        return builder.build();
    }

    private static String sectionTitle(CvKind kind) {
        return "Section " + marker(kind);
    }

    /**
     * The kind's name with its underscore removed. An underscore is markdown
     * for italic, and a fixture carrying two of them would be reporting the
     * markdown parser rather than the template.
     */
    private static String marker(CvKind kind) {
        return kind.name().replace("_", " ");
    }

    private static String itemTitle(CvKind kind) {
        // PARAGRAPH reads the body alone, so its item title never reaches the
        // page; the body carries the marker for that kind instead.
        return kind == CvKind.PARAGRAPH ? bodyLine(kind) : "Item " + marker(kind);
    }

    private static String bodyLine(CvKind kind) {
        return "Body of " + marker(kind);
    }

    private static CvDocument document(ModuleSection module) {
        return CvDocument.builder().identity(identity()).section(module).build();
    }

    private static CvIdentity identity() {
        return CvIdentity.builder()
                .name("Jordan", "Rivera")
                .jobTitle("Backend Engineer")
                .contact("+1 555 0100", "jordan@example.com", "Berlin, DE")
                .build();
    }

    /** Every string the composed layout carries, joined. */
    private static String composedText(DocumentTemplate<CvDocument> template, CvDocument doc) {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(DocumentPageSize.A4)
                .margin(24, 24, 24, 24)
                .create()) {
            template.compose(session, doc);
            StringBuilder text = new StringBuilder();
            collectText(session.roots(), text);
            return text.toString();
        }
    }

    private static void collectText(List<DocumentNode> nodes, StringBuilder out) {
        for (DocumentNode node : nodes) {
            if (node instanceof ParagraphNode paragraph) {
                out.append(paragraph.text()).append(' ');
            }
            collectText(node.children(), out);
        }
    }
}
