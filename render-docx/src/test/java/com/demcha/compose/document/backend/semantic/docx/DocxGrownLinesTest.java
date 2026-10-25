package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.font.FontName;
import org.apache.poi.xwpf.usermodel.XWPFAbstractNum;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFNumbering;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.junit.jupiter.api.Test;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTAbstractNum;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTLvl;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTP;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSpacing;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STLineSpacingRule;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STNumberFormat;

import java.math.BigInteger;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * How tall an editor stands a written paragraph's line, and where its text stands in it.
 *
 * <p>The expected reaches are the faces' own tables ({@link DocxNaturalLine}), which Word and
 * LibreOffice were measured to set a line in: a 10pt Lato line reaches 9.87pt up and 2.13pt down,
 * a line of 10pt Poppins and 10pt Spectral reaches as high as the Poppins, its line gap above it,
 * and as deep as the Spectral.</p>
 *
 * @author Artem Demchyshyn
 */
class DocxGrownLinesTest {

    private static final DocumentTextStyle LATO = face(FontName.LATO, 10);
    private static final double LATO_UP = 0.987;
    private static final double LATO_DOWN = 0.213;

    @Test
    void aParagraphReachesAsHighAndAsDeepAsItsTallestAndDeepestRun() throws Exception {
        DocxGrownLines lines = shipping(LATO, face(FontName.POPPINS, 10), face(FontName.SPECTRAL, 10));
        XWPFParagraph paragraph = paragraph();
        run(paragraph, "Poppins", 10);
        run(paragraph, "Spectral", 10);

        DocxGrownLines.Reach reach = lines.reachOf(paragraph.getCTP());

        assertThat(reach.above()).as("Poppins' ascent and its line gap").isCloseTo(11.5, within(0.01));
        assertThat(reach.below()).as("Spectral's descent").isCloseTo(4.63, within(0.01));
        assertThat(reach.shallowest()).as("the mark's, in Normal's Lato").isCloseTo(LATO_DOWN * 10, within(0.01));
    }

    @Test
    void aRunOrAMarkStatingNoFaceOrSizeIsReadInTheNormalStyle() throws Exception {
        DocxGrownLines lines = shipping(LATO, LATO);
        XWPFParagraph plain = paragraph();
        plain.createRun().setText("Plain");
        XWPFParagraph marked = paragraph();
        run(marked, "Lato", 10);
        CTPPr properties = marked.getCTP().isSetPPr() ? marked.getCTP().getPPr() : marked.getCTP().addNewPPr();
        properties.addNewRPr().addNewSz().setVal(BigInteger.valueOf(24));

        assertThat(lines.reachOf(plain.getCTP()).height()).as("Lato at 10pt, Normal's")
                .isCloseTo(12.0, within(0.01));
        assertThat(lines.reachOf(marked.getCTP()).below()).as("the 12pt mark, in Normal's face")
                .isCloseTo(LATO_DOWN * 12, within(0.01));
    }

    @Test
    void aBoldRunIsReadInTheBoldFace() throws Exception {
        DocxGrownLines lines = shipping(LATO, DocumentTextStyle.builder().fontName(FontName.LATO).size(10)
                .decoration(DocumentTextDecoration.BOLD).build());
        XWPFParagraph bold = paragraph();
        run(bold, "Lato", 10).setBold(true);
        XWPFParagraph italic = paragraph();
        run(italic, "Lato", 10).setItalic(true);

        assertThat(lines.reachOf(bold.getCTP())).as("the bold face is shipped").isNotNull();
        assertThat(lines.reachOf(italic.getCTP())).as("the italic face is not").isNull();
        assertThat(shipping(LATO).reachOf(bold.getCTP())).as("in a document shipping the regular face alone")
                .isNull();
    }

    @Test
    void aLinksRunAndAFieldsAreReadAndARunBoldOffIsUpright() throws Exception {
        DocxGrownLines lines = shipping(LATO, LATO);
        XWPFParagraph linked = paragraph();
        var link = linked.getCTP().addNewHyperlink().addNewR();
        link.addNewRPr().addNewSz().setVal(BigInteger.valueOf(24));
        link.addNewT().setStringValue("Linked");
        XWPFParagraph field = paragraph();
        run(field, "Lato", 10).getCTR().addNewFldChar()
                .setFldCharType(org.openxmlformats.schemas.wordprocessingml.x2006.main.STFldCharType.BEGIN);
        run(field, "Lato", 10).getCTR().addNewInstrText().setStringValue(" PAGEREF terms \\h ");
        XWPFParagraph upright = paragraph();
        var bold = run(upright, "Lato", 10).getCTR().getRPr().addNewB();
        try (var cursor = bold.newCursor()) {
            cursor.setAttributeText(new javax.xml.namespace.QName(
                    "http://schemas.openxmlformats.org/wordprocessingml/2006/main", "val"), "0");
        }

        assertThat(lines.reachOf(linked.getCTP()).below()).as("the link's 12pt run")
                .isCloseTo(LATO_DOWN * 12, within(0.01));
        assertThat(lines.reachOf(field.getCTP())).as("a field's runs").isNotNull();
        assertThat(lines.reachOf(upright.getCTP())).as("bold off, in the regular face, the only one shipped")
                .isNotNull();
    }

    @Test
    void aRunHoldingOnlyAShapeAnchoredToTheParagraphIsPassedOver() throws Exception {
        // The run states nothing, so it is the 14pt Normal style's: measured, it leaves a 7.5pt line
        // of least height as tall as it was, in Word and in LibreOffice.
        DocxGrownLines lines = shipping(face(FontName.LATO, 14), face(FontName.LATO, 7.5));
        XWPFParagraph paragraph = paragraph();
        paragraph.createRun().getCTR().addNewDrawing().addNewAnchor();
        run(paragraph, "Lato", 7.5);
        CTPPr properties = paragraph.getCTP().isSetPPr() ? paragraph.getCTP().getPPr() : paragraph.getCTP().addNewPPr();
        properties.addNewRPr().addNewSz().setVal(BigInteger.valueOf(15));
        XWPFParagraph inline = paragraph();
        inline.createRun().getCTR().addNewDrawing().addNewInline();
        run(inline, "Lato", 7.5);

        assertThat(lines.reachOf(paragraph.getCTP()).below()).isCloseTo(LATO_DOWN * 7.5, within(0.01));
        assertThat(lines.reachOf(inline.getCTP())).as("a picture set in the line, its size not written").isNull();
    }

    @Test
    void aParagraphHoldingWhatALineIsNotReadByHasNoReach() throws Exception {
        DocxGrownLines lines = shipping(LATO, LATO);

        XWPFParagraph picture = paragraph();
        run(picture, "Lato", 10).getCTR().addNewDrawing();
        XWPFParagraph raised = paragraph();
        run(raised, "Lato", 10).getCTR().getRPr().addNewPosition().setVal(BigInteger.valueOf(4));
        XWPFParagraph script = paragraph();
        run(script, "Lato", 10).getCTR().getRPr().addNewVertAlign()
                .setVal(org.openxmlformats.schemas.officeDocument.x2006.sharedTypes.STVerticalAlignRun.SUPERSCRIPT);
        XWPFParagraph rightToLeft = paragraph();
        run(rightToLeft, "Lato", 10).getCTR().getRPr().addNewRtl();
        XWPFParagraph substituted = paragraph();
        run(substituted, "Helvetica", 10);
        XWPFParagraph listed = paragraph();
        run(listed, "Lato", 10);
        listed.getCTP().addNewPPr().addNewNumPr();

        assertThat(lines.reachOf(picture.getCTP())).as("a drawing in a run of text").isNull();
        assertThat(lines.reachOf(raised.getCTP())).as("a raise, which a least line does not keep").isNull();
        assertThat(lines.reachOf(script.getCTP())).as("a script").isNull();
        assertThat(lines.reachOf(rightToLeft.getCTP())).as("right to left, in its complex-script face").isNull();
        assertThat(lines.reachOf(substituted.getCTP())).as("a face the document does not ship").isNull();
        assertThat(lines.reachOf(listed.getCTP())).as("a list's item naming no list").isNull();
        assertThat(DocxGrownLines.NONE.reachOf(paragraph().getCTP())).as("nothing shipped").isNull();
    }

    @Test
    void aLineMayGrowWhereItIsAtLeastItsRunsOwnAndTheyShareOneDepth() throws Exception {
        DocxGrownLines lines = shipping(LATO, LATO, face(FontName.LATO, 8));
        XWPFParagraph own = lato(240, 10);
        XWPFParagraph taller = lato(300, 10);
        XWPFParagraph shorter = lato(240 - 1, 10);
        XWPFParagraph mixed = lato(300, 10);
        run(mixed, "Lato", 8);

        assertThat(lines.mayGrow(own.getCTP())).as("its runs' own line").isTrue();
        assertThat(lines.mayGrow(taller.getCTP())).isTrue();
        assertThat(lines.mayGrow(shorter.getCTP())).as("a twip short").isFalse();
        assertThat(lines.mayGrow(mixed.getCTP())).as("10 and 8pt runs, each line its own depth").isFalse();
    }

    @Test
    void runsOfSeveralDepthsMayGrowWhereEveryLineIsAsDeepAsTheDeepest() throws Exception {
        DocxGrownLines lines = shipping(LATO, LATO, face(FontName.LATO, 8));
        XWPFParagraph mixed = lato(300, 10);
        run(mixed, "Lato", 8);
        XWPFParagraph broken = lato(300, 10);
        run(broken, "Lato", 8).addBreak();

        assertThat(lines.mayGrow(mixed.getCTP())).as("lines not known").isFalse();
        assertThat(lines.mayGrow(mixed.getCTP(), new double[]{LATO_DOWN * 10})).as("one line, as deep as its 10pt run")
                .isTrue();
        assertThat(lines.mayGrow(mixed.getCTP(), new double[]{LATO_DOWN * 10, LATO_DOWN * 10})).isTrue();
        assertThat(lines.mayGrow(mixed.getCTP(), new double[]{LATO_DOWN * 10, LATO_DOWN * 8}))
                .as("a second line of the 8pt run alone").isFalse();
        assertThat(lines.mayGrow(mixed.getCTP(), new double[]{Double.NaN})).as("a line not read").isFalse();
        assertThat(lines.mayGrow(broken.getCTP(), new double[]{LATO_DOWN * 10})).as("a break written").isFalse();
        XWPFParagraph deeperMark = lato(300, 10);
        run(deeperMark, "Lato", 8);
        deeperMark.getCTP().getPPr().addNewRPr().addNewSz().setVal(BigInteger.valueOf(24));
        assertThat(lines.mayGrow(deeperMark.getCTP(), new double[]{LATO_DOWN * 10}))
                .as("a 12pt mark, read deeper than the line").isFalse();
    }

    @Test
    void aLineIsAsDeepAsItsDeepestRunWhereThatIsItsTallest() throws Exception {
        DocxGrownLines lines = shipping(LATO, LATO, face(FontName.LATO, 8), face(FontName.SPECTRAL, 7));
        var lato8 = span(FontName.LATO, 8, 9.6);
        var lato10 = span(FontName.LATO, 10, 12);
        var spectral7 = span(FontName.SPECTRAL, 7, 10.65);

        assertThat(lines.depthOfLine(List.of(lato8, lato10))).isCloseTo(LATO_DOWN * 10, within(1e-9));
        assertThat(lines.depthOfLine(List.of(lato10, spectral7)))
                .as("10pt Lato the tallest, by which the page sets the line, 7pt Spectral the deepest").isNaN();
        assertThat(lines.depthOfLine(List.of(lato10, span(FontName.HELVETICA, 8, 9.25)))).as("a run not read").isNaN();
        assertThat(lines.depthOfLine(List.of())).isNaN();
        // The page passes over spaces ending a line; Word is not known to.
        var spaces10 = span(" ", FontName.LATO, 10, 12);
        assertThat(lines.depthOfLine(List.of(lato8, spaces10)))
                .as("8pt text, the 10pt spaces after it the deepest but not the page's tallest").isNaN();
        assertThat(lines.depthOfLine(List.of(lato10, span(" ", FontName.LATO, 8, 9.6))))
                .isCloseTo(LATO_DOWN * 10, within(1e-9));
        assertThat(lines.depthOfLine(List.of(spaces10))).as("spaces alone").isNaN();
    }

    /** A run of text the page set in {@code name} at {@code size}, its line {@code height} tall. */
    private static com.demcha.compose.document.layout.payloads.ParagraphTextSpan span(FontName name, double size,
                                                                                     double height) {
        return span("Text", name, size, height);
    }

    /** The same, of {@code text}. */
    private static com.demcha.compose.document.layout.payloads.ParagraphTextSpan span(String text, FontName name,
                                                                                     double size, double height) {
        return new com.demcha.compose.document.layout.payloads.ParagraphTextSpan(text,
                new com.demcha.compose.engine.components.content.text.TextStyle(name, size,
                        com.demcha.compose.engine.components.content.text.TextDecoration.DEFAULT, java.awt.Color.BLACK),
                10, height, null, null);
    }

    @Test
    void aRunsDepthIsItsFacesDescentAtTheSizeItIsWrittenAt() throws Exception {
        DocxGrownLines lines = shipping(LATO, LATO);
        java.awt.Color black = java.awt.Color.BLACK;

        assertThat(lines.depthOf(new com.demcha.compose.engine.components.content.text.TextStyle(FontName.LATO, 7.35,
                com.demcha.compose.engine.components.content.text.TextDecoration.DEFAULT, black)))
                .as("7.35pt, written at 7.5").isCloseTo(LATO_DOWN * 7.5, within(1e-9));
        assertThat(lines.depthOf(new com.demcha.compose.engine.components.content.text.TextStyle(FontName.LATO, 10,
                com.demcha.compose.engine.components.content.text.TextDecoration.BOLD, black)))
                .as("a bold face not shipped").isNaN();
        assertThat(lines.depthOf(new com.demcha.compose.engine.components.content.text.TextStyle(FontName.HELVETICA, 10,
                com.demcha.compose.engine.components.content.text.TextDecoration.DEFAULT, black)))
                .as("a face not shipped").isNaN();
    }

    @Test
    void aLeastLineStandsItsBaselineItsRunsDescentAboveItsFootAndAnExactOneFourFifthsDown() throws Exception {
        DocxGrownLines lines = shipping(LATO, LATO);
        XWPFParagraph exact = lato(280, 10);
        XWPFParagraph least = lato(280, 10);
        rule(least, STLineSpacingRule.AT_LEAST);
        XWPFParagraph grown = lato(200, 10);
        rule(grown, STLineSpacingRule.AT_LEAST);
        XWPFParagraph auto = lato(240, 10);
        rule(auto, STLineSpacingRule.AUTO);

        assertThat(lines.lineOf(exact.getCTP())).isEqualTo(new DocxGrownLines.Line(14, 14 * 0.8));
        assertThat(lines.lineOf(least.getCTP()).height()).isEqualTo(14);
        assertThat(lines.lineOf(least.getCTP()).baseline()).isCloseTo(14 - LATO_DOWN * 10, within(0.01));
        assertThat(lines.lineOf(grown.getCTP()).height()).as("grown to its runs' own").isCloseTo(12, within(0.01));
        assertThat(lines.lineOf(grown.getCTP()).baseline()).isCloseTo(LATO_UP * 10, within(0.01));
        assertThat(lines.lineOf(auto.getCTP())).as("the editor's own measure").isNull();
    }

    @Test
    void aLineMayGrowOnlyWhereItsPicturesStayWithinItsTextInBothEditors() throws Exception {
        // A 12pt line of 10pt Lato leaves 9.87pt above the text's 2.13pt descent. Word lowers a
        // picture by its run's position; LibreOffice stands it a tenth of a point above the baseline.
        DocxGrownLines lines = shipping(LATO, LATO);
        XWPFParagraph small = lato(240, 10);
        picture(small, 6, 0, 0);
        XWPFParagraph lowered = lato(240, 10);
        picture(lowered, 9, -3, 0);
        XWPFParagraph tall = lato(240, 10);
        picture(tall, 9.8, 0, 0);
        XWPFParagraph deep = lato(240, 10);
        picture(deep, 6, -6, 0);
        XWPFParagraph ownSize = lato(240, 10);
        picture(ownSize, 6, 0, 48);
        XWPFParagraph broken = lato(240, 10);
        broken.getRuns().get(0).addBreak();
        picture(broken, 6, 0, 0);
        XWPFParagraph spaced = lato(240, 10);
        spaced.getRuns().get(0).setText("    ", 0);
        picture(spaced, 6, 0, 0);
        XWPFParagraph unbroken = lato(240, 10);
        unbroken.getRuns().get(0).setText("   ", 0);
        picture(unbroken, 6, 0, 0);
        XWPFParagraph raised = lato(240, 10);
        picture(raised, 6, 8, 0);

        assertThat(lines.mayGrow(small.getCTP())).as("on the baseline, under the text's top").isTrue();
        assertThat(lines.mayGrow(lowered.getCTP())).as("Word's 7.5pt up and 1.5pt down, LibreOffice's 9.1pt up")
                .isTrue();
        assertThat(lines.mayGrow(tall.getCTP())).as("9.9pt up in LibreOffice").isFalse();
        assertThat(lines.mayGrow(deep.getCTP())).as("3pt down in Word, past the text's descent").isFalse();
        assertThat(lines.mayGrow(ownSize.getCTP())).as("the picture run's 24pt sets none of the line").isTrue();
        assertThat(lines.reachOf(broken.getCTP())).as("a broken line may hold the picture alone").isNull();
        assertThat(lines.reachOf(spaced.getCTP())).as("pictures among spaces, no letter").isNull();
        assertThat(lines.reachOf(unbroken.getCTP())).as("among no-break spaces").isNull();
        assertThat(lines.mayGrow(raised.getCTP())).as("raised 4pt, 10pt up in Word; 6.1pt in LibreOffice").isFalse();
        assertThat(lines.reachOf(small.getCTP()).height()).as("the text's line").isCloseTo(12, within(0.01));

        XWPFParagraph grownToItsPicture = lato(200, 10);
        picture(grownToItsPicture, 6, 0, 0);
        // Written 14pt for a 9pt picture raised 2pt, 11pt up in Word: taller than the text's own
        // line, the line holds it, but its text's own line does not.
        XWPFParagraph grownToARaise = lato(280, 10);
        picture(grownToARaise, 9, 4, 0);
        rule(small, STLineSpacingRule.AT_LEAST);
        rule(tall, STLineSpacingRule.AT_LEAST);
        rule(grownToItsPicture, STLineSpacingRule.AT_LEAST);
        rule(grownToARaise, STLineSpacingRule.AT_LEAST);
        assertThat(lines.lineOf(small.getCTP()).baseline()).isCloseTo(12 - LATO_DOWN * 10, within(0.01));
        assertThat(lines.lineOf(tall.getCTP())).as("set apart by the two editors").isNull();
        assertThat(lines.lineOf(grownToItsPicture.getCTP())).as("written shorter than its text's own line")
                .isNull();
        assertThat(lines.lineOf(grownToARaise.getCTP())).as("grown to a raised picture").isNull();
    }

    @Test
    void aPictureRunHoldingAnythingButPicturesOfAWrittenSizeIsNotRead() throws Exception {
        DocxGrownLines lines = shipping(LATO, LATO);
        XWPFParagraph anchoredBeside = lato(240, 10);
        var drawing = anchoredBeside.createRun().getCTR().addNewDrawing();
        drawing.addNewAnchor();
        var extent = drawing.addNewInline().addNewExtent();
        extent.setCx(76200);
        extent.setCy(76200);
        XWPFParagraph measured = lato(240, 10);
        picture(measured, 6, 0, 0);
        measured.getCTP().getRList().get(1).addNewRPr().addNewPosition().setVal("2pt");

        assertThat(lines.reachOf(anchoredBeside.getCTP())).as("an anchor beside the picture").isNull();
        assertThat(lines.reachOf(measured.getCTP())).as("a position in points, not half points").isNull();
    }

    @Test
    void aWordListsItemIsReadWithItsMarkerInItsLevelsFaceOverItsMarks() throws Exception {
        try (XWPFDocument numbered = new XWPFDocument()) {
            DocxGrownLines lines = shipping(numbered::getNumbering, LATO, LATO, face(FontName.BARLOW, 10));
            XWPFParagraph plain = lato(240, 10);
            XWPFParagraph stated = item(numbered, "•", level -> face(level, "Lato", 20));
            XWPFParagraph ofTheMark = item(numbered, "•", level -> { });
            XWPFParagraph larger = item(numbered, "•", level -> face(level, "Lato", 40));
            // A white bullet: Lato holds one, Barlow none.
            XWPFParagraph ofABarlowMark = item(numbered, "◦", level -> { });
            markIn(ofABarlowMark, "Barlow");
            XWPFParagraph latoOverABarlowMark = item(numbered, "◦", level -> face(level, "Lato", 20));
            markIn(latoOverABarlowMark, "Barlow");

            assertThat(lines.reachOf(stated.getCTP())).as("a bullet in the text's face and size")
                    .isEqualTo(lines.reachOf(plain.getCTP()));
            assertThat(lines.mayGrow(stated.getCTP())).isTrue();
            assertThat(lines.reachOf(ofTheMark.getCTP())).as("its level stating none, in its mark's, Normal's Lato")
                    .isEqualTo(lines.reachOf(plain.getCTP()));
            assertThat(lines.mayGrow(ofTheMark.getCTP())).isTrue();
            assertThat(lines.reachOf(larger.getCTP()).above()).as("a 20pt bullet's line in the first")
                    .isCloseTo(LATO_UP * 20, within(0.01));
            assertThat(lines.mayGrow(larger.getCTP())).isFalse();
            assertThat(lines.reachOf(ofABarlowMark.getCTP())).as("in its mark's Barlow, which holds no white bullet")
                    .isNull();
            assertThat(lines.reachOf(latoOverABarlowMark.getCTP())).as("in its level's Lato over its mark's Barlow")
                    .isNotNull();
        }
    }

    /** Sets a paragraph's mark in {@code family}, at 10pt. */
    private static void markIn(XWPFParagraph paragraph, String family) {
        var mark = paragraph.getCTP().getPPr().isSetRPr() ? paragraph.getCTP().getPPr().getRPr()
                : paragraph.getCTP().getPPr().addNewRPr();
        mark.addNewRFonts().setAscii(family);
        mark.addNewSz().setVal(BigInteger.valueOf(20));
    }

    @Test
    void aWordListsMarkerNotSetInItsOwnFaceIsNotRead() throws Exception {
        try (XWPFDocument numbered = new XWPFDocument()) {
            DocxGrownLines lines = shipping(numbered::getNumbering, LATO, LATO);
            XWPFParagraph arrowhead = item(numbered, "➤", level -> face(level, "Lato", 20));
            XWPFParagraph numberedByWord = item(numbered, "%1.", level -> face(level, "Lato", 20));
            XWPFParagraph decimal = item(numbered, "1.", level -> {
                face(level, "Lato", 20);
                level.getNumFmt().setVal(STNumberFormat.DECIMAL);
            });
            XWPFParagraph bold = item(numbered, "•", level -> face(level, "Lato", 20).addNewB());
            XWPFParagraph raised = item(numbered, "•", level -> face(level, "Lato", 20).addNewPosition()
                    .setVal(BigInteger.valueOf(2)));
            XWPFParagraph pictured = item(numbered, "•", level -> {
                face(level, "Lato", 20);
                level.addNewLvlPicBulletId().setVal(BigInteger.ZERO);
            });
            XWPFParagraph deeper = item(numbered, "•", level -> face(level, "Lato", 20));
            deeper.getCTP().getPPr().getNumPr().getIlvl().setVal(BigInteger.ONE);
            XWPFParagraph overridden = item(numbered, "•", level -> face(level, "Lato", 20));
            numbered.getNumbering().getNum(overridden.getCTP().getPPr().getNumPr().getNumId().getVal())
                    .getCTNum().addNewLvlOverride().setIlvl(BigInteger.ZERO);
            XWPFParagraph linked = item(numbered, "•", level -> face(level, "Lato", 20));
            definitionOf(numbered, linked).addNewNumStyleLink().setVal("Bullets");
            XWPFParagraph styled = item(numbered, "•", level -> face(level, "Lato", 20));
            definitionOf(numbered, styled).addNewStyleLink().setVal("Bullets");
            XWPFParagraph unknown = item(numbered, "•", level -> face(level, "Lato", 20));
            unknown.getCTP().getPPr().getNumPr().getNumId().setVal(BigInteger.valueOf(99));
            XWPFParagraph unnumbered = item(numbered, "•", level -> face(level, "Lato", 20));
            DocxGrownLines withoutNumbering = shipping(LATO, LATO);

            assertThat(lines.reachOf(arrowhead.getCTP())).as("an arrowhead Lato holds no glyph for").isNull();
            assertThat(lines.reachOf(numberedByWord.getCTP())).as("a level's number").isNull();
            assertThat(lines.reachOf(decimal.getCTP())).as("a level Word numbers").isNull();
            assertThat(lines.reachOf(bold.getCTP())).as("in bold, a face the document does not ship").isNull();
            assertThat(lines.reachOf(raised.getCTP())).as("raised").isNull();
            assertThat(lines.reachOf(pictured.getCTP())).as("a picture bullet").isNull();
            assertThat(lines.reachOf(deeper.getCTP())).as("a depth its list does not define").isNull();
            assertThat(lines.reachOf(overridden.getCTP())).as("a level overridden").isNull();
            assertThat(lines.reachOf(linked.getCTP())).as("a list a numbering style defines").isNull();
            assertThat(lines.reachOf(styled.getCTP())).as("a list defining a numbering style").isNull();
            assertThat(lines.reachOf(unknown.getCTP())).as("a list the numbering part does not hold").isNull();
            assertThat(withoutNumbering.reachOf(unnumbered.getCTP())).as("no numbering part").isNull();
        }
    }

    /** The list definition a numbered item's list takes its levels from. */
    private static CTAbstractNum definitionOf(XWPFDocument numbered, XWPFParagraph item) {
        var num = numbered.getNumbering().getNum(item.getCTP().getPPr().getNumPr().getNumId().getVal());
        return numbered.getNumbering().getAbstractNum(num.getCTNum().getAbstractNumId().getVal()).getCTAbstractNum();
    }

    /**
     * A list's item of one 10pt Lato line, exact at 240 twips, numbered by a new list of
     * {@code numbered} whose one level writes {@code marker} as a bullet, as {@code level} states it.
     */
    private static XWPFParagraph item(XWPFDocument numbered, String marker,
                                      java.util.function.Consumer<CTLvl> level) {
        CTAbstractNum definition = CTAbstractNum.Factory.newInstance();
        definition.setAbstractNumId(BigInteger.valueOf(numbered.createNumbering().getAbstractNums().size()));
        CTLvl first = definition.addNewLvl();
        first.setIlvl(BigInteger.ZERO);
        first.addNewNumFmt().setVal(STNumberFormat.BULLET);
        first.addNewLvlText().setVal(marker);
        level.accept(first);
        BigInteger numId = numbered.getNumbering().addNum(
                numbered.getNumbering().addAbstractNum(new XWPFAbstractNum(definition)));
        XWPFParagraph paragraph = lato(240, 10);
        paragraph.setNumID(numId);
        paragraph.setNumILvl(BigInteger.ZERO);
        return paragraph;
    }

    /** States a level's marker's face and size, {@code halfPoints} half points. */
    private static org.openxmlformats.schemas.wordprocessingml.x2006.main.CTRPr face(CTLvl level, String family,
                                                                                    int halfPoints) {
        var properties = level.addNewRPr();
        properties.addNewRFonts().setAscii(family);
        properties.addNewSz().setVal(BigInteger.valueOf(halfPoints));
        return properties;
    }

    /**
     * Adds a run holding a picture {@code points} tall set in the line, its run's position
     * {@code position} half points and, where not 0, its size {@code halfPoints}.
     */
    private static void picture(XWPFParagraph paragraph, double points, int position, int halfPoints) {
        var run = paragraph.createRun().getCTR();
        if (position != 0 || halfPoints != 0) {
            var properties = run.addNewRPr();
            if (position != 0) {
                properties.addNewPosition().setVal(BigInteger.valueOf(position));
            }
            if (halfPoints != 0) {
                properties.addNewSz().setVal(BigInteger.valueOf(halfPoints));
            }
        }
        var extent = run.addNewDrawing().addNewInline().addNewExtent();
        extent.setCx(Math.round(points * 12700));
        extent.setCy(Math.round(points * 12700));
    }

    /**
     * The lines of a document that ships the faces of {@code faces} and of its Normal style
     * {@code normal}, as an export does: Normal is the style the most of its text is set in.
     */
    private static DocxGrownLines shipping(DocumentTextStyle normal, DocumentTextStyle... faces) throws Exception {
        return shipping(() -> null, normal, faces);
    }

    /** The same, its lists' items numbered by {@code numbering}. */
    private static DocxGrownLines shipping(java.util.function.Supplier<XWPFNumbering> numbering,
                                           DocumentTextStyle normal, DocumentTextStyle... faces) throws Exception {
        var graph = DocxExports.graphOf(page -> {
            page.addParagraph(p -> p.text("Normal").textStyle(normal));
            for (DocumentTextStyle face : faces) {
                page.addParagraph(p -> p.text("Face").textStyle(face));
            }
        });
        try (XWPFDocument document = new XWPFDocument()) {
            return new DocxGrownLines(DocxFontTable.write(document, graph, List.of(), List.of(), null,
                    new DocxExportReport.Builder()), normal, numbering);
        }
    }

    private static DocumentTextStyle face(FontName name, double size) {
        return DocumentTextStyle.builder().fontName(name).size(size).build();
    }

    private static XWPFParagraph paragraph() {
        return new XWPFDocument().createParagraph();
    }

    private static XWPFRun run(XWPFParagraph paragraph, String family, double size) {
        XWPFRun run = paragraph.createRun();
        run.setText(family);
        run.setFontFamily(family);
        run.setFontSize(size);
        return run;
    }

    /** A paragraph of one 10pt Lato run, its line {@code twips} tall, exact. */
    private static XWPFParagraph lato(long twips, double size) {
        XWPFParagraph paragraph = paragraph();
        run(paragraph, "Lato", size);
        CTP ctp = paragraph.getCTP();
        CTPPr properties = ctp.isSetPPr() ? ctp.getPPr() : ctp.addNewPPr();
        CTSpacing spacing = properties.addNewSpacing();
        spacing.setLine(BigInteger.valueOf(twips));
        spacing.setLineRule(STLineSpacingRule.EXACT);
        return paragraph;
    }

    private static void rule(XWPFParagraph paragraph, STLineSpacingRule.Enum rule) {
        paragraph.getCTP().getPPr().getSpacing().setLineRule(rule);
    }
}
