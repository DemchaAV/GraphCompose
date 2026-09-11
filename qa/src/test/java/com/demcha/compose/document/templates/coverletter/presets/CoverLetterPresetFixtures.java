package com.demcha.compose.document.templates.coverletter.presets;

import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.coverletter.data.CoverLetterDocument;
import com.demcha.compose.document.templates.cv.data.CvIdentity;
import org.junit.jupiter.params.provider.Arguments;

import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Shared roster and canonical sample letter for the layered
 * cover-letter presets — the letter sibling of
 * {@code CvPresetFixtures}.
 *
 * <p>Both preset gates read from here so they always describe the same
 * render: {@code CoverLetterV2VisualParityTest} compares the rasterised
 * pages per-pixel, {@code CoverLetterPresetLayoutSnapshotTest} compares
 * the post-layout node tree. Adding a preset to {@link #presets()}
 * enrols it in both.</p>
 */
final class CoverLetterPresetFixtures {

    private CoverLetterPresetFixtures() {
    }

    /**
     * Every layered cover-letter preset, as {@code (slug,
     * recommendedMargin, factory)} triples.
     */
    static Stream<Arguments> presets() {
        return Stream.of(
                Arguments.of("executive",
                        ExecutiveLetter.RECOMMENDED_MARGIN,
                        (Supplier<DocumentTemplate<CoverLetterDocument>>) ExecutiveLetter::create),
                Arguments.of("modern_professional",
                        ModernProfessionalLetter.RECOMMENDED_MARGIN,
                        (Supplier<DocumentTemplate<CoverLetterDocument>>) ModernProfessionalLetter::create),
                Arguments.of("boxed_sections",
                        BoxedSectionsLetter.RECOMMENDED_MARGIN,
                        (Supplier<DocumentTemplate<CoverLetterDocument>>) BoxedSectionsLetter::create),
                Arguments.of("classic_serif",
                        ClassicSerifLetter.RECOMMENDED_MARGIN,
                        (Supplier<DocumentTemplate<CoverLetterDocument>>) ClassicSerifLetter::create),
                Arguments.of("editorial_blue",
                        EditorialBlueLetter.RECOMMENDED_MARGIN,
                        (Supplier<DocumentTemplate<CoverLetterDocument>>) EditorialBlueLetter::create),
                Arguments.of("centered_headline",
                        CenteredHeadlineLetter.RECOMMENDED_MARGIN,
                        (Supplier<DocumentTemplate<CoverLetterDocument>>) CenteredHeadlineLetter::create),
                Arguments.of("blue_banner",
                        BlueBannerLetter.RECOMMENDED_MARGIN,
                        (Supplier<DocumentTemplate<CoverLetterDocument>>) BlueBannerLetter::create),
                Arguments.of("engineering_resume",
                        EngineeringResumeLetter.RECOMMENDED_MARGIN,
                        (Supplier<DocumentTemplate<CoverLetterDocument>>) EngineeringResumeLetter::create),
                Arguments.of("panel",
                        PanelLetter.RECOMMENDED_MARGIN,
                        (Supplier<DocumentTemplate<CoverLetterDocument>>) PanelLetter::create),
                Arguments.of("compact_mono",
                        CompactMonoLetter.RECOMMENDED_MARGIN,
                        (Supplier<DocumentTemplate<CoverLetterDocument>>) CompactMonoLetter::create),
                Arguments.of("nordic_clean",
                        NordicCleanLetter.RECOMMENDED_MARGIN,
                        (Supplier<DocumentTemplate<CoverLetterDocument>>) NordicCleanLetter::create),
                Arguments.of("sidebar_portrait",
                        SidebarPortraitLetter.RECOMMENDED_MARGIN,
                        (Supplier<DocumentTemplate<CoverLetterDocument>>) SidebarPortraitLetter::create),
                Arguments.of("monogram_sidebar",
                        MonogramSidebarLetter.RECOMMENDED_MARGIN,
                        (Supplier<DocumentTemplate<CoverLetterDocument>>) MonogramSidebarLetter::create),
                Arguments.of("timeline_minimal",
                        TimelineMinimalLetter.RECOMMENDED_MARGIN,
                        (Supplier<DocumentTemplate<CoverLetterDocument>>) TimelineMinimalLetter::create),
                // Slug kept as-is: it names the committed pixel baseline
                // PNG directory, which renaming would orphan.
                Arguments.of("mint-editorial-letter",
                        MintEditorialLetter.RECOMMENDED_MARGIN,
                        (Supplier<DocumentTemplate<CoverLetterDocument>>) MintEditorialLetter::create));
    }

    /**
     * Canonical sample letter — the same Jordan Rivera identity as
     * {@code CvPresetFixtures} so the letter masthead is verified
     * against the same content the CV gates use, plus a greeting, three
     * body paragraphs with inline markdown, and a closing.
     *
     * <p>Kept inline (not pulled from the examples module) so the tests
     * depend only on main + main-test code.</p>
     */
    static CoverLetterDocument canonicalLetter() {
        return CoverLetterDocument.builder()
                .identity(CvIdentity.builder()
                        .name("Jordan", "Rivera")
                        .jobTitle("Platform Engineer")
                        .contact("+44 20 5555 1000",
                                "jordan.rivera@example.com",
                                "London, UK")
                        .link("LinkedIn", "https://linkedin.com/in/jordan-rivera-demo")
                        .link("GitHub", "https://github.com/jrivera-demo")
                        .build())
                .greeting("Dear Hiring Team at **Northwind Systems**,")
                .paragraph("I am excited to share my interest in the Senior "
                        + "Platform Engineer role. My recent work has focused "
                        + "on building **reusable document-generation systems** "
                        + "that balance public API design, render quality, and "
                        + "maintainability.")
                .paragraph("I enjoy translating fuzzy workflow requirements into "
                        + "clear template abstractions, reliable test coverage, "
                        + "and examples that make adoption easier for the rest "
                        + "of the team.")
                .paragraph("I would welcome the opportunity to bring that same "
                        + "mix of engineering rigor and product thinking to your "
                        + "platform group.")
                .closing("Sincerely,")
                .build();
    }
}
