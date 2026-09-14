package com.demcha.compose.document.templates.cv.presets;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.templates.cv.data.CvDocument;
import com.demcha.compose.document.templates.cv.data.CvIdentity;
import com.demcha.compose.document.templates.cv.data.EntriesSection;
import com.demcha.compose.document.templates.cv.data.ParagraphSection;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Measuring the name must not cost the caller what it composed first.
 *
 * <p>The presets that draw the name before their sidebar lay the name out on its own to learn
 * how tall it is, and a session has one list of roots: the measurement sets the caller's roots
 * aside and puts them back. A CV composed after a cover note has to keep the cover note.</p>
 */
class ReadingOrderColumnsTest {

    @Test
    void measuringPutsBackWhatTheCallerComposed() {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(DocumentPageSize.A4)
                .create()) {
            session.pageFlow(page -> page.addParagraph(p -> p.name("CoverNote").text("Cover note")));
            List<DocumentNode> composed = session.roots();

            Map<String, ReadingOrderColumns.Box> sizes = ReadingOrderColumns.measure(session,
                    page -> page.addParagraph(p -> p.name("Probe").text("Jane Doe")), "Probe");

            assertThat(session.roots()).isEqualTo(composed);
            assertThat(sizes.get("Probe").width()).isPositive();
            assertThat(sizes.get("Probe").height()).isPositive();
        }
    }

    @Test
    void aNameTheProbeDoesNotLayOutFailsAndStillPutsTheRootsBack() {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(DocumentPageSize.A4)
                .create()) {
            session.pageFlow(page -> page.addParagraph(p -> p.name("CoverNote").text("Cover note")));
            List<DocumentNode> composed = session.roots();

            assertThatThrownBy(() -> ReadingOrderColumns.measure(session,
                    page -> page.addParagraph(p -> p.name("Probe").text("Jane Doe")), "Missing"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Missing");
            assertThat(session.roots()).isEqualTo(composed);
        }
    }

    @Test
    void aCvComposedAfterACoverNoteKeepsTheCoverNote() throws Exception {
        byte[] pdf;
        try (DocumentSession session = GraphCompose.document()
                .pageSize(DocumentPageSize.A4)
                .create()) {
            session.pageFlow(page -> page.addParagraph(p -> p.name("CoverNote").text("Cover note")));
            NavySidebar.create().compose(session, document());
            pdf = session.toPdfBytes();
        }

        String text;
        try (PDDocument document = Loader.loadPDF(pdf)) {
            text = new PDFTextStripper().getText(document).replaceAll("\\s+", " ");
        }
        assertThat(text).contains("Cover note");
        assertThat(text).containsIgnoringCase("JANE DOE");
    }

    private static CvDocument document() {
        return CvDocument.builder()
                .identity(CvIdentity.builder()
                        .name("Jane", "Doe")
                        .jobTitle("Backend Engineer")
                        .contact("+44 0", "j@d.com", "London")
                        .build())
                .sections(
                        new ParagraphSection("Summary", "Builds rendering services."),
                        EntriesSection.builder("Experience")
                                .entry("Senior Engineer", "Acme Rendering",
                                        "2021-2024", "Built rendering services.")
                                .build())
                .build();
    }
}
