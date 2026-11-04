package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.TimelineMarker;
import com.demcha.compose.document.dsl.TimelineRailEnd;
import com.demcha.compose.document.layout.PlacedFragment;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentStroke;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.junit.jupiter.api.Test;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTBorder;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTDrawing;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTcPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTrPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STHeightRule;

import java.awt.Color;
import java.io.ByteArrayInputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A timeline whose entries are each a row alone has its rail written as the left border of the cells
 * it runs along, its entries one table: the rail then stretches with the entries when one grows in the
 * editor, where a drawing keeps the length the layout gave it.
 */
class DocxTimelineRailAlongCellsTest {

    private static final DocumentColor RAIL = DocumentColor.rgb(0xC0, 0x30, 0x30);
    private static final DocumentColor DOT = DocumentColor.rgb(0x1A, 0x56, 0x94);
    private static final String RAIL_HEX = "C03030";

    @Test
    void aRailFromMarkerToMarkerCutsTheFirstAndLastEntryAtTheirMarkers() throws Exception {
        try (DocumentSession session = timeline(true, TimelineRailEnd.MARKER, TimelineRailEnd.MARKER, 3, 600)) {
            Written written = export(session);
            DocxLayoutMetrics.RailedTimeline timeline = written.timeline();
            DocxLayoutMetrics.RailedEntry first = timeline.entries().get(0);
            DocxLayoutMetrics.RailedEntry last = timeline.entries().get(2);
            PlacedFragment rail = timeline.rails().get(0);

            assertThat(written.document().getTables()).as("the entries are one table").hasSize(1);
            XWPFTable table = written.document().getTables().get(0);
            // first entry cut in two at its marker, a gap, the second entry, a gap, the last cut in two
            assertThat(table.getRows()).hasSize(7);
            int rail1 = railCell(table, rail, first);
            assertThat(bordered(table, rail1)).containsExactly(false, true, true, true, true, true, false);

            assertThat(exactHeight(table.getRow(0)))
                    .as("the first entry's upper row ends at the rail's top, its marker's centre")
                    .isEqualTo(twips(first.top() - (rail.y() + rail.height())));
            // A row lifted into the gap above it starts higher by as much as its marker is set lower in
            // it: the rail ends at the same place on its marker in both entries.
            assertThat(exactHeight(table.getRow(5)) - markerSetDown(table.getRow(5), rail1 - 1))
                    .as("the last entry's upper row ends at its marker's centre, as the first one's does")
                    .isEqualTo(exactHeight(table.getRow(0)) - markerSetDown(table.getRow(0), rail1 - 1))
                    .isEqualTo(twips(last.top() - rail.y()) - markerSetDown(table.getRow(0), rail1 - 1));
            assertThat(keptWithTheNext(table.getRow(0))).as("a page never parts the first entry's two rows")
                    .containsOnly(true);
            assertThat(keptWithTheNext(table.getRow(5))).as("nor the last entry's").containsOnly(true);
            assertThat(vMerge(table.getRow(0).getCell(rail1 + 1))).isEqualTo("restart");
            assertThat(vMerge(table.getRow(1).getCell(rail1 + 1))).isEqualTo("continue");
            assertThat(text(table.getRow(0))).contains("Role 1");
            assertThat(text(table.getRow(1))).isEmpty();

            assertThat(written.xml()).as("the drawn rail is gone").doesNotContain(RAIL_HEX + "\"/></a:solidFill>");
            assertThat(written.notes()).as("written as it is, named nowhere")
                    .noneMatch(note -> note.subject().equals("timeline rail"));
            assertThat(bodyOutline(written.document()))
                    .as("the heading, the timeline's one table, what follows it — no gap paragraph left between")
                    .containsExactly("Experience", "TABLE", "After the timeline.");
            assertThat(text(table.getRow(3))).contains("Role 2");
            assertThat(text(table.getRow(5))).contains("Role 3");
        }
    }

    @Test
    void aRailFromEntryToEntryBordersEveryRowWhole() throws Exception {
        try (DocumentSession session = timeline(true, TimelineRailEnd.ENTRY_BOUND, TimelineRailEnd.ENTRY_BOUND, 3,
                600)) {
            Written written = export(session);
            XWPFTable table = written.document().getTables().get(0);
            assertThat(written.document().getTables()).hasSize(1);
            assertThat(table.getRows()).as("three entries and the two gaps between them").hasSize(5);
            int rail = railCell(table, written.timeline().rails().get(0), written.timeline().entries().get(0));
            assertThat(bordered(table, rail)).containsOnly(true);
            assertThat(exactHeight(table.getRow(0))).as("no entry cut").isNull();
            assertThat(exactHeight(table.getRow(1))).as("a gap is a row of its height").isPositive();
        }
    }

    @Test
    void aRailBesideTheMarkersGetsAColumnOfItsOwnInTheGutter() throws Exception {
        try (DocumentSession session = timeline(false, TimelineRailEnd.ENTRY_BOUND, TimelineRailEnd.ENTRY_BOUND, 3,
                600)) {
            Written written = export(session);
            XWPFTable table = written.document().getTables().get(0);
            DocxLayoutMetrics.RailedEntry first = written.timeline().entries().get(0);
            PlacedFragment rail = written.timeline().rails().get(0);
            assertThat(rail.x()).as("the rail stands left of the entries").isLessThan(first.left());

            List<BigInteger> grid = new ArrayList<>();
            table.getCTTbl().getTblGrid().getGridColList().forEach(column -> grid.add((BigInteger) column.getW()));
            assertThat(grid.get(0).intValue()).as("a column from the rail to the entries")
                    .isEqualTo(twips(first.left() - rail.x()));
            assertThat(bordered(table, 0)).containsOnly(true);
            BigInteger indent = (BigInteger) table.getCTTbl().getTblPr().getTblInd().getW();
            assertThat(indent.intValue()).as("the table moved left by that column, its entries where they were")
                    .isCloseTo(twips(rail.x() - 30), org.assertj.core.data.Offset.offset(2));
        }
    }

    @Test
    void aRailAcrossAPageBreakIsOneTableAcrossIt() throws Exception {
        try (DocumentSession session = timeline(true, TimelineRailEnd.MARKER, TimelineRailEnd.MARKER, 7, 300)) {
            Written written = export(session);
            assertThat(written.timeline().rails()).extracting(PlacedFragment::pageIndex).containsExactly(0, 1);
            assertThat(written.document().getTables()).hasSize(1);
            // seven entries, the first and the last cut in two, and six gaps
            assertThat(written.document().getTables().get(0).getRows()).hasSize(15);
            assertThat(written.xml()).doesNotContain(RAIL_HEX + "\"/></a:solidFill>");
        }
    }

    @Test
    void anEntryWithABodyBelowItsRowKeepsTheDrawnRail() throws Exception {
        try (DocumentSession session = GraphCompose.document().pageSize(400, 600).margin(DocumentInsets.of(30))
                .create()) {
            session.pageFlow(flow -> flow.addTimeline(t -> {
                t.markerOnRail().connector(RAIL, 1.5);
                for (int index = 1; index <= 3; index++) {
                    t.entry(TimelineMarker.dot(8, DOT), e -> e.title("Role").body("A body below the row."));
                }
            }));
            DocxLayoutMetrics layout = DocxLayoutMetrics.of(session.documentGraph(), session.layoutGraph());
            assertThat(layout.railedTimelines()).as("its entries are more than a row").isEmpty();

            AtomicReference<DocxExportReport> report = new AtomicReference<>();
            byte[] docx = session.export(new DocxSemanticBackend(report::set));
            try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(docx))) {
                assertThat(document.getTables()).as("an entry a table").hasSize(3);
                assertThat(document.getDocument().xmlText()).contains(RAIL_HEX + "\"/></a:solidFill>");
            }
            assertThat(report.get().notes()).anyMatch(note -> note.subject().equals("timeline rail"));
        }
    }

    @Test
    void aRowHeldToAHeightIsCutIntoTwoThatHoldItBetweenThem() throws Exception {
        // A marker taller than its one line of text holds its row to the marker's height: cut, the
        // upper row takes the rail's end, the lower one the rest, and neither keeps the whole.
        try (DocumentSession session = GraphCompose.document().pageSize(400, 600).margin(DocumentInsets.of(30))
                .create()) {
            session.pageFlow(flow -> flow.addTimeline(t -> {
                t.markerOnRail().rail(rail -> rail.stroke(DocumentStroke.of(RAIL, 1.5))
                        .from(TimelineRailEnd.MARKER).to(TimelineRailEnd.MARKER));
                for (int index = 1; index <= 3; index++) {
                    int n = index;
                    t.entry(TimelineMarker.dot(30, DOT), e -> e.content(section -> section.addParagraph("Role " + n)));
                }
            }));
            Written written = export(session);
            XWPFTable table = written.document().getTables().get(0);
            for (XWPFTableRow row : table.getRows()) {
                CTTrPr properties = row.getCtRow().getTrPr();
                assertThat(properties == null ? 0 : properties.sizeOfTrHeightArray()).as("one height a row")
                        .isLessThanOrEqualTo(1);
            }
            Integer upper = exactHeight(table.getRow(0));
            BigInteger lower = (BigInteger) table.getRow(1).getCtRow().getTrPr().getTrHeightArray(0).getVal();
            assertThat(table.getRow(1).getCtRow().getTrPr().getTrHeightArray(0).getHRule())
                    .isEqualTo(STHeightRule.AT_LEAST);
            assertThat(upper + lower.intValue()).as("the two hold the marker's 30pt between them, no more")
                    .isCloseTo(twips(30), org.assertj.core.data.Offset.offset(2));
        }
    }

    @Test
    void entriesKeptTogetherStayWholeAsRowsOfOneTable() throws Exception {
        // One-row tables whose paragraphs all keep with the next are let break for LibreOffice's
        // sake (letTheOneRowTablesKeptWithTheNextBreak): a timeline kept together keeps every entry
        // but the last so. As rows among others, the entries are kept whole again, as the layout
        // keeps them.
        try (DocumentSession session = GraphCompose.document().pageSize(400, 600).margin(DocumentInsets.of(30))
                .create()) {
            session.pageFlow(flow -> flow.addTimeline(t -> {
                t.markerOnRail().keepTogether().connector(RAIL, 1.5);
                for (int index = 1; index <= 3; index++) {
                    int n = index;
                    t.entry(TimelineMarker.dot(8, DOT), e -> e.content(section -> section
                            .addParagraph("Role " + n).addParagraph("Built the export pipeline.")));
                }
            }));
            XWPFTable table = export(session).document().getTables().get(0);
            for (XWPFTableRow row : table.getRows()) {
                assertThat(row.isCantSplitRow()).as("a row the page never breaks inside").isTrue();
            }
            // Every entry but the last, and every gap, kept with what follows, as the paragraphs
            // they were written as were: the timeline stays on one page in Word.
            List<XWPFTableRow> rows = table.getRows();
            for (XWPFTableRow row : rows.subList(0, rows.size() - 1)) {
                assertThat(keptWithTheNext(row)).as("kept with the next row").containsOnly(true);
            }
        }
    }

    @Test
    void aSeeThroughRailKeepsItsDrawnRail() throws Exception {
        // A border has no translucency: written as one, a rail drawn at 40% would show solid.
        try (DocumentSession session = timelineOfColour(DocumentColor.rgba(0xC0, 0x30, 0x30, 102))) {
            DocxLayoutMetrics layout = DocxLayoutMetrics.of(session.documentGraph(), session.layoutGraph());
            assertThat(layout.railedTimelines()).hasSize(1);
            AtomicReference<DocxExportReport> report = new AtomicReference<>();
            byte[] docx = session.export(new DocxSemanticBackend(report::set));
            try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(docx))) {
                assertThat(document.getTables()).as("an entry a table").hasSize(3);
            }
            assertThat(report.get().notes()).anyMatch(note -> note.subject().equals("timeline rail"));
        }
    }

    @Test
    void aMarkerOfTextTheRailWouldCutKeepsTheDrawnRail() throws Exception {
        // A numbered marker's figure stands in the column the rail cuts: kept in the part left of
        // the rail, it would no longer stand where the page sets it.
        try (DocumentSession session = GraphCompose.document().pageSize(400, 600).margin(DocumentInsets.of(30))
                .create()) {
            session.pageFlow(flow -> flow.addTimeline(t -> {
                t.markerOnRail().connector(RAIL, 1.5);
                for (int index = 1; index <= 3; index++) {
                    int n = index;
                    t.entry(TimelineMarker.numbered(n, 16, DOT, DocumentColor.WHITE),
                            e -> e.content(section -> section.addParagraph("Role " + n)));
                }
            }));
            AtomicReference<DocxExportReport> report = new AtomicReference<>();
            byte[] docx = session.export(new DocxSemanticBackend(report::set));
            try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(docx))) {
                assertThat(document.getDocument().xmlText()).contains(RAIL_HEX + "\"/></a:solidFill>");
            }
            assertThat(report.get().notes()).anyMatch(note -> note.subject().equals("timeline rail"));
        }
    }

    @Test
    void aTimelineInAnotherOnesBodyIsWrittenAlongItsCellsTheOuterOneDrawn() throws Exception {
        // A row cannot stand in a row, so a timeline is set in another one's entry only below its row,
        // which keeps the outer rail drawn; the inner timeline, its entries rows alone, is written
        // along its cells all the same.
        DocumentColor inner = DocumentColor.rgb(0x30, 0x80, 0x30);
        try (DocumentSession session = GraphCompose.document().pageSize(400, 600).margin(DocumentInsets.of(30))
                .create()) {
            session.pageFlow(flow -> flow.addTimeline(outer -> {
                outer.markerOnRail().connector(RAIL, 1.5);
                outer.entry(TimelineMarker.dot(8, DOT), e -> e.title("Role 1").add(below -> below
                        .addTimeline(nested -> {
                            nested.markerOnRail().connector(inner, 1);
                            for (int index = 1; index <= 2; index++) {
                                int n = index;
                                nested.entry(TimelineMarker.dot(6, DOT),
                                        step -> step.content(s -> s.addParagraph("Step " + n)));
                            }
                        })));
                outer.entry(TimelineMarker.dot(8, DOT), e -> e.title("Role 2"));
            }));
            DocxLayoutMetrics layout = DocxLayoutMetrics.of(session.documentGraph(), session.layoutGraph());
            assertThat(layout.railedTimelines()).as("the inner one alone").hasSize(1);
            AtomicReference<DocxExportReport> report = new AtomicReference<>();
            byte[] docx = session.export(new DocxSemanticBackend(report::set));
            try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(docx))) {
                String xml = document.getDocument().xmlText();
                assertThat(xml).as("the outer rail drawn").contains(RAIL_HEX + "\"/></a:solidFill>");
                assertThat(xml).as("the inner rail a border").doesNotContain("308030\"/></a:solidFill>")
                        .contains("w:color=\"308030\"");
            }
            assertThat(report.get().notes()).filteredOn(note -> note.subject().equals("timeline rail")
                    && note.detail().startsWith("drawn as a filled bar")).hasSize(1);
        }
    }

    @Test
    void twoRailsLeftDrawnInOneSectionAreNamedOnce() throws Exception {
        // Both rails stand beside the markers of a timeline in a card, so both are left drawn at the
        // end of the export; sharing the pass's path, they are named once, as drawn rails always were.
        try (DocumentSession session = GraphCompose.document().pageSize(400, 600).margin(DocumentInsets.of(30))
                .create()) {
            session.pageFlow(flow -> {
                for (String name : List.of("First", "Second")) {
                    flow.addSection(name, card -> card.fillColor(DocumentColor.rgb(238, 243, 249))
                            .addTimeline(t -> {
                                t.connector(RAIL, 1.5);
                                for (int index = 1; index <= 2; index++) {
                                    int n = index;
                                    t.entry(TimelineMarker.dot(8, DOT), e -> e.title("Role " + n).meta("2020"));
                                }
                            }));
                }
            });
            AtomicReference<DocxExportReport> report = new AtomicReference<>();
            session.export(new DocxSemanticBackend(report::set));
            assertThat(report.get().notes()).filteredOn(note -> note.subject().equals("timeline rail")
                    && note.detail().startsWith("drawn as a filled bar")).hasSize(1);
        }
    }

    @Test
    void aRailBesideTheMarkersOfATimelineInACellKeepsItsDrawnRail() throws Exception {
        // The gutter column would have to stand left of a table nested in a cell — here a painted
        // card's, written as a table of one cell — which stays in its cell whatever its indent: the
        // timeline is left as written, its rail drawn and named once.
        try (DocumentSession session = GraphCompose.document().pageSize(400, 600).margin(DocumentInsets.of(30))
                .create()) {
            session.pageFlow(flow -> flow.addSection("Card", card -> card
                    .fillColor(DocumentColor.rgb(238, 243, 249))
                    .addTimeline(t -> {
                        t.connector(RAIL, 1.5);
                        for (int index = 1; index <= 3; index++) {
                            int n = index;
                            t.entry(TimelineMarker.dot(8, DOT), e -> e.title("Role " + n).meta("2020"));
                        }
                    })));
            DocxLayoutMetrics layout = DocxLayoutMetrics.of(session.documentGraph(), session.layoutGraph());
            assertThat(layout.railedTimelines()).as("its entries are rows alone").hasSize(1);

            AtomicReference<DocxExportReport> report = new AtomicReference<>();
            byte[] docx = session.export(new DocxSemanticBackend(report::set));
            try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(docx))) {
                assertThat(document.getDocument().xmlText()).contains(RAIL_HEX + "\"/></a:solidFill>");
            }
            assertThat(report.get().notes()).filteredOn(note -> note.subject().equals("timeline rail")
                    && note.detail().startsWith("drawn as a filled bar")).as("named once, as drawn").hasSize(1);
            assertThat(report.get().notes()).filteredOn(note -> note.subject().equals("timeline rail")
                    && note.detail().startsWith("drawn behind the text")).as("drawn, the card's shading covers it")
                    .hasSize(1);
        }
    }

    @Test
    void aRailWrittenAsItsCellsBorderInAPaintedCardIsNotNamedCovered() throws Exception {
        // Through its markers, the rail of a timeline in a card is written as its cells' border, which
        // the card's shading does not cover as it covers a drawing behind the text.
        try (DocumentSession session = GraphCompose.document().pageSize(400, 600).margin(DocumentInsets.of(30))
                .create()) {
            session.pageFlow(flow -> flow.addSection("Card", card -> card
                    .fillColor(DocumentColor.rgb(238, 243, 249))
                    .addTimeline(t -> {
                        t.markerOnRail().connector(RAIL, 1.5);
                        for (int index = 1; index <= 3; index++) {
                            int n = index;
                            t.entry(TimelineMarker.dot(8, DOT), e -> e.title("Role " + n).meta("2020"));
                        }
                    })));
            Written written = export(session);
            assertThat(written.xml()).doesNotContain(RAIL_HEX + "\"/></a:solidFill>");
            assertThat(written.notes()).noneMatch(note -> note.subject().equals("timeline rail"));
        }
    }

    @Test
    void aBarPlacedFromNoCellOfItsFirstEntryLeavesTheTimelineAsItWas() throws Exception {
        try (XWPFDocument document = new XWPFDocument()) {
            XWPFTable upper = entryTable(document);
            gapParagraph(document);
            XWPFTable lower = entryTable(document);
            CTDrawing bar = document.createParagraph().createRun().getCTR().addNewDrawing();
            org.openxmlformats.schemas.drawingml.x2006.wordprocessingDrawing.CTPosH across =
                    bar.addNewAnchor().addNewPositionH();
            across.setRelativeFrom(org.openxmlformats.schemas.drawingml.x2006.wordprocessingDrawing.STRelFromH.COLUMN);
            across.setPosOffset(0);
            String before = document.getDocument().xmlText();

            assertThat(DocxTimelineRails.apply(plan(upper, lower, 0, 0, bar))).isFalse();
            assertThat(document.getDocument().xmlText()).isEqualTo(before);
        }
    }

    @Test
    void entriesWrittenAsTablesOfDifferentPropertiesAreLeftAsTheyWere() throws Exception {
        // The merged rows would take the first table's indent and margins, and the second entry
        // would no longer stand where it was written.
        try (XWPFDocument document = new XWPFDocument()) {
            XWPFTable upper = entryTable(document);
            gapParagraph(document);
            XWPFTable lower = entryTable(document);
            org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblWidth indent =
                    lower.getCTTbl().getTblPr().addNewTblInd();
            indent.setType(org.openxmlformats.schemas.wordprocessingml.x2006.main.STTblWidth.DXA);
            indent.setW(BigInteger.valueOf(240));
            CTDrawing bar = barInFirstCell(upper);
            String before = document.getDocument().xmlText();

            assertThat(DocxTimelineRails.apply(plan(upper, lower, 0, 0, bar))).isFalse();
            assertThat(document.getDocument().xmlText()).isEqualTo(before);
        }
    }

    @Test
    void aRailEndingOnAnotherPageThanItsLastEntryLeavesTheTimelineAsItWas() throws Exception {
        try (XWPFDocument document = new XWPFDocument()) {
            XWPFTable upper = entryTable(document);
            gapParagraph(document);
            XWPFTable lower = entryTable(document);
            CTDrawing bar = barInFirstCell(upper);
            String before = document.getDocument().xmlText();

            assertThat(DocxTimelineRails.apply(plan(upper, lower, 0, 1, bar))).isFalse();
            assertThat(document.getDocument().xmlText()).isEqualTo(before);
            assertThat(DocxTimelineRails.apply(plan(upper, lower, 0, 0, bar))).as("the same on one page")
                    .isTrue();
        }
    }

    @Test
    void theRailIsCutWhereItsBarWasWrittenNotWhereTheLayoutPutTheRow() throws Exception {
        try (XWPFDocument document = new XWPFDocument()) {
            XWPFTable upper = entryTable(document);
            XWPFParagraph gap = document.createParagraph();
            gap.getCTP().addNewPPr().addNewSpacing().setAfter(BigInteger.valueOf(200));
            gap.getCTP().getPPr().getSpacing().setLine(BigInteger.valueOf(2));
            gap.getCTP().getPPr().getSpacing().setLineRule(
                    org.openxmlformats.schemas.wordprocessingml.x2006.main.STLineSpacingRule.EXACT);
            XWPFTable lower = entryTable(document);
            // The bar stands 100 twips left of the second cell, which starts 400 twips in: its centre,
            // a 1pt bar's half further, is 310 twips in. The layout says 100.
            CTDrawing bar = upper.getRow(0).getCell(1).getParagraphs().get(0).createRun().getCTR().addNewDrawing();
            org.openxmlformats.schemas.drawingml.x2006.wordprocessingDrawing.CTPosH across =
                    bar.addNewAnchor().addNewPositionH();
            across.setRelativeFrom(org.openxmlformats.schemas.drawingml.x2006.wordprocessingDrawing.STRelFromH.COLUMN);
            across.setPosOffset(-100 * 635);

            DocxLayoutMetrics.RailedEntry entry = new DocxLayoutMetrics.RailedEntry(null, 0, 100, 500, 480);
            DocxLayoutMetrics.RailedEntry next = new DocxLayoutMetrics.RailedEntry(null, 0, 100, 470, 450);
            boolean written = DocxTimelineRails.apply(new DocxTimelineRails.Plan(List.of(upper, lower),
                    List.of(entry, next), 500, 0, 450, 0, Color.RED, 1, List.of(bar)));

            assertThat(written).isTrue();
            List<Integer> grid = new ArrayList<>();
            upper.getCTTbl().getTblGrid().getGridColList()
                    .forEach(column -> grid.add(((BigInteger) column.getW()).intValue()));
            assertThat(grid).containsExactly(310, 90, 4000);
        }
    }

    @Test
    void aTimelineWithAnythingButAGapBetweenItsEntriesIsLeftAsItWas() throws Exception {
        try (XWPFDocument document = new XWPFDocument()) {
            XWPFTable upper = entryTable(document);
            XWPFParagraph between = document.createParagraph();
            between.createRun().setText("Between the entries");
            XWPFTable lower = entryTable(document);
            // A bar placed from a cell of the first entry, as the export places it: only what stands
            // between the entries keeps this timeline as it was.
            CTDrawing bar = barInFirstCell(upper);
            String before = document.getDocument().xmlText();

            DocxLayoutMetrics.RailedEntry entry = new DocxLayoutMetrics.RailedEntry(null, 0, 100, 500, 480);
            DocxLayoutMetrics.RailedEntry next = new DocxLayoutMetrics.RailedEntry(null, 0, 100, 470, 450);
            boolean written = DocxTimelineRails.apply(new DocxTimelineRails.Plan(List.of(upper, lower),
                    List.of(entry, next), 500, 0, 450, 0, Color.RED, 1, List.of(bar)));

            assertThat(written).isFalse();
            assertThat(document.getDocument().xmlText()).isEqualTo(before);
        }
    }

    // ---- fixtures and readings

    private record Written(XWPFDocument document, String xml, List<DocxExportReport.Note> notes,
                           DocxLayoutMetrics.RailedTimeline timeline) {
    }

    private static Written export(DocumentSession session) throws Exception {
        DocxLayoutMetrics layout = DocxLayoutMetrics.of(session.documentGraph(), session.layoutGraph());
        assertThat(layout.railedTimelines()).hasSize(1);
        AtomicReference<DocxExportReport> report = new AtomicReference<>();
        byte[] docx = session.export(new DocxSemanticBackend(report::set));
        XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(docx));
        return new Written(document, document.getDocument().xmlText(), report.get().notes(),
                layout.railedTimelines().get(0));
    }

    /** Content in the row alone, as the templates write their entries: no body below it. */
    private static DocumentSession timeline(boolean onTheRail, TimelineRailEnd from, TimelineRailEnd to,
                                            int entries, double pageHeight) {
        DocumentSession session = GraphCompose.document().pageSize(400, pageHeight).margin(DocumentInsets.of(30))
                .create();
        session.pageFlow(flow -> {
            flow.addParagraph("Experience");
            flow.addTimeline(t -> {
                if (onTheRail) {
                    t.markerOnRail();
                }
                t.rail(rail -> rail.stroke(DocumentStroke.of(RAIL, 1.5)).from(from).to(to));
                for (int index = 1; index <= entries; index++) {
                    int n = index;
                    t.entry(TimelineMarker.dot(8, DOT), e -> e.content(section -> section
                            .addParagraph("Role " + n)
                            .addParagraph("Built the export pipeline and its tests, entry " + n + ".")));
                }
            });
            flow.addParagraph("After the timeline.");
        });
        return session;
    }

    /** Three content-only entries through their markers, the rail of a colour. */
    private static DocumentSession timelineOfColour(DocumentColor colour) {
        DocumentSession session = GraphCompose.document().pageSize(400, 600).margin(DocumentInsets.of(30)).create();
        session.pageFlow(flow -> flow.addTimeline(t -> {
            t.markerOnRail().connector(colour, 1.5);
            for (int index = 1; index <= 3; index++) {
                int n = index;
                t.entry(TimelineMarker.dot(8, DOT), e -> e.content(section -> section.addParagraph("Role " + n)));
            }
        }));
        return session;
    }

    /** An empty paragraph one exact line tall with space after it, as the export keeps two entries apart. */
    private static void gapParagraph(XWPFDocument document) {
        XWPFParagraph gap = document.createParagraph();
        gap.getCTP().addNewPPr().addNewSpacing().setAfter(BigInteger.valueOf(200));
        gap.getCTP().getPPr().getSpacing().setLine(BigInteger.valueOf(2));
        gap.getCTP().getPPr().getSpacing().setLineRule(
                org.openxmlformats.schemas.wordprocessingml.x2006.main.STLineSpacingRule.EXACT);
    }

    /** A bar placed 100 twips into the first entry's second cell. */
    private static CTDrawing barInFirstCell(XWPFTable entry) {
        CTDrawing bar = entry.getRow(0).getCell(1).getParagraphs().get(0).createRun().getCTR().addNewDrawing();
        org.openxmlformats.schemas.drawingml.x2006.wordprocessingDrawing.CTPosH across =
                bar.addNewAnchor().addNewPositionH();
        across.setRelativeFrom(org.openxmlformats.schemas.drawingml.x2006.wordprocessingDrawing.STRelFromH.COLUMN);
        across.setPosOffset(100 * 635);
        return bar;
    }

    /** Two entries from the top of their pages, the rail from the first's top to the second's foot. */
    private static DocxTimelineRails.Plan plan(XWPFTable upper, XWPFTable lower, int topPage, int bottomPage,
                                               CTDrawing bar) {
        DocxLayoutMetrics.RailedEntry entry = new DocxLayoutMetrics.RailedEntry(null, 0, 100, 500, 480);
        DocxLayoutMetrics.RailedEntry next = new DocxLayoutMetrics.RailedEntry(null, 0, 100, 470, 450);
        return new DocxTimelineRails.Plan(List.of(upper, lower), List.of(entry, next), 500, topPage, 450,
                bottomPage, Color.RED, 1, List.of(bar));
    }

    private static XWPFTable entryTable(XWPFDocument document) {
        XWPFTable table = document.createTable(1, 2);
        if (table.getCTTbl().getTblGrid() == null) {
            table.getCTTbl().addNewTblGrid();
        }
        while (table.getCTTbl().getTblGrid().sizeOfGridColArray() > 0) {
            table.getCTTbl().getTblGrid().removeGridCol(0);
        }
        int[] widths = {400, 4000};
        for (int index = 0; index < widths.length; index++) {
            table.getCTTbl().getTblGrid().addNewGridCol().setW(BigInteger.valueOf(widths[index]));
            org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTcPr properties =
                    table.getRow(0).getCell(index).getCTTc().isSetTcPr()
                            ? table.getRow(0).getCell(index).getCTTc().getTcPr()
                            : table.getRow(0).getCell(index).getCTTc().addNewTcPr();
            org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblWidth width =
                    properties.isSetTcW() ? properties.getTcW() : properties.addNewTcW();
            width.setType(org.openxmlformats.schemas.wordprocessingml.x2006.main.STTblWidth.DXA);
            width.setW(BigInteger.valueOf(widths[index]));
        }
        return table;
    }

    /**
     * The index of the cell whose left edge stands where the rail does, as the grid says it: within
     * two twips, the bar's offset in its cell being written in EMU and the grid in twips.
     */
    private static int railCell(XWPFTable table, PlacedFragment rail, DocxLayoutMetrics.RailedEntry entry) {
        int offset = twips(rail.x() - entry.left());
        int edge = 0;
        List<Integer> edges = new ArrayList<>();
        List<?> columns = table.getCTTbl().getTblGrid().getGridColList();
        for (int index = 0; index < columns.size(); index++) {
            edges.add(edge);
            if (Math.abs(edge - offset) <= 2) {
                return index;
            }
            edge += ((BigInteger) table.getCTTbl().getTblGrid().getGridColArray(index).getW()).intValue();
        }
        throw new AssertionError("no column starts at the rail, " + offset + " twips in: " + edges);
    }

    private static List<Boolean> bordered(XWPFTable table, int cell) {
        List<Boolean> rows = new ArrayList<>();
        for (XWPFTableRow row : table.getRows()) {
            CTTcPr properties = row.getCell(cell).getCTTc().getTcPr();
            CTBorder left = properties.isSetTcBorders() && properties.getTcBorders().isSetLeft()
                    ? properties.getTcBorders().getLeft() : null;
            rows.add(left != null && left.getVal() == org.openxmlformats.schemas.wordprocessingml.x2006.main.STBorder.SINGLE
                     && RAIL_HEX.equals(left.xgetColor().getStringValue())
                     && BigInteger.valueOf(12).equals(left.getSz()));
        }
        return rows;
    }

    private static Integer exactHeight(XWPFTableRow row) {
        CTTrPr properties = row.getCtRow().getTrPr();
        if (properties == null || properties.sizeOfTrHeightArray() == 0
            || properties.getTrHeightArray(0).getHRule() != STHeightRule.EXACT) {
            return null;
        }
        return ((BigInteger) properties.getTrHeightArray(0).getVal()).intValue();
    }

    /** Whether each paragraph of a row's cells keeps with the next. */
    private static List<Boolean> keptWithTheNext(XWPFTableRow row) {
        List<Boolean> kept = new ArrayList<>();
        for (XWPFTableCell cell : row.getTableCells()) {
            for (XWPFParagraph paragraph : cell.getParagraphs()) {
                kept.add(paragraph.getCTP().isSetPPr() && paragraph.getCTP().getPPr().isSetKeepNext());
            }
        }
        return kept;
    }

    /** The space above the paragraph carrying a row's marker, in twips. */
    private static int markerSetDown(XWPFTableRow row, int cell) {
        XWPFParagraph carrier = row.getCell(cell).getParagraphs().get(0);
        Object before = carrier.getCTP().getPPr().getSpacing().getBefore();
        return before == null ? 0 : ((BigInteger) before).intValue();
    }

    private static String vMerge(XWPFTableCell cell) {
        CTTcPr properties = cell.getCTTc().getTcPr();
        if (!properties.isSetVMerge()) {
            return null;
        }
        return properties.getVMerge().isSetVal() ? properties.getVMerge().getVal().toString() : "continue";
    }

    private static String text(XWPFTableRow row) {
        StringBuilder text = new StringBuilder();
        for (XWPFTableCell cell : row.getTableCells()) {
            text.append(cell.getText());
        }
        return text.toString().trim();
    }

    /** The body's blocks: a paragraph's text where it has any, a table as TABLE. */
    private static List<String> bodyOutline(XWPFDocument document) {
        List<String> outline = new ArrayList<>();
        for (org.apache.poi.xwpf.usermodel.IBodyElement element : document.getBodyElements()) {
            if (element instanceof XWPFTable) {
                outline.add("TABLE");
            } else if (element instanceof XWPFParagraph paragraph && !paragraph.getText().isBlank()) {
                outline.add(paragraph.getText());
            }
        }
        return outline;
    }

    private static int twips(double points) {
        return (int) Math.round(points * 20);
    }
}
