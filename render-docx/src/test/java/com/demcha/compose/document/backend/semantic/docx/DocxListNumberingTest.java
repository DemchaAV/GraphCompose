package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.node.ListItem;
import com.demcha.compose.document.node.ListMarker;
import com.demcha.compose.document.node.ListNode;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.style.DocumentInsets;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.math.BigInteger;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A list exports as a list Word owns, not as a marker typed into the text.
 *
 * <p>The difference is invisible until somebody edits the file: a marker written into
 * the run text looks right and then produces a blank paragraph on Enter instead of the
 * next item. Measured in Word 16.0 against the old export, {@code ListFormat.ListType}
 * came back as "no numbering" — the reader had bulleted-looking paragraphs, not a
 * list.</p>
 *
 * <p>Numbering is only attached where Word can express what was authored, which is why
 * several kinds of list deliberately keep the older text form.</p>
 *
 * @author Artem Demchyshyn
 */
class DocxListNumberingTest {

    @Test
    void aBulletListShouldBecomeAWordListWithTheAuthoredMarker() throws Exception {
        try (XWPFDocument document = export(flow -> flow
                .addList(list -> list.name("Flat").bullet().items("Java", "SQL")))) {

            List<XWPFParagraph> items = items(document);
            assertThat(items).hasSize(2);
            for (XWPFParagraph item : items) {
                assertThat(item.getNumID()).as("the paragraph belongs to a list").isNotNull();
                assertThat(item.getNumIlvl()).isEqualTo(BigInteger.ZERO);
            }
            // Word draws the marker now, so it must not also be in the text.
            assertThat(items.get(0).getText()).isEqualTo("Java");
            assertThat(items.get(1).getText()).isEqualTo("SQL");
            assertThat(document.getNumbering()).as("a numbering part exists").isNotNull();
        }
    }

    @Test
    void nestingShouldUseLevelsRatherThanSpaces() throws Exception {
        try (XWPFDocument document = export(flow -> flow
                .addList(list -> list.name("Outline")
                        .addItem("alpha", l1 -> l1
                                .addItem("beta", l2 -> l2
                                        .addItem("gamma")))))) {

            List<XWPFParagraph> items = items(document);
            assertThat(items).hasSize(3);
            assertThat(items.get(0).getNumIlvl()).isEqualTo(BigInteger.ZERO);
            assertThat(items.get(1).getNumIlvl()).isEqualTo(BigInteger.ONE);
            assertThat(items.get(2).getNumIlvl()).isEqualTo(BigInteger.TWO);
            // The indent is a level, so no padding characters reach the text at all.
            assertThat(items.get(1).getText()).isEqualTo("beta");
            assertThat(items.get(2).getText()).isEqualTo("gamma");
        }
    }

    @Test
    void everyDepthShouldKeepTheMarkerItWasAuthoredWith() throws Exception {
        try (XWPFDocument document = export(flow -> flow
                .addList(list -> list.name("Outline")
                        .addItem("alpha", l1 -> l1.addItem("beta"))))) {

            var levels = document.getNumbering()
                    .getAbstractNum(BigInteger.ZERO).getAbstractNum().getLvlList();
            assertThat(levels).hasSize(2);
            assertThat(levels.get(0).getLvlText().getVal()).isEqualTo("\u2022");
            assertThat(levels.get(1).getLvlText().getVal())
                    .as("the depth cascade the export already used, now as list levels")
                    .isEqualTo("\u25E6");
        }
    }

    @Test
    void aCustomMarkerShouldBecomeTheLevelText() throws Exception {
        try (XWPFDocument document = export(flow -> flow
                .addList(list -> list.name("Ticks").marker(ListMarker.custom("\u2713"))
                        .items("done")))) {

            var level = document.getNumbering()
                    .getAbstractNum(BigInteger.ZERO).getAbstractNum().getLvlArray(0);
            assertThat(level.getLvlText().getVal()).isEqualTo("\u2713");
            assertThat(items(document).get(0).getText()).isEqualTo("done");
        }
    }

    @Test
    void aMarkerlessListShouldStayPlainParagraphs() throws Exception {
        // Numbering always draws something and indents; a list that asked for no marker
        // would gain both. Nothing is gained by making it a list, so it is not one.
        try (XWPFDocument document = export(flow -> flow
                .addList(list -> list.name("Bare").noMarker().items("alpha", "beta")))) {

            assertThat(document.getNumbering()).isNull();
            assertThat(items(document).get(0).getNumID()).isNull();
        }
    }

    @Test
    void aPerDepthMarkerShouldBecomeThatLevelsText() throws Exception {
        try (XWPFDocument document = export(flow -> flow
                .addList(list -> list.name("Outline")
                        .markerFor(1, ListMarker.custom("\u2013"))
                        .addItem("alpha", l1 -> l1.addItem("beta"))))) {

            var levels = document.getNumbering()
                    .getAbstractNum(BigInteger.ZERO).getAbstractNum().getLvlList();
            assertThat(levels.get(1).getLvlText().getVal())
                    .as("markerFor(depth) chooses that level's text")
                    .isEqualTo("\u2013");
        }
    }

    @Test
    void aListWhoseSiblingsDisagreeOnTheMarkerShouldStayText() throws Exception {
        // A Word list definition names one marker per level. Two different markers at the
        // same depth cannot both be it, so the whole list keeps writing its markers as
        // text rather than silently having one of them replaced by the other. The list
        // DSL assigns markers per depth and cannot author this; ListNode is public API,
        // so a caller can hand the exporter exactly this shape.
        ListNode mixed = new ListNode("Mixed", List.of(),
                List.of(new ListItem("alpha", ListMarker.custom("\u2713"), List.of()),
                        new ListItem("beta", ListMarker.custom("\u2717"), List.of())),
                ListMarker.bullet(), DocumentTextStyle.DEFAULT, TextAlign.LEFT,
                0, 0, "", true, DocumentInsets.zero(), DocumentInsets.zero());

        try (XWPFDocument document = export(flow -> flow.add(mixed))) {
            assertThat(document.getNumbering()).isNull();
            List<XWPFParagraph> items = items(document);
            assertThat(items.get(0).getText()).isEqualTo("\u2713 alpha");
            assertThat(items.get(1).getText()).isEqualTo("\u2717 beta");
        }
    }

    @Test
    void aDrawnMarkerShouldStayOnTheRunPathItAlreadyUsed() throws Exception {
        // A marker made of runs has no Word list analogue; the rich path writes what it
        // can and warns about what it cannot, and numbering must not hide that.
        try (XWPFDocument document = export(flow -> flow
                .addList(list -> list.name("Drawn")
                        .marker(ListMarker.ofRuns(List.of()))
                        .items("alpha")))) {

            assertThat(document.getNumbering()).isNull();
        }
    }

    private static List<XWPFParagraph> items(XWPFDocument document) {
        return document.getParagraphs().stream()
                .filter(p -> !p.getText().isBlank())
                .toList();
    }

    private static XWPFDocument export(
            Consumer<com.demcha.compose.document.dsl.PageFlowBuilder> author) throws Exception {
        byte[] docx;
        try (DocumentSession session = GraphCompose.document()
                .pageSize(595, 842)
                .margin(DocumentInsets.of(36))
                .create()) {
            var flow = session.dsl().pageFlow().name("Flow");
            author.accept(flow);
            flow.build();
            docx = session.export(new DocxSemanticBackend());
        }
        return new XWPFDocument(new ByteArrayInputStream(docx));
    }
}
