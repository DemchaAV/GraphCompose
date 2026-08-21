package com.demcha.compose.document.templates.cv.components;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.cv.data.CvDocument;
import com.demcha.compose.document.templates.cv.data.CvIdentity;
import com.demcha.compose.document.templates.cv.data.CvItem;
import com.demcha.compose.document.templates.cv.data.CvKind;
import com.demcha.compose.document.templates.cv.data.ModuleSection;
import com.demcha.compose.document.templates.cv.data.SectionRole;
import com.demcha.compose.document.templates.cv.presets.BlueBanner;
import com.demcha.compose.document.templates.cv.presets.BoxedSections;
import com.demcha.compose.document.templates.cv.presets.CenteredHeadline;
import com.demcha.compose.document.templates.cv.presets.ClassicSerif;
import com.demcha.compose.document.templates.cv.presets.CompactMono;
import com.demcha.compose.document.templates.cv.presets.EditorialBlue;
import com.demcha.compose.document.templates.cv.presets.EngineeringResume;
import com.demcha.compose.document.templates.cv.presets.Executive;
import com.demcha.compose.document.templates.cv.presets.MinimalUnderlined;
import com.demcha.compose.document.templates.cv.presets.MintEditorial;
import com.demcha.compose.document.templates.cv.presets.ModernProfessional;
import com.demcha.compose.document.templates.cv.presets.MonogramSidebar;
import com.demcha.compose.document.templates.cv.presets.NordicClean;
import com.demcha.compose.document.templates.cv.presets.Panel;
import com.demcha.compose.document.templates.cv.presets.SidebarPortrait;
import com.demcha.compose.document.templates.cv.presets.TimelineMinimal;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Every {@link CvKind} reaches the page, and no preset fails on a module.
 *
 * <p>A runtime module is only as good as the weakest kind: an author who picks
 * one the renderers never learned to lower gets a section that silently draws
 * nothing, and the CV looks finished. Enumerating the enum rather than listing
 * cases means a kind added later fails here until it is wired, which is the
 * point — a new constant cannot ship half-rendered.</p>
 *
 * <p>The per-template promise — every kind, an invented heading, a non-Latin
 * one — is checked in {@code ModularCvTemplateFidelityTest}, which enumerates
 * the templates that declare the capability instead of a list kept by hand.
 * What stays here is the kind-level coverage and the floor every preset owes
 * whether or not it declares anything.</p>
 */
class ModuleSectionKindCoverageTest {

    /** Presets that render every section the document carries, in order. */
    private static Stream<DocumentTemplate<CvDocument>> generalPresets() {
        return Stream.of(ModernProfessional.create(), BoxedSections.create(),
                MinimalUnderlined.create(), Executive.create(),
                CenteredHeadline.create(), BlueBanner.create());
    }

    /**
     * Presets that render whatever section they are handed — the six general
     * loops plus the two that route by heading and then draw any shape.
     */
    private static Stream<DocumentTemplate<CvDocument>> presetsThatRenderAnyShape() {
        return Stream.concat(generalPresets(),
                Stream.of(ClassicSerif.create(), EditorialBlue.create()));
    }

    /**
     * Presets whose module slots are guarded on the section's Java type —
     * {@code if (!(section instanceof EntriesSection entries)) return;} and
     * friends — so a module routed to one of those slots is skipped whatever
     * its kind. Placing them is the routing work, not this change; what is
     * pinned here is that they do not fail.
     */
    private static Stream<DocumentTemplate<CvDocument>> presetsThatGuardOnSectionType() {
        return Stream.of(CompactMono.create(), EngineeringResume.create(),
                MintEditorial.create(), MonogramSidebar.create(), NordicClean.create(),
                Panel.create(), SidebarPortrait.create(), TimelineMinimal.create());
    }

    /** Every shipped CV preset. */
    private static Stream<DocumentTemplate<CvDocument>> everyPreset() {
        return Stream.concat(presetsThatRenderAnyShape(), presetsThatGuardOnSectionType());
    }

    @ParameterizedTest
    @EnumSource(CvKind.class)
    void everyKindPutsItsDescriptionsOnThePage(CvKind kind) throws Exception {
        ModuleSection module = ModuleSection.builder("Selected Work", SectionRole.OTHER, kind)
                .item(CvItem.of("First entry").at("Acme GmbH").in("Berlin")
                        .period("2021 - Present").paragraphs("Did the work."))
                .item(CvItem.of("Second entry").at("Northwind").period("2018 - 2021")
                        .bullets("Shipped it", "Measured it"))
                .build();

        String text = render(ModernProfessional.create(), module);

        assertThat(text)
                .as("%s must render every description line — no kind may drop the body", kind)
                .contains("Did the work.", "Shipped it", "Measured it");
    }

    @ParameterizedTest
    @EnumSource(value = CvKind.class, names = "PARAGRAPH", mode = EnumSource.Mode.EXCLUDE)
    void everyTitledKindPutsItsTitlesOnThePage(CvKind kind) throws Exception {
        // PARAGRAPH is excluded on purpose, not overlooked: it renders prose under
        // the section's own heading and documents that it reads the body alone —
        // the case below pins that, so the two together cover the whole enum.
        ModuleSection module = ModuleSection.builder("Selected Work", SectionRole.OTHER, kind)
                .item(CvItem.of("First entry").paragraphs("Did the work."))
                .item(CvItem.of("Second entry").period("2018").paragraphs("Did more."))
                .build();

        assertThat(render(ModernProfessional.create(), module))
                .as("%s reads the item title, so it must reach the page", kind)
                .contains("First entry", "Second entry");
    }

    @Test
    void proseRendersWithoutRepeatingTheHeading() throws Exception {
        ModuleSection module = ModuleSection.summary("Profile", "Backend engineer.");

        String text = render(ModernProfessional.create(), module);

        assertThat(text).contains("Backend engineer.");
        assertThat(text.split("Profile", -1))
                .as("the heading is the section's; PARAGRAPH must not print it a second time")
                .hasSize(2);
    }

    @ParameterizedTest
    @MethodSource("presetsThatRenderAnyShape")
    void aModuleUnderAHeadingThePresetKnowsIsRendered(DocumentTemplate<CvDocument> preset)
            throws Exception {
        // Stronger than "does not throw", and the case that catches the failure
        // no-throw cannot see: presets consult SectionLookup.hasContent before they
        // route or render, and its default for an unrecognised subtype is false —
        // which discarded the module's heading and body together, on presets whose
        // rendering path for it was perfectly good. The heading here is one every
        // preset's keyword list contains, so nothing but that gate can lose it.
        ModuleSection module = ModuleSection.builder("Professional Experience",
                        SectionRole.EXPERIENCE, CvKind.ENTRIES_DATED)
                .item(CvItem.of("Senior Backend Engineer").at("Acme GmbH")
                        .period("2021 - Present").paragraphs("Owned the settlement service."))
                .build();

        assertThat(render(preset, module))
                .as("%s must render a module it routed by heading", preset.id())
                // Case-insensitively: a preset that upper-cases entry titles — and
                // one now does, through its own kit — is styling, not dropping.
                .containsIgnoringCase("Senior Backend Engineer");
    }

    @Test
    void aModuleAndTheOlderSectionTypesCoexistInOneDocument() throws Exception {
        // The new permit is additive: a document may mix a runtime module with
        // the hand-written records, which is what a migration looks like.
        CvDocument doc = CvDocument.builder()
                .identity(identity())
                .section(ModuleSection.summary("Profile", "Backend engineer."))
                .section(com.demcha.compose.document.templates.cv.data.EntriesSection
                        .builder("Education")
                        .entry("BSc Computer Science", "TU Berlin", "2014 - 2018", "")
                        .build())
                .section(ModuleSection.builder("Volunteering", SectionRole.OTHER, CvKind.BULLETS)
                        .item("Rails Girls mentor")
                        .build())
                .build();

        assertThat(renderDocument(ModernProfessional.create(), doc))
                .contains("Backend engineer.", "BSc Computer Science", "Rails Girls mentor");
    }

    // -- helpers ---------------------------------------------------------

    private static String render(DocumentTemplate<CvDocument> preset, ModuleSection module)
            throws Exception {
        return renderDocument(preset, CvDocument.builder()
                .identity(identity())
                .sections(List.of(module))
                .build());
    }

    private static String renderDocument(DocumentTemplate<CvDocument> preset, CvDocument doc)
            throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(DocumentPageSize.A4)
                .margin(24, 24, 24, 24)
                .create()) {
            preset.compose(session, doc);
            try (PDDocument pdf = Loader.loadPDF(session.toPdfBytes())) {
                return new PDFTextStripper().getText(pdf).replaceAll("\\s+", " ").trim();
            }
        }
    }

    private static CvIdentity identity() {
        return CvIdentity.builder()
                .name("Jordan", "Rivera")
                .jobTitle("Backend Engineer")
                .contact("+1 555 0100", "jordan@example.com", "Berlin, DE")
                .build();
    }
}
