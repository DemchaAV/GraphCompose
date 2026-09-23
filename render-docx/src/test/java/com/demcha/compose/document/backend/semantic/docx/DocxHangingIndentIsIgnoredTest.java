package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.ListBuilder;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.style.DocumentInsets;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.junit.jupiter.api.Test;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPPr;

import java.io.ByteArrayInputStream;
import java.math.BigInteger;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@code hangingIndent} is fixed-layout geometry, and the semantic DOCX export does not
 * lay text out — so it exports a list exactly the same way whether the flag is set or
 * not.
 *
 * <p>That is a decision rather than an omission, and it was made against measurements.
 * Word places content at absolute indents and has no way to be told "start the text one
 * marker width plus a gap from here"; every mechanism that looks like it would (a hanging
 * indent, a hanging indent with a tab stop, real Word numbering) positions content
 * absolutely, so the distance left beside the marker is always the column minus the
 * marker's own width — a number only Word knows. Reproducing the geometry would mean
 * measuring the marker, and the semantic backend has no font runtime to measure with, by
 * design: its only dependencies are the core model and POI.</p>
 *
 * <p>The approximations were built and rendered through Word before being rejected. A
 * reserved-column approximation renders a gap that is not the configured one — a 0pt gap
 * came out as 5.72pt, a 4pt gap as 9.68pt — and a marker wider than the column misaligns
 * outright. Shipping that would mean {@code markerGap(8)} rendering as something other
 * than 8.</p>
 *
 * <p>A list <em>is</em> real Word numbering now, which the old wording of this test read
 * as ruling out. It does not: numbering was rejected as a way to honour {@code markerGap},
 * and it still does not honour it. What it buys is behaviour — Enter continues the list —
 * at the price of a marker column that is a stated constant rather than the configured
 * gap. So the decision this test pins is unchanged and its subject is narrower than it
 * was: not "no numbering", but "the flag and the gap change nothing about the output".</p>
 *
 * @author Artem Demchyshyn
 */
class DocxHangingIndentIsIgnoredTest {

    @Test
    void aFlatListExportsIdenticallyWithAndWithoutHangingIndent() throws Exception {
        Consumer<ListBuilder> shape = list -> list.name("Flat").bullet()
                .items("Alpha", "Beta", "Gamma");

        assertThat(listTexts(shape.andThen(l -> l.hangingIndent(true).markerGap(16))))
                .isEqualTo(listTexts(shape))
                .containsExactly("Alpha", "Beta", "Gamma");
    }

    @Test
    void aMultiCharacterMarkerExportsIdenticallyAndBecomesTheLevelText() throws Exception {
        Consumer<ListBuilder> shape = list -> list.name("Wide").marker("=>").items("Alpha");

        assertThat(listTexts(shape.andThen(l -> l.hangingIndent(true).markerGap(8))))
                .isEqualTo(listTexts(shape))
                .containsExactly("Alpha");
        assertThat(markerPerDepth(shape.andThen(l -> l.hangingIndent(true).markerGap(8))))
                .as("a wide marker is the level's text, whatever the flag says")
                .containsExactly("=>");
    }

    @Test
    void theGapIsNotRepresentedAtAllSoEveryValueExportsTheSame() throws Exception {
        // If the gap ever leaked into DOCX — as spaces, as indentation, or as the
        // level's own indent — these would stop agreeing, which is the failure this
        // test exists to catch.
        List<String> zeroText = listTexts(l -> l.bullet().hangingIndent(true).markerGap(0).items("Alpha"));
        List<Integer> zeroIndent = levelIndents(l -> l.bullet().hangingIndent(true).markerGap(0).items("Alpha"));
        for (double gap : List.of(4.0, 8.0, 16.0)) {
            assertThat(listTexts(l -> l.bullet().hangingIndent(true).markerGap(gap).items("Alpha")))
                    .as("text at gap %s", gap)
                    .isEqualTo(zeroText);
            assertThat(levelIndents(l -> l.bullet().hangingIndent(true).markerGap(gap).items("Alpha")))
                    .as("level indent at gap %s", gap)
                    .isEqualTo(zeroIndent);
        }
        assertThat(zeroText).containsExactly("Alpha");
    }

    @Test
    void aLongItemStaysOneParagraphWithEveryCharacterOfItsText() throws Exception {
        String text = "Long item text that the PDF backend wraps across several visual lines "
                      + "and that Word wraps for itself, so no character of it may be lost here.";

        List<String> texts = listTexts(l -> l.bullet().hangingIndent(true).items(text));
        assertThat(texts).hasSize(1);
        assertThat(texts.get(0)).isEqualTo(text);
    }

    @Test
    void nestedListsKeepTheirCascadeWhicheverWayTheFlagIsSet() throws Exception {
        Consumer<ListBuilder> shape = list -> list.name("Outline")
                .addItem("alpha", l1 -> l1.addItem("beta", l2 -> l2.addItem("gamma")));

        assertThat(listTexts(shape.andThen(l -> l.hangingIndent(true).markerGap(12))))
                .isEqualTo(listTexts(shape))
                .containsExactly("alpha", "beta", "gamma");
        assertThat(markerPerDepth(shape.andThen(l -> l.hangingIndent(true).markerGap(12))))
                .isEqualTo(markerPerDepth(shape))
                .containsExactly("•", "◦", "▪");
    }

    @Test
    void aMarkerlessListAndAMarkerOnlyRowBothSurviveUnchanged() throws Exception {
        // A list that asked for no marker gains nothing from numbering and would gain an
        // indent it did not ask for, so it stays plain paragraphs — with or without the
        // flag.
        assertThat(listTexts(l -> l.noMarker().hangingIndent(true).markerGap(16).items("Alpha")))
                .containsExactly("Alpha");
        try (XWPFDocument document = export(flow -> flow.addList(
                l -> l.noMarker().hangingIndent(true).markerGap(16).items("Alpha")))) {
            assertThat(document.getNumbering())
                    .as("opting into the geometry does not turn a markerless list into a "
                        + "numbered one, which would add the very indent it declined")
                    .isNull();
        }

        // The flat path drops a blank item whatever its marker — that is the
        // legacy rule, and opting in does not change the DOCX side of it.
        assertThat(listTexts(l -> l.bullet().hangingIndent(true).items("Alpha", "   ", "Beta")))
                .containsExactly("Alpha", "Beta");

        // A nested parent with an empty label still gets its own row: the marker is the
        // level's now, so the row carries no text and is found by its w:numPr instead.
        try (XWPFDocument document = export(flow -> flow.addList(l -> l.hangingIndent(true)
                .addItem("", c -> c.addItem("Child"))))) {
            List<XWPFParagraph> items = document.getParagraphs().stream()
                    .filter(p -> p.getCTP().getPPr() != null && p.getCTP().getPPr().isSetNumPr())
                    .toList();
            assertThat(items).hasSize(2);
            assertThat(items.get(0).getText()).isEmpty();
            assertThat(items.get(0).getNumIlvl()).isEqualTo(BigInteger.ZERO);
            assertThat(items.get(1).getText()).isEqualTo("Child");
            assertThat(items.get(1).getNumIlvl()).isEqualTo(BigInteger.ONE);
        }
    }

    @Test
    void theSameNumberingIsWrittenEitherWayAndNothingIsApproximatedOnTheParagraph() throws Exception {
        try (XWPFDocument withFlag = export(flow -> flow.addList(list -> list
                .name("Flat").bullet().hangingIndent(true).markerGap(16)
                .items("Alpha", "Beta")));
             XWPFDocument without = export(flow -> flow.addList(list -> list
                     .name("Flat").bullet()
                     .items("Alpha", "Beta")))) {

            List<XWPFParagraph> paragraphs = items(withFlag);
            assertThat(paragraphs).hasSize(2);
            for (XWPFParagraph paragraph : paragraphs) {
                CTPPr properties = paragraph.getCTP().getPPr();
                assertThat(properties.isSetNumPr()).as("the item belongs to a list").isTrue();
                assertThat(properties.isSetInd())
                        .as("no w:ind on the paragraph — the geometry is the level's, "
                            + "and it is not approximated from the gap")
                        .isFalse();
                assertThat(properties.isSetTabs()).as("no tab stops").isFalse();
                assertThat(paragraph.getRuns()).as("one run, as before").hasSize(1);
            }
            assertThat(indentsOf(withFlag))
                    .as("the flag changes no part of the list definition")
                    .isEqualTo(indentsOf(without));
        }
    }

    @Test
    void aMarkerGapNeverBecomesSpacesInTheRunText() throws Exception {
        // The specific failure worth naming: a gap smuggled in as padding would
        // still "look right" in a viewer and be wrong in the file.
        List<String> texts = listTexts(l -> l.bullet().hangingIndent(true).markerGap(16).items("Alpha"));
        assertThat(texts.get(0))
                .as("no padding run, no non-breaking spaces, no tab")
                .isEqualTo("Alpha")
                .doesNotContain("  ")
                .doesNotContain(" ")
                .doesNotContain("\t");
    }

    // ------------------------------------------------------------------

    private static List<XWPFParagraph> items(XWPFDocument document) {
        return document.getParagraphs().stream()
                .filter(p -> !p.getText().isBlank())
                .toList();
    }

    private static List<String> listTexts(Consumer<ListBuilder> spec) throws Exception {
        try (XWPFDocument document = export(flow -> flow.addList(spec))) {
            return document.getParagraphs().stream()
                    .map(XWPFParagraph::getText)
                    .filter(text -> !text.isEmpty())
                    .toList();
        }
    }

    private static List<String> markerPerDepth(Consumer<ListBuilder> spec) throws Exception {
        try (XWPFDocument document = export(flow -> flow.addList(spec))) {
            return document.getNumbering()
                    .getAbstractNum(BigInteger.ZERO).getAbstractNum().getLvlList().stream()
                    .map(level -> level.getLvlText().getVal())
                    .toList();
        }
    }

    private static List<Integer> levelIndents(Consumer<ListBuilder> spec) throws Exception {
        try (XWPFDocument document = export(flow -> flow.addList(spec))) {
            return indentsOf(document);
        }
    }

    /** Left and hanging of every level, flattened, so two definitions compare as one list. */
    private static List<Integer> indentsOf(XWPFDocument document) {
        return document.getNumbering()
                .getAbstractNum(BigInteger.ZERO).getAbstractNum().getLvlList().stream()
                .flatMap(level -> List.of(twips(level.getPPr().getInd().getLeft()),
                        twips(level.getPPr().getInd().getHanging())).stream())
                .toList();
    }

    /**
     * Reads a twip measure back as a number.
     *
     * <p>{@code ST_SignedTwipsMeasure} is an xmlbeans union, so the accessor is typed
     * {@code Object} and hands back whichever member matched.</p>
     */
    private static int twips(Object measure) {
        return (int) DocxTwips.of(measure);
    }

    private static XWPFDocument export(Consumer<PageFlowBuilder> author) throws Exception {
        byte[] docxBytes;
        try (DocumentSession session = GraphCompose.document()
                .pageSize(595, 842)
                .margin(DocumentInsets.of(36))
                .create()) {
            PageFlowBuilder flow = session.dsl().pageFlow().name("Flow");
            author.accept(flow);
            flow.build();
            docxBytes = session.export(new DocxSemanticBackend());
        }
        return new XWPFDocument(new ByteArrayInputStream(docxBytes));
    }
}
