package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.dsl.RowBuilder;
import com.demcha.compose.document.output.DocumentHeaderFooterZone;
import com.demcha.compose.document.output.DocumentPageZone;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextStyle;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFHeaderFooter;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.junit.jupiter.api.Test;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTFramePr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPageMar;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSpacing;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STLineSpacingRule;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * A page zone's line stands its text on the baseline the page sets it on.
 *
 * <p>A zone's line is an exact line, as tall as its tallest part's line on the page and as a
 * picture in it needs, standing as far from its edge as puts that part's baseline where the page
 * has it — both editors stand an exact line's baseline four fifths down it, as measured
 * ({@link DocxTextBands#BASELINE_SHARE}). So the room a lone part's padding and margin hold above
 * and below its text is in that distance. Placed by its content's edges, Word's own line stood a
 * header's text its padding high. A zone that shares its kind with another page zone stands in a
 * frame at its own height, as a band does.</p>
 *
 * <p>The page's baseline is read from the PDF the engine draws; Word's from the file: the
 * distance from the edge, or the frame's place, and the exact line.</p>
 */
class DocxZoneLineTest {

    private static final double PAGE_HEIGHT = 600;
    private static final DocumentTextStyle CHROME = DocumentTextStyle.DEFAULT.withSize(8);

    @Test
    void aHeadersTextStandsOnThePagesBaseline() throws Exception {
        Exported exported = export(zone(DocumentHeaderFooterZone.HEADER, 40, new DocumentInsets(9, 0, 0, 0),
                "Chrome", CHROME));

        assertThat(fromTheTop(exported.margin().getHeader()) + share(exported.headerLine()))
                .as("Word's baseline, from the page's top")
                .isCloseTo(exported.baseline("Chrome"), within(0.1));
    }

    @Test
    void aFootersTextStandsOnThePagesBaseline() throws Exception {
        Exported exported = export(zone(DocumentHeaderFooterZone.FOOTER, 40, new DocumentInsets(6, 0, 0, 0),
                "Chrome", CHROME));
        double line = lineOf(exported.footerLine());

        assertThat(PAGE_HEIGHT - fromTheTop(exported.margin().getFooter()) - (line - share(exported.footerLine())))
                .isCloseTo(exported.baseline("Chrome"), within(0.1));
    }

    @Test
    void theRoomALonePartHoldsAboveItsTextIsInTheDistance() throws Exception {
        // A page number padded 6pt down stands 6pt lower on the page, and in Word.
        Exported plain = export(fieldZone(DocumentInsets.zero()));
        Exported padded = export(fieldZone(new DocumentInsets(6, 0, 0, 0)));

        assertThat(fromTheTop(padded.margin().getHeader()) - fromTheTop(plain.margin().getHeader()))
                .isCloseTo(padded.baseline("1") - plain.baseline("1"), within(0.1))
                .isCloseTo(6, within(0.1));
    }

    @Test
    void aLonePageFieldStandsOnThePagesBaselineAndItsMarginIsInTheDistanceToo() throws Exception {
        Exported header = export(fieldZone(DocumentHeaderFooterZone.HEADER, new DocumentInsets(6, 0, 0, 0),
                DocumentInsets.zero()));
        Exported footer = export(fieldZone(DocumentHeaderFooterZone.FOOTER, DocumentInsets.zero(),
                new DocumentInsets(0, 0, 6, 0)));
        Exported plain = export(fieldZone(DocumentHeaderFooterZone.HEADER, DocumentInsets.zero(), DocumentInsets.zero()));
        Exported spaced = export(fieldZone(DocumentHeaderFooterZone.HEADER, DocumentInsets.zero(),
                new DocumentInsets(6, 0, 0, 0)));

        assertThat(fromTheTop(header.margin().getHeader()) + share(header.headerLine()))
                .isCloseTo(header.baseline("1"), within(0.1));
        assertThat(PAGE_HEIGHT - fromTheTop(footer.margin().getFooter())
                   - (lineOf(footer.footerLine()) - share(footer.footerLine())))
                .isCloseTo(footer.baseline("1"), within(0.1));
        assertThat(fromTheTop(spaced.margin().getHeader()) - fromTheTop(plain.margin().getHeader()))
                .as("its margin above, as its padding")
                .isCloseTo(spaced.baseline("1") - plain.baseline("1"), within(0.1))
                .isCloseTo(6, within(0.1));
    }

    @Test
    void aFootersLineOfPartsStandsOnItsTallestPartsBaseline() throws Exception {
        Exported exported = export(DocumentPageZone.builder().zone(DocumentHeaderFooterZone.FOOTER).height(40)
                .padding(new DocumentInsets(4, 0, 0, 0))
                .content(page -> new RowBuilder().name("Line")
                        .addParagraph(p -> p.text("v2.4").textStyle(CHROME))
                        .flexSpacer()
                        .addParagraph(p -> p.text("Acme").textStyle(DocumentTextStyle.DEFAULT.withSize(18)))
                        .build())
                .build());
        XWPFParagraph line = exported.footerLine();

        assertThat(PAGE_HEIGHT - fromTheTop(exported.margin().getFooter()) - (lineOf(line) - share(line)))
                .isCloseTo(exported.baseline("Acme"), within(0.1));
        assertThat(lineOf(line)).as("as tall as the 18pt part's line, not the 8pt one's").isBetween(16.0, 18.0);
    }

    @Test
    void aFootersParagraphOfTwoLinesStandsBothOnThePagesBaselines() throws Exception {
        // Word grows a footer up from its distance: the paragraph stands a line further from the
        // edge than one line would, its first line on the page's first baseline.
        Exported exported = export(zone(DocumentHeaderFooterZone.FOOTER, 40, new DocumentInsets(4, 0, 0, 0),
                "First\nSecond", CHROME));
        XWPFParagraph line = exported.footerLine();
        double foot = PAGE_HEIGHT - fromTheTop(exported.margin().getFooter());

        assertThat(foot - (lineOf(line) - share(line))).as("the second line's baseline")
                .isCloseTo(exported.baseline("Second"), within(0.1));
        assertThat(foot - (lineOf(line) - share(line)) - lineOf(line)).as("the first line's baseline")
                .isCloseTo(exported.baseline("First"), within(0.1));
    }

    @Test
    void aFootersRowBesideAPartOfTwoLinesStandsItsFirstLineOnThePagesBaseline() throws Exception {
        // The part of two lines makes Word's paragraph two lines tall, whichever part is tallest:
        // the part before its break stands on the first, where the page sets it.
        Exported exported = export(footerRow(p -> p.text("Acme").textStyle(CHROME),
                p -> p.text("First\nSecond").textStyle(CHROME)));
        XWPFParagraph line = exported.footerLine();

        assertThat(PAGE_HEIGHT - fromTheTop(exported.margin().getFooter()) - (lineOf(line) - share(line))
                   - lineOf(line)).as("the first line's baseline").isCloseTo(exported.baseline("Acme"), within(0.1));
        assertThat(exported.report().bySubject().get("page zone")).extracting(DocxExportReport.Note::detail)
                .containsExactly("a footer written as one line of Word's footer; 1 of its 2 parts stands off where "
                                 + "the page sets them");
    }

    @Test
    void wherePartsAfterAPartOfTwoLinesStandIsNotMeasured() throws Exception {
        // Word sets what follows the break on its second line, whatever the page sets it on.
        Exported exported = export(footerRow(p -> p.text("First\nSecond").textStyle(CHROME),
                p -> p.text("Acme").textStyle(CHROME)));

        assertThat(exported.report().bySubject().get("page zone")).extracting(DocxExportReport.Note::detail)
                .containsExactly("a footer written as one line of Word's footer; 1 of its 2 parts stands off where "
                                 + "the page sets them; where 1 of its 2 parts stands is not measured");
    }

    @Test
    void aHeaderOfLinesReachingPastTheMarginHoldsTheBodyAtIt() throws Exception {
        // Three 18pt lines of a zone the body runs under reach some 55pt down a page whose margin
        // is 36pt, where one line would not reach the margin.
        Exported exported = export(DocumentPageZone.builder().zone(DocumentHeaderFooterZone.HEADER).height(60)
                .reserveSpace(false).padding(new DocumentInsets(4, 0, 0, 0))
                .content(page -> new ParagraphBuilder().text("One\nTwo\nThree")
                        .textStyle(DocumentTextStyle.DEFAULT.withSize(18)).build())
                .build(), 36);

        assertThat(fromTheTop(exported.margin().getHeader()) + lineOf(exported.headerLine()))
                .as("one line would stay inside the margin").isLessThan(36);
        assertThat(DocxTwips.of(exported.margin().getTop())).as("written negative").isNegative();
        assertThat(exported.report().bySubject().get("page zone")).extracting(DocxExportReport.Note::detail)
                .anySatisfy(note -> assertThat(note).startsWith("a header whose line reaches "));
    }

    @Test
    void aPictureInAZoneHasRoomAboveTheBaselineAndItsPlaceIsNamed() throws Exception {
        // Word stands a zone's picture on the baseline, and an exact line cuts what passes its top:
        // the line is tall enough for it, the text still on the page's baseline.
        Exported exported = export(DocumentPageZone.builder().zone(DocumentHeaderFooterZone.HEADER).height(60)
                .padding(new DocumentInsets(4, 0, 0, 0))
                .content(page -> new ParagraphBuilder()
                        .inlineImage(com.demcha.compose.document.image.DocumentImageData.fromBytes(png()), 24, 24)
                        .inlineText(" Acme", CHROME).build())
                .build());
        XWPFParagraph line = exported.headerLine();

        assertThat(share(line)).as("room above the baseline for the 24pt picture").isGreaterThanOrEqualTo(24);
        assertThat(fromTheTop(exported.margin().getHeader()) + share(line))
                .isCloseTo(exported.baseline("Acme"), within(0.1));
        assertThat(exported.report().bySubject().get("page zone")).extracting(DocxExportReport.Note::detail)
                .containsExactly("a header written as one line of Word's header; a paragraph's picture stands on "
                                 + "the line's baseline, not where the page sets it");
    }

    @Test
    void aLineReachingPastTheMarginHoldsTheBodyAtItAndIsNamed() throws Exception {
        // A 30pt picture at the foot of a 36pt header band: the line it needs reaches past the
        // margin, where Word would move the body down.
        Exported exported = export(DocumentPageZone.builder().zone(DocumentHeaderFooterZone.HEADER).height(36)
                .padding(new DocumentInsets(4, 0, 0, 0))
                .content(page -> new ParagraphBuilder()
                        .inlineImage(com.demcha.compose.document.image.DocumentImageData.fromBytes(png()), 30, 30,
                                com.demcha.compose.document.node.InlineImageAlignment.BASELINE)
                        .inlineText(" Acme", CHROME).build())
                .build(), 36);

        assertThat(DocxTwips.of(exported.margin().getTop())).as("written negative").isNegative();
        // The picture stands on the baseline, where the page sets it too: the reach is all there is.
        assertThat(exported.report().bySubject().get("page zone")).extracting(DocxExportReport.Note::detail)
                .singleElement().satisfies(note -> assertThat(note).startsWith("a header whose line reaches ")
                        .endsWith("past the page margin, which is written negative so that Word holds the body at the "
                                  + "margin, as the page does; LibreOffice moves the body clear of it"));
    }

    @Test
    void aPartFittedSmallerIsWrittenAtTheSizeThePageFitsItToInThePagesLine() throws Exception {
        Exported exported = export(DocumentPageZone.builder().zone(DocumentHeaderFooterZone.HEADER).height(40)
                .padding(new DocumentInsets(4, 0, 0, 0))
                .content(page -> new ParagraphBuilder()
                        .text("A running header far too long to set at its eighteen points across this page")
                        .textStyle(DocumentTextStyle.DEFAULT.withSize(18)).autoSize(18, 6).build())
                .build());
        double written = exported.headerLine().getRuns().get(0).getFontSizeAsDouble();
        assertThat(written).as("fitted smaller than its style's 18pt").isLessThan(18);
        Exported unfitted = export(zone(DocumentHeaderFooterZone.HEADER, 40, new DocumentInsets(4, 0, 0, 0), "Acme",
                DocumentTextStyle.DEFAULT.withSize(written)));

        assertThat(lineOf(exported.headerLine())).as("the page's line, as a part's of that size is")
                .isCloseTo(lineOf(unfitted.headerLine()), within(0.05));
        assertThat(exported.report().bySubject()).doesNotContainKey("page zone");
    }

    @Test
    void aPartWhoseLinesDoNotTellTheSizeThePageFitsItToIsGivenItsStylesLine() throws Exception {
        // Fitted to 12pt, the size its first run has of its own: the lines hold 12 and 10, each a
        // run's own, and do not tell which is the paragraph's. Written at its style's 18pt, in a
        // line the page's height its letters' tops would be cut.
        Exported exported = export(DocumentPageZone.builder().zone(DocumentHeaderFooterZone.HEADER).height(40)
                .padding(new DocumentInsets(4, 0, 0, 0))
                .content(page -> new ParagraphBuilder().textStyle(DocumentTextStyle.DEFAULT.withSize(18))
                        .inlineText("A ", DocumentTextStyle.DEFAULT.withSize(12))
                        .inlineText("B ", DocumentTextStyle.DEFAULT.withSize(10))
                        .inlineText("C").autoSize(12, 6).build())
                .build());
        Exported unfitted = export(zone(DocumentHeaderFooterZone.HEADER, 40, new DocumentInsets(4, 0, 0, 0), "Acme",
                DocumentTextStyle.DEFAULT.withSize(18)));

        assertThat(exported.headerLine().getRuns()).extracting(XWPFRun::getFontSizeAsDouble).containsExactly(12.0, 10.0, 18.0);
        assertThat(lineOf(exported.headerLine())).as("the 18pt style's line")
                .isCloseTo(lineOf(unfitted.headerLine()), within(0.05));
        assertThat(exported.report().bySubject().get("page zone")).extracting(DocxExportReport.Note::detail)
                .singleElement().asString()
                .endsWith("a paragraph's text is written at 18pt — the size the page fits it to is not measured");
    }

    @Test
    void aZoneWhoseFaceDiffersOnItsFirstPageIsNotMeasured() throws Exception {
        // Written at 18pt for no page in particular, it is set at 8pt on page 1: a line measured by
        // that would cut the written letters' tops.
        Exported exported = export(session -> session.chrome().zone(DocumentPageZone.footer(40,
                page -> new ParagraphBuilder().text("Acme")
                        .textStyle(page.isLast() ? DocumentTextStyle.DEFAULT.withSize(18) : CHROME).build())),
                true);
        CTSpacing spacing = exported.footerLine().getCTP().getPPr().getSpacing();

        assertThat(spacing.isSetLineRule()).as("the line left Word's").isFalse();
        assertThat(exported.report().bySubject().get("page zone")).extracting(DocxExportReport.Note::detail)
                .containsExactly("a footer written as one line of Word's footer; whether its text stands where the "
                                 + "page sets it is not measured");
    }

    @Test
    void aZoneThatBuildsNothingForNoPageInParticularIsNamed() throws Exception {
        Exported exported = export(session -> session.chrome().zone(DocumentPageZone.footer(40,
                page -> page.isPaginated() ? new ParagraphBuilder().text("Drawn").textStyle(CHROME).build() : null)),
                false);

        assertThat(exported.document().getFooterList()).as("nothing to write").isEmpty();
        assertThat(exported.report().bySubject().get("page zone")).extracting(DocxExportReport.Note::detail)
                .containsExactly("a footer is not written: built for no page in particular, its content is none; "
                                 + "a zone absent from some pages says so through appliesTo");
    }

    @Test
    void aZoneThatReadsOtherwiseOnItsFirstPageIsNotMeasured() throws Exception {
        // Written for no page in particular, it reads "End"; the page set "Continued" on page 1.
        Exported exported = export(session -> session.chrome().zone(DocumentPageZone.footer(40,
                page -> new ParagraphBuilder().text(page.isLast() ? "End" : "Continued").textStyle(CHROME).build())),
                true);
        CTSpacing spacing = exported.footerLine().getCTP().getPPr().getSpacing();

        assertThat(spacing.isSetLineRule()).as("the line left Word's").isFalse();
        assertThat(exported.report().bySubject().get("page zone")).extracting(DocxExportReport.Note::detail)
                .containsExactly("a footer written as one line of Word's footer; whether its text stands where the "
                                 + "page sets it is not measured");
    }

    @Test
    void withoutALayoutTheLineIsWords() throws Exception {
        byte[] docx;
        try (DocumentSession session = GraphCompose.document().pageSize(400, PAGE_HEIGHT)
                .margin(DocumentInsets.of(72)).create()) {
            session.chrome().zone(zone(DocumentHeaderFooterZone.HEADER, 40, new DocumentInsets(9, 0, 0, 0),
                    "Chrome", CHROME));
            session.pageFlow(page -> page.addParagraph("Body"));
            CapturingBackend captured = new CapturingBackend();
            session.export(captured);
            docx = new DocxSemanticBackend().export(captured.graph,
                    new com.demcha.compose.document.backend.semantic.SemanticExportContext(captured.canvas,
                            List.of(), null, captured.options));
        }
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(docx))) {
            CTSpacing spacing = document.getHeaderList().get(0).getParagraphs().get(0).getCTP().getPPr().getSpacing();
            assertThat(spacing.isSetLineRule()).as("no exact line with nothing to measure it by").isFalse();
        }
    }

    @Test
    void aFramedZoneLeavesTheDistanceOfAZoneInTheFlow() throws Exception {
        // A zone the layout does not measure stays in the flow, at the distance from the edge its
        // content gives; a framed zone of its kind, placed after it, leaves that distance alone.
        DocumentPageZone unmeasured = DocumentPageZone.builder().zone(DocumentHeaderFooterZone.HEADER).height(40)
                .padding(new DocumentInsets(20, 0, 0, 0))
                .content(page -> new ParagraphBuilder().text(page.isLast() ? "End" : "Continued").textStyle(CHROME)
                        .build())
                .build();
        Exported alone = export(session -> session.chrome().zone(unmeasured), true);
        Exported withAFramedOne = export(session -> {
            session.chrome().zone(unmeasured);
            session.chrome().zone(zone(DocumentHeaderFooterZone.HEADER, 40, new DocumentInsets(4, 0, 0, 0),
                    "Framed", CHROME));
        }, true);

        assertThat(DocxTwips.of(withAFramedOne.margin().getHeader()))
                .isEqualTo(DocxTwips.of(alone.margin().getHeader()));
        assertThat(headerParagraph(withAFramedOne.document(), "Framed").getCTP().getPPr().isSetFramePr()).isTrue();
    }

    @Test
    void twoZonesAtOneHeightAreTwoFramesNotOne() throws Exception {
        // Word takes adjacent paragraphs with the same frame for one frame: a hairline parts them.
        Exported exported = export(session -> {
            session.chrome().zone(zone(DocumentHeaderFooterZone.HEADER, 40, new DocumentInsets(8, 0, 0, 0),
                    "Left", CHROME));
            session.chrome().zone(zone(DocumentHeaderFooterZone.HEADER, 40, new DocumentInsets(8, 0, 0, 0),
                    "Again", CHROME));
        }, false);
        List<XWPFParagraph> paragraphs = exported.document().getHeaderList().get(0).getParagraphs();

        assertThat(paragraphs).extracting(XWPFParagraph::getText).containsSubsequence("Left", "", "Again");
        for (String text : List.of("Left", "Again")) {
            assertThat(headerParagraph(exported.document(), text).getCTP().getPPr().isSetFramePr())
                    .as("%s in a frame", text).isTrue();
        }
        assertThat(paragraphs.get(paragraphs.indexOf(headerParagraph(exported.document(), "Left")) + 1)
                .getCTP().getPPr().isSetFramePr()).as("the hairline between stands in the flow").isFalse();
    }

    @Test
    void aLineOfPartsStandsOnItsTallestPartsBaseline() throws Exception {
        Exported exported = export(DocumentPageZone.builder().zone(DocumentHeaderFooterZone.HEADER).height(40)
                .padding(new DocumentInsets(4, 0, 0, 0))
                .content(page -> new RowBuilder().name("Line")
                        .addParagraph(p -> p.text("v2.4").textStyle(CHROME))
                        .flexSpacer()
                        .addParagraph(p -> p.text("Acme").textStyle(DocumentTextStyle.DEFAULT.withSize(18)))
                        .build())
                .build());

        assertThat(fromTheTop(exported.margin().getHeader()) + share(exported.headerLine()))
                .isCloseTo(exported.baseline("Acme"), within(0.1));
        assertThat(lineOf(exported.headerLine())).as("as tall as the 18pt part's line, not the 8pt one's")
                .isBetween(16.0, 18.0);
    }

    @Test
    void zonesThatShareAKindEachStandInAFrameAtTheirOwnHeight() throws Exception {
        // A cover's header on the first page and the running header on the rest: Word holds one
        // distance from the edge for both, so each stands in a frame where the page sets it.
        Exported exported = export(session -> {
            session.chrome().zone(DocumentPageZone.builder().zone(DocumentHeaderFooterZone.HEADER).height(60)
                    .padding(new DocumentInsets(24, 0, 0, 0)).appliesTo(page -> page.isFirst())
                    .content(page -> new ParagraphBuilder().text("Cover").textStyle(CHROME).build()).build());
            session.chrome().zone(DocumentPageZone.builder().zone(DocumentHeaderFooterZone.HEADER).height(60)
                    .padding(new DocumentInsets(8, 0, 0, 0)).appliesTo(page -> !page.isFirst())
                    .content(page -> new ParagraphBuilder().text("Running").textStyle(CHROME).build()).build());
        }, true);

        for (String text : List.of("Cover", "Running")) {
            XWPFParagraph line = headerParagraph(exported.document(), text);
            CTFramePr frame = line.getCTP().getPPr().getFramePr();
            assertThat(frame).as("%s in a frame", text).isNotNull();
            assertThat(DocxTwips.of(frame.getY()) / 20.0 + share(line)).as("%s's baseline", text)
                    .isCloseTo(exported.baseline(text), within(0.1));
        }
    }

    @Test
    void zonesOfOneKindOnTheSamePagesEachStandAtTheirOwnHeightInOnePart() throws Exception {
        // Both on every page, in one part, a header's or a footer's: written in the flow, the
        // second line stood under the first, whatever the page set it at.
        for (DocumentHeaderFooterZone kind : DocumentHeaderFooterZone.values()) {
            Exported exported = export(session -> {
                session.chrome().zone(zone(kind, 60, new DocumentInsets(30, 0, 0, 0), "Lower", CHROME));
                session.chrome().zone(zone(kind, 60, new DocumentInsets(8, 0, 0, 0), "Upper", CHROME));
            }, false);

            for (String text : List.of("Lower", "Upper")) {
                XWPFParagraph line = headerParagraph(exported.document(), text);
                CTFramePr frame = line.getCTP().getPPr().getFramePr();
                assertThat(frame).as("%s's %s in a frame", kind, text).isNotNull();
                assertThat(frame.getHRule()).as("at least the line tall, a part of more lines going on below")
                        .isEqualTo(org.openxmlformats.schemas.wordprocessingml.x2006.main.STHeightRule.AT_LEAST);
                assertThat(DocxTwips.of(frame.getH()) / 20.0).as("%s's %s's frame, its line tall", kind, text)
                        .isCloseTo(lineOf(line), within(0.05));
                assertThat(DocxTwips.of(frame.getY()) / 20.0 + share(line)).as("%s's %s's baseline", kind, text)
                        .isCloseTo(exported.baseline(text), within(0.1));
            }
        }
    }

    @Test
    void aFramedFooterOfTwoLinesStandsItsFirstOnThePagesBaselineInAFrameOfBoth() throws Exception {
        Exported exported = export(session -> {
            session.chrome().zone(zone(DocumentHeaderFooterZone.FOOTER, 60, new DocumentInsets(30, 0, 0, 0),
                    "Lower", CHROME));
            session.chrome().zone(zone(DocumentHeaderFooterZone.FOOTER, 60, new DocumentInsets(4, 0, 0, 0),
                    "First\nSecond", CHROME));
        }, false);
        XWPFParagraph line = headerParagraph(exported.document(), "First\nSecond");
        CTFramePr frame = line.getCTP().getPPr().getFramePr();

        assertThat(DocxTwips.of(frame.getY()) / 20.0 + share(line)).as("the first line's baseline")
                .isCloseTo(exported.baseline("First"), within(0.1));
        assertThat(DocxTwips.of(frame.getH()) / 20.0).as("a frame of both lines")
                .isCloseTo(2 * lineOf(line), within(0.05));
    }

    @Test
    void aZoneParagraphsAnchorIsNamed() throws Exception {
        Exported exported = export(zone(DocumentHeaderFooterZone.FOOTER, 40, DocumentInsets.zero(),
                "Chrome", CHROME, "chrome-anchor"));

        assertThat(exported.report().bySubject().get("page zone")).extracting(DocxExportReport.Note::detail)
                .containsExactly("a footer written as one line of Word's footer; a paragraph's anchor has no "
                                 + "bookmark in the Word file: a link to it points at none");
    }

    @Test
    void aLineThePageSetsAtTheEdgeStandsAtIt() throws Exception {
        // An exact line's baseline is four fifths down it; text set against the page's top edge
        // stands a little higher than that, and the line stops at the edge.
        Exported exported = export(zone(DocumentHeaderFooterZone.HEADER, 40, DocumentInsets.zero(), "Acme",
                DocumentTextStyle.DEFAULT.withSize(18)));

        assertThat(fromTheTop(exported.margin().getHeader())).isZero();
        assertThat(share(exported.headerLine()) - exported.baseline("Acme")).as("lower than the page's, by a hair")
                .isBetween(0.0, 1.0);
        assertThat(exported.report().bySubject()).as("within the place a part keeps").doesNotContainKey("page zone");
    }

    @Test
    void aLineStoppedAtTheEdgeFurtherThanAPartKeepsItsPlaceIsNamed() throws Exception {
        // At 80pt the line's top would stand 1.8pt past the page's edge: stopped there, the text
        // stands that much lower than the page sets it.
        Exported exported = export(zone(DocumentHeaderFooterZone.HEADER, 120, DocumentInsets.zero(), "Acme",
                DocumentTextStyle.DEFAULT.withSize(80)));

        assertThat(share(exported.headerLine()) - exported.baseline("Acme")).isGreaterThan(1.5);
        assertThat(exported.report().bySubject().get("page zone")).extracting(DocxExportReport.Note::detail)
                .containsExactly("a header written as one line of Word's header; its text stands off where the page "
                                 + "sets it");
    }

    private static DocumentPageZone zone(DocumentHeaderFooterZone kind, double height, DocumentInsets padding,
                                         String text, DocumentTextStyle style) {
        return zone(kind, height, padding, text, style, null);
    }

    private static DocumentPageZone zone(DocumentHeaderFooterZone kind, double height, DocumentInsets padding,
                                         String text, DocumentTextStyle style, String anchor) {
        return DocumentPageZone.builder().zone(kind).height(height).padding(padding)
                .content(page -> {
                    ParagraphBuilder paragraph = new ParagraphBuilder().text(text).textStyle(style);
                    if (anchor != null) {
                        paragraph.anchor(anchor);
                    }
                    return paragraph.build();
                })
                .build();
    }

    /** A footer's row of two paragraphs, the second against the right margin. */
    private static DocumentPageZone footerRow(Consumer<ParagraphBuilder> left, Consumer<ParagraphBuilder> right) {
        return DocumentPageZone.builder().zone(DocumentHeaderFooterZone.FOOTER).height(40)
                .padding(new DocumentInsets(4, 0, 0, 0))
                .content(page -> new RowBuilder().name("Line").addParagraph(left).flexSpacer().addParagraph(right)
                        .build())
                .build();
    }

    /** A header holding the page number alone, 4pt down, its own padding round it. */
    private static DocumentPageZone fieldZone(DocumentInsets padding) {
        return fieldZone(DocumentHeaderFooterZone.HEADER, padding, DocumentInsets.zero());
    }

    /** A zone holding the page number alone, 4pt in from its page edge, its own sides round it. */
    private static DocumentPageZone fieldZone(DocumentHeaderFooterZone kind, DocumentInsets padding,
                                              DocumentInsets margin) {
        return DocumentPageZone.builder().zone(kind).height(40)
                .padding(kind == DocumentHeaderFooterZone.HEADER ? new DocumentInsets(4, 0, 0, 0)
                        : new DocumentInsets(0, 0, 4, 0))
                .content(page -> new RowBuilder().name("Line")
                        .add(new com.demcha.compose.document.node.PageFieldNode("",
                                com.demcha.compose.document.node.PageFieldKind.NUMBER, CHROME,
                                com.demcha.compose.document.node.TextAlign.LEFT, padding, margin))
                        .build())
                .build();
    }

    private static byte[] png() {
        java.awt.image.BufferedImage image = new java.awt.image.BufferedImage(8, 8,
                java.awt.image.BufferedImage.TYPE_INT_RGB);
        try (java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream()) {
            javax.imageio.ImageIO.write(image, "png", out);
            return out.toByteArray();
        } catch (IOException failure) {
            throw new java.io.UncheckedIOException(failure);
        }
    }

    /** Takes the graph, the canvas and the options a session hands any backend. */
    private static final class CapturingBackend
            implements com.demcha.compose.document.backend.semantic.SemanticBackend<byte[]> {

        private com.demcha.compose.document.layout.DocumentGraph graph;
        private com.demcha.compose.document.layout.LayoutCanvas canvas;
        private com.demcha.compose.document.output.DocumentOutputOptions options;

        @Override
        public String name() {
            return "capture";
        }

        @Override
        public byte[] export(com.demcha.compose.document.layout.DocumentGraph documentGraph,
                             com.demcha.compose.document.backend.semantic.SemanticExportContext context) {
            this.graph = documentGraph;
            this.canvas = context.canvas();
            this.options = context.outputOptions();
            return new byte[0];
        }
    }

    /**
     * How far down its exact line an exact line's baseline stands, in points: four fifths, as
     * measured in Word and LibreOffice (DocxTextBands.BASELINE_SHARE).
     */
    private static double share(XWPFParagraph paragraph) {
        return 0.8 * lineOf(paragraph);
    }

    private static double lineOf(XWPFParagraph paragraph) {
        CTSpacing spacing = paragraph.getCTP().getPPr().getSpacing();
        assertThat(spacing.getLineRule()).as("the zone's line is exact").isEqualTo(STLineSpacingRule.EXACT);
        return DocxTwips.of(spacing.getLine()) / 20.0;
    }

    private static double fromTheTop(Object twips) {
        return DocxTwips.of(twips) / 20.0;
    }

    /** A header's or a footer's paragraph reading the text. */
    private static XWPFParagraph headerParagraph(XWPFDocument document, String text) {
        List<XWPFHeaderFooter> parts = new ArrayList<>(document.getHeaderList());
        parts.addAll(document.getFooterList());
        for (XWPFHeaderFooter part : parts) {
            for (XWPFParagraph paragraph : part.getParagraphs()) {
                if (paragraph.getText().equals(text)) {
                    return paragraph;
                }
            }
        }
        throw new AssertionError("no header or footer paragraph reading " + text);
    }

    private record Exported(XWPFDocument document, DocxExportReport report, List<TextPosition> text) {

        CTPageMar margin() {
            return document.getDocument().getBody().getSectPr().getPgMar();
        }

        XWPFParagraph headerLine() {
            return document.getHeaderList().get(0).getParagraphs().get(0);
        }

        XWPFParagraph footerLine() {
            return document.getFooterList().get(0).getParagraphs().get(0);
        }

        /** Where the page sets a word's baseline, from its top, on the first page it draws it. */
        double baseline(String word) {
            StringBuilder letters = new StringBuilder();
            for (int start = 0; start < text.size(); start++) {
                letters.setLength(0);
                for (int index = start; index < text.size() && letters.length() < word.length(); index++) {
                    letters.append(text.get(index).getUnicode());
                }
                if (letters.toString().equals(word)) {
                    return text.get(start).getYDirAdj();
                }
            }
            throw new AssertionError("the page draws no " + word);
        }
    }

    private static Exported export(DocumentPageZone zone) throws Exception {
        return export(session -> session.chrome().zone(zone), false);
    }

    private static Exported export(DocumentPageZone zone, double margin) throws Exception {
        return export(session -> session.chrome().zone(zone), false, margin);
    }

    private static Exported export(Consumer<DocumentSession> chrome, boolean twoPages) throws Exception {
        return export(chrome, twoPages, 72);
    }

    private static Exported export(Consumer<DocumentSession> chrome, boolean twoPages, double margin) throws Exception {
        AtomicReference<DocxExportReport> report = new AtomicReference<>();
        try (DocumentSession session = GraphCompose.document()
                .pageSize(400, PAGE_HEIGHT)
                .margin(DocumentInsets.of(margin))
                .create()) {
            chrome.accept(session);
            session.pageFlow(page -> {
                page.addParagraph(p -> p.text("Body"));
                if (twoPages) {
                    page.addPageBreak(pageBreak -> { });
                    page.addParagraph(p -> p.text("More"));
                }
            });
            List<TextPosition> text = textOf(session.toPdfBytes());
            byte[] docx = session.export(new DocxSemanticBackend(report::set));
            return new Exported(new XWPFDocument(new ByteArrayInputStream(docx)), report.get(), text);
        }
    }

    /** Every letter the page draws, in the order it draws them, page by page. */
    private static List<TextPosition> textOf(byte[] pdf) throws IOException {
        List<TextPosition> letters = new ArrayList<>();
        try (PDDocument document = Loader.loadPDF(pdf)) {
            PDFTextStripper stripper = new PDFTextStripper() {
                @Override
                protected void processTextPosition(TextPosition text) {
                    letters.add(text);
                }
            };
            stripper.getText(document);
        }
        return letters;
    }
}
