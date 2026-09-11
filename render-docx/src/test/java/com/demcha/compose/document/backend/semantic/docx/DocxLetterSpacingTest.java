package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentLetterSpacing;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.font.FontName;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tracking through the DOCX backend, as Word's own run-level {@code w:spacing}.
 *
 * <p>The unit was measured by exporting a probe document through real Word and
 * reading the glyph positions out of the PDF it produced: {@code w:spacing} is
 * twentieths of a point, spent one unit per code point with the trailing one
 * included — the same rule PowerPoint and the PDF {@code Tc} operator follow.</p>
 *
 * <p>What is <em>not</em> asserted is any x coordinate. Word owns semantic
 * layout, and a DOCX export deliberately carries no measurement of its own; the
 * contract is that the tracking an author asked for arrives as the right native
 * value on the right run, and that the text is untouched.</p>
 */
class DocxLetterSpacingTest {

    private static final String NAME = "JANE DOE";
    private static final FontName FAMILY = FontName.LATO;
    /** {@code <w:spacing w:val="N"/>}, whatever prefix the serializer chose. */
    private static final Pattern SPACING =
            Pattern.compile("spacing[^/>]*val=\"(-?[0-9]+)\"");

    private static DocumentTextStyle style(DocumentLetterSpacing spacing, double size) {
        return DocumentTextStyle.builder()
                .fontName(FAMILY).size(size).letterSpacing(spacing).build();
    }

    @Test
    void pointsBecomeTwentiethsOfAPoint() throws Exception {
        // 1.25pt -> 25 twips.
        assertThat(spacingOf(render(NAME, style(DocumentLetterSpacing.points(1.25), 20))))
                .containsExactly(25);
    }

    @Test
    void aFontSizeShareArrivesResolvedAgainstTheStylesSize() throws Exception {
        // 12% of 20pt = 2.4pt -> 48 twips. The public value keeps the unit; the
        // backend resolves it here, because a semantic export never passes
        // through the engine style that would have resolved it already.
        assertThat(spacingOf(render(NAME, style(DocumentLetterSpacing.ofFontSize(0.12), 20))))
                .containsExactly(48);
    }

    @Test
    void theSameShareAtADifferentSizeResolvesDifferently() throws Exception {
        // 12% of 24pt = 2.88pt -> 57.6 -> 58 twips. Twentieths quantise to
        // 0.05pt; that is the format's granularity, not a rounding bug.
        assertThat(spacingOf(render(NAME, style(DocumentLetterSpacing.ofFontSize(0.12), 24))))
                .containsExactly(58);
    }

    @Test
    void negativeTrackingIsWrittenAsANegativeValue() throws Exception {
        assertThat(spacingOf(render(NAME, style(DocumentLetterSpacing.points(-0.75), 20))))
                .containsExactly(-15);
    }

    @Test
    void noTrackingWritesNoElementAtAll() throws Exception {
        // A document that never asks for tracking carries exactly the run
        // properties it carried before this existed.
        byte[] docx = render(NAME, style(DocumentLetterSpacing.NONE, 20));

        assertThat(spacingOf(docx)).isEmpty();
        assertThat(xmlOf(docx)).doesNotContain("<w:spacing");
        assertThat(runsOf(docx)).isNotEmpty();
    }

    @Test
    void theTextIsTheAuthorsTextAndNothingElse() throws Exception {
        byte[] tracked = render(NAME, style(DocumentLetterSpacing.points(4), 20));

        // Not "J A N E   D O E", and no padding run of spaces anywhere.
        assertThat(textOf(tracked)).isEqualTo(NAME);
        assertThat(xmlOf(tracked)).contains(NAME);
        assertThat(xmlOf(tracked)).doesNotContain("J A N E");
    }

    @Test
    void adjacentTrackedAndUntrackedRunsStayIndependent() throws Exception {
        byte[] docx = renderDocument(page -> page.addParagraph(p -> p
                .inlineText("AAAA", style(DocumentLetterSpacing.points(5), 20))
                .inlineText("BBBB", style(DocumentLetterSpacing.NONE, 20))));

        List<Integer> spacings = new ArrayList<>();
        for (XWPFRun run : runsOf(docx)) {
            spacings.add(spacingOf(run));
        }
        // w:spacing is a run property, so the untracked neighbour simply does
        // not carry one. There is no state to leak.
        assertThat(spacings).containsExactly(100, null);
        assertThat(textOf(docx)).isEqualTo("AAAABBBB");
    }

    // Markdown is deliberately not covered here: MarkDownParser runs inside
    // ParagraphWrapping, which is the fixed-layout path. A semantic export never
    // reaches it, so the markdown-keeps-its-tracking case belongs where markdown
    // is actually parsed — see PptxLetterSpacingTest.

    // --- helpers ---------------------------------------------------------

    private static byte[] render(String text, DocumentTextStyle style) throws Exception {
        return renderDocument(page -> page.addParagraph(p -> p.text(text).textStyle(style)));
    }

    private static byte[] renderDocument(
            Consumer<com.demcha.compose.document.dsl.PageFlowBuilder> body) throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(500, 300)
                .margin(DocumentInsets.of(20))
                .create()) {
            session.pageFlow(body);
            return session.export(new DocxSemanticBackend());
        }
    }

    private static List<Integer> spacingOf(byte[] docx) throws Exception {
        List<Integer> values = new ArrayList<>();
        for (XWPFRun run : runsOf(docx)) {
            Integer spacing = spacingOf(run);
            if (spacing != null) {
                values.add(spacing);
            }
        }
        return values;
    }

    /**
     * The {@code w:spacing} value as the file spells it.
     *
     * <p>Read out of the serialized XML rather than through the schema getter,
     * which returns the union type {@code Object}: the question this asks is
     * what the document says, and the element is the answer.</p>
     */
    private static Integer spacingOf(XWPFRun run) {
        Matcher matcher = SPACING.matcher(run.getCTR().xmlText());
        return matcher.find() ? Integer.valueOf(matcher.group(1)) : null;
    }

    private static List<XWPFRun> runsOf(byte[] docx) throws Exception {
        List<XWPFRun> runs = new ArrayList<>();
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(docx))) {
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                runs.addAll(paragraph.getRuns());
            }
        }
        return runs;
    }

    private static String textOf(byte[] docx) throws Exception {
        StringBuilder text = new StringBuilder();
        for (XWPFRun run : runsOf(docx)) {
            text.append(run.text());
        }
        return text.toString();
    }

    /** The body part as the file spells it. */
    private static String xmlOf(byte[] docx) throws Exception {
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(docx))) {
            return document.getDocument().xmlText();
        }
    }
}
