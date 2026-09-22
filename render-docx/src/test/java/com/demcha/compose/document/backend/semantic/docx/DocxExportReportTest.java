package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.chart.ChartData;
import com.demcha.compose.document.chart.ChartSpec;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentCornerRadius;
import com.demcha.compose.document.style.DocumentInsets;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * What the export could not carry, told to the program rather than to the log.
 *
 * <p>The export has always said what it drops — one line per kind, to a logger. A service
 * generating documents for other people has no log to read: it needs to know whether the
 * file it is about to send lost a chart, and which one. That is what the report is, and it
 * is deliberately not an error channel — an export that cannot proceed throws.</p>
 *
 * @author Artem Demchyshyn
 */
class DocxExportReportTest {

    private static final DocumentColor SURFACE = DocumentColor.rgb(238, 243, 249);

    @Test
    void aDocumentThatLosesNothingReportsNothing() throws Exception {
        assertThat(reportOf(page -> page.addParagraph(p -> p.text("Ordinary text")))
                .isEmpty())
                .as("an export with nothing to say says nothing")
                .isTrue();
    }

    @Test
    void aDroppedNodeIsNamedWithItsPath() throws Exception {
        DocxExportReport report = reportOf(page -> page
                .addParagraph(p -> p.text("Before"))
                .addLine(line -> line.name("Divider").thickness(1)));

        List<DocxExportReport.Note> notes = report.notes();
        assertThat(notes).hasSize(1);
        assertThat(notes.get(0).severity()).isEqualTo(DocxExportReport.Severity.DROPPED);
        assertThat(notes.get(0).path())
                .as("the path the layout graph addresses that node by, so it can be traced back")
                .contains("Divider");
        assertThat(notes.get(0).detail()).isNotEmpty();
    }

    @Test
    void everyDroppedNodeIsRecordedEvenThoughTheLogSaysItOnce() throws Exception {
        // The log is the summary and the report is the record: a caller asking what the
        // document lost wants the three it lost, not the fact that it lost a kind.
        DocxExportReport report = reportOf(page -> page
                .addLine(line -> line.name("First").thickness(1))
                .addLine(line -> line.name("Second").thickness(1))
                .addLine(line -> line.name("Third").thickness(1)));

        assertThat(report.count(DocxExportReport.Severity.DROPPED)).isEqualTo(3);
        assertThat(report.notes()).extracting(DocxExportReport.Note::path)
                .anyMatch(path -> path.contains("First"))
                .anyMatch(path -> path.contains("Third"));
    }

    @Test
    void whatSurvivesInAnotherFormIsApproximatedRatherThanDropped() throws Exception {
        // A rounded card keeps its fill and loses its corners. That is not the same as
        // losing the card, and a caller deciding whether to send the file needs the
        // difference.
        DocxExportReport report = reportOf(page -> page.addSection("Card", card -> card
                .fillColor(SURFACE)
                .cornerRadius(DocumentCornerRadius.of(14))
                .addParagraph(p -> p.text("Inside the card"))));

        assertThat(report.count(DocxExportReport.Severity.APPROXIMATED)).isEqualTo(1);
        assertThat(report.count(DocxExportReport.Severity.DROPPED)).isZero();
        assertThat(report.bySubject()).containsKey("corner radius");
        assertThat(report.notes().get(0).path()).contains("Card");
    }

    @Test
    void aChartSaysWhatItBecame() throws Exception {
        ChartData data = ChartData.builder()
                .categories("Q1", "Q2")
                .series("2025", 12.4, 15.1)
                .build();
        DocxExportReport report = reportOf(page -> page
                .chart(ChartSpec.bar().data(data).build()));

        assertThat(report.bySubject()).containsKey("chart");
        DocxExportReport.Note note = report.bySubject().get("chart").get(0);
        assertThat(note.severity()).isEqualTo(DocxExportReport.Severity.APPROXIMATED);
        assertThat(note.detail()).contains("data table");
        assertThat(note.path())
                .as("an unnamed node is addressed by its kind, which still locates it")
                .isEqualTo("ContainerNode[0]/Chart[0]");
    }

    @Test
    void withNoSinkTheExportIsUnchanged() throws Exception {
        // The report is opt-in, and asking for it must not change the document.
        byte[] withSink;
        byte[] without;
        try (DocumentSession session = session(page -> page.addParagraph(p -> p.text("Body")))) {
            withSink = session.export(new DocxSemanticBackend(report -> { }));
            without = session.export(new DocxSemanticBackend());
        }
        // Not byte-for-byte: a .docx carries a creation timestamp. The document is.
        assertThat(withSink.length).isCloseTo(without.length, org.assertj.core.data.Offset.offset(64));
    }

    @Test
    void aNoteReadsAsOneLine() {
        DocxExportReport.Note note = new DocxExportReport.Note(
                DocxExportReport.Severity.DROPPED, "chart", "Body[0]/Revenue[2]", "no analogue");

        assertThat(note.toString()).isEqualTo("DROPPED chart at Body[0]/Revenue[2]: no analogue");
    }

    private static DocxExportReport reportOf(Consumer<PageFlowBuilder> content) throws Exception {
        AtomicReference<DocxExportReport> captured = new AtomicReference<>();
        try (DocumentSession session = session(content)) {
            session.export(new DocxSemanticBackend(captured::set));
        }
        assertThat(captured.get()).as("the sink is called once the bytes exist").isNotNull();
        return captured.get();
    }

    private static DocumentSession session(Consumer<PageFlowBuilder> content) {
        DocumentSession session = GraphCompose.document()
                .pageSize(400, 600)
                .margin(DocumentInsets.of(20))
                .create();
        session.pageFlow(content::accept);
        return session;
    }
}
