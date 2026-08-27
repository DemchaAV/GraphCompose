package com.demcha.compose.document.templates.cv.presets;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.templates.cv.data.CvDocument;
import com.demcha.compose.document.templates.cv.data.CvIdentity;
import com.demcha.compose.document.templates.cv.data.EntriesSection;
import com.demcha.compose.document.templates.cv.data.ParagraphSection;
import com.demcha.compose.document.templates.cv.data.SkillsSection;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * What a CV rendered through Monogram Sidebar keeps, and what it is documented to drop.
 *
 * <p>The body is a column flow, so the preset carries every entry it is handed onto as
 * many pages as it needs — the per-section caps it used to enforce are gone, and this
 * pins that. What it draws <em>of</em> an entry is a separate question and not a design
 * decision at all: an experience entry's subtitle is the employer, and a CV that names a
 * position and a date but no employer is simply wrong.</p>
 */
class MonogramSidebarContentFidelityTest {

    @Test
    void anExperienceEntryNamesTheEmployerItWasGiven() throws Exception {
        String text = renderText(document());

        assertThat(text)
                .describedAs("the main column drew the position, the date and the "
                        + "description of each job and never its employer, so every "
                        + "company name was missing from the rendered CV")
                .contains("Acme Rendering");
    }

    @Test
    void everyExperienceEntryReachesThePage() throws Exception {
        String text = renderText(document());

        // Positions render in caps, employers as written.
        assertThat(text)
                .describedAs("the experience entries this preset draws")
                .contains("SENIOR ENGINEER")
                .contains("Acme Rendering")
                .contains("Northwind Data");

        assertThat(text)
                .describedAs("the body is a column flow now, so a third job continues "
                        + "onto the next page instead of being dropped without a word — "
                        + "the per-section caps this preset used to carry are gone")
                .contains("Nikoplast");
    }

    private static String renderText(CvDocument doc) throws Exception {
        byte[] pdf;
        try (DocumentSession session = GraphCompose.document()
                .pageSize(DocumentPageSize.A4)
                .margin(DocumentInsets.of(MonogramSidebar.RECOMMENDED_MARGIN))
                .create()) {
            MonogramSidebar.create().compose(session, doc);
            pdf = session.toPdfBytes();
        }
        try (PDDocument document = Loader.loadPDF(pdf)) {
            // Collapse the layout's own line breaks: this asks whether a phrase reached
            // the page at all, and where the engine chose to wrap it is the visual
            // regression tests' business.
            return new PDFTextStripper().getText(document).replaceAll("\\s+", " ");
        }
    }

    /** Three employers, so a preset that still capped its blocks would visibly drop one. */
    private static CvDocument document() {
        return CvDocument.builder()
                .identity(CvIdentity.builder()
                        .name("Jane", "Doe")
                        .jobTitle("Backend Engineer")
                        .contact("+44 0", "j@d.com", "London")
                        .build())
                .sections(
                        new ParagraphSection("Professional Summary",
                                "Builds document pipelines across the JVM."),
                        SkillsSection.builder("Technical Skills")
                                .group("Languages", "Java 21", "Kotlin")
                                .build(),
                        EntriesSection.builder("Professional Experience")
                                .entry("Senior Engineer", "Acme Rendering",
                                        "2021-2024", "Built rendering services.")
                                .entry("Engineer", "Northwind Data",
                                        "2018-2021", "Owned the ingestion pipeline.")
                                .entry("Junior Engineer", "Nikoplast",
                                        "2015-2018", "Maintained the order system.")
                                .build())
                .build();
    }
}
