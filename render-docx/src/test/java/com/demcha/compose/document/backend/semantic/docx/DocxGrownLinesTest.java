package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.font.FontName;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.junit.jupiter.api.Test;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTP;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSpacing;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STLineSpacingRule;

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
        assertThat(lines.reachOf(listed.getCTP())).as("a marker Word draws in the numbering's face").isNull();
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
        rule(small, STLineSpacingRule.AT_LEAST);
        rule(tall, STLineSpacingRule.AT_LEAST);
        rule(grownToItsPicture, STLineSpacingRule.AT_LEAST);
        assertThat(lines.lineOf(small.getCTP()).baseline()).isCloseTo(12 - LATO_DOWN * 10, within(0.01));
        assertThat(lines.lineOf(tall.getCTP())).as("set apart by the two editors").isNull();
        assertThat(lines.lineOf(grownToItsPicture.getCTP())).as("written shorter than its text's own line")
                .isNull();
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
        var graph = DocxExports.graphOf(page -> {
            page.addParagraph(p -> p.text("Normal").textStyle(normal));
            for (DocumentTextStyle face : faces) {
                page.addParagraph(p -> p.text("Face").textStyle(face));
            }
        });
        try (XWPFDocument document = new XWPFDocument()) {
            return new DocxGrownLines(DocxFontTable.write(document, graph, List.of(), List.of(), null,
                    new DocxExportReport.Builder()), normal);
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
