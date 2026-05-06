package com.demcha.compose.document.templates.coverletter.presets;

import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.templates.TemplateTestSupport;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.coverletter.spec.CoverLetterHeader;
import com.demcha.compose.document.templates.coverletter.spec.CoverLetterSpec;
import com.demcha.compose.document.theme.BusinessTheme;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Layout snapshot regression test for every Templates v2 cover-letter
 * preset. Mirrors {@code cv.presets.PresetLayoutSnapshotTest}.
 *
 * <p>A snapshot mismatch means the preset's rendered tree drifted —
 * either intentionally (re-run with
 * {@code -Dgraphcompose.updateSnapshots=true}) or as a regression.</p>
 */
class PresetLayoutSnapshotTest {

    private static final BusinessTheme THEME = BusinessTheme.modern();

    private static CoverLetterSpec canonicalLetterSpec() {
        return CoverLetterSpec.builder()
                .header(CoverLetterHeader.builder()
                        .name("Artem Demchyshyn")
                        .address("London, UK")
                        .phone("+44 20 5555 1000")
                        .email("artem@demo.dev")
                        .link("LinkedIn", "https://linkedin.com/in/graphcompose")
                        .link("GitHub", "https://github.com/DemchaAV")
                        .build())
                .greeting("Dear Hiring Team at **Northwind Systems**,")
                .paragraph("I am excited to share my interest in the Senior "
                        + "Platform Engineer role. My recent work has focused "
                        + "on building **reusable document-generation systems**.")
                .paragraph("I enjoy translating fuzzy workflow requirements into "
                        + "clear template abstractions and reliable test coverage.")
                .paragraph("I would welcome the opportunity to bring that mix "
                        + "of engineering rigor and product thinking to your team.")
                .closing("Sincerely, *Artem Demchyshyn*")
                .build();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("presets")
    void shouldMatchLayoutSnapshot(String slug,
                                   Function<BusinessTheme, DocumentTemplate<CoverLetterSpec>> factory) throws Exception {
        DocumentTemplate<CoverLetterSpec> template = factory.apply(THEME);
        try (DocumentSession document = TemplateTestSupport.openInMemoryDocument(
                PDRectangle.A4, 22, 18, 22, 22)) {
            template.compose(document, canonicalLetterSpec());
            TemplateTestSupport.assertCanonicalSnapshot(document, slug, "coverletter-v2");
        }
    }

    private static Stream<Arguments> presets() {
        return Stream.of(
                Arguments.of("template_v2_letter_modern_professional",
                        (Function<BusinessTheme, DocumentTemplate<CoverLetterSpec>>) ModernProfessionalLetter::create),
                Arguments.of("template_v2_letter_nordic_clean",
                        (Function<BusinessTheme, DocumentTemplate<CoverLetterSpec>>) NordicCleanLetter::create),
                Arguments.of("template_v2_letter_classic_serif",
                        (Function<BusinessTheme, DocumentTemplate<CoverLetterSpec>>) ClassicSerifLetter::create),
                Arguments.of("template_v2_letter_compact_mono",
                        (Function<BusinessTheme, DocumentTemplate<CoverLetterSpec>>) CompactMonoLetter::create),
                Arguments.of("template_v2_letter_executive",
                        (Function<BusinessTheme, DocumentTemplate<CoverLetterSpec>>) ExecutiveLetter::create),
                Arguments.of("template_v2_letter_engineering_resume",
                        (Function<BusinessTheme, DocumentTemplate<CoverLetterSpec>>) EngineeringResumeLetter::create),
                Arguments.of("template_v2_letter_timeline_minimal",
                        (Function<BusinessTheme, DocumentTemplate<CoverLetterSpec>>) TimelineMinimalLetter::create),
                Arguments.of("template_v2_letter_boxed_sections",
                        (Function<BusinessTheme, DocumentTemplate<CoverLetterSpec>>) BoxedSectionsLetter::create),
                Arguments.of("template_v2_letter_centered_headline",
                        (Function<BusinessTheme, DocumentTemplate<CoverLetterSpec>>) CenteredHeadlineLetter::create),
                Arguments.of("template_v2_letter_blue_banner",
                        (Function<BusinessTheme, DocumentTemplate<CoverLetterSpec>>) BlueBannerLetter::create),
                Arguments.of("template_v2_letter_editorial_blue",
                        (Function<BusinessTheme, DocumentTemplate<CoverLetterSpec>>) EditorialBlueLetter::create),
                Arguments.of("template_v2_letter_panel",
                        (Function<BusinessTheme, DocumentTemplate<CoverLetterSpec>>) PanelLetter::create),
                Arguments.of("template_v2_letter_sidebar_portrait",
                        (Function<BusinessTheme, DocumentTemplate<CoverLetterSpec>>) SidebarPortraitLetter::create),
                Arguments.of("template_v2_letter_monogram_sidebar",
                        (Function<BusinessTheme, DocumentTemplate<CoverLetterSpec>>) MonogramSidebarLetter::create));
    }
}
