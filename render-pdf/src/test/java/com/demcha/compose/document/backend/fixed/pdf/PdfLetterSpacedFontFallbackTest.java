package com.demcha.compose.document.backend.fixed.pdf;

import com.demcha.compose.document.node.TextDirection;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentLetterSpacing;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.table.DocumentTableCell;
import com.demcha.compose.document.table.DocumentTableColumn;
import com.demcha.compose.document.table.DocumentTableStyle;
import com.demcha.compose.font.FontName;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.demcha.compose.document.backend.fixed.pdf.LetterSpacedPdf.Shown;
import static com.demcha.compose.document.backend.fixed.pdf.LetterSpacedPdf.actualTexts;
import static com.demcha.compose.document.backend.fixed.pdf.LetterSpacedPdf.letterSpacedResources;
import static com.demcha.compose.document.backend.fixed.pdf.LetterSpacedPdf.render;
import static com.demcha.compose.document.backend.fixed.pdf.LetterSpacedPdf.shownRuns;
import static com.demcha.compose.document.backend.fixed.pdf.LetterSpacedPdf.style;
import static com.demcha.compose.document.backend.fixed.pdf.LetterSpacedPdf.text;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * The runs letter-spaced resources cannot serve keep the {@code Tc} path they always had.
 *
 * <p>Each case is one the representation deliberately leaves alone &mdash; tightening, a face with
 * no embedded program, text drawn in visual order, a tracking too small for a whole thousandth of
 * an em, and text a face's GSUB substitutions would rewrite &mdash; plus the decision a table cell
 * makes once for all its lines. A case that wrongly took the widths would show up as a
 * letter-spaced resource on the page or as a missing {@code Tc}, which is what these assert.</p>
 */
class PdfLetterSpacedFontFallbackTest {

    private static final DocumentColor CHIP = DocumentColor.rgb(230, 230, 240);

    @Test
    void negativeTrackingKeepsCharacterSpacing() throws Exception {
        byte[] rendered = render(page -> page.addParagraph(p -> p
                .text("TIGHT HEADING").textStyle(style(false, 20, DocumentLetterSpacing.points(-0.5)))));
        try (PDDocument document = Loader.loadPDF(rendered)) {
            PDPage page = document.getPage(0);
            assertThat(shownRuns(page)).anySatisfy(run ->
                    assertThat(run.characterSpacing()).isCloseTo(-0.5f, within(1.0e-4f)));
            assertThat(letterSpacedResources(page)).isEmpty();
        }
        assertThat(text(rendered)).isEqualTo("TIGHT HEADING");
    }

    @Test
    void aStandardFourteenFaceKeepsCharacterSpacingAndActualText() throws Exception {
        byte[] rendered = render(page -> page.addParagraph(p -> p
                .text("HELVETICA HEADING").textStyle(style(FontName.HELVETICA, 20, DocumentLetterSpacing.points(3)))));
        try (PDDocument document = Loader.loadPDF(rendered)) {
            PDPage page = document.getPage(0);
            assertThat(shownRuns(page)).anySatisfy(run ->
                    assertThat(run.characterSpacing()).isCloseTo(3f, within(1.0e-4f)));
            assertThat(actualTexts(page)).contains("HELVETICA HEADING");
            assertThat(letterSpacedResources(page)).isEmpty();
        }
    }

    @Test
    void aRightToLeftRunKeepsCharacterSpacing() throws Exception {
        byte[] rendered = render(page -> page.addParagraph(p -> p
                .text("שלום עולם")
                .direction(TextDirection.RTL)
                .textStyle(style(FontName.DAVID_LIBRE, 20, DocumentLetterSpacing.points(3)))));
        try (PDDocument document = Loader.loadPDF(rendered)) {
            PDPage page = document.getPage(0);
            assertThat(shownRuns(page)).anySatisfy(run ->
                    assertThat(run.characterSpacing()).isCloseTo(3f, within(1.0e-4f)));
            assertThat(letterSpacedResources(page)).isEmpty();
        }
    }

    @Test
    void aHighlightChipWhoseTextNeedsBidiKeepsCharacterSpacing() throws Exception {
        // A chip drawn in visual order keeps Tc like any reordered run; David Libre could serve it.
        byte[] rendered = render(page -> page.addParagraph(p -> p
                .inlineHighlight("שלום", style(FontName.DAVID_LIBRE, 20, DocumentLetterSpacing.points(3)),
                        CHIP, 4, DocumentInsets.of(2))));
        try (PDDocument document = Loader.loadPDF(rendered)) {
            PDPage page = document.getPage(0);
            assertThat(shownRuns(page)).anySatisfy(run ->
                    assertThat(run.characterSpacing()).isCloseTo(3f, within(1.0e-4f)));
            assertThat(letterSpacedResources(page)).isEmpty();
        }
    }

    @Test
    void trackingBelowHalfAThousandthOfAnEmKeepsCharacterSpacing() throws Exception {
        // 0.01pt, the finest tracking fixed layout states, is 0.42 thousandths of a 24pt em: there
        // is no whole thousandth for the widths to carry.
        byte[] rendered = render(page -> page.addParagraph(p -> p
                .text("QUIET TRACKING").textStyle(style(false, 24, DocumentLetterSpacing.points(0.01)))));
        try (PDDocument document = Loader.loadPDF(rendered)) {
            PDPage page = document.getPage(0);
            assertThat(shownRuns(page)).anySatisfy(run ->
                    assertThat(run.characterSpacing()).isCloseTo(0.01f, within(1.0e-4f)));
            assertThat(page.getResources().getFontNames())
                    .as("only the base font, not a resource with nothing added to its widths")
                    .hasSize(1);
        }
    }

    @Test
    void aFaceWhoseSubstitutionsServeAnotherScriptStillCarriesLatinSpacingInItsWidths() throws Exception {
        // FontBox keeps Poppins' GSUB for Devanagari. Its worker leaves a Latin heading's glyphs
        // unchanged, so the letter-spaced face draws exactly what the base face would.
        byte[] rendered = render(page -> page.addParagraph(p -> p
                .text("ARTEM DEMCHYSHYN").textStyle(style(FontName.POPPINS, 24, DocumentLetterSpacing.points(4.32)))));
        try (PDDocument document = Loader.loadPDF(rendered)) {
            assertThat(letterSpacedResources(document.getPage(0))).hasSize(1);
        }
        assertThat(text(rendered)).isEqualTo("ARTEM DEMCHYSHYN");
    }

    @Test
    void aRunTheSubstitutionsWouldRewriteKeepsCharacterSpacing() throws Exception {
        // Devanagari conjuncts are what that worker rewrites. A resource that encodes through the
        // character map alone would draw other glyphs, so this run keeps the base face and Tc.
        byte[] rendered = render(page -> page.addParagraph(p -> p
                .text("क्षत्रिय").textStyle(style(FontName.POPPINS, 24, DocumentLetterSpacing.points(2)))));
        try (PDDocument document = Loader.loadPDF(rendered)) {
            PDPage page = document.getPage(0);
            assertThat(shownRuns(page)).anySatisfy(run ->
                    assertThat(run.characterSpacing()).isCloseTo(2f, within(1.0e-4f)));
            assertThat(letterSpacedResources(page)).isEmpty();
        }
    }

    @Test
    void aTableCellIsDecidedByTheTextItDraws() throws Exception {
        // A cell decides once, from the text of the lines it draws: the Latin cell takes the
        // letter-spaced face (2.16pt at 12pt is 180 thousandths), the Devanagari cell keeps Tc.
        DocumentTextStyle latin = style(FontName.POPPINS, 12, DocumentLetterSpacing.points(2.16));
        DocumentTextStyle devanagari = style(FontName.POPPINS, 12, DocumentLetterSpacing.points(1));
        byte[] rendered = render(page -> {
            page.addTable(table -> table
                    .columns(DocumentTableColumn.fixed(300))
                    .defaultCellStyle(DocumentTableStyle.builder().textStyle(latin).build())
                    .row("TECHNICAL SKILLS"));
            page.addTable(table -> table
                    .columns(DocumentTableColumn.fixed(300))
                    .defaultCellStyle(DocumentTableStyle.builder().textStyle(devanagari).build())
                    .row("क्षत्रिय"));
        });
        try (PDDocument document = Loader.loadPDF(rendered)) {
            PDPage page = document.getPage(0);
            assertThat(letterSpacedResources(page)).as("the Latin cell's resource, and only that").hasSize(1);
            assertThat(shownRuns(page)).anySatisfy(run ->
                    assertThat(run.characterSpacing()).isCloseTo(1f, within(1.0e-4f)));
        }
    }

    @Test
    void aTableCellWithOneLineTheFaceCannotServeKeepsCharacterSpacingOnEveryLine() throws Exception {
        // The first line alone would qualify; the second would draw other glyphs. One style, one
        // decision: the whole cell keeps Tc.
        byte[] rendered = render(page -> page.addTable(table -> table
                .columns(DocumentTableColumn.fixed(300))
                .defaultCellStyle(DocumentTableStyle.builder()
                        .textStyle(style(FontName.POPPINS, 12, DocumentLetterSpacing.points(1))).build())
                .rowCells(DocumentTableCell.lines("TECHNICAL SKILLS", "क्षत्रिय"))));
        try (PDDocument document = Loader.loadPDF(rendered)) {
            PDPage page = document.getPage(0);
            assertThat(letterSpacedResources(page)).isEmpty();
            assertThat(shownRuns(page)).hasSize(2).allSatisfy(run ->
                    assertThat(run.characterSpacing()).isCloseTo(1f, within(1.0e-4f)));
        }
    }

    @Test
    void aReorderedLineInATrackedCellSwitchesBackToCharacterSpacing() throws Exception {
        // Latin first keeps the cell left to right, so only the Hebrew line is drawn in visual
        // order. 3.6pt at 20pt is 180 thousandths of an em, so the Latin line carries no Tc.
        byte[] rendered = render(page -> page.addTable(table -> table
                .columns(DocumentTableColumn.fixed(300))
                .defaultCellStyle(DocumentTableStyle.builder()
                        .textStyle(style(FontName.DAVID_LIBRE, 20, DocumentLetterSpacing.points(3.6))).build())
                .rowCells(DocumentTableCell.lines("TOTAL", "שלום"))));
        try (PDDocument document = Loader.loadPDF(rendered)) {
            PDPage page = document.getPage(0);
            assertThat(letterSpacedResources(page)).hasSize(1);
            List<Shown> shown = shownRuns(page);
            assertThat(shown).hasSize(2);
            assertThat(shown.get(0).characterSpacing()).as("the Latin line: its widths carry the spacing").isZero();
            assertThat(shown.get(1).characterSpacing()).as("the Hebrew line: Tc carries it")
                    .isCloseTo(3.6f, within(1.0e-4f));
            assertThat(shown.get(1).font()).isNotEqualTo(shown.get(0).font());
        }
    }
}
