package com.demcha.compose.document.templates.cv.presets;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.cv.data.CvDocument;
import com.demcha.compose.document.templates.cv.data.CvIdentity;
import com.demcha.compose.document.templates.cv.data.EntriesSection;
import com.demcha.compose.document.templates.cv.data.ParagraphSection;
import com.demcha.compose.document.templates.cv.data.SkillsSection;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A CV is read twice: once by a person looking at the page, and once by software reading
 * the text layer underneath it. This holds every preset to the second reading.
 *
 * <p>The two came apart silently. PDFBox draws a font's ligatures for the engine, so in
 * most of the bundled families {@code ti}, {@code tf} and {@code ft} each became one
 * glyph that the font's glyph-to-character map had no entry for — and the letters were
 * simply gone from the text layer. {@code Platform} extracted as {@code Pla orm} while the
 * page looked perfect. For a CV that is not a cosmetic problem: an applicant tracking
 * system parses the text layer, so the keyword an applicant was matched on was not in the
 * file, and neither the author nor the renderer had any way to see it.</p>
 *
 * <p>Every preset gets the same document, and the probe words are ordinary English words
 * carrying the pairs that broke. Asking for them back is the only check that sees this —
 * a rendered page cannot show it, and a layout snapshot holds what the engine meant to
 * draw rather than what the file says it drew.</p>
 */
class CvPresetTextLayerTest {

    /**
     * Words carrying the letter pairs the bundled families ligate, planted in the profile
     * paragraph — the one block every preset renders as ordinary prose, rather than
     * upper-casing or letter-spacing it into something no extractor would return whole.
     */
    private static final List<String> PROBES =
            List.of("Platform", "certification", "retired", "drafts", "fifteen");

    @ParameterizedTest(name = "{0}")
    @MethodSource("presets")
    void theProfileTextIsInTheFileAsItWasWritten(
            String slug, double margin, Supplier<DocumentTemplate<CvDocument>> factory)
            throws Exception {

        String extracted = renderText(factory.get(), margin);

        assertThat(PROBES)
                .allSatisfy(probe -> assertThat(extracted)
                        .describedAs("\"%s\" was drawn on the page but is not in the "
                                + "text layer, so a search, a copy-and-paste and an "
                                + "applicant tracking system all miss it", probe)
                        .contains(probe));
    }

    private static String renderText(DocumentTemplate<CvDocument> template, double margin)
            throws Exception {
        byte[] pdf;
        float m = (float) margin;
        try (DocumentSession session = GraphCompose.document()
                .pageSize(DocumentPageSize.A4)
                .margin(m, m, m, m)
                .create()) {
            template.compose(session, probeDocument());
            pdf = session.toPdfBytes();
        }
        try (PDDocument document = Loader.loadPDF(pdf)) {
            // Collapse the layout's own line breaks: a word split across two lines is a
            // wrapping decision, not a text-layer defect.
            return new PDFTextStripper().getText(document).replaceAll("\\s+", " ");
        }
    }

    /** Short enough that no preset's profile block wraps it off the page. */
    private static CvDocument probeDocument() {
        return CvDocument.builder()
                .identity(CvIdentity.builder()
                        .name("Jane", "Doe")
                        .jobTitle("Backend Engineer")
                        .contact("+44 0", "j@d.com", "London")
                        .build())
                .sections(
                        new ParagraphSection("Professional Summary",
                                "Platform work, one certification, retired drafts, "
                                + "fifteen years."),
                        SkillsSection.builder("Technical Skills")
                                .group("Languages", "Java", "Kotlin")
                                .build(),
                        EntriesSection.builder("Professional Experience")
                                .entry("Senior Engineer", "Acme Rendering",
                                        "2021-2024", "Built rendering services.")
                                .build())
                .build();
    }

    private static Stream<Arguments> presets() {
        return Stream.of(
                preset("boxed_sections", BoxedSections.RECOMMENDED_MARGIN, BoxedSections::create),
                preset("minimal_underlined", MinimalUnderlined.RECOMMENDED_MARGIN, MinimalUnderlined::create),
                preset("modern_professional", ModernProfessional.RECOMMENDED_MARGIN, ModernProfessional::create),
                preset("nordic_clean", NordicClean.RECOMMENDED_MARGIN, NordicClean::create),
                preset("centered_headline", CenteredHeadline.RECOMMENDED_MARGIN, CenteredHeadline::create),
                preset("blue_banner", BlueBanner.RECOMMENDED_MARGIN, BlueBanner::create),
                preset("editorial_blue", EditorialBlue.RECOMMENDED_MARGIN, EditorialBlue::create),
                preset("classic_serif", ClassicSerif.RECOMMENDED_MARGIN, ClassicSerif::create),
                preset("compact_mono", CompactMono.RECOMMENDED_MARGIN, CompactMono::create),
                preset("executive", Executive.RECOMMENDED_MARGIN, Executive::create),
                preset("panel", Panel.RECOMMENDED_MARGIN, Panel::create),
                preset("timeline_minimal", TimelineMinimal.RECOMMENDED_MARGIN, TimelineMinimal::create),
                preset("engineering_resume", EngineeringResume.RECOMMENDED_MARGIN, EngineeringResume::create),
                preset("monogram_sidebar", MonogramSidebar.RECOMMENDED_MARGIN, MonogramSidebar::create),
                preset("sidebar_portrait", SidebarPortrait.RECOMMENDED_MARGIN, SidebarPortrait::create),
                preset("mint_editorial", MintEditorial.RECOMMENDED_MARGIN, MintEditorial::create));
    }

    private static Arguments preset(String slug, double margin,
                                    Supplier<DocumentTemplate<CvDocument>> factory) {
        return Arguments.of(slug, margin, factory);
    }
}
