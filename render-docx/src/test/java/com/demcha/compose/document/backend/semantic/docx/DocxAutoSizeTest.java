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
import com.demcha.compose.document.node.ParagraphNode;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.ClipPolicy;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentLetterSpacing;
import com.demcha.compose.document.style.DocumentTextIndent;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.table.DocumentTableCell;
import com.demcha.compose.document.table.DocumentTableColumn;
import com.demcha.compose.font.FontName;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.xmlbeans.SimpleValue;
import org.apache.xmlbeans.XmlObject;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

/**
 * An auto-sized paragraph's text is written at the size the page fits it to, smaller or larger
 * than its style's, on every path that writes a paragraph: the file holds what the page draws.
 * A run with a style of its own keeps it, as on the page. Where the page's lines do not tell the
 * size — no lines read, lines in sizes that do not say which is the paragraph's, or one place's of
 * the several a paragraph is added at — the text is written at its style's size, and named.
 */
class DocxAutoSizeTest {

    private static final String HEADLINE = "A headline far too long for one line at its size";
    private static final DocumentTextStyle TEN = DocumentTextStyle.DEFAULT.withSize(10);
    private static final double CONTENT = 180;
    private static final String W = "declare namespace w='http://schemas.openxmlformats.org/wordprocessingml/2006/main' ";
    private static final String UNMEASURED = "the size the page fits it to is not measured";

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
        assertThat(markSize(grown.paragraphWith("Hi"))).isEqualTo(24.0);
        assertThat(grown.notes()).isEmpty();
    }

    @Test
    void textThePageLaysOutWithEveryMarkIsWrittenAtTheOneSizeItsLinesHold() throws Exception {
        // A session that reads no markdown lays the marks out, in the one size it fits the text to.
        Export authored = export(false, page -> page.addParagraph(p -> p.name("Draft")
                .text("**Draft** status of the report for the quarter").textStyle(DocumentTextStyle.DEFAULT.withSize(24))
                .autoSize(24, 6)));
        double fitted = authored.firstSize("Draft");
        assertThat(fitted).isLessThan(24);
        assertThat(authored.paragraphWith("Draft").getRuns()).singleElement().satisfies(run -> {
            assertThat(run.text()).isEqualTo("**Draft** status of the report for the quarter");
            assertThat(run.getFontSizeAsDouble()).isEqualTo(wordsSize(fitted));
        });
        assertThat(authored.notes()).isEmpty();

        // So does one that reads it, where the parser keeps the mark: an underscore inside a word, in
        // Arabic, whose letters the page shapes before it reads the marks.
        Export arabic = export(page -> page.addParagraph(HEADLINE + " " + HEADLINE).addParagraph(p -> p.name("Kept")
                .text("مرحبا_بالعالم").textStyle(DocumentTextStyle.builder().fontName(FontName.AMIRI).size(10).build())
                .autoSize(20, 6)));
        double kept = arabic.firstSize("Kept");
        assertThat(kept).isNotEqualTo(10.0);
        assertThat(sizesOf(arabic.document(), "مرحبا_بالعالم")).containsOnly(halfPoints(kept));
        assertThat(arabic.notes()).isEmpty();
    }

    @Test
    void aRunWithAStyleOfItsOwnKeepsItsSizeAndOneWithNoneTakesTheFittedSize() throws Exception {
        Export export = export(page -> page.addParagraph(p -> p.name("Greeting").textStyle(TEN)
                .inlineText("Hi ", DocumentTextStyle.DEFAULT.withSize(12)).inlineText("there").autoSize(24)));
        assertThat(export.paragraphWith("there").getRuns()).filteredOn(run -> !run.text().isEmpty())
                .extracting(XWPFRun::text, XWPFRun::getFontSizeAsDouble)
                .containsExactly(tuple("Hi ", 12.0), tuple("there", 24.0));
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
    void markdownPiecesAreWrittenAtTheSizeThePageFitsThemTo() throws Exception {
        // Tracked in a share of the size, the pieces' tracking is the one the page resolves at the
        // fitted size.
        DocumentTextStyle tracked = DocumentTextStyle.builder().size(24)
                .letterSpacing(DocumentLetterSpacing.ofFontSize(0.05)).build();
        Export export = export(page -> page.addParagraph(p -> p.name("Revenue")
                .text("Revenue **grew** this quarter by a long way").textStyle(tracked).autoSize(24, 6)));
        double fitted = export.firstSize("Revenue");
        assertThat(fitted).isLessThan(24);
        assertThat(export.paragraphWith("grew").getRuns()).filteredOn(run -> !run.text().isEmpty())
                .extracting(XWPFRun::text, XWPFRun::isBold, XWPFRun::getFontSizeAsDouble)
                .containsExactly(tuple("Revenue ", false, wordsSize(fitted)), tuple("grew", true, wordsSize(fitted)),
                        tuple(" this quarter by a long way", false, wordsSize(fitted)));
        assertThat(export.notes()).isEmpty();

        // Composed in a table cell, matched to its lines as the page reads it.
        Export cell = export(page -> page.add(new TableBuilder().name("Sums").columns(DocumentTableColumn.fixed(120))
                .rowCells(DocumentTableCell.node(new ParagraphBuilder().name("Total").text("Total **due** now")
                        .textStyle(TEN).autoSize(20, 6).build()))
                .build()));
        double total = cell.sizeOf("due");
        assertThat(total).isNotEqualTo(10.0);
        assertThat(cell.document().getTables().get(0).getRow(0).getCell(0).getParagraphs().get(0).getRuns())
                .extracting(XWPFRun::text, XWPFRun::isBold, XWPFRun::getFontSizeAsDouble)
                .containsExactly(tuple("Total ", false, wordsSize(total)), tuple("due", true, wordsSize(total)),
                        tuple(" now", false, wordsSize(total)));
        assertThat(cell.notes()).isEmpty();

        // A heading at a multiple of a fitted size off Word's half point is named at the size the
        // file holds it at. Fitted on a grid of whole points from 24.5, the size is a half point,
        // and the second level's heading, half as large again, a quarter off one.
        Export heading = export(page -> page.addParagraph(p -> p.name("Second")
                .text("## A second level heading far too long_x").textStyle(DocumentTextStyle.DEFAULT.withSize(24))
                .autoSize(new com.demcha.compose.document.style.DocumentTextAutoSize(24.5, 4.5, 1.0))));
        double laid = heading.firstSize("Second");
        assertThat(laid * 2).as("off the half point").isNotEqualTo(Math.rint(laid * 2));
        assertThat(heading.paragraphWith("second").getRuns()).filteredOn(run -> run.text().contains("second"))
                .singleElement().satisfies(run -> assertThat(run.getFontSizeAsDouble()).isEqualTo(wordsSize(laid)));
        assertThat(heading.notes()).singleElement().asString()
                .contains("its markdown heading is written at " + points(wordsSize(laid)) + "pt in a line ");
    }

    private static String points(double size) {
        return java.math.BigDecimal.valueOf(size).stripTrailingZeros().toPlainString();
    }

    @Test
    void theOtherPathsThatWriteAParagraphWriteItAtTheSizeThePageFitsItTo() throws Exception {
        // The body is above; a page zone's line is DocxZoneLineTest's.
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
        assertThat(pair.notes()).isEmpty();

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
        assertThat(sizesOf(box.document(), "LM")).isNotEmpty().containsOnly(halfPoints(30));
        assertThat(box.notes()).as("the box's own note, and nothing of the size").isNotEmpty()
                .noneMatch(note -> note.contains("is written at"));

        // A badge's initials, in its shape.
        Export badge = export(page -> page.add(badge("JR")));
        double initials = badge.firstSize("Initials");
        assertThat(initials).isGreaterThan(8);
        assertThat(badge.xml()).contains("<wps:txbx>");
        assertThat(sizesOf(badge.document(), "JR")).isNotEmpty().containsOnly(halfPoints(initials));
        assertThat(badge.notes()).isEmpty();
        // Initials the page reads as markdown, in one face at the fitted size, stay in the shape.
        Export marked = export(page -> page.add(badge("*JR*")));
        assertThat(marked.xml()).contains("<wps:txbx>").doesNotContain("*JR*");
        assertThat(italicOf(marked.document(), "JR")).isNotEmpty().containsOnly(true);
        assertThat(sizesOf(marked.document(), "JR")).isNotEmpty().containsOnly(halfPoints(marked.firstSize("Initials")));
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
        assertThat(export.notes()).containsExactly("written as a paragraph; its text is written at 8pt — " + UNMEASURED);

        // Read as markdown in other letters than the pieces — Arabic, which the page shapes before
        // it reads the marks — its lines do not tell which size is its own: over two lines, a
        // heading's and the body's; over one, a heading's alone, twice the fitted size.
        DocumentTextStyle amiri = DocumentTextStyle.builder().fontName(FontName.AMIRI).size(10).build();
        for (String text : List.of("# مرحبا_\nسطر *ب*", "# مرحبا_")) {
            Export arabic = export(page -> page.addParagraph(HEADLINE + " " + HEADLINE)
                    .addParagraph(p -> p.text(text).textStyle(amiri).autoSize(20, 6)));
            assertThat(arabic.notes()).as(text).singleElement().asString()
                    .startsWith("written as a paragraph; its text is written at 10pt — " + UNMEASURED);
            assertThat(sizesOf(arabic.document(), text.replace("\n", ""))).as(text).containsOnly(halfPoints(10));
        }

        // One paragraph added at two places is fitted at each apart; its lines are one place's.
        ParagraphNode shared = new ParagraphBuilder().name("Shared").text("Shared heading text here").textStyle(TEN)
                .autoSize(30, 6).build();
        Export twice = export(page -> page.addParagraph(HEADLINE + " " + HEADLINE).add(shared)
                .add(new TableBuilder().name("Narrow").columns(DocumentTableColumn.fixed(60))
                        .rowCells(DocumentTableCell.node(shared)).build()));
        assertThat(sizesOf(twice.document(), "Shared heading text here")).hasSize(2).containsOnly(halfPoints(10));
        assertThat(twice.notes()).hasSize(2)
                .allSatisfy(note -> assertThat(note).endsWith("its text is written at 10pt — " + UNMEASURED));

        // With no layout, nothing tells it.
        Consumer<PageFlowBuilder> unlaid = page -> page.addParagraph(p -> p.text(HEADLINE + " " + HEADLINE).textStyle(TEN))
                .addParagraph(p -> p.text("Hi").textStyle(DocumentTextStyle.DEFAULT.withSize(24)).autoSize(24, 6));
        assertThat(sizesOf(DocxExports.withoutLayout(240, 600, 30, unlaid), "Hi")).containsExactly(halfPoints(24));
        assertThat(DocxExports.reportWithoutLayout(240, 600, 30, unlaid).bySubject().get("ParagraphNode"))
                .extracting(DocxExportReport.Note::detail)
                .containsExactly("written as a paragraph; its text is written at 24pt — " + UNMEASURED);
    }

    private static double wordsSize(double size) {
        return Math.round(size * 2) / 2.0;
    }

    private static double markSize(XWPFParagraph paragraph) {
        return ((Number) paragraph.getCTP().getPPr().getRPr().getSzArray(0).getVal()).doubleValue() / 2;
    }

    /** The runs reading a text, wherever in the body they stand: a text box's and a shape's too. */
    private static List<XmlObject> runsReading(XWPFDocument document, String text) {
        List<XmlObject> runs = new ArrayList<>();
        for (XmlObject run : document.getDocument().getBody().selectPath(W + ".//w:r")) {
            StringBuilder letters = new StringBuilder();
            for (XmlObject letter : run.selectPath(W + "./w:t")) {
                letters.append(((SimpleValue) letter).getStringValue());
            }
            if (letters.toString().equals(text)) {
                runs.add(run);
            }
        }
        return runs;
    }

    /**
     * The sizes the runs reading a text are written at, wherever in the body they stand, as the
     * file states them: in Word's half points ({@link #halfPoints}).
     */
    private static List<String> sizesOf(XWPFDocument document, String text) {
        List<String> sizes = new ArrayList<>();
        for (XmlObject run : runsReading(document, text)) {
            for (XmlObject size : run.selectPath(W + "./w:rPr/w:sz/@w:val")) {
                sizes.add(((SimpleValue) size).getStringValue());
            }
        }
        return sizes;
    }

    /** A size in points as Word states it, in half points to the nearest. */
    private static String halfPoints(double size) {
        return Long.toString(Math.round(size * 2));
    }

    /** Whether each run reading a text, wherever in the body it stands, is written italic. */
    private static List<Boolean> italicOf(XWPFDocument document, String text) {
        return runsReading(document, text).stream().map(run -> run.selectPath(W + "./w:rPr/w:i").length > 0).toList();
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
        return export(true, content);
    }

    private static Export export(boolean markdown, Consumer<PageFlowBuilder> content) throws Exception {
        AtomicReference<DocxExportReport> report = new AtomicReference<>();
        byte[] docx;
        Map<String, List<PlacedFragment>> fragments;
        try (DocumentSession session = GraphCompose.document().pageSize(240, 600).margin(DocumentInsets.of(30))
                .markdown(markdown).create()) {
            session.pageFlow(content::accept);
            fragments = session.layoutGraph().fragments().stream()
                    .collect(java.util.stream.Collectors.groupingBy(PlacedFragment::path));
            docx = session.export(new DocxSemanticBackend(report::set));
        }
        return new Export(new XWPFDocument(new ByteArrayInputStream(docx)), report.get(), fragments);
    }
}
