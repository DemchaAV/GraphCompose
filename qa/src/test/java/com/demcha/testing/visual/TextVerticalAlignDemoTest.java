package com.demcha.testing.visual;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.dsl.ShapeBuilder;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.node.TextVerticalAlign;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.font.FontName;
import com.demcha.compose.testing.visual.ImageDiff;
import com.demcha.compose.testing.visual.PdfVisualRegression;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies opt-in vertical text seating ({@link TextVerticalAlign}) inside a tall
 * shape-container "pill": the builder threads each mode through to the node, and
 * TOP / CENTER / BOTTOM visibly seat the rendered glyph at distinct heights
 * versus the default baseline. Also writes a large human-review comparison PDF.
 */
class TextVerticalAlignDemoTest {

    private static final DocumentColor PILL = DocumentColor.rgb(20, 60, 75);
    private static final DocumentColor PAPER = DocumentColor.rgb(252, 248, 240);
    private static final DocumentColor MUTED = DocumentColor.rgb(102, 106, 118);
    private static final DocumentColor GUIDE = DocumentColor.rgb(196, 153, 76);
    private static final PdfVisualRegression VISUAL = PdfVisualRegression.standard();

    @Test
    void verticalAlignThreadsThroughBuilderToNode() {
        assertThat(new ParagraphBuilder().text("Hi").build().verticalAlign())
                .isEqualTo(TextVerticalAlign.DEFAULT);
        for (TextVerticalAlign mode : TextVerticalAlign.values()) {
            assertThat(new ParagraphBuilder().text("Hi").verticalAlign(mode).build().verticalAlign())
                    .as("round-trip %s", mode)
                    .isEqualTo(mode);
        }
    }

    @Test
    void seatsRenderDistinctlyFromEachOther() throws Exception {
        BufferedImage dflt = render(TextVerticalAlign.DEFAULT);
        BufferedImage center = render(TextVerticalAlign.CENTER);
        BufferedImage bottom = render(TextVerticalAlign.BOTTOM);
        BufferedImage top = render(TextVerticalAlign.TOP);

        // descent > 0 makes the CENTER and BOTTOM shifts non-zero for any font,
        // and TOP vs BOTTOM seat the glyph at opposite ends of the line box.
        assertThat(ImageDiff.compare(dflt, center, 6).mismatchedPixelCount())
                .as("CENTER must move the glyph vs DEFAULT").isGreaterThan(50L);
        assertThat(ImageDiff.compare(dflt, bottom, 6).mismatchedPixelCount())
                .as("BOTTOM must move the glyph vs DEFAULT").isGreaterThan(50L);
        assertThat(ImageDiff.compare(top, bottom, 6).mismatchedPixelCount())
                .as("TOP and BOTTOM must seat the glyph at opposite ends").isGreaterThan(50L);

        Path out = Path.of("target/visual-tests/text-vertical-align/pill-seats.pdf");
        Files.createDirectories(out.getParent());
        Files.write(out, comparisonSheet());
    }

    private static BufferedImage render(TextVerticalAlign verticalAlign) throws Exception {
        return VISUAL.renderPages(pill(verticalAlign)).get(0);
    }

    private static byte[] pill(TextVerticalAlign verticalAlign) throws Exception {
        try (DocumentSession document = GraphCompose.document()
                .pageSize(180, 140)
                .pageBackground(PAPER)
                .margin(DocumentInsets.of(16))
                .create()) {
            document.pageFlow()
                    .name("PillSheet")
                    .addSection("PillRow", section -> section
                            .addContainer(card -> card
                                    .name("SeatPill")
                                    .roundedRect(140, 104, 26)
                                    .fillColor(PILL)
                                    .center(seatLabel(verticalAlign))))
                    .build();
            return document.toPdfBytes();
        }
    }

    private static byte[] comparisonSheet() throws Exception {
        try (DocumentSession document = GraphCompose.document()
                .pageSize(560, 200)
                .pageBackground(PAPER)
                .margin(DocumentInsets.of(16))
                .create()) {
            document.pageFlow()
                    .name("SeatComparison")
                    .addRow("Seats", row -> row
                            .spacing(12)
                            .evenWeights()
                            .addSection("ColDefault", col -> seatColumn(col, TextVerticalAlign.DEFAULT))
                            .addSection("ColTop", col -> seatColumn(col, TextVerticalAlign.TOP))
                            .addSection("ColCenter", col -> seatColumn(col, TextVerticalAlign.CENTER))
                            .addSection("ColBottom", col -> seatColumn(col, TextVerticalAlign.BOTTOM)))
                    .build();
            return document.toPdfBytes();
        }
    }

    // The gold guide line marks the pill's vertical centre, so CENTER is visibly
    // seated on it while TOP / BOTTOM sit above / below.
    private static void seatColumn(SectionBuilder col, TextVerticalAlign verticalAlign) {
        col.spacing(5)
                .addContainer(card -> card
                        .name("Pill" + verticalAlign)
                        .roundedRect(120, 104, 24)
                        .fillColor(PILL)
                        .center(guideLine())
                        .center(seatLabel(verticalAlign)))
                .addParagraph(p -> p
                        .text(verticalAlign.name())
                        .textStyle(caption())
                        .align(TextAlign.CENTER)
                        .margin(DocumentInsets.zero()));
    }

    private static DocumentNode guideLine() {
        return new ShapeBuilder().size(108, 1.4).fillColor(GUIDE).build();
    }

    private static DocumentNode seatLabel(TextVerticalAlign verticalAlign) {
        return new ParagraphBuilder()
                .text("GC")
                .textStyle(DocumentTextStyle.builder()
                        .fontName(FontName.HELVETICA_BOLD)
                        .decoration(DocumentTextDecoration.BOLD)
                        .size(46)
                        .color(DocumentColor.WHITE)
                        .build())
                .align(TextAlign.CENTER)
                .verticalAlign(verticalAlign)
                .margin(DocumentInsets.zero())
                .build();
    }

    private static DocumentTextStyle caption() {
        return DocumentTextStyle.builder()
                .fontName(FontName.HELVETICA)
                .size(9)
                .color(MUTED)
                .build();
    }
}
