package com.demcha.compose.document.backend.fixed.pptx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentLetterSpacing;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.table.DocumentTableColumn;
import com.demcha.compose.font.FontName;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFTextParagraph;
import org.apache.poi.xslf.usermodel.XSLFTextRun;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.junit.jupiter.api.Test;
import org.openxmlformats.schemas.drawingml.x2006.main.CTRegularTextRun;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tracking through the PPTX backend, as DrawingML's own {@code spc}.
 *
 * <p>The unit and the advance rule were measured by exporting a probe deck
 * through real PowerPoint and reading the glyph positions out of the PDF it
 * produced: {@code spc} is hundredths of a point, and PowerPoint spends one unit
 * per code point with the trailing one included — the same N rule the engine
 * measures with, so a line laid out against that measurement arrives at the
 * width it was given. What is asserted here is that the backend writes the
 * value that measurement calls for, and leaves the text alone.</p>
 */
class PptxLetterSpacingTest {

    private static final String NAME = "JANE DOE";
    private static final FontName FAMILY = FontName.LATO;
    private static final Pattern SPC = Pattern.compile("spc=\"(-?[0-9]+)\"");

    private static DocumentTextStyle style(DocumentLetterSpacing spacing, double size) {
        return DocumentTextStyle.builder()
                .fontName(FAMILY).size(size).letterSpacing(spacing).build();
    }

    @Test
    void pointsBecomeHundredthsOfAPoint() throws Exception {
        // 1.25pt -> 125. The unit is the format's, not the engine's.
        assertThat(spcOf(render(NAME, style(DocumentLetterSpacing.points(1.25), 20))))
                .containsExactly(125);
    }

    @Test
    void aFontSizeShareArrivesResolvedAgainstTheStylesSize() throws Exception {
        // 12% of 20pt = 2.4pt -> 240. The share is resolved before the backend
        // ever sees it; PPTX is handed points.
        assertThat(spcOf(render(NAME, style(DocumentLetterSpacing.ofFontSize(0.12), 20))))
                .containsExactly(240);
    }

    @Test
    void theSameShareAtADifferentSizeResolvesDifferently() throws Exception {
        // 12% of 24pt = 2.88pt -> 288, so the value really does follow the size
        // rather than being a constant the backend made up.
        assertThat(spcOf(render(NAME, style(DocumentLetterSpacing.ofFontSize(0.12), 24))))
                .containsExactly(288);
    }

    @Test
    void negativeTrackingIsWrittenAsANegativeValue() throws Exception {
        assertThat(spcOf(render(NAME, style(DocumentLetterSpacing.points(-0.75), 20))))
                .containsExactly(-75);
    }

    @Test
    void noTrackingWritesNoAttributeAtAll() throws Exception {
        // Absence is the default, so a deck without tracking carries exactly the
        // run properties it carried before this existed.
        byte[] pptx = render(NAME, style(DocumentLetterSpacing.NONE, 20));

        assertThat(spcOf(pptx)).isEmpty();
        assertThat(runsOf(pptx)).isNotEmpty();
    }

    @Test
    void aValueBetweenUnitsRoundsToTheNearestHundredth() throws Exception {
        // 1/3 pt = 0.3333... -> 33 hundredths. Rounded, not truncated toward
        // zero, and the residue is a hundredth of a point.
        assertThat(spcOf(render(NAME, style(DocumentLetterSpacing.points(1.0 / 3.0), 20))))
                .containsExactly(33);
    }

    @Test
    void theTextIsTheAuthorsTextAndNothingElse() throws Exception {
        byte[] tracked = render(NAME, style(DocumentLetterSpacing.points(4), 20));

        // Not "J A N E   D O E", and not eight runs of one letter either.
        assertThat(textOf(tracked)).isEqualTo(NAME);
    }

    @Test
    void aTrackedRunAndAnUntrackedOneDoNotAffectEachOther() throws Exception {
        // Each PPTX run carries its own rPr, so there is no shared state to
        // leak — but that is a claim about the format, and this holds it.
        byte[] pptx = renderDocument(page -> page.addParagraph(p -> p
                .inlineText("AAAA", style(DocumentLetterSpacing.points(5), 20))
                .inlineText("BBBB", style(DocumentLetterSpacing.NONE, 20))));

        List<XSLFTextRun> runs = runsOf(pptx);
        List<Integer> spacings = new ArrayList<>();
        for (XSLFTextRun run : runs) {
            spacings.add(spcOf(run));
        }
        assertThat(spacings).containsExactly(500, null);
        assertThat(textOf(pptx)).isEqualTo("AAAABBBB");
    }

    @Test
    void aMarkdownStyledRunKeepsTheTrackingOfTheParagraphItCameFrom() throws Exception {
        // Markdown is parsed in ParagraphWrapping, on the fixed-layout path, and
        // it builds each emphasised run by copying components off the paragraph's
        // style. Copying four of five would drop the tracking on exactly the
        // words an author bothered to emphasise.
        byte[] pptx = renderDocument(page -> page.addParagraph(p -> p
                .text("PLAIN **BOLD**")
                .textStyle(style(DocumentLetterSpacing.points(3), 20))));

        assertThat(runsOf(pptx)).hasSizeGreaterThan(1);
        assertThat(spcOf(pptx))
                .isNotEmpty()
                .allMatch(value -> value == 300);
    }

    @Test
    void aTableCellCarriesTrackingThroughTheSameSeam() throws Exception {
        byte[] pptx = renderDocument(page -> page.addTable(t -> t
                .columns(DocumentTableColumn.fixed(300))
                .defaultCellStyle(com.demcha.compose.document.table.DocumentTableStyle.builder()
                        .textStyle(style(DocumentLetterSpacing.points(2), 20)).build())
                .row(NAME)));

        assertThat(spcOf(pptx)).contains(200);
        assertThat(textOf(pptx)).contains(NAME);
    }

    // --- helpers ---------------------------------------------------------

    private static byte[] render(String text, DocumentTextStyle style) throws Exception {
        return renderDocument(page -> page.addParagraph(p -> p.text(text).textStyle(style)));
    }

    private static byte[] renderDocument(
            Consumer<com.demcha.compose.document.dsl.PageFlowBuilder> body) throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(500, 200)
                .margin(DocumentInsets.of(20))
                .create()) {
            session.pageFlow(body);
            return session.render(new PptxFixedLayoutBackend());
        }
    }

    /** Every {@code spc} actually written, in run order. */
    private static List<Integer> spcOf(byte[] pptx) throws Exception {
        List<Integer> values = new ArrayList<>();
        for (XSLFTextRun run : runsOf(pptx)) {
            Integer spc = spcOf(run);
            if (spc != null) {
                values.add(spc);
            }
        }
        return values;
    }

    /**
     * The {@code spc} attribute as the file spells it.
     *
     * <p>Read out of the serialized XML rather than through the schema getter,
     * which returns the union type {@code Object}: the question this asks is
     * what the document says, and the attribute is the answer.</p>
     */
    private static Integer spcOf(XSLFTextRun run) {
        if (!(run.getXmlObject() instanceof CTRegularTextRun ctRun)) {
            return null;
        }
        Matcher matcher = SPC.matcher(ctRun.xmlText());
        return matcher.find() ? Integer.valueOf(matcher.group(1)) : null;
    }

    private static List<XSLFTextRun> runsOf(byte[] pptx) throws Exception {
        List<XSLFTextRun> runs = new ArrayList<>();
        try (XMLSlideShow show = new XMLSlideShow(new ByteArrayInputStream(pptx))) {
            for (XSLFShape shape : show.getSlides().get(0).getShapes()) {
                if (shape instanceof XSLFTextShape textShape) {
                    for (XSLFTextParagraph paragraph : textShape.getTextParagraphs()) {
                        runs.addAll(paragraph.getTextRuns());
                    }
                }
            }
        }
        return runs;
    }

    private static String textOf(byte[] pptx) throws Exception {
        StringBuilder text = new StringBuilder();
        for (XSLFTextRun run : runsOf(pptx)) {
            text.append(run.getRawText());
        }
        return text.toString();
    }
}
