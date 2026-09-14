package com.demcha.compose.document.backend.fixed.pdf;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.api.MultiSectionDocument;
import com.demcha.compose.document.output.DocumentProtection;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentLetterSpacing;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.table.DocumentTableColumn;
import com.demcha.compose.document.table.DocumentTableStyle;
import org.apache.fontbox.ttf.TTFParser;
import org.apache.fontbox.ttf.TrueTypeFont;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static com.demcha.compose.document.backend.fixed.pdf.LetterSpacedPdf.REGULAR_RESOURCE;
import static com.demcha.compose.document.backend.fixed.pdf.LetterSpacedPdf.Shown;
import static com.demcha.compose.document.backend.fixed.pdf.LetterSpacedPdf.actualTexts;
import static com.demcha.compose.document.backend.fixed.pdf.LetterSpacedPdf.assertDrawnLikeCharacterSpacing;
import static com.demcha.compose.document.backend.fixed.pdf.LetterSpacedPdf.characterSpacingReference;
import static com.demcha.compose.document.backend.fixed.pdf.LetterSpacedPdf.descendant;
import static com.demcha.compose.document.backend.fixed.pdf.LetterSpacedPdf.gapBefore;
import static com.demcha.compose.document.backend.fixed.pdf.LetterSpacedPdf.letterSpacedResources;
import static com.demcha.compose.document.backend.fixed.pdf.LetterSpacedPdf.render;
import static com.demcha.compose.document.backend.fixed.pdf.LetterSpacedPdf.shownRuns;
import static com.demcha.compose.document.backend.fixed.pdf.LetterSpacedPdf.style;
import static com.demcha.compose.document.backend.fixed.pdf.LetterSpacedPdf.text;
import static com.demcha.compose.document.backend.fixed.pdf.LetterSpacedPdf.widths;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Letter spacing carried in glyph widths: same picture as {@code Tc}, no gaps for a reader.
 *
 * <p>Native letter spacing draws a run with {@code Tc}. That places every glyph correctly, but
 * the added space sits between the glyph boxes a reader derives from the font's widths, and
 * readers that ignore {@code ActualText} (pdf.js, pdfminer) read a tracked heading as single
 * letters. A positive tracked run is therefore drawn with a font resource over the same embedded
 * program whose widths include the tracking. These tests hold that representation to its
 * promises for every text path that draws it &mdash; paragraph runs, table cells and highlight
 * chips: every glyph lands where {@code Tc} would put it, the text layer says exactly what was
 * written, the page carries the widths and not the gap, protection and sections keep it, and one
 * resource serves a face and tracking for the whole document. What falls back to {@code Tc} is
 * held by {@link PdfLetterSpacedFontFallbackTest}.</p>
 */
class PdfLetterSpacedFontTest {

    private static final String PASSWORD = "keep-out";

    // --- A. geometry ----------------------------------------------------------------------

    @Test
    void everyGlyphLandsWhereCharacterSpacingWouldPutIt() throws Exception {
        record Case(String text, boolean bold, double size, double points) {
        }
        List<Case> cases = List.of(
                new Case("PROFESSIONAL SUMMARY", true, 9.6, 1.73),   // 0.18em as a CV banner resolves it
                new Case("ARTEM DEMCHYSHYN", false, 21.5, 3.87),    // 0.18em at a headline size
                new Case("SMALL POSITIVE TRACKING", false, 12, 0.24), // 0.02em
                new Case("POINT TRACKING", false, 10, 1.2),
                new Case("POINT TRACKING", false, 14, 1.2),
                new Case("POINT TRACKING", false, 24, 1.2));

        for (Case c : cases) {
            DocumentTextStyle tracked = style(c.bold(), c.size(), DocumentLetterSpacing.points(c.points()));
            DocumentTextStyle marker = style(c.bold(), c.size(), DocumentLetterSpacing.NONE);
            byte[] rendered = render(page -> page.addParagraph(p -> p
                    .inlineText(c.text(), tracked)
                    .inlineText("X", marker)));
            List<DrawnPen.Placement> drawn = DrawnPen.placements(rendered);
            assertThat(drawn).hasSize(c.text().length() + 1);

            // The same string, drawn from the same origin with the base font and Tc: the
            // representation this replaces. The trailing marker is where the pen stood after
            // the run, so it also pins the run's full advance, trailing unit included.
            List<DrawnPen.Placement> expected = DrawnPen.placements(characterSpacingReference(
                    c.text(), c.bold(), c.size(), c.points(), drawn.get(0).x(), drawn.get(0).y()));
            assertThat(expected).hasSameSizeAs(drawn);
            for (int i = 0; i != drawn.size(); i++) {
                assertThat(drawn.get(i).x())
                        .as("%s at %s pt tracked %s pt: glyph %d x", c.text(), c.size(), c.points(), i)
                        .isCloseTo(expected.get(i).x(), within(0.01));
                assertThat(drawn.get(i).y())
                        .as("%s at %s pt: glyph %d y", c.text(), c.size(), i)
                        .isCloseTo(expected.get(i).y(), within(0.01));
            }
        }
    }

    @Test
    void aReaderFindsNoGapBetweenTrackedLetters() throws Exception {
        byte[] rendered = render(page -> page.addParagraph(p -> p
                .text("PROFESSIONAL SUMMARY").textStyle(style(true, 9.6, DocumentLetterSpacing.points(1.73)))));
        List<DrawnPen.Placement> drawn = DrawnPen.placements(rendered);
        // The gap a reader measures: next origin minus this origin plus this glyph's width.
        // With widths that carry the tracking only the sub-thousandth remainder is left.
        for (int i = 1; i != drawn.size(); i++) {
            assertThat(gapBefore(drawn, i)).as("gap before glyph %d", i).isCloseTo(0.0, within(0.005));
        }

        // The same heading drawn with Tc leaves the whole tracking between the boxes: the
        // gap this test exists to catch, so it cannot pass on a page that still has it.
        List<DrawnPen.Placement> reference = DrawnPen.placements(characterSpacingReference(
                "PROFESSIONAL SUMMARY", true, 9.6, 1.73, drawn.get(0).x(), drawn.get(0).y()));
        assertThat(gapBefore(reference, 1)).isCloseTo(1.73, within(0.01));
    }

    @Test
    void aTrackedTableCellIsDrawnLikeCharacterSpacingWithoutTheGaps() throws Exception {
        byte[] rendered = render(page -> page.addTable(table -> table
                .columns(DocumentTableColumn.fixed(300))
                .defaultCellStyle(DocumentTableStyle.builder()
                        .textStyle(style(true, 9.6, DocumentLetterSpacing.points(1.73))).build())
                .row("TECHNICAL SKILLS")));

        assertDrawnLikeCharacterSpacing(rendered, "TECHNICAL SKILLS", true, 9.6, 1.73);
        try (PDDocument document = Loader.loadPDF(rendered)) {
            assertThat(letterSpacedResources(document.getPage(0))).hasSize(1);
        }
    }

    @Test
    void aTrackedHighlightChipIsDrawnLikeCharacterSpacingWithoutTheGaps() throws Exception {
        byte[] rendered = render(page -> page.addParagraph(p -> p
                .inlineHighlight("EDUCATION", style(true, 9.6, DocumentLetterSpacing.points(1.73)),
                        DocumentColor.rgb(230, 230, 240), 4, DocumentInsets.of(2))));

        assertDrawnLikeCharacterSpacing(rendered, "EDUCATION", true, 9.6, 1.73);
        try (PDDocument document = Loader.loadPDF(rendered)) {
            assertThat(letterSpacedResources(document.getPage(0))).hasSize(1);
        }
    }

    // --- B. text layer --------------------------------------------------------------------

    @Test
    void theTextLayerSaysExactlyWhatWasWritten() throws Exception {
        DocumentTextStyle banner = style(true, 9.6, DocumentLetterSpacing.points(1.73));
        DocumentTextStyle regular = style(false, 9.6, DocumentLetterSpacing.points(1.73));

        assertThat(text(render(page -> page.addParagraph(p -> p.text("PROFESSIONAL SUMMARY").textStyle(banner)))))
                .isEqualTo("PROFESSIONAL SUMMARY");
        assertThat(text(render(page -> page.addParagraph(p -> p
                .inlineText("PROFESSIONAL ", banner)
                .inlineText("EXPERIENCE", regular)))))
                .isEqualTo("PROFESSIONAL EXPERIENCE");
        assertThat(text(render(page -> page.addTable(table -> table
                .columns(DocumentTableColumn.fixed(300))
                .defaultCellStyle(DocumentTableStyle.builder().textStyle(banner).build())
                .row("TECHNICAL SKILLS")))))
                .isEqualTo("TECHNICAL SKILLS");
        assertThat(text(render(page -> page.addParagraph(p -> p
                .inlineHighlight("EDUCATION", banner, DocumentColor.rgb(230, 230, 240), 4, DocumentInsets.of(2))))))
                .isEqualTo("EDUCATION");
    }

    // --- C. structure ---------------------------------------------------------------------

    @Test
    void theTrackedRunUsesWidenedWidthsOverTheSameFontProgram() throws Exception {
        // 1.8pt at 10pt is exactly 180 thousandths of an em, so no remainder is left for Tc.
        byte[] rendered = render(page -> {
            page.addParagraph(p -> p.text("PROFESSIONAL SUMMARY").textStyle(style(true, 10, DocumentLetterSpacing.points(1.8))));
            page.addParagraph(p -> p.text("Professional summary").textStyle(style(true, 10, DocumentLetterSpacing.NONE)));
        });

        try (PDDocument document = Loader.loadPDF(rendered)) {
            PDPage page = document.getPage(0);
            COSDictionary fonts = page.getResources().getCOSObject().getCOSDictionary(COSName.FONT);
            List<COSDictionary> spaced = letterSpacedResources(page);
            assertThat(spaced).as("one letter-spaced font resource").hasSize(1);
            String viewName = null;
            String baseName = null;
            for (COSName name : fonts.keySet()) {
                if (descendant(fonts, name) == spaced.get(0)) {
                    viewName = name.getName();
                } else {
                    baseName = name.getName();
                }
            }
            assertThat(viewName).as("a letter-spaced font resource").isNotNull();
            assertThat(baseName).as("the base font resource").isNotNull();

            COSDictionary view = descendant(fonts, COSName.getPDFName(viewName));
            COSDictionary base = descendant(fonts, COSName.getPDFName(baseName));
            assertThat(view.getCOSDictionary(COSName.FONT_DESC).getDictionaryObject(COSName.FONT_FILE2))
                    .as("the resource draws with the base font program, not a copy")
                    .isSameAs(base.getCOSDictionary(COSName.FONT_DESC).getDictionaryObject(COSName.FONT_FILE2));
            assertThat(view.getDictionaryObject(COSName.CID_TO_GID_MAP))
                    .isSameAs(base.getDictionaryObject(COSName.CID_TO_GID_MAP));
            assertThat(fonts.getCOSDictionary(COSName.getPDFName(viewName)).getDictionaryObject(COSName.TO_UNICODE))
                    .isSameAs(fonts.getCOSDictionary(COSName.getPDFName(baseName)).getDictionaryObject(COSName.TO_UNICODE));

            assertThat(view.getInt(COSName.DW)).isEqualTo(base.getInt(COSName.DW, 1000) + 180);
            Map<Integer, Float> viewWidths = widths(view);
            Map<Integer, Float> baseWidths = widths(base);
            assertThat(viewWidths.keySet()).isEqualTo(baseWidths.keySet());
            for (Map.Entry<Integer, Float> entry : baseWidths.entrySet()) {
                assertThat(viewWidths.get(entry.getKey()))
                        .as("width of CID %d", entry.getKey())
                        .isEqualTo(entry.getValue() + 180f);
            }

            List<Shown> shown = shownRuns(page);
            String finalViewName = viewName;
            String finalBaseName = baseName;
            assertThat(shown).anySatisfy(run -> {
                assertThat(run.font()).isEqualTo(finalViewName);
                assertThat(run.characterSpacing()).as("no Tc carries the tracking").isZero();
            });
            assertThat(shown).anySatisfy(run -> assertThat(run.font()).isEqualTo(finalBaseName));
            assertThat(actualTexts(page))
                    .as("ActualText is kept for the readers that honour it")
                    .contains("PROFESSIONAL SUMMARY");
        }
    }

    // --- D. saving ------------------------------------------------------------------------

    @Test
    void aProtectedDocumentKeepsItsLetterSpacedResourcesUnderPassword() throws Exception {
        // The resources are completed between two saves, so protection has to wait for the
        // second: applied to the first, the real save would encrypt the document twice.
        byte[] pdf;
        try (DocumentSession document = GraphCompose.document()
                .pageSize(595, 842)
                .margin(DocumentInsets.of(40))
                .create()) {
            document.protect(DocumentProtection.builder().userPassword(PASSWORD).build());
            document.pageFlow(page -> page.addParagraph(p -> p
                    .text("PROFESSIONAL SUMMARY").textStyle(style(true, 10, DocumentLetterSpacing.points(1.8)))));
            pdf = document.toPdfBytes();
        }

        try (PDDocument opened = Loader.loadPDF(pdf, PASSWORD)) {
            assertThat(opened.isEncrypted()).as("the protection reached the file").isTrue();
            assertThat(letterSpacedResources(opened.getPage(0))).hasSize(1);
            assertThat(new PDFTextStripper().getText(opened).trim()).isEqualTo("PROFESSIONAL SUMMARY");
        }
    }

    // --- E. reuse -------------------------------------------------------------------------

    @Test
    void identicalTrackedStylesShareOneResource() throws Exception {
        DocumentTextStyle banner = style(true, 9.6, DocumentLetterSpacing.points(1.73));
        byte[] rendered = render(page -> {
            page.addParagraph(p -> p.text("PROFESSIONAL SUMMARY").textStyle(banner));
            page.addParagraph(p -> p.text("TECHNICAL SKILLS").textStyle(banner));
            page.addParagraph(p -> p.text("PROJECTS").textStyle(banner));
        });
        try (PDDocument document = Loader.loadPDF(rendered)) {
            assertThat(letterSpacedResources(document.getPage(0))).hasSize(1);
        }
    }

    @Test
    void emTrackingSharesAResourceAcrossSizesWhilePointTrackingDoesNot() throws Exception {
        // 0.18em resolves to 1.8pt at 10pt and 3.6pt at 20pt: 180 thousandths of an em both times.
        byte[] em = render(page -> {
            page.addParagraph(p -> p.text("TEN POINTS").textStyle(style(false, 10, DocumentLetterSpacing.ofFontSize(0.18))));
            page.addParagraph(p -> p.text("TWENTY POINTS").textStyle(style(false, 20, DocumentLetterSpacing.ofFontSize(0.18))));
        });
        // 1.2pt is 120 thousandths of an em at 10pt and 60 at 20pt: two different widths.
        byte[] points = render(page -> {
            page.addParagraph(p -> p.text("TEN POINTS").textStyle(style(false, 10, DocumentLetterSpacing.points(1.2))));
            page.addParagraph(p -> p.text("TWENTY POINTS").textStyle(style(false, 20, DocumentLetterSpacing.points(1.2))));
        });
        try (PDDocument emDocument = Loader.loadPDF(em); PDDocument pointsDocument = Loader.loadPDF(points)) {
            assertThat(letterSpacedResources(emDocument.getPage(0))).hasSize(1);
            assertThat(letterSpacedResources(pointsDocument.getPage(0))).hasSize(2);
        }
    }

    @Test
    void pagesShareTheResourceOfTheirDocument() throws Exception {
        DocumentTextStyle banner = style(true, 18, DocumentLetterSpacing.points(3));
        byte[] rendered;
        try (DocumentSession session = GraphCompose.document()
                .pageSize(300, 120)
                .margin(DocumentInsets.of(10))
                .create()) {
            session.pageFlow(page -> {
                for (int i = 0; i != 8; i++) {
                    page.addParagraph(p -> p.text("SECTION HEADING").textStyle(banner));
                }
            });
            rendered = session.toPdfBytes();
        }
        try (PDDocument document = Loader.loadPDF(rendered)) {
            assertThat(document.getNumberOfPages()).isGreaterThan(1);
            List<COSDictionary> first = letterSpacedResources(document.getPage(0));
            List<COSDictionary> last = letterSpacedResources(document.getPage(document.getNumberOfPages() - 1));
            assertThat(first).hasSize(1);
            assertThat(last).hasSize(1);
            assertThat(last.get(0)).isSameAs(first.get(0));
        }
    }

    @Test
    void sectionsShareTheResourceOfTheirDocument() throws Exception {
        DocumentTextStyle banner = style(true, 18, DocumentLetterSpacing.points(3));
        byte[] rendered;
        try (MultiSectionDocument document = GraphCompose.documents()
                .section(trackedSection(banner))
                .section(trackedSection(banner))
                .create()) {
            rendered = document.toPdfBytes();
        }
        try (PDDocument opened = Loader.loadPDF(rendered)) {
            assertThat(opened.getNumberOfPages()).isEqualTo(2);
            List<COSDictionary> first = letterSpacedResources(opened.getPage(0));
            List<COSDictionary> second = letterSpacedResources(opened.getPage(1));
            assertThat(first).hasSize(1);
            assertThat(second).hasSize(1);
            assertThat(second.get(0)).as("one registry for the combined document").isSameAs(first.get(0));
        }
    }

    @Test
    void aFaceHandedOutEarlierStandsForItsBase() throws Exception {
        // Asking again with a letter-spaced face must not stack its widths on the spacing it carries.
        try (PDDocument document = new PDDocument();
             InputStream program = PdfLetterSpacedFontTest.class.getResourceAsStream(REGULAR_RESOURCE)) {
            TrueTypeFont ttf = new TTFParser().parse(new RandomAccessReadBuffer(program.readAllBytes()));
            ttf.setEnableGsub(false);
            PDType0Font base = PDType0Font.load(document, ttf, true);
            PdfTrackedFontResources resources = new PdfTrackedFontResources(document);

            PdfRenderEnvironment.LetterSpacedFont first = resources.resolve(base, 10, 1.8, "AB");
            assertThat(first).isNotNull();
            PdfRenderEnvironment.LetterSpacedFont again = resources.resolve(first.font(), 10, 1.8, "AB");
            assertThat(again).isNotNull();
            assertThat(again.font()).isSameAs(first.font());
            assertThat(again.characterSpacing()).isZero();
        }
    }

    private static DocumentSession trackedSection(DocumentTextStyle style) {
        DocumentSession section = GraphCompose.document()
                .pageSize(300, 200)
                .margin(DocumentInsets.of(20))
                .create();
        section.pageFlow(page -> page.addParagraph(p -> p.text("SECTION HEADING").textStyle(style)));
        return section;
    }
}
