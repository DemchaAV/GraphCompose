package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.dsl.ShapeBuilder;
import com.demcha.compose.document.dsl.ShapeContainerBuilder;
import com.demcha.compose.document.layout.PlacedFragment;
import com.demcha.compose.document.layout.payloads.ParagraphFragmentPayload;
import com.demcha.compose.document.layout.payloads.ParagraphTextSpan;
import com.demcha.compose.document.node.DocumentBookmarkOptions;
import com.demcha.compose.document.node.DocumentLinkOptions;
import com.demcha.compose.document.node.LayerAlign;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.node.TextDirection;
import com.demcha.compose.document.output.DocumentPageZone;
import com.demcha.compose.document.style.ClipPolicy;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.table.DocumentTableCell;
import com.demcha.compose.document.table.DocumentTableColumn;
import com.demcha.compose.engine.components.content.text.TextDecoration;
import com.demcha.compose.font.FontName;
import org.apache.poi.openxml4j.opc.PackagePart;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A paragraph the page reads as markdown is written as the page sets it: its marks dropped, the
 * text they mark in the face — and a heading at the size — the page sets it in, and nothing named
 * but a heading the page draws past its line. The page's own lines say whether it reads the
 * paragraph so: where they hold the marks, as a session that reads no markdown lays them out, it
 * is written as it stands; where they hold other letters, it is written as authored and named.
 */
class DocxSessionMarkdownTest {

    private static final String MARKS = "its markdown marks are written as letters, where the page sets the text "
                                        + "they mark and drops them";
    private static final String HEADING_CUT = "pt tall, as tall as the paragraph's own line on the page: the page draws "
                                              + "its letters past the line, and Word cuts their tops on screen";
    private static final String NOTHING = "its text is written as authored, where the page reads it as markdown and "
                                          + "sets nothing";
    private static final String MARKS_UNMEASURED = "markdown marks are written as letters — whether the page reads them "
                                                   + "is not measured";

    @Test
    void aParagraphIsWrittenInThePiecesThePageSetsIt() throws Exception {
        Export export = export(true, page -> page.addParagraph(p -> p.name("Intro").text("Some **bold** and `code` text")));
        XWPFParagraph paragraph = export.paragraphWith("bold");
        assertThat(paragraph.getText()).isEqualTo("Some bold and code text");
        assertThat(paragraph.getRuns()).extracting(XWPFRun::text).containsExactly("Some ", "bold", " and code text");
        assertThat(paragraph.getRuns()).extracting(XWPFRun::isBold).containsExactly(false, true, false);
        assertThat(export.notes("ParagraphNode")).isEmpty();
    }

    @Test
    void aHeadingLineIsWrittenBoldAtTheSizeThePageSetsItAndEachLineBreaks() throws Exception {
        Export export = export(true, page -> page.addParagraph(p -> p.text("# Title\nnext *word*\n- dash")
                .textStyle(DocumentTextStyle.builder().size(10).build())));
        XWPFParagraph paragraph = export.paragraphWith("Title");
        List<XWPFRun> runs = paragraph.getRuns();
        assertThat(runs.get(0).text()).isEqualTo("Title");
        assertThat(runs.get(0).isBold()).isTrue();
        assertThat(runs.get(0).getFontSizeAsDouble()).isEqualTo(20.0);
        assertThat(runs).filteredOn(run -> "word".equals(run.text())).singleElement()
                .satisfies(run -> assertThat(run.isItalic()).isTrue());
        assertThat(paragraph.getCTP().xmlText()).as("a line break where the page starts a line").contains("<w:br/>");
        assertThat(paragraph.getText()).contains("- dash").doesNotContain("#").doesNotContain("*");
        // The page sets the heading in a line as tall as the paragraph's own, and draws it past.
        assertThat(export.notes("ParagraphNode")).singleElement().asString()
                .startsWith("written as a paragraph; its markdown heading is written at 20pt in a line ")
                .endsWith(HEADING_CUT);

        // The paragraph's mark closes its last line, sized as the piece that ends it.
        XWPFParagraph heading = export(true, page -> page.addParagraph(p -> p.text("# Title *x*")
                .textStyle(DocumentTextStyle.builder().size(10).build()))).paragraphWith("Title");
        assertThat(heading.getCTP().getPPr().getRPr().getSzArray(0).getVal()).hasToString("40");
    }

    @Test
    void aMarkOfDirectionIsWrittenAsThePageKeepsIt() throws Exception {
        Export export = export(true, page -> page.addParagraph("Price‎ **now**"));
        XWPFParagraph paragraph = export.paragraphWith("now");
        assertThat(paragraph.getRuns()).extracting(XWPFRun::isBold).containsExactly(false, true);
        assertThat(paragraph.getText()).isEqualTo("Price‎ now");
        assertThat(export.notes("ParagraphNode")).isEmpty();
    }

    @Test
    void aRightToLeftParagraphIsWrittenAsThePageSetsIt() throws Exception {
        Export export = export(true, page -> page.addParagraph(p -> p.text("שלום **עולם**").direction(TextDirection.RTL)));
        XWPFParagraph paragraph = export.paragraphWith("עולם");
        assertThat(paragraph.getRuns()).extracting(XWPFRun::text).containsExactly("שלום ", "עולם");
        assertThat(paragraph.getRuns()).extracting(XWPFRun::isBold).containsExactly(false, true);
        assertThat(paragraph.getRuns()).allSatisfy(run -> assertThat(run.getCTR().getRPr().xmlText()).contains("<w:rtl"));
        assertThat(export.notes("ParagraphNode")).isEmpty();
    }

    @Test
    void aParagraphThePageBreaksAcrossPagesIsWrittenAsThePageSetsIt() throws Exception {
        String sentence = "A line of **body** text that runs on and on. ";
        Export export = export(true, page -> page.addParagraph(p -> p.name("Long").text(sentence.repeat(40))));
        assertThat(export.fragmentsOf("Long")).as("the page breaks it across pages").isGreaterThan(1);
        XWPFParagraph paragraph = export.paragraphWith("runs on");
        assertThat(paragraph.getText()).doesNotContain("*");
        assertThat(paragraph.getRuns()).filteredOn(XWPFRun::isBold).hasSize(40);
        assertThat(export.notes("ParagraphNode")).isEmpty();
    }

    @Test
    void textThePageReadsIntoNothingIsWrittenAsItStandsAndNamed() throws Exception {
        // The page reads `***` as a rule, a lone `*` as an empty list item and a line set four
        // spaces in as a block of code it keeps no text of, and sets nothing.
        for (String text : List.of("***", "*", "    code *x*")) {
            Export export = export(true, page -> page.addParagraph("Above").addParagraph(text).addParagraph("Below"));
            assertThat(export.document().getDocument().xmlText()).contains(">" + text + "<");
            assertThat(export.notes("ParagraphNode")).as(text).containsExactly("written as a paragraph; " + NOTHING);
        }
        Export off = export(false, page -> page.addParagraph("***"));
        assertThat(off.document().getDocument().xmlText()).as("markdown off, the page sets the marks").contains(">***<");
        assertThat(off.notes("ParagraphNode")).isEmpty();
    }

    @Test
    void aSessionThatReadsNoMarkdownHasItsMarksWrittenAsTheyStand() throws Exception {
        Export export = export(false, page -> page.addParagraph("Some **bold** text"));
        XWPFParagraph paragraph = export.paragraphWith("bold");
        assertThat(paragraph.getText()).isEqualTo("Some **bold** text");
        assertThat(paragraph.getRuns()).singleElement().satisfies(run -> assertThat(run.isBold()).isFalse());
        assertThat(export.notes("ParagraphNode")).isEmpty();
        // Text the parser keeps whole, its white space and a tab among it, as it stands in either session.
        for (boolean markdown : List.of(false, true)) {
            assertThat(export(markdown, page -> page.addParagraph("a_b\tc")).paragraphWith("a_b").getText())
                    .as("markdown " + markdown).isEqualTo("a_b\tc");
        }
    }

    @Test
    void textTheParserReadsAsThePageDoesIsWrittenSoWhateverItHolds() throws Exception {
        // Each of these the page reads through its parser; were the pieces read here otherwise
        // than the page reads them, the paragraph would be written as authored and named.
        DocumentTextStyle tracked = DocumentTextStyle.builder().size(10)
                .letterSpacing(com.demcha.compose.document.style.DocumentLetterSpacing.ofFontSize(0.05)).build();
        for (String text : List.of("1. a *b*", "> *q* r", "\\*x\\* *y*", "***nested** emph*", "`a_b` *c*",
                "[*x*](https://example.org) y", "a &amp; *b*", "x  *y*  z", "## Sub *x*\nbody *y*")) {
            Export export = export(true, page -> page.addParagraph(p -> p.text(text).textStyle(tracked)));
            assertThat(export.notes("ParagraphNode")).as(text)
                    .noneMatch(note -> note.contains("markdown marks are written as letters"));
        }
    }

    @Test
    void anAutoSizedParagraphIsWrittenInThePiecesAtTheSizeThePageFitsItTo() throws Exception {
        Export export = export(true, page -> page.addParagraph(p -> p.name("Fit").text("Fit **this** text")
                .textStyle(DocumentTextStyle.builder().size(10).build()).autoSize(20, 6)));
        XWPFParagraph paragraph = export.paragraphWith("this");
        assertThat(paragraph.getRuns()).extracting(XWPFRun::text, XWPFRun::isBold, XWPFRun::getFontSizeAsDouble)
                .containsExactly(org.assertj.core.groups.Tuple.tuple("Fit ", false, 20.0),
                        org.assertj.core.groups.Tuple.tuple("this", true, 20.0),
                        org.assertj.core.groups.Tuple.tuple(" text", false, 20.0));
        assertThat(export.notes("ParagraphNode")).isEmpty();
        // A heading at its multiple of the size the page fits the text to, 30pt, as the page sets
        // it: drawn past the paragraph's line, which Word cuts it in, and named.
        Export heading = export(true, page -> page.addParagraph(p -> p.text("# Big_x")
                .textStyle(DocumentTextStyle.builder().size(10).build()).autoSize(30, 6)));
        XWPFParagraph big = heading.paragraphWith("Big");
        assertThat(big.getText()).isEqualTo("Big_x");
        assertThat(big.getRuns()).singleElement().satisfies(run -> assertThat(run.getFontSizeAsDouble()).isEqualTo(60.0));
        assertThat(heading.notes("ParagraphNode")).singleElement().asString()
                .startsWith("written as a paragraph; its markdown heading is written at 60pt in a line ")
                .endsWith(HEADING_CUT);
        // After a prefix the page sets before its first line, the share is read past the prefix.
        Export prefixed = export(true, page -> page.addParagraph(p -> p.text("# Big_x").bulletOffset("• ")
                .indentStrategy(com.demcha.compose.document.style.DocumentTextIndent.FIRST_LINE)
                .textStyle(DocumentTextStyle.builder().size(10).build()).autoSize(30, 6)));
        assertThat(prefixed.paragraphWith("Big").getRuns()).filteredOn(run -> run.text().contains("Big"))
                .singleElement().satisfies(run -> assertThat(run.getFontSizeAsDouble()).isEqualTo(60.0));
        // Fitted far below its style's 30pt, a heading at twice the fitted size is still smaller
        // than the style's; it stands taller than the line the page fits the text to, and is named.
        Export small = export(true, page -> page.addParagraph(p -> p.text("# A heading far too long_for one line")
                .textStyle(DocumentTextStyle.builder().size(30).build()).autoSize(30, 6)));
        assertThat(small.paragraphWith("heading").getRuns()).filteredOn(run -> run.text().contains("heading"))
                .singleElement().satisfies(run -> assertThat(run.getFontSizeAsDouble()).isLessThan(30.0));
        assertThat(small.notes("ParagraphNode")).singleElement().asString().endsWith(HEADING_CUT);
        // Over two lines it is fitted to its least size, its heading at twice that and its body at it.
        Export two = export(true, page -> page.addParagraph(p -> p.text("# Head_x\nbody *y*")
                .textStyle(DocumentTextStyle.builder().size(10).build()).autoSize(20, 6)));
        assertThat(two.paragraphWith("Head").getRuns()).filteredOn(run -> run.text().contains("_") || run.text().equals("y"))
                .extracting(XWPFRun::text, XWPFRun::getFontSizeAsDouble)
                .containsExactly(org.assertj.core.groups.Tuple.tuple("Head_x", 12.0),
                        org.assertj.core.groups.Tuple.tuple("y", 6.0));
    }

    @Test
    void thePiecesStandInTheFacesThePageSetsThemIn() throws Exception {
        // The page reads the text of a bold paragraph holding a mark into faces of the parser's
        // own, the paragraph's left aside: it sets Senior_Engineer regular, and the file follows.
        // Should the page come to keep the paragraph's face, this fails, and the docs that say so
        // are to change with it.
        DocumentTextStyle bold = DocumentTextStyle.builder().decoration(DocumentTextDecoration.BOLD).build();
        Export export = export(true, page -> page.addParagraph(p -> p.name("Role").text("Senior_Engineer").textStyle(bold)));
        assertThat(export.firstSpanFace("Role")).isEqualTo(TextDecoration.DEFAULT);
        assertThat(export.paragraphWith("Senior_Engineer").getRuns()).singleElement()
                .satisfies(run -> assertThat(run.isBold()).isFalse());
        assertThat(export.notes("ParagraphNode")).isEmpty();
    }

    @Test
    void aLinkedParagraphsPiecesShareOneLink() throws Exception {
        Export export = export(true, page -> page.addParagraph(p -> p.text("**Java** docs")
                .link(new DocumentLinkOptions("https://example.org"))));
        XWPFParagraph paragraph = export.paragraphWith("docs");
        assertThat(paragraph.getCTP().sizeOfHyperlinkArray()).isEqualTo(1);
        assertThat(paragraph.getCTP().getHyperlinkArray(0).sizeOfRArray()).isEqualTo(2);
        assertThat(paragraph.getText()).isEqualTo("Java docs");
    }

    @Test
    void anOutlineEntryListsTheTextAsWritten() throws Exception {
        Export export = export(true, page -> page.addParagraph(p -> p.text("**Results**")
                .bookmark(new DocumentBookmarkOptions("Results", 1))));
        assertThat(export.paragraphWith("Results").getText()).isEqualTo("Results");
        assertThat(export.notes("ParagraphNode")).isEmpty();
    }

    @Test
    void aZoneParagraphIsWrittenAsThePageSetsIt() throws Exception {
        AtomicReference<DocxExportReport> report = new AtomicReference<>();
        String footer = zoneFooter("**Confidential**", report);
        assertThat(footer).contains(">Confidential<").doesNotContain("**").contains("<w:b w:val=\"on\"/>");
        assertThat(report.get().bySubject().getOrDefault("page zone", List.of())).isEmpty();
        // A heading in a zone stands taller than the zone's line, as in the body.
        zoneFooter("# Confidential *now*", report);
        assertThat(report.get().bySubject().get("page zone")).extracting(DocxExportReport.Note::detail)
                .singleElement().asString()
                .startsWith("a footer written as one line of Word's footer; a paragraph's markdown heading is written "
                            + "at 16pt in a line ")
                .endsWith(HEADING_CUT);
    }

    private static String zoneFooter(String text, AtomicReference<DocxExportReport> report) throws Exception {
        byte[] docx;
        try (DocumentSession session = GraphCompose.document().pageSize(300, 400).margin(DocumentInsets.of(36)).create()) {
            session.chrome().zone(DocumentPageZone.footer(30, page -> new ParagraphBuilder().name("ZoneLine")
                    .text(text).textStyle(DocumentTextStyle.DEFAULT.withSize(8)).build()));
            session.pageFlow(page -> page.addParagraph("Body"));
            docx = session.export(new DocxSemanticBackend(report::set));
        }
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(docx))) {
            return partXml(document, "/word/footer");
        }
    }

    @Test
    void aParagraphComposedInATableCellIsWrittenAsThePageSetsIt() throws Exception {
        Export export = export(true, page -> page.add(new com.demcha.compose.document.dsl.TableBuilder().name("Rota")
                .columns(DocumentTableColumn.fixed(200))
                .rowCells(DocumentTableCell.node(new ParagraphBuilder().name("Note").text("Some **bold** text").build()))
                .build()));
        XWPFRun bold = export.document().getTables().get(0).getRow(0).getCell(0).getParagraphs().stream()
                .flatMap(paragraph -> paragraph.getRuns().stream()).filter(run -> "bold".equals(run.text()))
                .findFirst().orElseThrow();
        assertThat(bold.isBold()).isTrue();
        assertThat(export.document().getDocument().xmlText()).doesNotContain("**bold**");
        assertThat(export.notes("ParagraphNode")).isEmpty();
    }

    @Test
    void aCellsParagraphKeepsItsOwnLinesBesideOneReadAsMarkdown() throws Exception {
        // Laid out with its prefix, the first cell's line reads "• Total", its text neither as
        // authored nor as the page reads it; the cell below keeps its own line, "Total", rather than
        // lend it to the first, and is written at the page's line height.
        Export export = export(true, page -> page.add(new com.demcha.compose.document.dsl.TableBuilder().name("Sums")
                .columns(DocumentTableColumn.fixed(200))
                .rowCells(DocumentTableCell.node(new ParagraphBuilder().name("First").text("**Total**").bulletOffset("•")
                        .indentStrategy(com.demcha.compose.document.style.DocumentTextIndent.FIRST_LINE).build()))
                .rowCells(DocumentTableCell.node(new ParagraphBuilder().name("Second").text("Total").build()))
                .build()));
        XWPFParagraph second = export.document().getTables().get(0).getRow(1).getCell(0).getParagraphs().get(0);
        assertThat(second.getCTP().getPPr().getSpacing().getLineRule())
                .isEqualTo(org.openxmlformats.schemas.wordprocessingml.x2006.main.STLineSpacingRule.EXACT);
    }

    @Test
    void aCellReadAsMarkdownTakesNoLineThatSetsItsPiecesOtherwise() throws Exception {
        // A paragraph of the same text as authored after it takes its line first; the one left is
        // in a smaller, regular face, and held to it, its bold letters would be cut. It takes none:
        // Word's own line, its marks named as not measured.
        Export export = export(true, page -> page.add(new com.demcha.compose.document.dsl.TableBuilder().name("Sums")
                .columns(DocumentTableColumn.fixed(110), DocumentTableColumn.fixed(110))
                .rowCells(DocumentTableCell.node(new ParagraphBuilder().name("Bold").text("**Total**").build()),
                        DocumentTableCell.node(new ParagraphBuilder().name("Small").text("Total")
                                .textStyle(DocumentTextStyle.DEFAULT.withSize(6)).build()))
                .build()));
        XWPFParagraph bold = export.document().getTables().get(0).getRow(0).getCell(0).getParagraphs().get(0);
        org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPPr properties = bold.getCTP().getPPr();
        assertThat(properties != null && properties.isSetSpacing() && properties.getSpacing().isSetLineRule()
                   && properties.getSpacing().getLineRule()
                      == org.openxmlformats.schemas.wordprocessingml.x2006.main.STLineSpacingRule.EXACT)
                .as("held to no line of another's").isFalse();
        assertThat(export.notes("ParagraphNode")).containsExactly("written as a paragraph; its " + MARKS_UNMEASURED);
    }

    @Test
    void aBadgesInitialsAreWrittenAsThePageSetsThem() throws Exception {
        Export export = export(true, page -> page.add(badge("*JR*")));
        String body = export.document().getDocument().xmlText();
        assertThat(body).contains("<wps:txbx>").contains(">JR<").doesNotContain("*JR*").contains("<w:i w:val=\"on\"/>");
        assertThat(export.notes("ParagraphNode")).isEmpty();
        // Four letters and more with their marks, two as the page sets them: a badge's initials.
        assertThat(export(true, page -> page.add(badge("**JR**"))).document().getDocument().xmlText())
                .contains("<wps:txbx>").contains(">JR<").contains("<w:b w:val=\"on\"/>");
        // Initials in two faces are written in the flow, as initials of two runs' faces are.
        Export twoFaces = export(true, page -> page.add(badge("*J*R")));
        assertThat(twoFaces.document().getDocument().xmlText()).doesNotContain("<wps:txbx>");
        assertThat(twoFaces.paragraphWith("JR").getRuns()).filteredOn(run -> !run.text().isEmpty())
                .extracting(XWPFRun::text, XWPFRun::isItalic)
                .containsExactly(org.assertj.core.groups.Tuple.tuple("J", true), org.assertj.core.groups.Tuple.tuple("R", false));
        // A heading the page draws past its line is written in the flow, where its line is the
        // page's and the cut is named; in the shape Word would grow the line instead.
        Export heading = export(true, page -> page.add(badge("# *J*")));
        assertThat(heading.document().getDocument().xmlText()).doesNotContain("<wps:txbx>");
        assertThat(heading.notes("ParagraphNode")).singleElement().asString().contains("markdown heading").endsWith(HEADING_CUT);
    }

    @Test
    void aLinePairsSidesAreWrittenAsThePageSetsThem() throws Exception {
        Export export = export(true, page -> page.add(new ShapeContainerBuilder().name("EntryHead").rectangle(240, 20)
                .clipPolicy(ClipPolicy.OVERFLOW_VISIBLE)
                .position(new ParagraphBuilder().name("Title").text("**Lead** Engineer")
                        .bookmark(new DocumentBookmarkOptions("Lead Engineer 2022", 1)).build(), 0, 0, LayerAlign.CENTER_LEFT)
                .position(new ParagraphBuilder().name("Dates").text("*2022*").align(TextAlign.RIGHT).build(),
                        0, 0, LayerAlign.CENTER_RIGHT)
                .build()));
        XWPFParagraph line = export.paragraphWith("Engineer");
        assertThat(line.getText()).isEqualTo("Lead Engineer\t2022");
        assertThat(line.getRuns()).filteredOn(run -> "Lead".equals(run.text())).singleElement()
                .satisfies(run -> assertThat(run.isBold()).isTrue());
        assertThat(line.getRuns()).filteredOn(run -> "2022".equals(run.text())).singleElement()
                .satisfies(run -> assertThat(run.isItalic()).isTrue());
        // Word's outline lists the line's text as written.
        assertThat(export.notes("ParagraphNode")).isEmpty();
    }

    @Test
    void textOverTheFlowIsWrittenAsThePageSetsIt() throws Exception {
        Export export = export(true, page -> page.add(new ShapeContainerBuilder().name("Sidebar")
                .rectangle(100, 120).clipPolicy(ClipPolicy.OVERFLOW_VISIBLE)
                .margin(new DocumentInsets(-30, 0, -90, -30))
                .position(new ShapeBuilder().name("Block").size(100, 120)
                        .fillColor(DocumentColor.rgb(160, 80, 50)).build(), 0, 0, LayerAlign.TOP_LEFT, 0)
                .position(new ParagraphBuilder().name("Monogram").text("**LM**").build(), 10, 10, LayerAlign.TOP_LEFT, 1)
                .build()).addParagraph("Masthead"));
        String body = export.document().getDocument().xmlText();
        assertThat(body).contains("<wps:txbx>").doesNotContain("**LM**")
                .matches("(?s).*<w:b w:val=\"on\"/>(?:(?!</w:r>).)*<w:t>LM</w:t>.*");
        assertThat(export.notes("ParagraphNode")).isNotEmpty()
                .allSatisfy(note -> assertThat(note).doesNotContain("markdown"));
    }

    private static com.demcha.compose.document.node.DocumentNode badge(String initials) {
        return new ShapeContainerBuilder().name("Badge").circle(40).fillColor(DocumentColor.rgb(30, 50, 90))
                .center(new ParagraphBuilder().text(initials).build()).build();
    }

    @Test
    void theFacesThePiecesAreSetInTravelWithTheDocument() throws Exception {
        Export export = export(true, page -> page.addParagraph(p -> p.text("Set **in** Lato *here*")
                .textStyle(DocumentTextStyle.builder().fontName(FontName.LATO).build())));
        assertThat(export.paragraphWith("Lato").getText()).as("written in its pieces").isEqualTo("Set in Lato here");
        String table = partXml(export.document(), "/word/fontTable.xml");
        assertThat(table).contains("<w:embedRegular").contains("<w:embedBold").contains("<w:embedItalic")
                .doesNotContain("<w:embedBoldItalic");
    }

    @Test
    void textThePageSetsInOtherLettersThanThePiecesIsWrittenAsAuthoredAndNamed() throws Exception {
        // The page shapes Arabic letters into the forms it draws before it reads the marks: its
        // lines hold other letters than the text, and nothing tells which piece is which.
        Export export = export(true, page -> page.addParagraph(p -> p.text("مرحبا **بالعالم**")
                .textStyle(DocumentTextStyle.builder().fontName(FontName.AMIRI).build())));
        assertThat(export.document().getDocument().xmlText()).contains("**");
        assertThat(export.notes("ParagraphNode")).containsExactly("written as a paragraph; " + MARKS);
    }

    private record Export(XWPFDocument document, DocxExportReport report,
                          Map<String, List<PlacedFragment>> fragments) {

        XWPFParagraph paragraphWith(String text) {
            return document.getParagraphs().stream().filter(paragraph -> paragraph.getText().contains(text))
                    .findFirst().orElseThrow(() -> new AssertionError("no paragraph holds " + text));
        }

        long fragmentsOf(String name) {
            return fragments.entrySet().stream().filter(entry -> entry.getKey().contains(name))
                    .mapToLong(entry -> entry.getValue().size()).sum();
        }

        List<String> notes(String subject) {
            return report.bySubject().getOrDefault(subject, List.of()).stream().map(DocxExportReport.Note::detail).toList();
        }

        TextDecoration firstSpanFace(String name) {
            return fragments.entrySet().stream().filter(entry -> entry.getKey().contains(name))
                    .flatMap(entry -> entry.getValue().stream())
                    .map(PlacedFragment::payload).filter(ParagraphFragmentPayload.class::isInstance)
                    .map(ParagraphFragmentPayload.class::cast)
                    .flatMap(payload -> payload.lines().stream()).flatMap(line -> line.spans().stream())
                    .filter(ParagraphTextSpan.class::isInstance).map(ParagraphTextSpan.class::cast)
                    .findFirst().orElseThrow().textStyle().decoration();
        }
    }

    private static Export export(boolean markdown, Consumer<PageFlowBuilder> content) throws Exception {
        AtomicReference<DocxExportReport> report = new AtomicReference<>();
        byte[] docx;
        Map<String, List<PlacedFragment>> fragments;
        try (DocumentSession session = GraphCompose.document().pageSize(300, 400).margin(DocumentInsets.of(30))
                .markdown(markdown).create()) {
            session.pageFlow(content::accept);
            fragments = session.layoutGraph().fragments().stream()
                    .collect(java.util.stream.Collectors.groupingBy(PlacedFragment::path));
            docx = session.export(new DocxSemanticBackend(report::set));
        }
        return new Export(new XWPFDocument(new ByteArrayInputStream(docx)), report.get(), fragments);
    }

    private static String partXml(XWPFDocument document, String prefix) throws Exception {
        StringBuilder xml = new StringBuilder();
        for (PackagePart part : document.getPackage().getParts()) {
            if (part.getPartName().getName().startsWith(prefix)) {
                try (InputStream in = part.getInputStream()) {
                    xml.append(new String(in.readAllBytes(), StandardCharsets.UTF_8));
                }
            }
        }
        assertThat(xml).as("the document holds " + prefix).isNotEmpty();
        return xml.toString();
    }
}
