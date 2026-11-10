package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.output.DocumentHeaderFooter;
import com.demcha.compose.document.output.DocumentHeaderFooterZone;
import com.demcha.compose.document.style.DocumentInsets;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A text band's centred and right slots are spaced so Word sets them as wide as the page does.
 *
 * <p>Word states a type size in half points and stands a centred or right slot at a tab stop, so
 * its first letter moves with the difference: {@code CobaltRota}'s 7.28pt footer, set at 7.5pt,
 * stood 3.6pt left of the page's in Word.</p>
 */
class DocxBandSlotSpacingTest {

    private static final String RIGHT = "Quayside Bar  |  31 Aug - 6 Sep 2026";

    @Test
    void aRightOrCentredSlotIsSpacedByThePagesWidthLessWords() throws Exception {
        // 7.28pt is set at 7.5, wider; 7.7pt at 7.5, narrower.
        for (double size : new double[] {7.28, 7.7}) {
            int expected = twentieths(RIGHT, size);
            assertThat(expected).as("the premise: spaced at %spt", size).isNotZero();
            List<XWPFRun> right = slotRuns(footer(DocumentHeaderFooter.builder().leftText("Shifts").rightText(RIGHT), size),
                    "Quayside");
            List<XWPFRun> centre = slotRuns(footer(DocumentHeaderFooter.builder().leftText("Shifts").centerText(RIGHT), size),
                    "Quayside");

            assertThat(right).as("the right slot at %spt", size).isNotEmpty()
                    .allSatisfy(run -> assertThat(run.getCharacterSpacing()).isEqualTo(expected));
            assertThat(centre).as("the centre slot at %spt", size).isNotEmpty()
                    .allSatisfy(run -> assertThat(run.getCharacterSpacing()).isEqualTo(expected));
        }
    }

    @Test
    void theLeftSlotAPageNumberAndANearSlotAreLeftAsTheyAre() throws Exception {
        // The left slot starts where the page starts it. Word writes a page number's digits itself.
        // A short slot at 7.4pt, set at 7.5, grows by less than a point, though a twentieth a letter.
        String shortSlot = "Week 36 rota";
        XWPFDocument left = footer(DocumentHeaderFooter.builder().leftText(RIGHT).rightText("Shifts"), 7.28);
        XWPFDocument numbered = footer(DocumentHeaderFooter.builder().leftText("Shifts").rightText(RIGHT + " {page}"), 7.28);
        XWPFDocument near = footer(DocumentHeaderFooter.builder().leftText("Shifts").rightText(shortSlot), 7.4);
        assertThat(width(shortSlot, 7.4) * (7.5 / 7.4 - 1)).as("the premise: less than a point").isLessThan(1);
        assertThat(twentieths(shortSlot, 7.4)).as("the premise: spaced, but for the point").isNotZero();
        assertThat(twentieths(RIGHT, 7.28)).as("the premise: the same slot spaced as the right one").isNotZero();

        assertThat(slotRuns(left, "Quayside")).isNotEmpty().allSatisfy(run -> assertThat(run.getCharacterSpacing()).isZero());
        assertThat(slotRuns(numbered, "Quayside")).isNotEmpty().allSatisfy(run -> assertThat(run.getCharacterSpacing()).isZero());
        XWPFDocument counted = footer(DocumentHeaderFooter.builder().leftText("Shifts").centerText(RIGHT + " of {pages}"), 7.28);
        assertThat(slotRuns(counted, "Quayside")).isNotEmpty().allSatisfy(run -> assertThat(run.getCharacterSpacing()).isZero());
        assertThat(slotRuns(near, "Week")).isNotEmpty().allSatisfy(run -> assertThat(run.getCharacterSpacing()).isZero());
    }

    /** The page's width of a slot less Word's, a letter at a time, in twentieths of a point. */
    private static int twentieths(String text, double size) throws Exception {
        double asked = width(text, size);
        double set = asked * (Math.round(size * 2) / 2.0) / size;
        return (int) Math.round((asked - set) / text.codePointCount(0, text.length()) * 20);
    }

    /** The width the page draws a band's text at, in Helvetica, as its renderer measures it. */
    private static double width(String text, double size) throws Exception {
        return new PDType1Font(Standard14Fonts.FontName.HELVETICA).getStringWidth(text) / 1000 * size;
    }

    /** The runs of a footer's paragraph from the run whose text starts with the words given. */
    private static List<XWPFRun> slotRuns(XWPFDocument document, String start) {
        XWPFParagraph paragraph = document.getFooterList().stream()
                .flatMap(footer -> footer.getParagraphs().stream())
                .filter(p -> p.getText().contains(start)).findFirst().orElseThrow();
        List<XWPFRun> runs = new ArrayList<>();
        boolean in = false;
        for (XWPFRun run : paragraph.getRuns()) {
            String text = run.text();
            in |= text != null && text.startsWith(start);
            if (in && text != null && !text.isEmpty() && !text.contains("\t")) {
                runs.add(run);
            }
        }
        return runs;
    }

    private static XWPFDocument footer(DocumentHeaderFooter.DocumentHeaderFooterBuilder band, double size) throws Exception {
        byte[] docx;
        try (DocumentSession session = GraphCompose.document().pageSize(400, 300).margin(DocumentInsets.of(30)).create()) {
            session.footer(band.zone(DocumentHeaderFooterZone.FOOTER).height(20).fontSize((float) size).build());
            session.pageFlow(page -> page.addParagraph(p -> p.text("Body")));
            docx = session.export(new DocxSemanticBackend());
        }
        return new XWPFDocument(new ByteArrayInputStream(docx));
    }
}
