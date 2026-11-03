package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.dsl.ShapeBuilder;
import com.demcha.compose.document.style.DocumentColor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.junit.jupiter.api.Test;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTBorder;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STBorder;

import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A plate, a progress bar and a divider: boxes of one colour each, no taller than Word's thickest
 * border, and only the divider written as Word's rule.
 *
 * <p>A 10pt plate and a 6pt bar are no taller than Word's thickest border, and the export used to
 * write such a box standing in the flow as a rule: an empty paragraph bordered below. Since the
 * divider signal ({@code ShapeNode.divider}) only a divider is; the plate is drawn, as the bar's two
 * layers in their layer stack always were, each at its own size, where the page puts it and in its
 * own colour.</p>
 *
 * <p><strong>What these tests check, and what they do not.</strong> CI runs no Word, so these
 * tests hold the record an editor draws from — each drawing's size, its distance from the page's
 * left edge, the paragraph it is anchored to and its offset there, its colour and its paint order,
 * and the divider's border and indents — and not a picture. The picture was taken once, on this
 * document exported through a session and converted to PDF by Word 16.0.20527 and LibreOffice
 * 24.2, against the engine's own PDF of the same session:</p>
 *
 * <ul>
 *   <li>the plate, the bar's track and its fill stood at the page's place and size to within
 *       0.01pt in both editors, each in its exact colour, the fill over the track;</li>
 *   <li>Word drew the divider as its rule, from 20.04pt to 380.16pt across a column the page
 *       rules from 20pt to 380pt, 0.96pt thick at 75.02pt where the page draws it 1pt thick at
 *       74.85pt — within 0.17pt; LibreOffice, which draws a paragraph's border between its
 *       indents rather than past them, ruled it from 21.40pt to 378.45pt, within 0.05pt of the
 *       indents (see {@code writeRule}).</li>
 * </ul>
 *
 * <p>The values asserted here are the ones that reading was taken on, held exactly. A change that
 * moves any of them moves what Word draws, and the reading above no longer stands for it: take it
 * again in Word before changing a number here.</p>
 *
 * @author Artem Demchyshyn
 */
class DocxPlateBarAndDividerTest {

    private static final DocumentColor PLATE = DocumentColor.rgb(0x1A, 0x56, 0x94);
    private static final DocumentColor TRACK = DocumentColor.rgb(0xD9, 0xDE, 0xE5);
    private static final DocumentColor FILL = DocumentColor.rgb(0x2E, 0x9E, 0x5B);
    private static final DocumentColor INK = DocumentColor.rgb(0x33, 0x33, 0x33);

    private static final double PAGE_WIDTH = 400;
    private static final double PAGE_HEIGHT = 600;
    private static final double MARGIN = 20;

    /** English Metric Units in a point. */
    private static final long EMU = 12_700;

    /**
     * The plate's offset from the top of the paragraph it is anchored in, in EMU, as the measured
     * file wrote it: under a hundredth of a point.
     */
    private static final long PLATE_FROM_ITS_PARAGRAPH = 102;

    /** The bar's offset from the top of its paragraph, in EMU, as the measured file wrote it: 0.31pt. */
    private static final long BAR_FROM_ITS_PARAGRAPH = 3_912;

    /**
     * The divider's left indent, in twips, as the measured file wrote it: 1.4pt, which Word draws
     * its border past to start it at 20.04pt, 0.04pt inside the column's edge.
     */
    private static final long RULE_INDENT_LEFT = 28;

    /**
     * The divider's right indent, in twips: 1.5pt, which Word draws past to end it at 380.16pt,
     * 0.16pt past the column's edge.
     */
    private static final long RULE_INDENT_RIGHT = 30;

    private static final Consumer<PageFlowBuilder> CONTENT = page -> page
            .addParagraph("Status")
            .addShape(shape -> shape.name("Plate").size(10, 10).fillColor(PLATE))
            .addParagraph("Progress")
            .addLayerStack(stack -> stack.name("Bar")
                    .back(new ShapeBuilder().name("Track").size(200, 6).fillColor(TRACK).build())
                    .topLeft(new ShapeBuilder().name("Fill").size(120, 6).fillColor(FILL).build()))
            .addParagraph("Rule below")
            .addDivider(divider -> divider.name("Rule").width(PAGE_WIDTH - 2 * MARGIN).thickness(1).color(INK))
            .addParagraph("End");

    @Test
    void thePlateIsDrawnAtItsOwnSizePlaceAndColour() throws Exception {
        try (XWPFDocument document = export()) {
            List<Anchor> anchors = anchorsIn(document, "Progress");
            assertThat(anchors).as("the plate, alone in the paragraph after it").hasSize(1);
            Anchor plate = anchors.get(0);

            assertThat(plate.geometry()).as("a box, not a rule").isEqualTo("rect");
            assertThat(plate.width()).as("its width").isEqualTo(10 * EMU);
            assertThat(plate.height()).as("its height").isEqualTo(10 * EMU);
            assertThat(plate.fromLeftOfThePage()).as("at the page's left margin").isEqualTo(Math.round(MARGIN * EMU));
            assertThat(plate.verticalFrom()).as("moving with the paragraph that holds it").isEqualTo("paragraph");
            assertThat(plate.fromTheParagraph()).as("at its top").isEqualTo(PLATE_FROM_ITS_PARAGRAPH);
            assertThat(plate.colour()).isEqualTo("1A5694");
        }
    }

    @Test
    void theBarsTrackAndFillAreDrawnFromOneOriginWithTheFillOverTheTrack() throws Exception {
        try (XWPFDocument document = export()) {
            List<Anchor> bar = anchorsIn(document, "Rule below");
            assertThat(bar).as("the track and its fill, both drawn").hasSize(2);
            Anchor track = bar.get(0);
            Anchor fill = bar.get(1);

            assertThat(track.colour()).isEqualTo("D9DEE5");
            assertThat(track.width()).isEqualTo(200 * EMU);
            assertThat(track.height()).isEqualTo(6 * EMU);
            assertThat(fill.colour()).isEqualTo("2E9E5B");
            assertThat(fill.width()).isEqualTo(120 * EMU);
            assertThat(fill.height()).isEqualTo(6 * EMU);

            assertThat(List.of(track.geometry(), fill.geometry())).containsOnly("rect");
            assertThat(List.of(track.fromLeftOfThePage(), fill.fromLeftOfThePage()))
                    .as("both start at the page's left margin").containsOnly(Math.round(MARGIN * EMU));
            assertThat(fill.verticalFrom()).isEqualTo(track.verticalFrom()).isEqualTo("paragraph");
            assertThat(List.of(track.fromTheParagraph(), fill.fromTheParagraph()))
                    .as("one bar, not two one under the other").containsOnly(BAR_FROM_ITS_PARAGRAPH);
            // Word stacks a drawing behind the text under every one in front of it, and orders by
            // relativeHeight only within each: the fill is over the track when both are on one side
            // and the fill stands higher there.
            assertThat(fill.behindTheText()).as("on the same side of the text as the track")
                    .isEqualTo(track.behindTheText());
            assertThat(fill.stackOrder()).as("the fill painted over the track, as the page paints it")
                    .isGreaterThan(track.stackOrder());
        }
    }

    @Test
    void theDividerStaysWordsRuleAcrossTheColumn() throws Exception {
        try (XWPFDocument document = export()) {
            List<XWPFParagraph> ruled = document.getParagraphs().stream()
                    .filter(paragraph -> bordered(paragraph.getCTP().getPPr()))
                    .toList();
            assertThat(ruled).as("one bordered paragraph, the divider's; the plate and the bar are none")
                    .hasSize(1);
            XWPFParagraph rule = ruled.get(0);
            assertThat(rule.getText()).as("an empty paragraph").isEmpty();
            assertThat(rule.getCTP().xmlText()).as("holding no drawing").doesNotContain("<wp:anchor");
            int at = document.getParagraphs().indexOf(rule);
            assertThat(document.getParagraphs().get(at - 1).getText()).isEqualTo("Rule below");
            assertThat(document.getParagraphs().get(at + 1).getText()).isEqualTo("End");

            CTPPr properties = rule.getCTP().getPPr();
            CTBorder bottom = properties.getPBdr().getBottom();
            assertThat(bottom.getVal()).isEqualTo(STBorder.SINGLE);
            assertThat(bottom.getSz().intValue()).as("1pt, in eighths of a point").isEqualTo(8);
            assertThat(hex(bottom.getColor())).isEqualTo("333333");
            // Word draws a paragraph's border past its indents: these are the indents its ends were
            // measured at the column's edges from.
            assertThat(DocxTwips.of(properties.getInd().getLeft())).as("the left indent Word starts it past")
                    .isEqualTo(RULE_INDENT_LEFT);
            assertThat(DocxTwips.of(properties.getInd().getRight())).as("the right indent Word ends it past")
                    .isEqualTo(RULE_INDENT_RIGHT);
        }
    }

    @Test
    void theReportNamesTheFormEachIsWrittenIn() throws Exception {
        DocxExportReport report = DocxExports.report(PAGE_WIDTH, PAGE_HEIGHT, MARGIN, CONTENT);

        assertThat(report.count(DocxExportReport.Severity.DROPPED)).as("nothing of it lost").isZero();
        assertThat(detailAt(report, "Plate")).startsWith("drawn as a shape where the layout puts it");
        assertThat(detailAt(report, "Track")).startsWith("drawn as a shape where the layout puts it");
        assertThat(detailAt(report, "Fill")).startsWith("drawn as a shape where the layout puts it");
        assertThat(detailAt(report, "Bar")).startsWith("written as drawing alone");
        assertThat(detailAt(report, "Rule")).startsWith("written as a paragraph border rather than as a drawing");
    }

    @Test
    void aDividerOfThePlatesSizeIsStillARule() throws Exception {
        // The other side of the plate: the same box, asked to be a divider, is written as Word's
        // rule — the signal, not the size, is what keeps the plate a drawing.
        try (XWPFDocument document = DocxExports.withLayout(PAGE_WIDTH, PAGE_HEIGHT, MARGIN, page -> page
                .addDivider(divider -> divider.name("Plate").width(10).thickness(10).color(PLATE)))) {
            assertThat(document.getDocument().xmlText()).doesNotContain("<wp:anchor");
            assertThat(document.getParagraphs()).filteredOn(paragraph -> bordered(paragraph.getCTP().getPPr()))
                    .singleElement()
                    .satisfies(paragraph -> {
                        CTBorder bottom = paragraph.getCTP().getPPr().getPBdr().getBottom();
                        assertThat(bottom.getSz().intValue()).as("10pt, in eighths of a point").isEqualTo(80);
                        assertThat(hex(bottom.getColor())).isEqualTo("1A5694");
                    });
        }
    }

    private static XWPFDocument export() throws Exception {
        return DocxExports.withLayout(PAGE_WIDTH, PAGE_HEIGHT, MARGIN, CONTENT);
    }

    private static boolean bordered(CTPPr properties) {
        return properties != null && properties.isSetPBdr();
    }

    private static String hex(Object value) {
        return value instanceof byte[] bytes ? HexFormat.of().withUpperCase().formatHex(bytes)
                : String.valueOf(value).toUpperCase(Locale.ROOT);
    }

    private static String detailAt(DocxExportReport report, String name) {
        return report.notes().stream()
                .filter(note -> note.path() != null && note.path().matches(".*/" + name + "\\[\\d+]"))
                .map(DocxExportReport.Note::detail)
                .findFirst()
                .orElseThrow(() -> new AssertionError("no note at " + name + " in " + report.notes()));
    }

    /** A drawing anchored in the body, as the file records it. */
    private record Anchor(String geometry, long width, long height, long fromLeftOfThePage,
                          String verticalFrom, long fromTheParagraph, String colour, long stackOrder,
                          boolean behindTheText) {
    }

    private static final Pattern ANCHOR = Pattern.compile(
            "<wp:anchor\\b[^>]*relativeHeight=\"(\\d+)\" behindDoc=\"([01])\".*?</wp:anchor>", Pattern.DOTALL);

    /** The drawings anchored in the one paragraph whose text is {@code text}, in document order. */
    private static List<Anchor> anchorsIn(XWPFDocument document, String text) {
        XWPFParagraph holder = document.getParagraphs().stream()
                .filter(paragraph -> paragraph.getText().equals(text))
                .reduce((first, second) -> {
                    throw new AssertionError("more than one paragraph reads " + text);
                })
                .orElseThrow(() -> new AssertionError("no paragraph reads " + text));
        List<Anchor> anchors = new ArrayList<>();
        Matcher matcher = ANCHOR.matcher(holder.getCTP().xmlText());
        while (matcher.find()) {
            String xml = matcher.group();
            anchors.add(new Anchor(
                    group(xml, "prstGeom prst=\"(\\w+)\""),
                    Long.parseLong(group(xml, "<wp:extent cx=\"(\\d+)\"")),
                    Long.parseLong(group(xml, "<wp:extent cx=\"\\d+\" cy=\"(\\d+)\"")),
                    Long.parseLong(group(xml,
                            "<wp:positionH relativeFrom=\"page\"><wp:posOffset>(-?\\d+)</wp:posOffset>")),
                    group(xml, "<wp:positionV relativeFrom=\"(\\w+)\""),
                    Long.parseLong(group(xml, "<wp:positionV relativeFrom=\"\\w+\"><wp:posOffset>(-?\\d+)</wp:posOffset>")),
                    group(xml, "<a:solidFill><a:srgbClr val=\"(\\w+)\""),
                    Long.parseLong(matcher.group(1)),
                    "1".equals(matcher.group(2))));
        }
        return anchors;
    }

    private static String group(String xml, String regex) {
        Matcher matcher = Pattern.compile(regex).matcher(xml);
        if (!matcher.find()) {
            throw new AssertionError("no " + regex + " in " + xml);
        }
        return matcher.group(1);
    }
}
