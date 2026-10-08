package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.backend.semantic.SemanticBackend;
import com.demcha.compose.document.backend.semantic.SemanticExportContext;
import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.dsl.RowBuilder;
import com.demcha.compose.document.layout.DocumentGraph;
import com.demcha.compose.document.node.DocumentBookmarkOptions;
import com.demcha.compose.document.node.PageFieldKind;
import com.demcha.compose.document.node.PageFieldNode;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.node.TextDirection;
import com.demcha.compose.document.output.DocumentHeaderFooterZone;
import com.demcha.compose.document.output.DocumentPageZone;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextIndent;
import com.demcha.compose.document.style.DocumentTextStyle;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * What a page zone loses as the one line of a Word header or footer it is written as is in the
 * report: where its parts stand, and what a paragraph among them loses of its own.
 *
 * <p>Word sets the line's parts one after another from the page's left margin, and those after
 * the first spacer against its right margin. The page sets each where the zone's padding, a row's
 * columns and gap and the part's own alignment and sides put it. A zone whose parts stand where
 * Word sets them, and whose paragraphs lose nothing of their own, is not named.</p>
 */
class DocxZoneReportTest {

    private static final DocumentTextStyle CHROME = DocumentTextStyle.DEFAULT.withSize(8);
    private static final String FOOTER = "a footer written as one line of Word's footer; ";
    private static final String OFF = "its text stands off where the page sets it";

    @Test
    void aLineSetFromTheLeftMarginIsNotNamed() throws Exception {
        assertThat(zoneNotes(DocumentPageZone.footer(30, page -> text("Confidential").build()))).isEmpty();
        // Its parts after a spacer end at the right margin, where Word's right tab holds them.
        assertThat(zoneNotes(DocumentPageZone.footer(30, page -> new RowBuilder().name("Line")
                .addParagraph(p -> p.text("Confidential").textStyle(CHROME))
                .flexSpacer()
                .add(page.pageNumber(CHROME))
                .build()))).isEmpty();
    }

    @Test
    void aPartThePageSetsElsewhereThanWordIsNamed() throws Exception {
        assertThat(zoneNotes(DocumentPageZone.footer(30, page -> text("Confidential").align(TextAlign.CENTER).build())))
                .containsExactly(FOOTER + OFF);
        assertThat(zoneNotes(DocumentPageZone.footer(30, page -> text("Confidential").build()).toBuilder()
                .padding(new DocumentInsets(0, 0, 0, 20)).build()))
                .as("the zone's own padding").containsExactly(FOOTER + OFF);
        assertThat(zoneNotes(DocumentPageZone.header(30, page -> text("Confidential").margin(new DocumentInsets(0, 0, 0, 12))
                .build())))
                .as("a paragraph's own side, in a header").containsExactly("a header written as one line of Word's header; " + OFF);
        assertThat(zoneNotes(DocumentPageZone.footer(30, page -> page.pageNumber(CHROME))))
                .as("a page number set from the left").isEmpty();
        // The page sets a field in a box a point wider than its number, which its alignment moves
        // it within; its own side moves it as a paragraph's does.
        assertThat(zoneNotes(DocumentPageZone.footer(30, page -> new PageFieldNode("Number", PageFieldKind.NUMBER,
                CHROME, TextAlign.RIGHT, DocumentInsets.zero(), DocumentInsets.zero())))).as("aligned right").isEmpty();
        assertThat(zoneNotes(DocumentPageZone.footer(30, page -> new PageFieldNode("Number", PageFieldKind.NUMBER,
                CHROME, TextAlign.LEFT, DocumentInsets.zero(), new DocumentInsets(0, 0, 0, 12)))))
                .as("set in by its margin").containsExactly(FOOTER + OFF);
        // A row of a spacer and a page number sets the number against the right margin, as Word does.
        assertThat(zoneNotes(DocumentPageZone.footer(30, page -> new RowBuilder().name("Line")
                .flexSpacer()
                .add(page.pageNumber(CHROME))
                .build()))).isEmpty();
    }

    @Test
    void theLineTheReportReadsIsTheOneWritten() throws Exception {
        // One right tab, at the right margin, which the report's line holds its right side at.
        try (DocumentSession session = session(DocumentPageZone.footer(30, page -> new RowBuilder().name("Line")
                .addParagraph(p -> p.text("Confidential").textStyle(CHROME))
                .flexSpacer()
                .add(page.pageNumber(CHROME))
                .build()))) {
            try (org.apache.poi.xwpf.usermodel.XWPFDocument document = new org.apache.poi.xwpf.usermodel.XWPFDocument(
                    new java.io.ByteArrayInputStream(session.export(new DocxSemanticBackend())))) {
                var tabs = document.getFooterArray(0).getParagraphs().get(0).getCTP().getPPr().getTabs();
                assertThat(tabs.sizeOfTabArray()).isEqualTo(1);
                assertThat(tabs.getTabArray(0).getVal().toString()).isEqualTo("right");
                assertThat(DocxTwips.of(tabs.getTabArray(0).getPos())).isEqualTo(Math.round((300 - 2 * 36) * 20.0));
            }
        }
    }

    @Test
    void aRowsColumnsAndGapAreCountedWherePartsStandOffWordsLine() throws Exception {
        // Without a spacer, Word sets the second right after the first; the page sets it at its column.
        assertThat(zoneNotes(DocumentPageZone.footer(30, page -> new RowBuilder().name("Line")
                .addParagraph(p -> p.text("Confidential").textStyle(CHROME))
                .addParagraph(p -> p.text("Acme").textStyle(CHROME))
                .build())))
                .containsExactly(FOOTER + "1 of its 2 parts stands off where the page sets them");
        // After a spacer, the page keeps its gap between the parts; Word sets them against each other.
        assertThat(zoneNotes(DocumentPageZone.footer(30, page -> new RowBuilder().name("Line").spacing(8)
                .addParagraph(p -> p.text("Confidential").textStyle(CHROME))
                .flexSpacer()
                .addParagraph(p -> p.text("v2.4").textStyle(CHROME))
                .add(page.pageNumber(CHROME))
                .build())))
                .containsExactly(FOOTER + "1 of its 3 parts stands off where the page sets them");
        // A part after a second spacer goes to no tab the line holds.
        assertThat(zoneNotes(DocumentPageZone.footer(30, page -> new RowBuilder().name("Line")
                .addParagraph(p -> p.text("Confidential").textStyle(CHROME))
                .flexSpacer()
                .addParagraph(p -> p.text("v2.4").textStyle(CHROME))
                .flexSpacer()
                .add(page.pageNumber(CHROME))
                .build())))
                .containsExactly(FOOTER + "2 of its 3 parts stand off where the page sets them");
    }

    @Test
    void partsSetOneAfterAnotherStandWhereWordSetsThem() throws Exception {
        // Packed at either end, as Word sets them from the left margin and against the right.
        assertThat(zoneNotes(DocumentPageZone.footer(30, page -> new RowBuilder().name("Line")
                .addParagraph(p -> p.text("Confidential").textStyle(CHROME))
                .addParagraph(p -> p.text("Acme").textStyle(CHROME))
                .flexSpacer()
                .addParagraph(p -> p.text("v2.4").textStyle(CHROME))
                .add(page.pageNumber(CHROME))
                .build()))).isEmpty();
    }

    @Test
    void aPartOnABaselineOfItsOwnIsRaisedToItAndOneOfMoreLinesThanOneIsCounted() throws Exception {
        // Word sets a line's parts on one baseline, its tallest part's; the page sets the parts from
        // the top, the smaller ones higher, and each is raised to its own.
        assertThat(zoneNotes(DocumentPageZone.footer(40, page -> new RowBuilder().name("Line")
                .addParagraph(p -> p.text("Confidential").textStyle(CHROME))
                .flexSpacer()
                .addParagraph(p -> p.text("Acme").textStyle(DocumentTextStyle.DEFAULT.withSize(18)))
                .build()))).isEmpty();
        assertThat(zoneNotes(DocumentPageZone.footer(40, page -> new RowBuilder().name("Line")
                .addParagraph(p -> p.text("Acme").textStyle(DocumentTextStyle.DEFAULT.withSize(18)))
                .flexSpacer()
                .addParagraph(p -> p.text("v2.4").textStyle(CHROME))
                .add(page.pageNumber(CHROME))
                .build()))).as("a page field as a paragraph").isEmpty();
        assertThat(zoneNotes(DocumentPageZone.header(40, page -> new RowBuilder().name("Line")
                .addParagraph(p -> p.text("Acme").textStyle(DocumentTextStyle.DEFAULT.withSize(18)))
                .flexSpacer()
                .addParagraph(p -> p.text("v2.4").textStyle(CHROME))
                .add(page.pageNumber(CHROME))
                .build()))).as("in a header too").isEmpty();
        // A smaller part the page sets lower than the fifth of the line below Word's baseline holds
        // is not lowered: it moves up onto that baseline, and is counted.
        assertThat(zoneNotes(DocumentPageZone.footer(60, page -> new RowBuilder().name("Line")
                .addParagraph(p -> p.text("Acme").textStyle(DocumentTextStyle.DEFAULT.withSize(18)))
                .flexSpacer()
                .addParagraph(p -> p.text("v2.4").textStyle(CHROME).margin(new DocumentInsets(30, 0, 0, 0)))
                .build())))
                .containsExactly(FOOTER + "1 of its 2 parts stands off where the page sets them");
        // A part the page seats below its baseline, past what the line holds below Word's, is set
        // on Word's, and counted.
        assertThat(zoneNotes(DocumentPageZone.footer(40, page -> new RowBuilder().name("Line")
                .addParagraph(p -> p.text("Confidential").textStyle(DocumentTextStyle.DEFAULT.withSize(18)))
                .flexSpacer()
                .addParagraph(p -> p.text("Acme").textStyle(DocumentTextStyle.DEFAULT.withSize(18))
                        .verticalAlign(com.demcha.compose.document.node.TextVerticalAlign.CENTER))
                .build())))
                .containsExactly(FOOTER + "1 of its 2 parts stands off where the page sets them");
        // The zone's line holds none of what keeps a body paragraph's breaks where the page sets them.
        assertThat(zoneNotes(DocumentPageZone.footer(40, page -> text(
                "Confidential and proprietary: not for distribution outside the company").build())))
                .containsExactly(FOOTER + OFF);
    }

    @Test
    void aZoneParagraphsOwnLossesAreNamed() throws Exception {
        assertThat(zoneNotes(DocumentPageZone.footer(30, page -> text("Confidential")
                .direction(TextDirection.RTL).align(TextAlign.LEFT).build())))
                .singleElement().asString().contains("a paragraph's right-to-left text is written left to right");
        assertThat(zoneNotes(DocumentPageZone.footer(30, page -> text("Confidential").bulletOffset("• ")
                .indentStrategy(DocumentTextIndent.FIRST_LINE).build())))
                .as("a prefix Word does not write stands the text off, and its letters are lost")
                .containsExactly(FOOTER + OFF + "; a paragraph's bulletOffset letters, \"•\", are not written before "
                                 + "its first line");
        // Auto-sized, it is not named where its lines tell the size the page fits it to, which is written.
        assertThat(zoneNotes(DocumentPageZone.footer(30, page -> text("Hi").autoSize(14).build()))).isEmpty();
        // Fitted to 12pt, the size its first run has of its own, its lines do not tell which size is its own.
        assertThat(zoneNotes(DocumentPageZone.footer(30, page -> new ParagraphBuilder().name("ZoneLine")
                .textStyle(CHROME).inlineText("A ", CHROME.withSize(12)).inlineText("B ", CHROME.withSize(10))
                .inlineText("C").autoSize(12).build())))
                .containsExactly(FOOTER + "a paragraph's text is written at 8pt — the size the page fits it to is not "
                                 + "measured");
        assertThat(zoneNotes(DocumentPageZone.footer(30, page -> text("Confidential")
                .bookmark(new DocumentBookmarkOptions("Confidential", 0)).build())))
                .containsExactly(FOOTER + "a paragraph's outline entry is not written");
    }

    @Test
    void wherePartsStandPastOneWordSetsAtAnotherWidthIsNotMeasured() throws Exception {
        // Word writes no prefix, so the part after it starts where the layout does not tell.
        assertThat(zoneNotes(DocumentPageZone.footer(30, page -> new RowBuilder().name("Line")
                .addParagraph(p -> p.text("Confidential").textStyle(CHROME).bulletOffset("• ")
                        .indentStrategy(DocumentTextIndent.FIRST_LINE))
                .addParagraph(p -> p.text("Acme").textStyle(CHROME))
                .flexSpacer()
                .add(page.pageNumber(CHROME))
                .build())))
                .containsExactly(FOOTER + "1 of its 3 parts stands off where the page sets them; where 1 of its 3 "
                                 + "parts stands is not measured; a paragraph's bulletOffset letters, \"•\", are not "
                                 + "written before its first line");
        // A part auto-sized is written at the size the page fits it to, as wide as the page sets
        // it; set lower than Word's baseline, the parts after it stand off.
        assertThat(zoneNotes(DocumentPageZone.footer(40, page -> new RowBuilder().name("Line")
                .addParagraph(p -> p.text("Hi").textStyle(CHROME).autoSize(14))
                .addParagraph(p -> p.text("Acme").textStyle(CHROME))
                .addParagraph(p -> p.text("Co").textStyle(CHROME))
                .build())))
                .containsExactly(FOOTER + "2 of its 3 parts stand off where the page sets them");
        // Past a part auto-sized to a size its lines do not tell, where a part on its baseline
        // stands is not measured.
        assertThat(zoneNotes(DocumentPageZone.footer(40, page -> new RowBuilder().name("Line")
                .addParagraph(p -> p.textStyle(CHROME).inlineText("A ", CHROME.withSize(12))
                        .inlineText("B ", CHROME.withSize(10)).inlineText("C").autoSize(12))
                .addParagraph(p -> p.text("Acme").textStyle(CHROME.withSize(12)))
                .build())))
                .containsExactly(FOOTER + "where 1 of its 2 parts stands is not measured; a paragraph's text is "
                                 + "written at 8pt — the size the page fits it to is not measured");
        // Against the right margin, the part a prefix stands before ends where Word ends it; the
        // part before it, Word sets the prefix's width off.
        assertThat(zoneNotes(DocumentPageZone.footer(30, page -> new RowBuilder().name("Line")
                .addParagraph(p -> p.text("Confidential").textStyle(CHROME))
                .flexSpacer()
                .addParagraph(p -> p.text("v2.4").textStyle(CHROME))
                .addParagraph(p -> p.text("Acme").textStyle(CHROME).bulletOffset("   ")
                        .indentStrategy(DocumentTextIndent.FIRST_LINE))
                .add(page.pageNumber(CHROME))
                .build())))
                .containsExactly(FOOTER + "where 1 of its 4 parts stands is not measured");
    }

    @Test
    void aZoneWhoseNodesThePageNestsOtherwiseIsNotMeasured() throws Exception {
        // Written once for every page, the zone is built as for no page in particular: the last,
        // as far as it can tell, where the page's first is not, and its nodes are another tree.
        AtomicReference<DocxExportReport> captured = new AtomicReference<>();
        try (DocumentSession session = GraphCompose.document().pageSize(300, 400).margin(DocumentInsets.of(36)).create()) {
            session.chrome().zone(DocumentPageZone.footer(30, page -> page.isLast()
                    ? text("End").build()
                    : new RowBuilder().name("Line").addParagraph(p -> p.text("A").textStyle(CHROME)).build()));
            session.pageFlow(page -> page.addParagraph("Body").addPageBreak(pageBreak -> { })
                    .addParagraph("More"));
            session.export(new DocxSemanticBackend(captured::set));
        }
        assertThat(captured.get().bySubject().get("page zone")).extracting(DocxExportReport.Note::detail)
                .containsExactly(FOOTER + "whether its text stands where the page sets it is not measured");
    }

    @Test
    void aZoneWrittenOnSeveralKindsOfPageIsNamedOnce() throws Exception {
        // The first page's header makes a title page, so the footer goes into two of Word's footers.
        assertThat(zoneNotes(DocumentPageZone.footer(30, page -> text("Confidential").align(TextAlign.CENTER).build()),
                DocumentPageZone.header(30, page -> text("Cover").build()).toBuilder()
                        .appliesTo(page -> page.isFirst()).build()))
                .containsExactly(FOOTER + OFF);
    }

    @Test
    void withNoLayoutWhereItsTextStandsIsNotMeasured() throws Exception {
        AtomicReference<SemanticExportContext> context = new AtomicReference<>();
        AtomicReference<DocumentGraph> graph = new AtomicReference<>();
        AtomicReference<DocxExportReport> report = new AtomicReference<>();
        try (DocumentSession session = session(DocumentPageZone.footer(30, page -> text("Confidential").build()))) {
            session.export(new SemanticBackend<byte[]>() {
                @Override
                public String name() {
                    return "capture";
                }

                @Override
                public byte[] export(DocumentGraph documentGraph, SemanticExportContext exportContext) {
                    graph.set(documentGraph);
                    context.set(exportContext);
                    return new byte[0];
                }
            });
            new DocxSemanticBackend(report::set).export(graph.get(), new SemanticExportContext(
                    context.get().canvas(), List.of(), null, context.get().outputOptions()));
        }
        assertThat(report.get().bySubject().get("page zone")).extracting(DocxExportReport.Note::detail)
                .containsExactly(FOOTER + "whether its text stands where the page sets it is not measured");
    }

    private static ParagraphBuilder text(String text) {
        return new ParagraphBuilder().name("ZoneLine").text(text).textStyle(CHROME);
    }

    private static List<String> zoneNotes(DocumentPageZone... zones) throws Exception {
        AtomicReference<DocxExportReport> captured = new AtomicReference<>();
        try (DocumentSession session = session(zones)) {
            session.export(new DocxSemanticBackend(captured::set));
        }
        assertThat(captured.get()).as("the sink is called once the bytes exist").isNotNull();
        return captured.get().bySubject().getOrDefault("page zone", List.of()).stream()
                .map(DocxExportReport.Note::detail).toList();
    }

    private static DocumentSession session(DocumentPageZone... zones) {
        DocumentSession session = GraphCompose.document()
                .pageSize(300, 400)
                .margin(DocumentInsets.of(36))
                .create();
        for (DocumentPageZone zone : zones) {
            session.chrome().zone(zone);
        }
        session.pageFlow(page -> page.addParagraph("Body"));
        return session;
    }
}
