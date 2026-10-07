package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.dsl.ShapeBuilder;
import com.demcha.compose.document.dsl.ShapeContainerBuilder;
import com.demcha.compose.document.dsl.TableBuilder;
import com.demcha.compose.document.layout.PlacedFragment;
import com.demcha.compose.document.layout.payloads.ParagraphFragmentPayload;
import com.demcha.compose.document.layout.payloads.ParagraphTextSpan;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.LayerAlign;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.ClipPolicy;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextIndent;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.table.DocumentTableCell;
import com.demcha.compose.document.table.DocumentTableColumn;
import com.demcha.compose.font.FontName;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * An auto-sized paragraph's text is written at the size the page fits it to, smaller or larger
 * than its style's, on every path that writes a paragraph: the file holds what the page draws.
 * A run with a style of its own keeps it, as on the page. Where the page's lines do not tell the
 * size — no layout, or lines in sizes each a run's own — the text is written at its style's size,
 * and named.
 */
class DocxAutoSizeTest {

    private static final String HEADLINE = "A headline far too long for one line at its size";
    private static final DocumentTextStyle TEN = DocumentTextStyle.DEFAULT.withSize(10);
    private static final double CONTENT = 180;

    @Test
    void aParagraphIsWrittenAtTheSizeThePageFitsItTo() throws Exception {
        Export shrunk = export(page -> page.addParagraph(p -> p.name("Headline").text(HEADLINE)
                .textStyle(DocumentTextStyle.DEFAULT.withSize(24)).autoSize(24, 6)));
        double fitted = shrunk.firstSize("Headline");
        assertThat(fitted).as("the page fits it smaller").isLessThan(24);
        XWPFParagraph paragraph = shrunk.paragraphWith("headline");
        assertThat(paragraph.getRuns()).singleElement()
                .satisfies(run -> assertThat(run.getFontSizeAsDouble()).isEqualTo(wordsSize(fitted)));
        assertThat(markSize(paragraph)).as("the mark Word continues from").isEqualTo(wordsSize(fitted));
        assertThat(shrunk.notes()).isEmpty();

        // Up to a size above its style's, a line that fits at it is set and written at it.
        Export grown = export(page -> page.addParagraph(p -> p.name("Hi").text("Hi").textStyle(TEN).autoSize(24)));
        assertThat(grown.firstSize("Hi")).isEqualTo(24.0);
        assertThat(grown.paragraphWith("Hi").getRuns()).singleElement()
                .satisfies(run -> assertThat(run.getFontSizeAsDouble()).isEqualTo(24.0));
        assertThat(grown.notes()).isEmpty();
    }

    @Test
    void aRunWithAStyleOfItsOwnKeepsItsSizeAndOneWithNoneTakesTheFittedSize() throws Exception {
        Export export = export(page -> page.addParagraph(p -> p.name("Greeting").textStyle(TEN)
                .inlineText("Hi ", DocumentTextStyle.DEFAULT.withSize(12)).inlineText("there").autoSize(24)));
        assertThat(export.paragraphWith("there").getRuns()).filteredOn(run -> !run.text().isEmpty())
                .extracting(XWPFRun::text, XWPFRun::getFontSizeAsDouble)
                .containsExactly(org.assertj.core.groups.Tuple.tuple("Hi ", 12.0),
                        org.assertj.core.groups.Tuple.tuple("there", 24.0));
        assertThat(export.notes()).isEmpty();

        // A paragraph ending in a chip has its own style on the mark, not the chip's: at the fitted size.
        Export chip = export(page -> page.addParagraph(p -> p.name("Status").textStyle(TEN).inlineText("Status ")
                .inlineHighlight("OK", DocumentTextStyle.DEFAULT.withSize(9), DocumentColor.rgb(200, 40, 40), 2,
                        DocumentInsets.of(1))
                .autoSize(24)));
        double fitted = chip.firstSize("Status");
        assertThat(fitted).as("fitted to another size than its style's").isNotEqualTo(10.0);
        assertThat(markSize(chip.paragraphWith("Status"))).isEqualTo(wordsSize(fitted));
    }

    @Test
    void aPrefixIsMeasuredAtTheSizeThePageSetsItIn() throws Exception {
        // The page sets the prefix in the paragraph's style at the fitted size, every run keeping
        // its own: the indent it is written as is the one a paragraph of that size is given.
        Export fittedPrefix = export(page -> page.addParagraph(p -> p.name("Item")
                .textStyle(DocumentTextStyle.DEFAULT.withSize(24)).inlineText(HEADLINE, TEN)
                .bulletOffset("    ").indentStrategy(DocumentTextIndent.ALL_LINES).autoSize(24, 6)));
        double fitted = fittedPrefix.firstSize("Item");
        assertThat(fitted).as("at the fitted size, not the run's").isLessThan(24).isNotEqualTo(10.0);
        Export atThatSize = export(page -> page.addParagraph(p -> p.name("Item")
                .textStyle(DocumentTextStyle.DEFAULT.withSize(fitted)).inlineText(HEADLINE, TEN)
                .bulletOffset("    ").indentStrategy(DocumentTextIndent.ALL_LINES)));

        String indent = fittedPrefix.paragraphWith("headline").getCTP().getPPr().getInd().xmlText();
        assertThat(indent).contains("w:left=")
                .isEqualTo(atThatSize.paragraphWith("headline").getCTP().getPPr().getInd().xmlText());
        assertThat(fittedPrefix.notes()).isEmpty();
    }

    @Test
    void everyPathThatWritesAParagraphWritesItAtTheSizeThePageFitsItTo() throws Exception {
        // A paragraph composed in a table cell.
        Export cell = export(page -> page.add(new TableBuilder().name("Sums").columns(DocumentTableColumn.fixed(120))
                .rowCells(DocumentTableCell.node(new ParagraphBuilder().name("Total").text("Total due this month")
                        .textStyle(TEN).autoSize(20, 6).build()))
                .build()));
        double total = cell.sizeOf("Total");
        assertThat(total).isNotEqualTo(10.0);
        assertThat(cell.document().getTables().get(0).getRow(0).getCell(0).getParagraphs().get(0).getRuns())
                .singleElement().satisfies(run -> assertThat(run.getFontSizeAsDouble()).isEqualTo(wordsSize(total)));
        assertThat(cell.notes()).isEmpty();

        // One side of an overlay's left-and-right pair.
        Export pair = export(page -> page.add(new ShapeContainerBuilder().name("EntryHead").rectangle(CONTENT, 20)
                .clipPolicy(ClipPolicy.OVERFLOW_VISIBLE)
                .position(new ParagraphBuilder().name("Title").text("ENGINEER").textStyle(TEN).autoSize(14).build(),
                        0, 0, LayerAlign.CENTER_LEFT)
                .position(new ParagraphBuilder().name("Dates").text("2022").align(TextAlign.RIGHT).build(),
                        0, 0, LayerAlign.CENTER_RIGHT)
                .build()));
        assertThat(pair.firstSize("Title")).isEqualTo(14.0);
        assertThat(pair.paragraphWith("ENGINEER").getRuns()).filteredOn(run -> "ENGINEER".equals(run.text()))
                .singleElement().satisfies(run -> assertThat(run.getFontSizeAsDouble()).isEqualTo(14.0));

        // Text over the flow, in a text box.
        Export box = export(page -> page.add(new ShapeContainerBuilder().name("Sidebar")
                .rectangle(100, 120).clipPolicy(ClipPolicy.OVERFLOW_VISIBLE)
                .margin(new DocumentInsets(-30, 0, -90, -30))
                .position(new ShapeBuilder().name("Block").size(100, 120)
                        .fillColor(DocumentColor.rgb(160, 80, 50)).build(), 0, 0, LayerAlign.TOP_LEFT, 0)
                .position(new ParagraphBuilder().name("Monogram").text("LM").textStyle(TEN).autoSize(30).build(),
                        10, 10, LayerAlign.TOP_LEFT, 1)
                .build()).addParagraph("Masthead"));
        assertThat(box.firstSize("Monogram")).isEqualTo(30.0);
        assertThat(box.xml()).contains("<wps:txbx>");
        assertThat(sizesBefore(box.xml(), "LM")).isNotEmpty().containsOnly(30.0);

        // A badge's initials, in its shape.
        Export badge = export(page -> page.add(badge("JR")));
        double initials = badge.firstSize("Initials");
        assertThat(initials).isGreaterThan(8);
        assertThat(badge.xml()).contains("<wps:txbx>");
        assertThat(sizesBefore(badge.xml(), "JR")).isNotEmpty().containsOnly(wordsSize(initials));
        assertThat(badge.notes()).isEmpty();
        // Initials the page reads as markdown, in one face at the fitted size, stay in the shape.
        Export marked = export(page -> page.add(badge("*JR*")));
        assertThat(marked.xml()).contains("<wps:txbx>").contains("<w:i w:val=\"on\"/>").doesNotContain("*JR*");
        assertThat(sizesBefore(marked.xml(), "JR")).isNotEmpty().containsOnly(wordsSize(marked.firstSize("Initials")));
        assertThat(marked.notes()).isEmpty();
    }

    /** A disc holding initials of 8pt the page fits to its width. */
    private static DocumentNode badge(String initials) {
        return new ShapeContainerBuilder().name("Badge").circle(40).fillColor(DocumentColor.rgb(30, 50, 90))
                .center(new ParagraphBuilder().name("Initials").text(initials)
                        .textStyle(DocumentTextStyle.DEFAULT.withSize(8)).autoSize(30, 6).build())
                .build();
    }

    @Test
    void whereItsLinesDoNotTellTheSizeTheTextIsWrittenAtItsStylesAndNamed() throws Exception {
        // Fitted to 14pt beside runs of 14pt and 11pt of their own, the lines do not tell which is its.
        // The body text above sets the document's own size, which a run at it does not state.
        Export export = export(page -> page.addParagraph(p -> p.text(HEADLINE + " " + HEADLINE).textStyle(TEN))
                .addParagraph(p -> p.name("Runs").textStyle(DocumentTextStyle.DEFAULT.withSize(8))
                .inlineText("A ", DocumentTextStyle.DEFAULT.withSize(14))
                .inlineText("B ", DocumentTextStyle.DEFAULT.withSize(11)).inlineText("C").autoSize(14)));
        assertThat(export.paragraphWith("A B C").getRuns()).filteredOn(run -> !run.text().isEmpty())
                .extracting(XWPFRun::getFontSizeAsDouble).containsExactly(14.0, 11.0, 8.0);
        assertThat(export.notes()).containsExactly("written as a paragraph; its text is written at 8pt — the size the "
                                                   + "page fits it to is not measured");

        // Read as markdown in other letters than the pieces, over lines of two sizes — a heading's
        // and the body's — the lines do not tell which is its own either.
        Export arabic = export(page -> page.addParagraph(p -> p.text("# مرحبا_\nسطر *ب*")
                .textStyle(DocumentTextStyle.builder().fontName(FontName.AMIRI).size(10).build()).autoSize(20, 6)));
        assertThat(arabic.notes()).singleElement().asString().startsWith("written as a paragraph; its text is written "
                                                                         + "at 10pt — the size the page fits it to is "
                                                                         + "not measured");

        // With no layout, nothing tells it.
        XWPFDocument unlaid = DocxExports.withoutLayout(240, 600, 30, page -> page
                .addParagraph(p -> p.text(HEADLINE + " " + HEADLINE).textStyle(TEN))
                .addParagraph(p -> p.text("Hi").textStyle(DocumentTextStyle.DEFAULT.withSize(24)).autoSize(24, 6)));
        assertThat(unlaid.getParagraphs().stream().filter(paragraph -> paragraph.getText().equals("Hi")).findFirst()
                .orElseThrow().getRuns()).singleElement()
                .satisfies(run -> assertThat(run.getFontSizeAsDouble()).isEqualTo(24.0));
    }

    private static double wordsSize(double size) {
        return Math.round(size * 2) / 2.0;
    }

    private static double markSize(XWPFParagraph paragraph) {
        return ((Number) paragraph.getCTP().getPPr().getRPr().getSzArray(0).getVal()).doubleValue() / 2;
    }

    /** The sizes the runs reading a text are written at, wherever in the body they stand — a text box's or a shape's too. */
    private static List<Double> sizesBefore(String xml, String text) {
        Matcher matcher = Pattern.compile("<w:sz w:val=\"(\\d+)\"/>(?:(?!</w:r>).)*<w:t>" + Pattern.quote(text) + "</w:t>",
                Pattern.DOTALL).matcher(xml);
        List<Double> sizes = new ArrayList<>();
        while (matcher.find()) {
            sizes.add(Integer.parseInt(matcher.group(1)) / 2.0);
        }
        return sizes;
    }

    private record Export(XWPFDocument document, DocxExportReport report, Map<String, List<PlacedFragment>> fragments) {

        XWPFParagraph paragraphWith(String text) {
            return document.getParagraphs().stream().filter(paragraph -> paragraph.getText().contains(text))
                    .findFirst().orElseThrow(() -> new AssertionError("no paragraph holds " + text));
        }

        String xml() {
            return document.getDocument().xmlText();
        }

        List<String> notes() {
            return report.bySubject().getOrDefault("ParagraphNode", List.of()).stream()
                    .map(DocxExportReport.Note::detail).toList();
        }

        /** The size the page sets the first text of a node in. */
        double firstSize(String name) {
            return spans(fragments.entrySet().stream().filter(entry -> entry.getKey().contains(name))
                    .flatMap(entry -> entry.getValue().stream()))
                    .findFirst().orElseThrow().textStyle().size();
        }

        /** The size the page sets the first text holding some letters in: a table cell's node is laid out in its table's fragment. */
        double sizeOf(String letters) {
            return spans(fragments.values().stream().flatMap(List::stream))
                    .filter(span -> span.text() != null && span.text().contains(letters))
                    .findFirst().orElseThrow().textStyle().size();
        }

        private static java.util.stream.Stream<ParagraphTextSpan> spans(java.util.stream.Stream<PlacedFragment> laid) {
            return laid.map(PlacedFragment::payload).filter(ParagraphFragmentPayload.class::isInstance)
                    .map(ParagraphFragmentPayload.class::cast)
                    .flatMap(payload -> payload.lines().stream()).flatMap(line -> line.spans().stream())
                    .filter(ParagraphTextSpan.class::isInstance).map(ParagraphTextSpan.class::cast);
        }
    }

    private static Export export(Consumer<PageFlowBuilder> content) throws Exception {
        AtomicReference<DocxExportReport> report = new AtomicReference<>();
        byte[] docx;
        Map<String, List<PlacedFragment>> fragments;
        try (DocumentSession session = GraphCompose.document().pageSize(240, 600).margin(DocumentInsets.of(30)).create()) {
            session.pageFlow(content::accept);
            fragments = session.layoutGraph().fragments().stream()
                    .collect(java.util.stream.Collectors.groupingBy(PlacedFragment::path));
            docx = session.export(new DocxSemanticBackend(report::set));
        }
        return new Export(new XWPFDocument(new ByteArrayInputStream(docx)), report.get(), fragments);
    }
}
