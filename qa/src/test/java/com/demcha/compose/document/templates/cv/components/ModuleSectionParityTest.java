package com.demcha.compose.document.templates.cv.components;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.templates.cv.data.CvDocument;
import com.demcha.compose.document.templates.cv.data.CvIdentity;
import com.demcha.compose.document.templates.cv.data.CvItem;
import com.demcha.compose.document.templates.cv.data.CvKind;
import com.demcha.compose.document.templates.cv.data.CvSection;
import com.demcha.compose.document.templates.cv.data.EntriesSection;
import com.demcha.compose.document.templates.cv.data.ModuleSection;
import com.demcha.compose.document.templates.cv.data.ParagraphSection;
import com.demcha.compose.document.templates.cv.data.RowStyle;
import com.demcha.compose.document.templates.cv.data.RowsSection;
import com.demcha.compose.document.templates.cv.data.SectionRole;
import com.demcha.compose.document.templates.cv.presets.ModernProfessional;
import com.demcha.compose.testing.layout.LayoutSnapshotJson;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A {@code ModuleSection} chosen at runtime must lay out exactly like the
 * section a Java author would have written by hand for the same content.
 *
 * <p>That equivalence is the whole basis of the runtime module: it renders
 * through the existing components rather than beside them, so the two
 * authoring routes are two spellings of one document. Left unchecked it is a
 * claim in a Javadoc, and the failure it hides is silent — a module that
 * merely looks close, on a preset nobody re-renders, in a CV nobody compares
 * side by side.</p>
 *
 * <p>Each case pins both halves of "the same": the layout snapshot, which
 * carries node structure and bounds but not text, and the extracted PDF text,
 * which carries the words but not their positions. Either alone passes
 * documents the other would catch.</p>
 */
class ModuleSectionParityTest {

    @Test
    void datedEntriesMatchAHandWrittenEntriesSection() throws Exception {
        CvSection handWritten = EntriesSection.builder("Professional Experience")
                .entry("Senior Backend Engineer", "Acme GmbH", "2021 - Present",
                        "Cut p99 latency by 40%.")
                .entry("Backend Engineer", "Northwind Systems", "2018 - 2021",
                        "Owned the billing service.")
                .build();

        CvSection module = ModuleSection.builder("Professional Experience",
                        SectionRole.EXPERIENCE, CvKind.ENTRIES_DATED)
                .item(CvItem.of("Senior Backend Engineer").at("Acme GmbH")
                        .period("2021 - Present").paragraphs("Cut p99 latency by 40%."))
                .item(CvItem.of("Backend Engineer").at("Northwind Systems")
                        .period("2018 - 2021").paragraphs("Owned the billing service."))
                .build();

        assertSameRender(handWritten, module);
    }

    @Test
    void anInlineListMatchesAHandWrittenPlainRowsSection() throws Exception {
        CvSection handWritten = RowsSection.builder("Additional Information", RowStyle.PLAIN)
                .row("Languages", "English (Fluent), German (B2)")
                .row("Interests", "Chess, long-distance cycling")
                .build();

        CvSection module = ModuleSection.builder("Additional Information",
                        SectionRole.OTHER, CvKind.INLINE_LIST)
                .item(CvItem.of("Languages").paragraphs("English (Fluent)", "German (B2)"))
                .item(CvItem.of("Interests").paragraphs("Chess", "long-distance cycling"))
                .build();

        assertSameRender(handWritten, module);
    }

    @Test
    void oneLineBulletsMatchAHandWrittenBulletedRowsSection() throws Exception {
        CvSection handWritten = RowsSection.builder("Highlights", RowStyle.BULLETED)
                .row("Throughput", "Doubled it")
                .row("Onboarding", "Cut to two days")
                .build();

        CvSection module = ModuleSection.builder("Highlights", SectionRole.OTHER, CvKind.BULLETS)
                .item(CvItem.of("Throughput").paragraphs("Doubled it"))
                .item(CvItem.of("Onboarding").paragraphs("Cut to two days"))
                .build();

        assertSameRender(handWritten, module);
    }

    @Test
    void stackedBulletsMatchAHandWrittenStackedRowsSection() throws Exception {
        CvSection handWritten = RowsSection.builder("Projects", RowStyle.BULLETED_STACKED)
                .row("GraphCompose (Java 21, PDFBox)",
                        "A declarative layout engine for programmatic documents.")
                .row("Ledger (Kotlin)", "Double-entry bookkeeping for small studios.")
                .build();

        // paragraphs(), not bullets(): a stacked row indents its description under
        // the title, which is what prose does. BodyStyle.BULLETS asks for a bullet
        // on each description line instead — a different shape, pinned by the case
        // below rather than smuggled into this comparison.
        CvSection module = ModuleSection.builder("Projects", SectionRole.PROJECTS,
                        CvKind.BULLETS_STACKED)
                .item(CvItem.of("GraphCompose (Java 21, PDFBox)")
                        .paragraphs("A declarative layout engine for programmatic documents."))
                .item(CvItem.of("Ledger (Kotlin)")
                        .paragraphs("Double-entry bookkeeping for small studios."))
                .build();

        assertSameRender(handWritten, module);
    }

    @Test
    void aBulletedBodyNestsABulletUnderTheItemsOwn() throws Exception {
        CvSection prose = ModuleSection.builder("Projects", SectionRole.PROJECTS,
                        CvKind.BULLETS_STACKED)
                .item(CvItem.of("GraphCompose").paragraphs("Shipped it", "Measured it"))
                .build();
        CvSection bulleted = ModuleSection.builder("Projects", SectionRole.PROJECTS,
                        CvKind.BULLETS_STACKED)
                .item(CvItem.of("GraphCompose").bullets("Shipped it", "Measured it"))
                .build();

        assertThat(text(bulleted))
                .as("BodyStyle.BULLETS must reach the page as bullets, not as indented prose")
                .isNotEqualTo(text(prose))
                .contains("• Shipped it", "• Measured it");
    }

    @Test
    void proseMatchesAHandWrittenParagraphSection() throws Exception {
        CvSection handWritten = new ParagraphSection("Professional Summary",
                "Backend engineer with ten years on payment systems.");

        CvSection module = ModuleSection.summary("Professional Summary",
                "Backend engineer with ten years on payment systems.");

        assertSameRender(handWritten, module);
    }

    @Test
    void undatedEntriesMatchAHandWrittenEntriesSectionWithBlankDates() throws Exception {
        // The blank-date path is a change to EntryRenderer itself, so pin it the
        // same way: an undated module and the hand-written section that has always
        // been able to express one must produce the same layout.
        CvSection handWritten = EntriesSection.builder("Certifications")
                .entry("AWS Solutions Architect", "Amazon", "", "")
                .entry("CKA", "Linux Foundation", "", "")
                .build();

        CvSection module = ModuleSection.builder("Certifications", SectionRole.OTHER,
                        CvKind.ENTRIES)
                .item(CvItem.of("AWS Solutions Architect").at("Amazon"))
                .item(CvItem.of("CKA").at("Linux Foundation"))
                .build();

        assertSameRender(handWritten, module);
    }

    @Test
    void anUndatedEntryDropsTheDateColumnRatherThanReservingIt() throws Exception {
        // The kind's whole contract is that it ignores the period. Rendering an
        // empty date column instead would still "ignore" it while narrowing every
        // title on the page, so pin the shape, not just the absent text.
        CvSection dated = ModuleSection.builder("Certifications", SectionRole.OTHER,
                        CvKind.ENTRIES_DATED)
                .item(CvItem.of("AWS Solutions Architect").at("Amazon").period("2024"))
                .build();
        CvSection undated = ModuleSection.builder("Certifications", SectionRole.OTHER,
                        CvKind.ENTRIES)
                .item(CvItem.of("AWS Solutions Architect").at("Amazon").period("2024"))
                .build();

        assertThat(layoutJson(undated))
                .as("an undated entry must not lay out like a dated one")
                .isNotEqualTo(layoutJson(dated));
        assertThat(text(undated)).contains("AWS Solutions Architect", "Amazon");
        assertThat(text(undated))
                .as("the period must not reach the page under CvKind.ENTRIES")
                .doesNotContain("2024");
        assertThat(text(dated)).contains("2024");
    }

    @Test
    void anItemLinkRendersAsAClickableTitle() throws Exception {
        CvSection module = ModuleSection.builder("Projects", SectionRole.PROJECTS,
                        CvKind.BULLETS_STACKED)
                .item(CvItem.of("GraphCompose").linkedTo("https://example.dev/gc")
                        .paragraphs("A layout engine."))
                .build();

        assertThat(text(module))
                .as("the link URL is the target, not the visible text")
                .contains("GraphCompose")
                .doesNotContain("https://example.dev/gc");
        assertThat(text(module))
                .as("markdown markers are instructions, not content — none may reach the page")
                .doesNotContain("*", "[", "]");
        assertThat(externalLinkTargets(module)).contains("https://example.dev/gc");
    }

    @Test
    void aBracketedTitleNeverLeaksItsUrlAsVisibleText() throws Exception {
        // The markdown link label admits no brackets, so wrapping this title would
        // match nothing and print the whole construction. Losing the click target
        // is the acceptable outcome here; printing the URL is not.
        CvSection module = ModuleSection.builder("Projects", SectionRole.PROJECTS,
                        CvKind.BULLETS_STACKED)
                .item(CvItem.of("Ledger [v2]").linkedTo("https://example.dev/ledger")
                        .paragraphs("Double-entry bookkeeping."))
                .build();

        assertThat(text(module))
                .contains("Ledger [v2]", "Double-entry bookkeeping.")
                .doesNotContain("https://example.dev/ledger");
    }

    @Test
    void aTitleOnlyBulletHasNoColonPointingAtNothing() throws Exception {
        CvSection module = ModuleSection.builder("Interests", SectionRole.OTHER, CvKind.BULLETS)
                .item("Chess")
                .item("Long-distance cycling")
                .build();

        assertThat(text(module))
                .contains("Chess", "Long-distance cycling")
                .doesNotContain("Chess:", "cycling:");
    }

    @Test
    void anInlineListWithNothingToListRendersItsLabelAlone() throws Exception {
        CvSection module = ModuleSection.builder("Languages", SectionRole.LANGUAGES,
                        CvKind.INLINE_LIST)
                .item("English")
                .build();

        assertThat(text(module)).contains("English").doesNotContain("English:");
    }

    @Test
    void everyKindIgnoresExactlyTheFieldsItSaysItIgnores() throws Exception {
        // The contract that makes one item record serve every module is that the
        // kind decides what is read. Stated in CvKind's Javadoc and the docs table;
        // pinned here, per kind, by rendering one item that carries everything.
        CvItem everything = CvItem.of("Item title")
                .at("SubtitleValue").in("LocationValue").period("PeriodValue")
                .paragraphs("Body line.");

        assertThat(render(CvKind.PARAGRAPH, everything))
                .as("PARAGRAPH reads the body alone")
                .contains("Body line.")
                .doesNotContain("Item title", "SubtitleValue", "PeriodValue", "LocationValue");
        assertThat(render(CvKind.BULLETS, everything))
                .as("BULLETS reads title and body")
                .contains("Item title", "Body line.")
                .doesNotContain("SubtitleValue", "PeriodValue", "LocationValue");
        assertThat(render(CvKind.BULLETS_STACKED, everything))
                .as("BULLETS_STACKED reads title and body")
                .contains("Item title", "Body line.")
                .doesNotContain("SubtitleValue", "PeriodValue", "LocationValue");
        assertThat(render(CvKind.INLINE_LIST, everything))
                .as("INLINE_LIST reads title and body")
                .contains("Item title", "Body line.")
                .doesNotContain("SubtitleValue", "PeriodValue", "LocationValue");
        assertThat(render(CvKind.ENTRIES, everything))
                .as("ENTRIES reads everything but the period")
                .contains("Item title", "SubtitleValue", "LocationValue", "Body line.")
                .doesNotContain("PeriodValue");
        assertThat(render(CvKind.ENTRIES_DATED, everything))
                .as("ENTRIES_DATED reads every field")
                .contains("Item title", "SubtitleValue", "LocationValue", "PeriodValue",
                        "Body line.");
    }

    private static String render(CvKind kind, CvItem item) throws Exception {
        return text(ModuleSection.of("Section", SectionRole.OTHER, kind, item));
    }

    // -- helpers ---------------------------------------------------------

    private static void assertSameRender(CvSection handWritten, CvSection module) throws Exception {
        assertThat(layoutJson(module))
                .as("a runtime module must lay out node-for-node like the hand-written section")
                .isEqualTo(layoutJson(handWritten));
        assertThat(text(module))
                .as("...and carry the same words: the snapshot above compares structure, not content")
                .isEqualTo(text(handWritten));
    }

    private static String layoutJson(CvSection section) throws Exception {
        try (DocumentSession session = newSession()) {
            ModernProfessional.create().compose(session, docWith(section));
            return LayoutSnapshotJson.toJson(session.layoutSnapshot());
        }
    }

    private static String text(CvSection section) throws Exception {
        try (DocumentSession session = newSession()) {
            ModernProfessional.create().compose(session, docWith(section));
            try (PDDocument pdf = Loader.loadPDF(session.toPdfBytes())) {
                return new PDFTextStripper().getText(pdf).replaceAll("\\s+", " ").trim();
            }
        }
    }

    private static java.util.List<String> externalLinkTargets(CvSection section) throws Exception {
        try (DocumentSession session = newSession()) {
            ModernProfessional.create().compose(session, docWith(section));
            try (PDDocument pdf = Loader.loadPDF(session.toPdfBytes())) {
                java.util.List<String> targets = new java.util.ArrayList<>();
                for (org.apache.pdfbox.pdmodel.PDPage page : pdf.getPages()) {
                    for (org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation annotation
                            : page.getAnnotations()) {
                        if (annotation instanceof org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink link
                                && link.getAction()
                                instanceof org.apache.pdfbox.pdmodel.interactive.action.PDActionURI uri) {
                            targets.add(uri.getURI());
                        }
                    }
                }
                return targets;
            }
        }
    }

    private static CvDocument docWith(CvSection section) {
        return CvDocument.builder()
                .identity(CvIdentity.builder()
                        .name("Jordan", "Rivera")
                        .jobTitle("Backend Engineer")
                        .contact("+1 555 0100", "jordan@example.com", "Berlin, DE")
                        .build())
                .section(section)
                .build();
    }

    private static DocumentSession newSession() {
        float margin = (float) ModernProfessional.RECOMMENDED_MARGIN;
        return GraphCompose.document()
                .pageSize(DocumentPageSize.A4)
                .margin(margin, margin, margin, margin)
                .create();
    }
}
