package com.demcha.compose.document.templates.cv.presets;

import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.templates.TemplateTestSupport;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.blocks.EducationBlock;
import com.demcha.compose.document.templates.blocks.KeyValueBlock;
import com.demcha.compose.document.templates.blocks.MultiParagraphBlock;
import com.demcha.compose.document.templates.blocks.WorkHistoryBlock;
import com.demcha.compose.document.templates.cv.spec.CvHeader;
import com.demcha.compose.document.templates.cv.spec.CvModule;
import com.demcha.compose.document.templates.cv.spec.CvSpec;
import com.demcha.compose.document.theme.BusinessTheme;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * Regression tests for the v1.6.4 BoxedSections changes:
 *
 * <ul>
 *   <li>{@link WorkHistoryBlock} renders each item with the structured
 *       title / date / organisation / description layout (not the
 *       legacy "everything on one line" fallback).</li>
 *   <li>{@link EducationBlock} renders each item with the same
 *       structured layout (degree / year / institution / details).</li>
 *   <li>{@link KeyValueBlock} renders each entry as <b>bold key:</b>
 *       followed by a regular-weight value.</li>
 *   <li>{@link MultiParagraphBlock} entries whose post-pipe segment is
 *       prose containing sentence-ending punctuation (e.g.
 *       {@code "... | 2019. First-class honours. Specialisation ..."})
 *       are rejected by the deprecated {@code parseWorkEntry} parser
 *       and fall back to plain paragraph rendering instead of
 *       collapsing the description into the date column.</li>
 * </ul>
 *
 * <p>Each test renders a focused {@link CvSpec} into an in-memory
 * session and compares the resulting layout tree against a checked-in
 * baseline JSON under
 * {@code src/test/resources/layout-snapshots/canonical-templates/cv-v2/regression/}.
 * Re-run with {@code -Dgraphcompose.updateSnapshots=true} to refresh
 * a baseline after an intentional change.</p>
 */
class BoxedSectionsStructuredRegressionTest {

    private static final BusinessTheme THEME = BusinessTheme.modern();

    private static CvHeader header() {
        return CvHeader.builder()
                .name("Artem Demchyshyn")
                .address("London, UK")
                .email("artem@demo.dev")
                .build();
    }

    @Test
    void workHistoryBlockRendersStructuredLayout() throws Exception {
        CvSpec spec = CvSpec.builder()
                .header(header())
                .module(CvModule.of("Professional Experience",
                        new WorkHistoryBlock(List.of(
                                new WorkHistoryBlock.Item(
                                        "Senior Platform Engineer",
                                        "Northwind Systems",
                                        "2024-Present",
                                        "Led the reusable document-generation platform."),
                                new WorkHistoryBlock.Item(
                                        "Software Engineer",
                                        "BrightLeaf Labs",
                                        "2021-2024",
                                        "Built backend services and rendering pipelines.")))))
                .build();
        DocumentTemplate<CvSpec> template = BoxedSections.create(THEME);
        try (DocumentSession document = TemplateTestSupport.openInMemoryDocument(
                PDRectangle.A4, 15, 10, 15, 15)) {
            template.compose(document, spec);
            TemplateTestSupport.assertCanonicalSnapshot(document,
                    "boxed_sections_work_history_block", "cv-v2", "regression");
        }
    }

    @Test
    void educationBlockRendersStructuredLayout() throws Exception {
        CvSpec spec = CvSpec.builder()
                .header(header())
                .module(CvModule.of("Education & Certifications",
                        new EducationBlock(List.of(
                                new EducationBlock.Item(
                                        "MSc Computer Science",
                                        "University of Manchester",
                                        "2021",
                                        "Distinction. Thesis: layout primitives for deterministic rendering."),
                                new EducationBlock.Item(
                                        "Oracle Java Certification",
                                        "Professional track",
                                        "2023",
                                        "Java 17 platform deep-dive: records, sealed types, pattern matching.")))))
                .build();
        DocumentTemplate<CvSpec> template = BoxedSections.create(THEME);
        try (DocumentSession document = TemplateTestSupport.openInMemoryDocument(
                PDRectangle.A4, 15, 10, 15, 15)) {
            template.compose(document, spec);
            TemplateTestSupport.assertCanonicalSnapshot(document,
                    "boxed_sections_education_block", "cv-v2", "regression");
        }
    }

    @Test
    void keyValueBlockRendersKeysInBold() throws Exception {
        CvSpec spec = CvSpec.builder()
                .header(header())
                .module(CvModule.of("Additional Information",
                        new KeyValueBlock(List.of(
                                new KeyValueBlock.Entry("Languages",
                                        "English (Fluent), German (Intermediate)"),
                                new KeyValueBlock.Entry("Work Eligibility",
                                        "Eligible to work in the UK and the EU"),
                                new KeyValueBlock.Entry("Open Source",
                                        "Maintainer of GraphCompose.")))))
                .build();
        DocumentTemplate<CvSpec> template = BoxedSections.create(THEME);
        try (DocumentSession document = TemplateTestSupport.openInMemoryDocument(
                PDRectangle.A4, 15, 10, 15, 15)) {
            template.compose(document, spec);
            TemplateTestSupport.assertCanonicalSnapshot(document,
                    "boxed_sections_key_value_bold_key", "cv-v2", "regression");
        }
    }

    @Test
    void multiParagraphProseDoesNotCollapseIntoDateColumn() throws Exception {
        // Locks the parseWorkEntry prose-rejection: lines like
        // "... | 2019. First-class honours. Specialisation ..." used
        // to collapse the whole post-pipe segment into the right-
        // aligned date column (the hyphen inside "First-class" tricked
        // looksLikeDate into accepting the prose as a date). The
        // deprecated parser now bails on sentence-ending punctuation
        // when no explicit date/description separator is present and
        // BoxedSections renders the line as a plain paragraph instead.
        CvSpec spec = CvSpec.builder()
                .header(header())
                .module(CvModule.of("Education & Certifications",
                        new MultiParagraphBlock(List.of(
                                "**BSc Software Engineering** - Imperial College London | "
                                        + "2019. First-class honours. Specialisation in "
                                        + "compilers and static analysis.",
                                "**Oracle Java Certification** - Professional track | "
                                        + "2023. Java 17 platform deep-dive: records, "
                                        + "sealed types, pattern matching."))))
                .build();
        DocumentTemplate<CvSpec> template = BoxedSections.create(THEME);
        try (DocumentSession document = TemplateTestSupport.openInMemoryDocument(
                PDRectangle.A4, 15, 10, 15, 15)) {
            template.compose(document, spec);
            TemplateTestSupport.assertCanonicalSnapshot(document,
                    "boxed_sections_prose_rejection_legacy_path", "cv-v2", "regression");
        }
    }
}
