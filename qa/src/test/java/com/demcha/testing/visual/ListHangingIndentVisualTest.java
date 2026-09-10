package com.demcha.testing.visual;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.font.FontName;
import com.demcha.compose.testing.visual.PdfVisualRegression;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * The one thing geometry assertions cannot show: what a hanging indent looks
 * like. Three markers of visibly different widths, each on an item long enough
 * to wrap three or more times, so the alignment of the wrapped lines under their
 * own first line is the picture rather than a number.
 *
 * <p>Deliberately a small page. A full-page baseline drifts across platforms by
 * more than the signal it is supposed to carry; a tight one does not.</p>
 */
class ListHangingIndentVisualTest {

    private static final DocumentColor INK = DocumentColor.rgb(28, 36, 52);
    private static final DocumentColor PAPER = DocumentColor.rgb(253, 252, 250);

    /**
     * A bundled face, deliberately — the baseline depends on it.
     *
     * <p>A Standard-14 font like Helvetica carries no font file into the PDF, so
     * a renderer substitutes whatever the host provides and the same document
     * rasterises differently on Windows and on Linux; a stored baseline then
     * fails everywhere it was not recorded. A bundled face is embedded as a
     * subset, so every platform draws the same outlines. Every other text-heavy
     * baseline in this repo is bundled-font for the same reason.</p>
     */
    private static final DocumentTextStyle BODY = DocumentTextStyle.builder()
            .fontName(FontName.LATO)
            .size(9)
            .color(INK)
            .build();

    @Test
    void wrappedLinesHangUnderTheirOwnFirstLineWhateverTheMarkerWidth() throws Exception {
        byte[] pdf = sheet();

        // The baseline is only portable while the font travels with the document.
        // Asserted rather than assumed: a substituted face rasterises differently
        // per host, and the failure would land on whichever machine did not
        // record the baseline — which is a confusing way to learn it.
        assertEveryFontIsEmbedded(pdf);

        PdfVisualRegression.standard().assertMatchesBaseline("list-hanging-indent", pdf);

        Path out = Path.of("target/visual-tests/list-hanging-indent/list-hanging-indent.pdf");
        Files.createDirectories(out.getParent());
        Files.write(out, pdf);
    }

    private static void assertEveryFontIsEmbedded(byte[] pdf) throws Exception {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            PDResources resources = document.getPage(0).getResources();
            for (COSName name : resources.getFontNames()) {
                PDFont font = resources.getFont(name);
                assertThat(font.isEmbedded())
                        .as("%s must be embedded, or this baseline is host-specific", font.getName())
                        .isTrue();
            }
        }
    }

    private static byte[] sheet() throws Exception {
        try (DocumentSession document = GraphCompose.document()
                .pageSize(240, 260)
                .pageBackground(PAPER)
                .margin(DocumentInsets.of(16))
                .create()) {
            document.pageFlow()
                    .name("HangingIndentSheet")
                    .spacing(10)
                    .addList(l -> l
                            .name("Bullet")
                            .bullet()
                            .hangingIndent(true)
                            .textStyle(BODY)
                            .itemSpacing(4)
                            .items("Every wrapped line of this item begins directly beneath the first "
                                   + "word of its own first line, not beneath the bullet."))
                    .addList(l -> l
                            .name("Dash")
                            .dash()
                            .hangingIndent(true)
                            .textStyle(BODY)
                            .itemSpacing(4)
                            .items("A dash is narrower than a bullet, so its content column starts "
                                   + "further left, and its wrapped lines follow it there."))
                    .addList(l -> l
                            .name("Wide")
                            .marker("=>")
                            .hangingIndent(true)
                            .markerGap(6)
                            .textStyle(BODY)
                            .itemSpacing(4)
                            .items("A wide marker pushes the whole content column right by exactly its "
                                   + "own measured width plus the gap, and the wrapped lines move with it."))
                    .build();
            return document.toPdfBytes();
        }
    }
}
