package com.demcha.compose.document.templates.coverletter.v2.presets;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.coverletter.v2.data.CoverLetterDocument;
import com.demcha.compose.document.templates.cv.v2.data.CvIdentity;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke test for every v2 cover-letter preset — one parametrized check
 * instead of a near-identical file per preset, because all letters
 * share the same {@code LetterBody} shape and differ only in their
 * (already pixel-gated) masthead.
 *
 * <p>Asserts each preset's stable {@code id()} / {@code displayName()}
 * and that it composes a non-empty document. Pixel fidelity of each
 * masthead is covered separately by {@link CoverLetterV2VisualParityTest}.</p>
 */
class CoverLetterV2SmokeTest {

    @ParameterizedTest(name = "{1}")
    @MethodSource("presets")
    void exposes_stable_identity_and_renders(
            Supplier<DocumentTemplate<CoverLetterDocument>> factory,
            String id,
            String displayName) throws Exception {
        DocumentTemplate<CoverLetterDocument> template = factory.get();
        assertThat(template.id()).isEqualTo(id);
        assertThat(template.displayName()).isEqualTo(displayName);

        try (DocumentSession session = GraphCompose.document()
                .pageSize(DocumentPageSize.A4)
                .margin(48, 48, 48, 48)
                .create()) {
            template.compose(session, sampleDocument());
            assertThat(session.roots()).isNotEmpty();
        }
    }

    private static Stream<Arguments> presets() {
        return Stream.of(
                Arguments.of((Supplier<DocumentTemplate<CoverLetterDocument>>) ExecutiveLetter::create,
                        "executive-letter", "Executive Letter"),
                Arguments.of((Supplier<DocumentTemplate<CoverLetterDocument>>) ModernProfessionalLetter::create,
                        "modern-professional-letter", "Modern Professional Letter"),
                Arguments.of((Supplier<DocumentTemplate<CoverLetterDocument>>) BoxedSectionsLetter::create,
                        "boxed-sections-letter", "Boxed Sections Letter"),
                Arguments.of((Supplier<DocumentTemplate<CoverLetterDocument>>) ClassicSerifLetter::create,
                        "classic-serif-letter", "Classic Serif Letter"),
                Arguments.of((Supplier<DocumentTemplate<CoverLetterDocument>>) EditorialBlueLetter::create,
                        "editorial-blue-letter", "Editorial Blue Letter"),
                Arguments.of((Supplier<DocumentTemplate<CoverLetterDocument>>) CenteredHeadlineLetter::create,
                        "centered-headline-letter", "Centered Headline Letter"),
                Arguments.of((Supplier<DocumentTemplate<CoverLetterDocument>>) BlueBannerLetter::create,
                        "blue-banner-letter", "Blue Banner Letter"),
                Arguments.of((Supplier<DocumentTemplate<CoverLetterDocument>>) EngineeringResumeLetter::create,
                        "engineering-resume-letter", "Engineering Resume Letter"),
                Arguments.of((Supplier<DocumentTemplate<CoverLetterDocument>>) PanelLetter::create,
                        "panel-letter", "Panel Letter"),
                Arguments.of((Supplier<DocumentTemplate<CoverLetterDocument>>) CompactMonoLetter::create,
                        "compact-mono-letter", "Compact Mono Letter"),
                Arguments.of((Supplier<DocumentTemplate<CoverLetterDocument>>) NordicCleanLetter::create,
                        "nordic-clean-letter", "Nordic Clean Letter"),
                Arguments.of((Supplier<DocumentTemplate<CoverLetterDocument>>) SidebarPortraitLetter::create,
                        "sidebar-portrait-letter", "Sidebar Portrait Letter"),
                Arguments.of((Supplier<DocumentTemplate<CoverLetterDocument>>) MonogramSidebarLetter::create,
                        "monogram-sidebar-letter", "Monogram Sidebar Letter"),
                Arguments.of((Supplier<DocumentTemplate<CoverLetterDocument>>) TimelineMinimalLetter::create,
                        "timeline-minimal-letter", "Timeline Minimal Letter"));
    }

    private static CoverLetterDocument sampleDocument() {
        return CoverLetterDocument.builder()
                .identity(CvIdentity.builder()
                        .name("Jordan", "Rivera")
                        .jobTitle("Platform Engineer")
                        .contact("+44 20 5555 1000", "jordan.rivera@example.com",
                                "London, UK")
                        .link("LinkedIn", "https://linkedin.com/in/jordan-rivera-demo")
                        .link("GitHub", "https://github.com/jrivera-demo")
                        .build())
                .greeting("Dear Hiring Team at **Northwind Systems**,")
                .paragraph("I am excited to share my interest in the Senior "
                        + "Platform Engineer role, building **reusable "
                        + "document-generation systems**.")
                .paragraph("I enjoy turning fuzzy requirements into clear template "
                        + "abstractions and reliable test coverage.")
                .closing("Sincerely,")
                .build();
    }
}
