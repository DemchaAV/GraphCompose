package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.image.DocumentImageData;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.table.DocumentTableCell;
import com.demcha.compose.document.table.DocumentTableColumn;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A cell built from a node carries that node, whatever it is.
 *
 * <p>{@code DocumentTableCell.node(...)} lets a cell hold anything the document can hold,
 * and the export wrote paragraphs out of it and nothing else: a cell built from an image or
 * a list came out empty — not wrong, <em>empty</em>, with the content the page draws simply
 * missing from the file and one line in a log to say so. Silent loss inside a table is the
 * worst place for it, because a table is where a document keeps the things a reader counts.</p>
 *
 * <p>The fix is not a second writer that knows about cells. The cell became a destination,
 * so the writers that handle a node anywhere handle it here too — which is why a nested
 * table works without anything in this class knowing how a table is written.</p>
 *
 * @author Artem Demchyshyn
 */
class DocxComposedCellTest {

    @Test
    void aCellBuiltFromAnImageCarriesThePicture() throws Exception {
        XWPFTableCell cell = onlyTableCell(page -> page.addTable(t -> t
                .columns(DocumentTableColumn.auto(), DocumentTableColumn.auto())
                .rowCells(DocumentTableCell.text("Logo"),
                        DocumentTableCell.node(image()))));

        assertThat(cell.getParagraphs())
                .as("the picture is in the cell, not dropped with a warning")
                .anyMatch(p -> !p.getRuns().isEmpty() && !p.getRuns().get(0).getEmbeddedPictures().isEmpty());
    }

    @Test
    void aCellBuiltFromAListCarriesEveryItem() throws Exception {
        XWPFTableCell cell = onlyTableCell(page -> page.addTable(t -> t
                .columns(DocumentTableColumn.auto(), DocumentTableColumn.auto())
                .rowCells(DocumentTableCell.text("Included"),
                        DocumentTableCell.node(list("Setup", "Training", "Support")))));

        assertThat(cell.getParagraphs())
                .extracting(p -> p.getText())
                .contains("Setup", "Training", "Support");
        assertThat(cell.getParagraphs())
                .as("and they are list items, so Enter continues the list in the cell too")
                .anyMatch(p -> p.getCTP().getPPr() != null && p.getCTP().getPPr().isSetNumPr());
    }

    @Test
    void aCellBuiltFromATableCarriesARealNestedTable() throws Exception {
        XWPFTableCell cell = onlyTableCell(page -> page.addTable(t -> t
                .columns(DocumentTableColumn.auto(), DocumentTableColumn.auto())
                .rowCells(DocumentTableCell.text("Breakdown"),
                        DocumentTableCell.node(innerTable()))));

        assertThat(cell.getTables()).hasSize(1);
        assertThat(cell.getTables().get(0).getRow(0).getCell(0).getText()).isEqualTo("Hours");
        assertThat(cell.getTables().get(0).getRow(0).getCell(1).getText()).isEqualTo("12");
    }

    @Test
    void aNestedTableIsFollowedByAParagraph() throws Exception {
        // Word requires a cell to end with a paragraph. A cell ending in a table is
        // malformed, and Word refuses the file rather than showing the table — so the
        // guard is on the structure, not on the render.
        XWPFTableCell cell = onlyTableCell(page -> page.addTable(t -> t
                .columns(DocumentTableColumn.auto(), DocumentTableColumn.auto())
                .rowCells(DocumentTableCell.text("Breakdown"),
                        DocumentTableCell.node(innerTable()))));

        String xml = cell.getCTTc().xmlText();
        assertThat(xml.lastIndexOf("<w:p>"))
                .as("the last block in the cell is a paragraph, not the table")
                .isGreaterThan(xml.lastIndexOf("<w:tbl>"));
    }

    @Test
    void aNestedTableIsGivenTheWidthOfTheColumnItSitsIn() throws Exception {
        // Not the width the page gives it — the layout reports a composed cell's content
        // under the owner's path, so which measured row belongs to which nested table
        // cannot be told apart there. But a nested table with no width at all is squeezed
        // by Word to about one character a line, which is not a document anybody can read.
        XWPFTableCell cell = onlyTableCell(page -> page.addTable(t -> t
                .columns(DocumentTableColumn.auto(), DocumentTableColumn.auto())
                .rowCells(DocumentTableCell.text("Breakdown"),
                        DocumentTableCell.node(innerTable()))));

        long column = DocxTwips.of(cell.getTableRow().getTable()
                .getCTTbl().getTblGrid().getGridColArray(1).getW());
        long nested = DocxTwips.of(cell.getTables().get(0)
                .getCTTbl().getTblPr().getTblW().getW());

        assertThat(cell.getTables().get(0).getCTTbl().getTblPr().getTblW().getType().toString())
                .as("stated, not left to Word")
                .isEqualTo("dxa");
        // The column less the margins the cell itself states: the engine's own default
        // cell padding, 4pt a side, which this export writes as w:tcMar.
        assertThat(nested).isEqualTo(column - 2 * 80);
    }

    @Test
    void aWrapperInsideTheCellIsStillTransparent() throws Exception {
        // A section around the content is not content: its children are written where it
        // stood, here as much as anywhere else.
        XWPFTableCell cell = onlyTableCell(page -> page.addTable(t -> t
                .columns(DocumentTableColumn.auto(), DocumentTableColumn.auto())
                .rowCells(DocumentTableCell.text("Notes"),
                        DocumentTableCell.node(new com.demcha.compose.document.dsl.SectionBuilder()
                                .name("Wrapper")
                                .addParagraph(p -> p.text("First"))
                                .addParagraph(p -> p.text("Second"))
                                .build()))));

        assertThat(cell.getParagraphs())
                .extracting(p -> p.getText())
                .contains("First", "Second");
    }

    @Test
    void anImageInACellDoesNotEscapeIntoTheBody() throws Exception {
        // The destination is restored after the cell, not cleared: the paragraph after the
        // table has to land in the body.
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> page
                .addTable(t -> t
                        .columns(DocumentTableColumn.auto())
                        .rowCells(DocumentTableCell.node(image())))
                .addParagraph(p -> p.text("After the table")))) {

            assertThat(document.getParagraphs())
                    .extracting(p -> p.getText())
                    .as("written to the body, not appended to the cell")
                    .contains("After the table");
        }
    }

    @Test
    void aFilledPillInACellIsAPanelWithItsTextInside() throws Exception {
        // Composed in a cell, the pill has no place in the layout, and its outline was dropped:
        // a rota's shift chips came out as bare hours.
        XWPFTableCell cell = onlyTableCell(page -> page.addTable(t -> t
                .columns(DocumentTableColumn.auto(), DocumentTableColumn.auto())
                .rowCells(DocumentTableCell.text("Mon"), DocumentTableCell.node(pill()))));

        assertThat(cell.getTables()).as("the pill, as a table of one cell").hasSize(1);
        XWPFTableCell chip = cell.getTables().get(0).getRow(0).getCell(0);
        assertThat(chip.getColor()).isEqualToIgnoringCase("1A5694");
        assertThat(chip.getText()).contains("09:00-17:00");
        assertThat(((Number) chip.getCTTc().getTcPr().getTcW().getW()).longValue())
                .as("as wide as its outline, and a point for the editor's face")
                .isEqualTo(61L * 20);
        assertThat(chip.getVerticalAlignment()).isEqualTo(XWPFTableCell.XWPFVertAlign.CENTER);
        // Held to its outline's 14pt, as the page draws it: left to its line, CobaltRota's 17.5pt
        // chips closed to 13pt round their hours.
        assertThat(cell.getTables().get(0).getRow(0).getHeight()).as("held to its outline")
                .isEqualTo(14 * 20);
        assertThat(cell.getTables().get(0).getRow(0).getCtRow().getTrPr().getTrHeightArray(0).getHRule())
                .as("at least: a longer label still grows it")
                .isEqualTo(org.openxmlformats.schemas.wordprocessingml.x2006.main.STHeightRule.AT_LEAST);
    }

    @Test
    void anOutlinedPillInACellIsAPanelWithItsBorders() throws Exception {
        XWPFTableCell cell = onlyTableCell(page -> page.addTable(t -> t
                .columns(DocumentTableColumn.auto(), DocumentTableColumn.auto())
                .rowCells(DocumentTableCell.text("Mon"), DocumentTableCell.node(
                        new com.demcha.compose.document.dsl.ShapeContainerBuilder()
                                .name("Soft").roundedRect(60, 14, 7)
                                .stroke(com.demcha.compose.document.style.DocumentStroke.of(
                                        com.demcha.compose.document.style.DocumentColor.rgb(26, 86, 148), 1))
                                .center(new com.demcha.compose.document.dsl.ParagraphBuilder().text("12:00").build())
                                .build()))));

        assertThat(cell.getTables()).hasSize(1);
        XWPFTableCell chip = cell.getTables().get(0).getRow(0).getCell(0);
        assertThat(chip.getCTTc().getTcPr().getTcBorders().getTop().xmlText()).containsIgnoringCase("1A5694");
        assertThat(chip.getText()).contains("12:00");
    }

    @Test
    void aChipsLabelTallerThanItsOutlineIsCutToIt() throws Exception {
        // CobaltRota's stacked chips: a 9.2pt outline round 8.2pt text on a 10pt line. Grown to
        // the line in Word, each stood 0.8pt taller than the page draws it.
        XWPFTableCell chip = chipCell(null);
        double line = lineOf(chip);

        assertThat(line).as("the outline's height, not the text's line").isCloseTo(9.2, org.assertj.core.api.Assertions.within(0.06));
    }

    @Test
    void anOutlinedChipsLabelIsCutAlikeAsFarAsItsLettersAllow() throws Exception {
        // Word keeps the 1.1pt borders outside the content, leaving 7pt: less than the letters
        // and their margin need. Lato's 8.2pt line is 9.84pt, its baseline 1.75pt above the foot,
        // and the digits reach 0.07pt below it: 0.75pt from them, the foot can give 0.93pt, and
        // the cell centring the line, the top gives no more — 9.84 less twice 0.93. Beside another
        // cell, which gives back none of what the chip grows by, Word sets the line from the top
        // of the chip it grows, not where the page starts it: it is read from the middle.
        XWPFTableCell chip = chipCell(com.demcha.compose.document.style.DocumentStroke.of(
                com.demcha.compose.document.style.DocumentColor.rgb(26, 86, 148), 1.1));

        assertThat(lineOf(chip)).isCloseTo(7.98, org.assertj.core.api.Assertions.within(0.06));
    }

    @Test
    void aPaddedOutlinedChipStaysItsOutlinesHeightInWord() throws Exception {
        // The cell's margins are its padding less half each border, the page drawing that half
        // inside the box; Word keeps the borders outside the content. Margins, borders and the
        // line come to the 17.5pt outline, not 1pt short of it.
        com.demcha.compose.document.dsl.ShapeContainerBuilder chip = new com.demcha.compose.document.dsl.ShapeContainerBuilder()
                .name("Padded").roundedRect(90, 17.5, 4).padding(DocumentInsets.of(2))
                .stroke(com.demcha.compose.document.style.DocumentStroke.of(
                        com.demcha.compose.document.style.DocumentColor.rgb(26, 86, 148), 1.1))
                .center(new com.demcha.compose.document.dsl.ParagraphBuilder().text("09:00-16:00")
                        .textStyle(com.demcha.compose.document.style.DocumentTextStyle.builder()
                                .fontName(com.demcha.compose.font.FontName.LATO).size(12).build()).build());
        DocumentNode node = chip.build();
        XWPFTableCell cell = onlyTableCell(page -> page.addTable(t -> t
                .columns(DocumentTableColumn.auto(), DocumentTableColumn.fixed(100))
                .rowCells(DocumentTableCell.text("Mon"), DocumentTableCell.node(node))));
        XWPFTableCell padded = cell.getTables().get(0).getRow(0).getCell(0);
        var margins = padded.getCTTc().getTcPr().getTcMar();
        double around = (DocxTwips.of(margins.getTop().getW()) + DocxTwips.of(margins.getBottom().getW())) / 20.0 + 2 * 1.1;

        assertThat(around + lineOf(padded)).isCloseTo(17.5, org.assertj.core.api.Assertions.within(0.1));
    }

    @Test
    void aLabelSetFromTheChipsTopIsLeftAsItWas() throws Exception {
        // It passes the outline below only: cut on both sides, its text would stand high.
        XWPFTableCell chip = chipCell(null, com.demcha.compose.document.node.LayerAlign.TOP_LEFT, 8.2);

        assertThat(lineOf(chip)).as("the text's own line").isGreaterThan(9.8);
    }

    @Test
    void anOutlinedChipHoldsItsRowLessBothBorders() throws Exception {
        // Word and LibreOffice draw a cell's top and bottom borders outside the height its row
        // holds: CobaltRota's outlined chips, 9.2pt outlines with a 1.125pt border, stood 10.3pt
        // tall in Word, and each row holding one 1.1pt taller than the page's. A 1.125pt border is
        // written at nine eighths, and a row counts it 22 twips.
        XWPFTableCell outlined = chipCell(com.demcha.compose.document.style.DocumentStroke.of(
                com.demcha.compose.document.style.DocumentColor.rgb(20, 160, 70), 1.125));
        XWPFTableCell filled = chipCell(null);

        assertThat(heightOf(outlined.getTableRow().getTable())).as("184 twips less two borders of 22")
                .isEqualTo(140);
        assertThat(heightOf(filled.getTableRow().getTable())).as("a chip with no border keeps its outline's")
                .isEqualTo(184);
    }

    @Test
    void anOutlinedChipHoldsItsRowLessItsBordersAsWordDrawsThem() throws Exception {
        // A border is written in eighths of a point, and a row counts it two and a half twips an
        // eighth, rounded down: a 0.7pt outline, written at six eighths, takes 15 twips, and a
        // 0.9pt one, at seven, 17. Taken off as the page strokes them, 14 and 18, a 20.3pt chip
        // outlined at 0.7pt stood a twip taller in Word and LibreOffice, and six of them in a
        // table's rows six twips lower below them.
        XWPFTableCell thin = aroundTheChip(stroke(0.7), com.demcha.compose.document.node.LayerAlign.CENTER, 8.2, 20.3)
                .getTables().get(0).getRow(0).getCell(0);
        XWPFTableCell thick = aroundTheChip(stroke(0.9), com.demcha.compose.document.node.LayerAlign.CENTER, 8.2, 20.3)
                .getTables().get(0).getRow(0).getCell(0);

        assertThat(heightOf(thin.getTableRow().getTable())).as("406 twips less two borders of 15").isEqualTo(376);
        assertThat(heightOf(thick.getTableRow().getTable())).as("406 twips less two borders of 17").isEqualTo(372);
    }

    @Test
    void anOutlinedChipsLabelHasTheRoomItsBordersLeaveAsWordDrawsThem() throws Exception {
        // An 11.8pt chip outlined at 0.7pt leaves Lato's line 236 twips less two borders of 15,
        // 206, short of the 212 that would seat it. Counted at 14 each, the line and the row it is
        // held at were written 208 and 207, and stood the chip that much taller in Word.
        XWPFTableCell chip = beside(labelledChip("09:00-16:00", com.demcha.compose.font.FontName.LATO, 8.2, 11.8,
                DocumentInsets.zero(), stroke(0.7), null));

        assertThat(lineOf(chip)).isEqualTo(206 / 20.0);
        assertThat(heightOf(chip.getTableRow().getTable())).isEqualTo(206);
    }

    @Test
    void aChipComposedInACellIsHeldAndItsLabelCutRoundedDownToTheTwip() throws Exception {
        // CobaltRota's stacked chips are 183.69 twips: held at 184 and their labels cut to 184,
        // two in a day cell stood every row they were in a twip taller than the page's in Word and
        // LibreOffice. The row the chip is in holds the page's height, rounded once.
        XWPFTableCell chip = aroundTheChip(null, com.demcha.compose.document.node.LayerAlign.CENTER, 8.2, 9.1846)
                .getTables().get(0).getRow(0).getCell(0);

        assertThat(heightOf(chip.getTableRow().getTable())).as("its row").isEqualTo(183);
        assertThat(lineOf(chip)).as("its label's line").isEqualTo(183 / 20.0);
    }

    @Test
    void theCellAnOutlinedChipIsAllOfGivesBackWhatItsLettersKeepPastItsOutline() throws Exception {
        // Its 1.1pt borders leave the label 7pt. The page starts the 9.84pt line at the 9.2pt
        // chip's top, the letters 0.32pt below its middle, and Word sets the line on that middle
        // here: the foot gives 0.93pt less those 0.32, and the line is 172 twips (above). The
        // chip's row holds 140, and Word grows the chip by the 32 between. The cell round it, its
        // 4pt padding less the 1pt rule 60 twips above and below, gives them back, 16 and 16.
        XWPFTableCell outlined = aloneInARow(OUTLINE, DocumentInsets.of(4));
        XWPFTableCell filled = aloneInARow(null, DocumentInsets.of(4));
        XWPFTableCell chip = outlined.getTables().get(0).getRow(0).getCell(0);

        assertThat(DocxTwips.of(chip.getParagraphs().get(0).getCTP().getPPr().getSpacing().getLine())
                   - heightOf(chip.getTableRow().getTable())).as("the premise: what Word grows the chip by")
                .isEqualTo(32);
        assertThat(verticalMarginsOf(outlined)).containsExactly(60L - 16, 60L - 16);
        assertThat(verticalMarginsOf(filled)).as("a chip its label fits gives nothing back").containsExactly(60L, 60L);
    }

    @Test
    void anOddNumberOfTwipsIsGivenBackHalfRoundedDownAboveAndTheRestBelow() throws Exception {
        // A 1.05pt outline is written at eight eighths, 20 twips: a 9.195pt chip's row holds 183 less
        // two of them, 143, and its letters keep its label at 172 — 29 twips, 14 above and 15 below.
        com.demcha.compose.document.style.DocumentStroke outline = com.demcha.compose.document.style.DocumentStroke.of(
                com.demcha.compose.document.style.DocumentColor.rgb(26, 86, 148), 1.05);
        XWPFTableCell around = aloneInARow(outline, DocumentInsets.of(4), 9.195);
        XWPFTableCell chip = around.getTables().get(0).getRow(0).getCell(0);

        assertThat(DocxTwips.of(chip.getParagraphs().get(0).getCTP().getPPr().getSpacing().getLine())
                   - heightOf(chip.getTableRow().getTable())).as("the premise: an odd number of twips").isEqualTo(29);
        assertThat(verticalMarginsOf(around)).containsExactly(60L - 14, 60L - 15);
    }

    @Test
    void aCellWhoseMarginsHoldJustWhatItsChipGrowsByGivesItAllBack() throws Exception {
        // The chip of the odd case above, its cell's margins 14 twips above and 15 below — its
        // padding less the 1pt rule: all Word grows the chip by, given back, the label seated on
        // the chip's middle. Its borders counted at the 1.05pt the page strokes rather than the 20
        // twips Word draws, the chip would grow by 30, more than the margins above hold.
        XWPFTableCell around = aloneInARow(com.demcha.compose.document.style.DocumentStroke.of(
                        com.demcha.compose.document.style.DocumentColor.rgb(26, 86, 148), 1.05),
                new DocumentInsets(1.7, 4, 1.75, 4), 9.195);

        assertThat(verticalMarginsOf(around)).containsExactly(0L, 0L);
        assertThat(lineOf(around.getTables().get(0).getRow(0).getCell(0))).isEqualTo(172 / 20.0);
    }

    @Test
    void aCellGivesBackNoMoreThanItsMarginsHold() throws Exception {
        // 1.25pt of padding less the 1pt rule leaves 5 twips above and below, less than the 10 each.
        // Given back less than it grows by, the chip grows downwards in Word, its line set from its
        // top: the label is read from the chip's middle, as beside another cell.
        XWPFTableCell around = aloneInARow(OUTLINE, DocumentInsets.of(1.25));

        assertThat(verticalMarginsOf(around)).containsExactly(0L, 0L);
        assertThat(lineOf(around.getTables().get(0).getRow(0).getCell(0))).isEqualTo(8.0);
    }

    @Test
    void aLabelWhoseLettersNoLineOnTheChipsMiddleHoldsIsLeftWhereWordSetsIt() throws Exception {
        // The page starts Lato's 9.84pt line at the 9.2pt chip's top; the accent over the "É" stands
        // too near that top for any line on the chip's middle to hold it there, a position lowering
        // the text out of the line. Read from the middle, there is nothing to cut either: the line
        // is written whole and the cell round the chip gives nothing back.
        DocumentNode node = labelledChip("ÉÅ", com.demcha.compose.font.FontName.LATO, 8.2,
                9.2, DocumentInsets.zero(), null, null);
        XWPFTableCell around = onlyTableCell(page -> page.addTable(t -> t
                .columns(DocumentTableColumn.fixed(100))
                .defaultCellStyle(com.demcha.compose.document.table.DocumentTableStyle.builder().padding(DocumentInsets.of(4)).build())
                .rowCells(DocumentTableCell.node(node))));
        XWPFTableCell chip = around.getTables().get(0).getRow(0).getCell(0);

        assertThat(lineOf(chip)).isEqualTo(197 / 20.0);
        assertThat(positionOf(chip)).isZero();
        assertThat(verticalMarginsOf(around)).containsExactly(60L, 60L);
    }

    @Test
    void aChipBesideAnotherCellGivesNothingBack() throws Exception {
        // Word gives every cell of a row the row's largest margins: the cell beside it keeps its 60.
        assertThat(verticalMarginsOf(aroundTheChip(OUTLINE, com.demcha.compose.document.node.LayerAlign.CENTER,
                8.2, 9.2))).containsExactly(60L, 60L);
    }

    @Test
    void aLabelTallerThanItsChipIsSeatedFromTheChipsTopWhereThePageStartsIt() throws Exception {
        // CobaltRota's stacked chips: Carlito's 8.17pt line is 9.97pt, its baseline its 2.19pt
        // descent above the foot. The page starts the line at the 9.18pt chip's top, the baseline
        // 7.78pt below it; Word, the line cut to the chip's 9.15pt, four fifths down it, 7.32pt:
        // 0.46pt high, lowered half a point. Read from the middle the cell centres the line on,
        // 0.4pt above where the page starts it, the two stood within a quarter point and nothing
        // was written: the labels stood 0.43pt high in Word.
        XWPFTableCell chip = labelled(com.demcha.compose.font.FontName.CARLITO, 8.17, 9.18);

        assertThat(lineOf(chip)).isEqualTo(183 / 20.0);
        assertThat(positionOf(chip)).as("in half points").isEqualTo(-1);
    }

    @Test
    void aLabelItsChipHoldsWholeIsWrittenAtTheHeightThatSeatsItsText() throws Exception {
        // CobaltRota's single-shift chips: Carlito's 10.4pt line is 12.7pt, and the page sets the
        // text its 2.79pt descent above the foot, 3.55pt below the middle both centre the line on.
        // Word and LibreOffice stand it four fifths down the line, three tenths of it below that
        // middle — 0.26pt low on 12.7pt. Written 11.85pt, the text stands where the page sets it.
        // The chip and its row keep their height, and no position is written.
        XWPFTableCell chip = labelled(com.demcha.compose.font.FontName.CARLITO, 10.4, 17.5);

        assertThat(lineOf(chip)).isEqualTo(237 / 20.0);
        assertThat(heightOf(chip.getTableRow().getTable())).isEqualTo(350);
        assertThat(verticalMarginsOf(chip)).containsExactly(0L, 0L);
        assertThat(positionOf(chip)).isZero();
    }

    @Test
    void aLabelThePageSetsLowerIsWrittenOnATallerLine() throws Exception {
        // Lato's 8.2pt line is 9.84pt and its baseline 1.75pt above the foot: 3.17pt below the
        // middle, which three tenths of a 10.6pt line reach.
        XWPFTableCell chip = labelled(com.demcha.compose.font.FontName.LATO, 8.2, 14);

        assertThat(lineOf(chip)).isEqualTo(212 / 20.0);
        assertThat(heightOf(chip.getTableRow().getTable())).as("the chip's row is not grown").isEqualTo(280);
        assertThat(positionOf(chip)).isZero();
    }

    @Test
    void aLabelsLineIsNoTallerThanTheRoomItsChipLeaves() throws Exception {
        // A 10.3pt chip leaves Lato's line 206 twips, short of the 212 that would seat it.
        XWPFTableCell chip = labelled(com.demcha.compose.font.FontName.LATO, 8.2, 10.3);

        assertThat(lineOf(chip)).isEqualTo(206 / 20.0);
        assertThat(heightOf(chip.getTableRow().getTable())).isEqualTo(206);
    }

    @Test
    void aLabelThatCouldMoveLessThanATenthOfAPointIsLeftWhereItIs() throws Exception {
        // A 10pt chip leaves Lato's 9.84pt line 10pt: its text would move 0.05pt, a step of Word's
        // 0.12pt grid or nothing.
        XWPFTableCell chip = labelled(com.demcha.compose.font.FontName.LATO, 8.2, 10);

        assertThat(lineOf(chip)).isEqualTo(197 / 20.0);
    }

    @Test
    void aLabelsLettersKeepItsLineTallEnoughToHoldThem() throws Exception {
        // Its descenders reach 1.8pt below a baseline 3.55pt below the middle: three quarters of a
        // point past them, the line is no shorter than 12.2pt, not the 11.85pt that seats it.
        XWPFTableCell chip = beside(labelledChip("gjpqy", com.demcha.compose.font.FontName.CARLITO, 10.4, 17.5,
                DocumentInsets.zero(), null, null));

        assertThat(lineOf(chip)).isEqualTo(244 / 20.0);
    }

    @Test
    void aDeepFacesLabelIsSeatedByItsLineNotAPosition() throws Exception {
        // Spectral's descent stands Word's baseline nearly 2pt below the page's on its 18.25pt
        // line; written 11.9pt, the line seats it, and LibreOffice does not move a raised text
        // further than it is raised.
        XWPFTableCell chip = labelled(com.demcha.compose.font.FontName.SPECTRAL, 12, 24);

        assertThat(lineOf(chip)).isEqualTo(238 / 20.0);
        assertThat(positionOf(chip)).isZero();
        assertThat(heightOf(chip.getTableRow().getTable())).isEqualTo(480);
    }

    @Test
    void aPaddedChipsLabelIsSeatedInItsOutline() throws Exception {
        // The page sets the padding round the outline: written round the chip's table, the outline
        // holds its height whole, with no margin inside it, and the line is seated. Measured, the
        // label stood on the page's baseline in Word and LibreOffice.
        XWPFTableCell chip = beside(labelledChip("09:00-16:00", com.demcha.compose.font.FontName.CARLITO, 10.4, 17.5,
                DocumentInsets.of(2), null, null));

        assertThat(lineOf(chip)).isEqualTo(237 / 20.0);
        assertThat(heightOf(chip.getTableRow().getTable())).isEqualTo(350);
        assertThat(verticalMarginsOf(chip)).containsExactly(0L, 0L);
    }

    @Test
    void aLabelTallerThanTheRoomWhoseLettersLeaveNothingToCutIsWrittenWhole() throws Exception {
        // A 10pt outlined chip holds 156 twips, its 1.1pt borders off; the accent above the "É" and
        // the descenders below leave its 9.84pt line nothing to cut, and Word grows the chip to it.
        XWPFTableCell chip = beside(labelledChip("Égj", com.demcha.compose.font.FontName.LATO, 8.2, 10,
                DocumentInsets.zero(), OUTLINE, null));

        assertThat(lineOf(chip)).isEqualTo(197 / 20.0);
        assertThat(heightOf(chip.getTableRow().getTable())).isEqualTo(156);
        assertThat(verticalMarginsOf(chip)).containsExactly(0L, 0L);
    }

    @Test
    void aPaddedChipsLabelItsOutlineHoldsIsSeatedOnTheOutlinesMiddle() throws Exception {
        // The page lays a shape's layers out in its outline, its padding round it: Lato's 14.4pt
        // line fits the 17.5pt outline and stands on its middle. Its padding written round the
        // chip's table, the line holds the 15.3pt its borders leave and is seated there. Measured,
        // the label stood on the page's baseline in Word and LibreOffice, to a tenth of a point.
        XWPFTableCell chip = beside(labelledChip("09:00-16:00", com.demcha.compose.font.FontName.LATO, 12, 17.5,
                DocumentInsets.of(2), OUTLINE, null));

        assertThat(lineOf(chip)).isEqualTo(15.3);
        assertThat(positionOf(chip)).isZero();
    }

    @Test
    void aLabelThePageSeatsOffItsBaselineIsSeatedThereToo() throws Exception {
        // Seated by its cap band, the page sets the text about a quarter point higher than its
        // baseline: 3.33pt below the middle, and the line 11.1pt.
        XWPFTableCell chip = beside(labelledChip("09:00-16:00", com.demcha.compose.font.FontName.CARLITO, 10.4, 17.5,
                DocumentInsets.zero(), null, com.demcha.compose.document.node.TextVerticalAlign.CENTER));

        assertThat(lineOf(chip)).isEqualTo(222 / 20.0);
        assertThat(positionOf(chip)).isZero();
    }

    @Test
    void aLabelThePageSeatsByItsCapsIsHeldByItsLettersAndRaisedTheRest() throws Exception {
        // Seated at its line's top, the page raises the text about 3.2pt, nearly to the middle.
        // Its capitals, raised with it, keep the line 14.9pt tall — measured from the line's own
        // baseline, which the letters' reach is read from — and the 4pt it still calls for is
        // its position.
        XWPFTableCell chip = beside(labelledChip("09:00-16:00", com.demcha.compose.font.FontName.CARLITO, 10.4, 17.5,
                DocumentInsets.zero(), null, com.demcha.compose.document.node.TextVerticalAlign.TOP));

        assertThat(lineOf(chip)).isEqualTo(298 / 20.0);
        assertThat(positionOf(chip)).isEqualTo(8);
    }

    @Test
    void aLabelInAStandardFaceTheExportDoesNotEmbedIsNotSeatedByItsLine() throws Exception {
        // Helvetica's letters are read through a stand-in, in another font's units, twice as tall:
        // seated by them, a 10pt label's line came out 26pt. It keeps the page's 9.25pt.
        XWPFTableCell chip = beside(labelledChip("Shift 09:00", com.demcha.compose.font.FontName.HELVETICA, 10, 40,
                DocumentInsets.zero(), null, null));

        assertThat(lineOf(chip)).isEqualTo(185 / 20.0);
        assertThat(positionOf(chip)).isZero();
    }

    /** The cell of a filled chip of a height, centring one line in a face and size. */
    private static XWPFTableCell labelled(com.demcha.compose.font.FontName font, double size, double height)
            throws Exception {
        return beside(labelledChip("09:00-16:00", font, size, height, DocumentInsets.zero(), null, null));
    }

    /** A filled chip centring one line, outlined where a stroke is given and seated where an anchor is. */
    private static DocumentNode labelledChip(String text, com.demcha.compose.font.FontName font, double size,
                                             double height, DocumentInsets padding,
                                             com.demcha.compose.document.style.DocumentStroke stroke,
                                             com.demcha.compose.document.node.TextVerticalAlign seat) {
        com.demcha.compose.document.dsl.ParagraphBuilder label = new com.demcha.compose.document.dsl.ParagraphBuilder().text(text)
                .textStyle(com.demcha.compose.document.style.DocumentTextStyle.builder().fontName(font).size(size).build());
        if (seat != null) {
            label.verticalAlign(seat);
        }
        com.demcha.compose.document.dsl.ShapeContainerBuilder chip = new com.demcha.compose.document.dsl.ShapeContainerBuilder()
                .name("Chip").roundedRect(90, height, 4).padding(padding)
                .fillColor(com.demcha.compose.document.style.DocumentColor.rgb(20, 160, 70))
                .center(label.build());
        if (stroke != null) {
            chip.stroke(stroke);
        }
        return chip.build();
    }

    /** The cell of a chip composed in a table cell beside another. */
    private static XWPFTableCell beside(DocumentNode node) throws Exception {
        return onlyTableCell(page -> page.addTable(t -> t
                .columns(DocumentTableColumn.auto(), DocumentTableColumn.fixed(100))
                .rowCells(DocumentTableCell.text("Mon"), DocumentTableCell.node(node))))
                .getTables().get(0).getRow(0).getCell(0);
    }

    /** How far a chip's label's text is raised, in half points; 0 where no position is written. */
    private static long positionOf(XWPFTableCell chip) {
        var properties = chip.getParagraphs().get(0).getRuns().get(0).getCTR().getRPr();
        return properties == null || properties.sizeOfPositionArray() == 0 ? 0
                : ((Number) properties.getPositionArray(0).getVal()).longValue();
    }

    /** CobaltRota's outlined chips' border. */
    private static final com.demcha.compose.document.style.DocumentStroke OUTLINE =
            com.demcha.compose.document.style.DocumentStroke.of(com.demcha.compose.document.style.DocumentColor.rgb(26, 86, 148), 1.1);

    /** An outline {@code width} wide. */
    private static com.demcha.compose.document.style.DocumentStroke stroke(double width) {
        return com.demcha.compose.document.style.DocumentStroke.of(com.demcha.compose.document.style.DocumentColor.rgb(26, 86, 148), width);
    }

    /** The cell of a one-column table a 9.2pt chip is all of. */
    private static XWPFTableCell aloneInARow(com.demcha.compose.document.style.DocumentStroke stroke,
                                             DocumentInsets padding) throws Exception {
        return aloneInARow(stroke, padding, 9.2);
    }

    /** The cell of a one-column table a chip {@code height} tall is all of. */
    private static XWPFTableCell aloneInARow(com.demcha.compose.document.style.DocumentStroke stroke,
                                             DocumentInsets padding, double height) throws Exception {
        DocumentNode node = chip(stroke, height);
        return onlyTableCell(page -> page.addTable(t -> t
                .columns(DocumentTableColumn.fixed(100))
                .defaultCellStyle(com.demcha.compose.document.table.DocumentTableStyle.builder().padding(padding).build())
                .rowCells(DocumentTableCell.node(node))));
    }

    /** A filled chip round 8.2pt Lato, outlined where a stroke is given. */
    private static DocumentNode chip(com.demcha.compose.document.style.DocumentStroke stroke, double height) {
        com.demcha.compose.document.dsl.ShapeContainerBuilder chip = new com.demcha.compose.document.dsl.ShapeContainerBuilder()
                .name("Stacked").roundedRect(90, height, 4).padding(DocumentInsets.zero())
                .fillColor(com.demcha.compose.document.style.DocumentColor.rgb(20, 160, 70))
                .center(new com.demcha.compose.document.dsl.ParagraphBuilder().text("09:00-16:00")
                        .textStyle(com.demcha.compose.document.style.DocumentTextStyle.builder()
                                .fontName(com.demcha.compose.font.FontName.LATO).size(8.2).build()).build());
        if (stroke != null) {
            chip.stroke(stroke);
        }
        return chip.build();
    }

    private static XWPFTableCell chipCell(com.demcha.compose.document.style.DocumentStroke stroke) throws Exception {
        return chipCell(stroke, com.demcha.compose.document.node.LayerAlign.CENTER, 8.2);
    }

    private static XWPFTableCell chipCell(com.demcha.compose.document.style.DocumentStroke stroke,
                                          com.demcha.compose.document.node.LayerAlign align, double size) throws Exception {
        return aroundTheChip(stroke, align, size, 9.2).getTables().get(0).getRow(0).getCell(0);
    }

    /** The table cell a chip is composed in. */
    private static XWPFTableCell aroundTheChip(com.demcha.compose.document.style.DocumentStroke stroke,
                                               com.demcha.compose.document.node.LayerAlign align, double size,
                                               double height) throws Exception {
        com.demcha.compose.document.dsl.ShapeContainerBuilder chip = new com.demcha.compose.document.dsl.ShapeContainerBuilder()
                .name("Stacked").roundedRect(90, height, 4).padding(DocumentInsets.zero())
                .fillColor(com.demcha.compose.document.style.DocumentColor.rgb(20, 160, 70))
                .layer(new com.demcha.compose.document.dsl.ParagraphBuilder().text("09:00-16:00")
                        .textStyle(com.demcha.compose.document.style.DocumentTextStyle.builder()
                                .fontName(com.demcha.compose.font.FontName.LATO).size(size).build()).build(), align);
        if (stroke != null) {
            chip.stroke(stroke);
        }
        DocumentNode node = chip.build();
        return onlyTableCell(page -> page.addTable(t -> t
                .columns(DocumentTableColumn.auto(), DocumentTableColumn.fixed(100))
                .rowCells(DocumentTableCell.text("Mon"), DocumentTableCell.node(node))));
    }

    /** A cell's top and bottom margins, in twips. */
    private static long[] verticalMarginsOf(XWPFTableCell cell) {
        var margins = cell.getCTTc().getTcPr().getTcMar();
        return new long[] {DocxTwips.of(margins.getTop().getW()), DocxTwips.of(margins.getBottom().getW())};
    }

    private static double lineOf(XWPFTableCell chip) {
        var spacing = chip.getParagraphs().get(0).getCTP().getPPr().getSpacing();
        return DocxTwips.of(spacing.getLine()) / 20.0;
    }

    @Test
    void aTileHoldingOnlyDrawingIsDrawnWhereThePageDrawsItNotAPanel() throws Exception {
        java.util.concurrent.atomic.AtomicReference<DocxExportReport> report = new java.util.concurrent.atomic.AtomicReference<>();
        String body = export(report, DocumentTableCell.node(new com.demcha.compose.document.dsl.ShapeContainerBuilder()
                .name("Swatch").roundedRect(40, 12, 4)
                .fillColor(com.demcha.compose.document.style.DocumentColor.rgb(26, 86, 148))
                .center(new com.demcha.compose.document.dsl.EllipseBuilder().circle(4)
                        .fillColor(com.demcha.compose.document.style.DocumentColor.WHITE).build())
                .build()));

        assertThat(body.split("<w:tbl>", -1)).as("the table alone, no empty panel nested in it").hasSize(2);
        assertThat(anchors(body)).as("the tile and the dot on it, anchored where the page draws them")
                .anyMatch(anchor -> anchor.contains("prst=\"roundRect\""))
                .anyMatch(anchor -> anchor.contains("prst=\"ellipse\""));
        assertThat(anchors(body)).as("a composed tile is not taken into its cell: it stays on the page")
                .allMatch(anchor -> anchor.contains("<wp:positionV relativeFrom=\"page\">"));
        assertThat(report.get().count(DocxExportReport.Severity.DROPPED)).isZero();
    }

    @Test
    void thePillsSquaredCornersAreReportedAndItIsNotDrawnAgainAsAShape() throws Exception {
        java.util.concurrent.atomic.AtomicReference<DocxExportReport> report = new java.util.concurrent.atomic.AtomicReference<>();
        String body = export(report, DocumentTableCell.node(pill()));

        assertThat(report.get().notes()).anyMatch(note -> note.severity() == DocxExportReport.Severity.APPROXIMATED
                                                          && note.subject().equals("corner radius"));
        assertThat(report.get().count(DocxExportReport.Severity.DROPPED)).isZero();
        assertThat(anchors(body)).as("a panel frames its text; its box is not drawn over it as well").isEmpty();
    }

    @Test
    void aDiscUnderTextInACellIsDrawnBehindIt() throws Exception {
        java.util.concurrent.atomic.AtomicReference<DocxExportReport> report = new java.util.concurrent.atomic.AtomicReference<>();
        String body = export(report, DocumentTableCell.node(new com.demcha.compose.document.dsl.ShapeContainerBuilder()
                .name("Dot").circle(14)
                .fillColor(com.demcha.compose.document.style.DocumentColor.rgb(26, 86, 148))
                .center(new com.demcha.compose.document.dsl.ParagraphBuilder().text("A").build())
                .build()));

        assertThat(anchors(body)).singleElement().asString()
                .contains("prst=\"ellipse\"").contains("behindDoc=\"1\"");
        assertThat(report.get().count(DocxExportReport.Severity.DROPPED)).isZero();
    }

    @Test
    void anIconInAPaintedCellStandsInFrontAndADiscUnderItsNumberStaysBehind() throws Exception {
        // Both editors paint a cell's shading over a drawing behind the text.
        com.demcha.compose.document.table.DocumentTableStyle navy = com.demcha.compose.document.table.DocumentTableStyle
                .builder().fillColor(com.demcha.compose.document.style.DocumentColor.rgb(16, 32, 80)).build();
        java.util.concurrent.atomic.AtomicReference<DocxExportReport> report = new java.util.concurrent.atomic.AtomicReference<>();
        String body = export(report, List.of(
                DocumentTableCell.node(new com.demcha.compose.document.dsl.EllipseBuilder().name("Icon").circle(8)
                        .fillColor(com.demcha.compose.document.style.DocumentColor.WHITE).build()).withStyle(navy),
                DocumentTableCell.node(new com.demcha.compose.document.dsl.ShapeContainerBuilder()
                        .name("Disc").circle(14)
                        .fillColor(com.demcha.compose.document.style.DocumentColor.rgb(26, 86, 148))
                        .center(new com.demcha.compose.document.dsl.ParagraphBuilder().text("7").build())
                        .build()).withStyle(navy)));

        assertThat(anchors(body)).as("the icon, alone in its navy cell").anyMatch(anchor ->
                anchor.contains("prst=\"ellipse\"") && anchor.contains("behindDoc=\"0\"") && anchor.contains("FFFFFF"));
        assertThat(anchors(body)).as("the disc, under its number").anyMatch(anchor ->
                anchor.contains("prst=\"ellipse\"") && anchor.contains("behindDoc=\"1\"") && anchor.contains("1A5694"));
    }

    @Test
    void anIconInAWhiteCellStandsInFrontOfItsShading() throws Exception {
        // A cell whose style names white is shaded white in Word, and the shading hid the icon.
        com.demcha.compose.document.table.DocumentTableStyle white = com.demcha.compose.document.table.DocumentTableStyle
                .builder().fillColor(com.demcha.compose.document.style.DocumentColor.WHITE).build();
        java.util.concurrent.atomic.AtomicReference<DocxExportReport> report = new java.util.concurrent.atomic.AtomicReference<>();
        String body = export(report, List.of(DocumentTableCell.node(
                new com.demcha.compose.document.dsl.EllipseBuilder().name("Icon").circle(8)
                        .fillColor(com.demcha.compose.document.style.DocumentColor.rgb(26, 86, 148)).build())
                .withStyle(white)));

        assertThat(body).contains("w:fill=\"FFFFFF\"");
        assertThat(anchors(body)).singleElement().asString().contains("behindDoc=\"0\"");
    }

    @Test
    void aBoxLostInACellIsStillReportedWhenItsTableDrawsSomethingElse() throws Exception {
        // A rectangle framing text inside a layer stack is no panel, and its table leaves it to one.
        java.util.concurrent.atomic.AtomicReference<DocxExportReport> report = new java.util.concurrent.atomic.AtomicReference<>();
        export(report, List.of(
                DocumentTableCell.node(new com.demcha.compose.document.dsl.EllipseBuilder().name("Icon").circle(8)
                        .fillColor(com.demcha.compose.document.style.DocumentColor.rgb(26, 86, 148)).build()),
                DocumentTableCell.node(new com.demcha.compose.document.dsl.LayerStackBuilder().name("Pill")
                        .layer(new com.demcha.compose.document.dsl.ShapeBuilder().name("PillBox").size(60, 14)
                                .fillColor(com.demcha.compose.document.style.DocumentColor.rgb(26, 86, 148)).build())
                        .layer(new com.demcha.compose.document.dsl.ParagraphBuilder().text("Open").build())
                        .build())));

        assertThat(report.get().count(DocxExportReport.Severity.DROPPED)).as("the box, not lost in silence")
                .isPositive();
    }

    @Test
    void aBoxInACellBesideOneItsTableLeavesToAPanelIsDrawnAndNotReportedDropped() throws Exception {
        // The table leaves the panel's box to the panel and draws the tile and the box beside it;
        // asked of the whole table, both were reported dropped while it drew them.
        com.demcha.compose.document.style.DocumentColor ink = com.demcha.compose.document.style.DocumentColor.rgb(26, 86, 148);
        java.util.concurrent.atomic.AtomicReference<DocxExportReport> report = new java.util.concurrent.atomic.AtomicReference<>();
        String body = export(report, List.of(
                DocumentTableCell.node(new com.demcha.compose.document.dsl.ShapeContainerBuilder().name("Panel")
                        .roundedRect(80, 30, 6).fillColor(com.demcha.compose.document.style.DocumentColor.rgb(238, 243, 249))
                        .center(new com.demcha.compose.document.dsl.ParagraphBuilder().text("Open").build()).build()),
                DocumentTableCell.node(new com.demcha.compose.document.dsl.ShapeContainerBuilder().name("Tile")
                        .rectangle(30, 30).fillColor(ink)
                        .center(new com.demcha.compose.document.dsl.EllipseBuilder().circle(10)
                                .fillColor(com.demcha.compose.document.style.DocumentColor.WHITE).build()).build()),
                DocumentTableCell.node(new com.demcha.compose.document.dsl.ShapeBuilder().name("Box").size(20, 20)
                        .fillColor(ink).build())));

        assertThat(report.get().count(DocxExportReport.Severity.DROPPED)).isZero();
        assertThat(anchors(body)).as("the tile's box and its dot, and the box").hasSize(3);
    }

    @Test
    void aBoxReachingPastItsCellIsStillReportedWhereItsTableLeftIt() throws Exception {
        // A fixed box wider than its column stands in its cell though the cell does not hold it.
        com.demcha.compose.document.style.DocumentColor ink = com.demcha.compose.document.style.DocumentColor.rgb(26, 86, 148);
        java.util.concurrent.atomic.AtomicReference<DocxExportReport> report = new java.util.concurrent.atomic.AtomicReference<>();
        try (com.demcha.compose.document.api.DocumentSession session = com.demcha.compose.GraphCompose.document()
                .pageSize(400, 600).margin(DocumentInsets.of(20)).create()) {
            session.pageFlow(page -> page.addTable(t -> t
                    .columns(DocumentTableColumn.fixed(60), DocumentTableColumn.fixed(60))
                    .rowCells(
                            DocumentTableCell.node(new com.demcha.compose.document.dsl.EllipseBuilder().name("Icon")
                                    .circle(8).fillColor(ink).build()),
                            DocumentTableCell.node(new com.demcha.compose.document.dsl.LayerStackBuilder().name("Pill")
                                    .layer(new com.demcha.compose.document.dsl.ShapeBuilder().name("PillBox")
                                            .size(140, 14).fillColor(ink).build())
                                    .layer(new com.demcha.compose.document.dsl.ParagraphBuilder().text("Open").build())
                                    .build()))));
            session.export(new DocxSemanticBackend(report::set));
        }

        assertThat(report.get().bySubject().get("ShapeNode")).as("the box, not lost in silence")
                .singleElement().extracting(DocxExportReport.Note::severity)
                .isEqualTo(DocxExportReport.Severity.DROPPED);
    }

    @Test
    void aBoxPushedOutOfEveryCellIsStillReported() throws Exception {
        // Pulled above its table by a negative margin, it stands in no cell the report can ask.
        com.demcha.compose.document.style.DocumentColor ink = com.demcha.compose.document.style.DocumentColor.rgb(26, 86, 148);
        java.util.concurrent.atomic.AtomicReference<DocxExportReport> report = new java.util.concurrent.atomic.AtomicReference<>();
        try (com.demcha.compose.document.api.DocumentSession session = com.demcha.compose.GraphCompose.document()
                .pageSize(400, 600).margin(DocumentInsets.of(20)).create()) {
            session.pageFlow(page -> page.addParagraph("Above").addSpacer(s -> s.height(80)).addTable(t -> t
                    .columns(DocumentTableColumn.fixed(60), DocumentTableColumn.fixed(160))
                    .rowCells(
                            DocumentTableCell.node(new com.demcha.compose.document.dsl.EllipseBuilder().name("Icon")
                                    .circle(8).fillColor(ink).build()),
                            DocumentTableCell.node(new com.demcha.compose.document.dsl.LayerStackBuilder().name("Pill")
                                    .margin(new DocumentInsets(-60, 0, 60, 0))
                                    .layer(new com.demcha.compose.document.dsl.ShapeBuilder().name("PillBox")
                                            .size(60, 14).fillColor(ink).build())
                                    .layer(new com.demcha.compose.document.dsl.ParagraphBuilder().text("Open").build())
                                    .build()))));
            session.export(new DocxSemanticBackend(report::set));
        }

        assertThat(report.get().bySubject().get("ShapeNode")).as("the box, not lost in silence")
                .singleElement().extracting(DocxExportReport.Note::severity)
                .isEqualTo(DocxExportReport.Severity.DROPPED);
    }

    @Test
    void aFlatPathInACellIsDrawnAndAGradientOneIsStillReported() throws Exception {
        // An SVG icon carries its colour as fillColor, not as a paint; only a gradient goes undrawn.
        java.util.concurrent.atomic.AtomicReference<DocxExportReport> report = new java.util.concurrent.atomic.AtomicReference<>();
        String body = export(report, List.of(DocumentTableCell.node(triangle()
                .fillColor(com.demcha.compose.document.style.DocumentColor.rgb(26, 86, 148)).name("Icon").build())));

        assertThat(anchors(body)).singleElement().asString().contains("1A5694");
        assertThat(report.get().count(DocxExportReport.Severity.DROPPED)).isZero();

        export(report, List.of(
                DocumentTableCell.node(new com.demcha.compose.document.dsl.EllipseBuilder().name("Dot").circle(8)
                        .fillColor(com.demcha.compose.document.style.DocumentColor.rgb(26, 86, 148)).build()),
                DocumentTableCell.node(triangle().name("Glow").fill(com.demcha.compose.document.style.DocumentPaint.linear(
                        com.demcha.compose.document.style.DocumentColor.WHITE,
                        com.demcha.compose.document.style.DocumentColor.rgb(26, 86, 148))).build())));

        assertThat(report.get().count(DocxExportReport.Severity.DROPPED)).as("the gradient path").isPositive();
    }

    @Test
    void aRowHoldingPaddedContentKeepsTheHeightThePageGaveIt() throws Exception {
        // The padding is the composed section's, not the cell's: Word, given only the text,
        // closed the row round it.
        try (XWPFDocument document = exportDocument(List.of(DocumentTableCell.node(
                new com.demcha.compose.document.dsl.SectionBuilder()
                        .name("Padded").padding(com.demcha.compose.document.style.DocumentInsets.of(12))
                        .addParagraph(p -> p.text("Coastline Advanced")).build())))) {
            assertThat(heightOf(document.getTables().get(0))).as("the text and 24pt of padding")
                    .isGreaterThan(24 * 20 + 8 * 20);
        }
    }

    @Test
    void aRowsHeightIsWrittenLessTheMarginsItsCellsHold() throws Exception {
        // Two rows the page makes equally tall: padded inside the composed content, and padded as
        // the cell. LibreOffice adds a cell's margins to the written height, so the second row's
        // is written less them.
        DocumentInsets twelve = new DocumentInsets(12, 0, 12, 0);
        try (com.demcha.compose.document.api.DocumentSession session = com.demcha.compose.GraphCompose.document()
                .pageSize(400, 600).margin(DocumentInsets.of(20)).create()) {
            session.pageFlow(page -> page
                    .addTable(t -> t.columns(DocumentTableColumn.fixed(200))
                            .rowCells(DocumentTableCell.node(new com.demcha.compose.document.dsl.SectionBuilder()
                                    .name("Padded").padding(twelve)
                                    .addParagraph(p -> p.text("Row")).build())
                                    .withStyle(com.demcha.compose.document.table.DocumentTableStyle.builder()
                                            .padding(DocumentInsets.zero()).build())))
                    .addTable(t -> t.columns(DocumentTableColumn.fixed(200))
                            .rowCells(DocumentTableCell.node(new com.demcha.compose.document.dsl.ParagraphBuilder()
                                    .text("Row").build())
                                    .withStyle(com.demcha.compose.document.table.DocumentTableStyle.builder()
                                            .padding(twelve).build()))));
            byte[] docx = session.export(new DocxSemanticBackend());
            try (XWPFDocument document = new XWPFDocument(new java.io.ByteArrayInputStream(docx))) {
                int inside = heightOf(document.getTables().get(0));
                int asTheCell = heightOf(document.getTables().get(1));

                // 12pt above and below, less the default 1pt rule on each side that Word gives
                // room of its own, and less what the second cell's first line rose into its
                // padding: the margin left is that much less, and the row's height that much more.
                assertThat(inside - asTheCell).as("the second cell's 22pt of margins")
                        .isEqualTo(22 * 20 - DocxExports.DEFAULT_LINE_RAISE);
            }
        }
    }

    @Test
    void aParagraphHoldingAPictureKeepsItsOwnLineFromALaterOneOfTheSameText() throws Exception {
        // Its laid-out text is its text runs'. Left unpaired, it left its 8pt line for the 16pt
        // "Paid" after it, whose text was then clipped to that line.
        try (XWPFDocument document = exportDocument(List.of(
                DocumentTableCell.node(new com.demcha.compose.document.dsl.ParagraphBuilder()
                        .inlineImage(DocumentImageData.fromBytes(pngBytes()), 6, 6)
                        .inlineText("Paid", com.demcha.compose.document.style.DocumentTextStyle.DEFAULT.withSize(8))
                        .build()),
                DocumentTableCell.node(new com.demcha.compose.document.dsl.ParagraphBuilder().text("Paid")
                        .textStyle(com.demcha.compose.document.style.DocumentTextStyle.DEFAULT.withSize(16)).build())))) {
            List<org.apache.poi.xwpf.usermodel.XWPFParagraph> paid = cellParagraphs(document, "Paid");

            assertThat(paid).hasSize(2);
            assertThat(lineOf(paid.get(1))).as("a 16pt line, not the 8pt one").isGreaterThan(14 * 20);
        }
    }

    /** A table's first row's written height, which the export writes "at least". */
    private static int heightOf(XWPFTable table) {
        org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTrPr properties = table.getRow(0).getCtRow().getTrPr();
        assertThat(properties).isNotNull();
        assertThat(properties.sizeOfTrHeightArray()).isEqualTo(1);
        org.openxmlformats.schemas.wordprocessingml.x2006.main.CTHeight height = properties.getTrHeightArray(0);
        assertThat(height.getHRule()).isEqualTo(org.openxmlformats.schemas.wordprocessingml.x2006.main.STHeightRule.AT_LEAST);
        return ((Number) height.getVal()).intValue();
    }

    /** A paragraph's written line height, in twips. */
    private static int lineOf(org.apache.poi.xwpf.usermodel.XWPFParagraph paragraph) {
        return ((Number) paragraph.getCTP().getPPr().getSpacing().getLine()).intValue();
    }

    /** The paragraphs of the body's tables reading a text, in document order. */
    private static List<org.apache.poi.xwpf.usermodel.XWPFParagraph> cellParagraphs(XWPFDocument document, String text) {
        List<org.apache.poi.xwpf.usermodel.XWPFParagraph> found = new java.util.ArrayList<>();
        for (XWPFTable table : document.getTables()) {
            for (org.apache.poi.xwpf.usermodel.XWPFTableRow row : table.getRows()) {
                for (XWPFTableCell cell : row.getTableCells()) {
                    for (org.apache.poi.xwpf.usermodel.XWPFParagraph paragraph : cell.getParagraphs()) {
                        if (paragraph.getText().equals(text)) {
                            found.add(paragraph);
                        }
                    }
                }
            }
        }
        return found;
    }

    @Test
    void theSameTextComposedTwiceTakesEachItsOwnLine() throws Exception {
        // Paragraphs are paired with their table's fragments in the order both were laid out.
        try (XWPFDocument document = exportDocument(List.of(
                DocumentTableCell.node(new com.demcha.compose.document.dsl.ParagraphBuilder().text("0.00")
                        .textStyle(com.demcha.compose.document.style.DocumentTextStyle.DEFAULT.withSize(8)).build()),
                DocumentTableCell.node(new com.demcha.compose.document.dsl.ParagraphBuilder().text("0.00")
                        .textStyle(com.demcha.compose.document.style.DocumentTextStyle.DEFAULT.withSize(16)).build())))) {
            List<org.apache.poi.xwpf.usermodel.XWPFParagraph> amounts = cellParagraphs(document, "0.00");

            assertThat(amounts).hasSize(2).allSatisfy(amount -> assertThat(
                    amount.getCTP().getPPr().getSpacing().getLineRule()).hasToString("exact"));
            assertThat(lineOf(amounts.get(1))).isEqualTo(2 * lineOf(amounts.get(0)));
        }
    }

    /** The export of a table of one row: a text cell, then the composed ones. */
    private static XWPFDocument exportDocument(List<DocumentTableCell> composed) throws Exception {
        try (com.demcha.compose.document.api.DocumentSession session = com.demcha.compose.GraphCompose.document()
                .pageSize(400, 600).margin(DocumentInsets.of(20)).create()) {
            List<DocumentTableCell> cells = new java.util.ArrayList<>();
            cells.add(DocumentTableCell.text("Mon"));
            cells.addAll(composed);
            List<DocumentTableColumn> columns = new java.util.ArrayList<>();
            for (int i = 0; i < cells.size(); i++) {
                columns.add(DocumentTableColumn.auto());
            }
            session.pageFlow(page -> page.addTable(t -> t
                    .columns(columns.toArray(DocumentTableColumn[]::new))
                    .rowCells(cells)));
            return new XWPFDocument(new java.io.ByteArrayInputStream(session.export(new DocxSemanticBackend())));
        }
    }

    private static com.demcha.compose.document.dsl.PathBuilder triangle() {
        return new com.demcha.compose.document.dsl.PathBuilder().size(10, 10)
                .moveTo(0, 0).lineTo(10, 0).lineTo(5, 10).closePath();
    }

    private static String export(java.util.concurrent.atomic.AtomicReference<DocxExportReport> report,
                                 List<DocumentTableCell> composed) throws Exception {
        try (com.demcha.compose.document.api.DocumentSession session = com.demcha.compose.GraphCompose.document()
                .pageSize(400, 600).margin(com.demcha.compose.document.style.DocumentInsets.of(20)).create()) {
            List<DocumentTableCell> cells = new java.util.ArrayList<>();
            cells.add(DocumentTableCell.text("Mon"));
            cells.addAll(composed);
            List<DocumentTableColumn> columns = new java.util.ArrayList<>();
            for (int i = 0; i < cells.size(); i++) {
                columns.add(DocumentTableColumn.auto());
            }
            session.pageFlow(page -> page.addTable(t -> t
                    .columns(columns.toArray(DocumentTableColumn[]::new))
                    .rowCells(cells)));
            byte[] docx = session.export(new DocxSemanticBackend(report::set));
            try (XWPFDocument document = new XWPFDocument(new java.io.ByteArrayInputStream(docx))) {
                return document.getDocument().xmlText();
            }
        }
    }

    private static String export(java.util.concurrent.atomic.AtomicReference<DocxExportReport> report,
                                 DocumentTableCell composed) throws Exception {
        try (com.demcha.compose.document.api.DocumentSession session = com.demcha.compose.GraphCompose.document()
                .pageSize(400, 600).margin(com.demcha.compose.document.style.DocumentInsets.of(20)).create()) {
            session.pageFlow(page -> page.addTable(t -> t
                    .columns(DocumentTableColumn.auto(), DocumentTableColumn.auto())
                    .rowCells(DocumentTableCell.text("Mon"), composed)));
            byte[] docx = session.export(new DocxSemanticBackend(report::set));
            try (XWPFDocument document = new XWPFDocument(new java.io.ByteArrayInputStream(docx))) {
                return document.getDocument().xmlText();
            }
        }
    }

    private static List<String> anchors(String xml) {
        List<String> anchors = new java.util.ArrayList<>();
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("<wp:anchor .*?</wp:anchor>", java.util.regex.Pattern.DOTALL).matcher(xml);
        while (matcher.find()) {
            anchors.add(matcher.group());
        }
        return anchors;
    }

    private static DocumentNode pill() {
        return new com.demcha.compose.document.dsl.ShapeContainerBuilder()
                .name("Chip")
                .roundedRect(60, 14, 7)
                .fillColor(com.demcha.compose.document.style.DocumentColor.rgb(26, 86, 148))
                .center(new com.demcha.compose.document.dsl.ParagraphBuilder().text("09:00-17:00").build())
                .build();
    }

    private static DocumentNode image() {
        return new com.demcha.compose.document.dsl.ImageBuilder()
                .name("Logo")
                .source(DocumentImageData.fromBytes(pngBytes()))
                .width(40)
                .height(20)
                .build();
    }

    private static DocumentNode list(String... items) {
        return new com.demcha.compose.document.dsl.ListBuilder()
                .name("Items")
                .bullet()
                .items(items)
                .build();
    }

    private static DocumentNode innerTable() {
        return new com.demcha.compose.document.dsl.TableBuilder()
                .name("Inner")
                .columns(DocumentTableColumn.auto(), DocumentTableColumn.auto())
                .row("Hours", "12")
                .build();
    }

    private static byte[] pngBytes() {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(new BufferedImage(40, 20, BufferedImage.TYPE_INT_RGB), "png", out);
            return out.toByteArray();
        } catch (Exception failure) {
            throw new IllegalStateException(failure);
        }
    }

    /** The second cell of the only table — the one these cases compose. */
    private static XWPFTableCell onlyTableCell(Consumer<PageFlowBuilder> content) throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, content)) {
            assertThat(document.getTables()).hasSize(1);
            XWPFTable table = document.getTables().get(0);
            return table.getRow(0).getCell(table.getRow(0).getTableCells().size() - 1);
        }
    }
}
