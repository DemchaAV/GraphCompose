package com.demcha.compose.document.backend.fixed.pdf;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.LineBuilder;
import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.dsl.TableBuilder;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.table.DocumentTableStyle;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.style.DocumentTextStyle;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The PDF backend draws underline/strikethrough marks and honours the alpha
 * channel on text, lines, and table paint — surfaces where it previously
 * rendered opaque plain glyphs while the PPTX backend already carried the
 * decoration and translucency. Each case renders a pair of documents that
 * differ only in the tested style and compares the rasterized ink.
 */
class PdfDecorationAndAlphaRenderTest {

    private static final int DARK = 120;

    private static BufferedImage render(Consumer<DocumentSession> content) throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(300, 160)
                .margin(DocumentInsets.of(20))
                .create()) {
            content.accept(session);
            return session.toImages(150).get(0);
        }
    }

    private static BufferedImage renderText(DocumentTextStyle style) throws Exception {
        // Caps only: no descenders, so every plain-ink row sits above the
        // baseline and an underline strictly extends the ink downward.
        return render(session -> session.add(new ParagraphBuilder().name("Sample")
                .text("SPEED LINES")
                .textStyle(style)
                .build()));
    }

    private static int darkPixels(BufferedImage image) {
        int count = 0;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if (luminance(image.getRGB(x, y)) < DARK) {
                    count++;
                }
            }
        }
        return count;
    }

    private static int lowestInkRow(BufferedImage image) {
        for (int y = image.getHeight() - 1; y >= 0; y--) {
            for (int x = 0; x < image.getWidth(); x++) {
                if (luminance(image.getRGB(x, y)) < DARK) {
                    return y;
                }
            }
        }
        return -1;
    }

    private static int highestInkRow(BufferedImage image) {
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if (luminance(image.getRGB(x, y)) < DARK) {
                    return y;
                }
            }
        }
        return -1;
    }

    private static int minLuminance(BufferedImage image) {
        int min = 255;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                min = Math.min(min, luminance(image.getRGB(x, y)));
            }
        }
        return min;
    }

    private static int luminance(int rgb) {
        return (((rgb >> 16) & 0xFF) + ((rgb >> 8) & 0xFF) + (rgb & 0xFF)) / 3;
    }

    @Test
    void underlineDrawsAMarkStrictlyBelowTheGlyphs() throws Exception {
        BufferedImage plain = renderText(DocumentTextStyle.builder().size(16).build());
        BufferedImage underlined = renderText(DocumentTextStyle.builder().size(16)
                .decoration(DocumentTextDecoration.UNDERLINE).build());
        assertThat(darkPixels(underlined))
                .as("the underline adds ink")
                .isGreaterThan(darkPixels(plain));
        assertThat(lowestInkRow(underlined))
                .as("the mark sits below the baseline, extending the ink downward")
                .isGreaterThan(lowestInkRow(plain));
    }

    @Test
    void strikethroughDrawsAMarkWithinTheGlyphBand() throws Exception {
        BufferedImage plain = renderText(DocumentTextStyle.builder().size(16).build());
        BufferedImage struck = renderText(DocumentTextStyle.builder().size(16)
                .decoration(DocumentTextDecoration.STRIKETHROUGH).build());
        assertThat(darkPixels(struck))
                .as("the strikethrough adds ink")
                .isGreaterThan(darkPixels(plain));
        assertThat(lowestInkRow(struck))
                .as("the mark crosses the glyphs instead of extending below them")
                .isCloseTo(lowestInkRow(plain), org.assertj.core.data.Offset.offset(1));
        assertThat(highestInkRow(struck))
                .as("the mark stays below the cap tops instead of floating above")
                .isCloseTo(highestInkRow(plain), org.assertj.core.data.Offset.offset(1));
    }

    @Test
    void aTranslucentTailDoesNotBleachAnEarlierRunsUnderline() throws Exception {
        // The mark of the opaque underlined run is painted after the whole
        // line's text — including the translucent tail whose gs alpha
        // survives ET. Without the ambient-alpha reset the mark inherits
        // that translucency and renders bleached.
        BufferedImage image = render(session -> session.add(new ParagraphBuilder().name("Mixed")
                .inlineText("MARKED", DocumentTextStyle.builder().size(16)
                        .decoration(DocumentTextDecoration.UNDERLINE).build())
                .inlineText(" GHOST", DocumentTextStyle.builder().size(16)
                        .color(DocumentColor.rgba(0, 0, 0, 40)).build())
                .build()));
        BufferedImage plain = render(session -> session.add(new ParagraphBuilder().name("Mixed")
                .inlineText("MARKED", DocumentTextStyle.builder().size(16).build())
                .inlineText(" GHOST", DocumentTextStyle.builder().size(16)
                        .color(DocumentColor.rgba(0, 0, 0, 40)).build())
                .build()));
        // The underline band lies strictly below every plain-ink row; its
        // darkest pixel must be as dark as the opaque glyphs above it.
        int bandTop = lowestInkRow(plain) + 1;
        int minInBand = 255;
        for (int y = bandTop; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                minInBand = Math.min(minInBand, luminance(image.getRGB(x, y)));
            }
        }
        assertThat(minInBand)
                .as("the opaque run's mark must not inherit the tail's alpha")
                .isLessThan(60);
    }

    @Test
    void underlinedChipTextDrawsAMark() throws Exception {
        DocumentTextStyle chipStyle = DocumentTextStyle.builder().size(14).build();
        DocumentTextStyle underlined = DocumentTextStyle.builder().size(14)
                .decoration(DocumentTextDecoration.UNDERLINE).build();
        BufferedImage plain = render(session -> session.add(new ParagraphBuilder().name("Chips")
                .inlineHighlight("SPEED", chipStyle, DocumentColor.rgb(230, 230, 240), 4,
                        DocumentInsets.of(3))
                .build()));
        BufferedImage marked = render(session -> session.add(new ParagraphBuilder().name("Chips")
                .inlineHighlight("SPEED", underlined, DocumentColor.rgb(230, 230, 240), 4,
                        DocumentInsets.of(3))
                .build()));
        assertThat(darkPixels(marked))
                .as("the chip glyph run carries its underline mark")
                .isGreaterThan(darkPixels(plain));
    }

    @Test
    void translucentTextRendersLighterThanOpaque() throws Exception {
        BufferedImage opaque = renderText(DocumentTextStyle.builder().size(16)
                .color(DocumentColor.rgb(0, 0, 0)).build());
        BufferedImage translucent = renderText(DocumentTextStyle.builder().size(16)
                .color(DocumentColor.rgba(0, 0, 0, 100)).build());
        assertThat(minLuminance(opaque)).isLessThan(60);
        assertThat(minLuminance(translucent))
                .as("40%-alpha glyphs must blend with the white page")
                .isGreaterThan(100);
    }

    @Test
    void translucentLineRendersLighterThanOpaque() throws Exception {
        BufferedImage opaque = render(session -> session.add(new LineBuilder()
                .name("Rule").horizontal(200).thickness(6)
                .color(DocumentColor.rgb(0, 0, 0)).build()));
        BufferedImage translucent = render(session -> session.add(new LineBuilder()
                .name("Rule").horizontal(200).thickness(6)
                .color(DocumentColor.rgba(0, 0, 0, 100)).build()));
        assertThat(minLuminance(opaque)).isLessThan(60);
        assertThat(minLuminance(translucent)).isGreaterThan(100);
    }

    @Test
    void translucentTableCellFillRendersLighterThanOpaque() throws Exception {
        // Empty cell text and a zero-width stroke: the fill is the only ink,
        // so the darkest pixel measures the fill itself.
        BufferedImage opaque = render(session -> session.add(new TableBuilder()
                .name("Fills")
                .defaultCellStyle(DocumentTableStyle.builder()
                        .fillColor(DocumentColor.rgb(0, 0, 160))
                        .stroke(com.demcha.compose.document.style.DocumentStroke.of(
                                DocumentColor.WHITE, 0))
                        .build())
                .row("")
                .build()));
        BufferedImage translucent = render(session -> session.add(new TableBuilder()
                .name("Fills")
                .defaultCellStyle(DocumentTableStyle.builder()
                        .fillColor(DocumentColor.rgba(0, 0, 160, 100))
                        .stroke(com.demcha.compose.document.style.DocumentStroke.of(
                                DocumentColor.WHITE, 0))
                        .build())
                .row("")
                .build()));
        assertThat(minLuminance(opaque)).isLessThan(80);
        assertThat(minLuminance(translucent)).isGreaterThan(100);
    }

    @Test
    void anOpaqueInlineShapeAfterATranslucentRunStaysFullyDark() throws Exception {
        // The dot draws in its own q..Q AFTER the translucent run's gs was
        // emitted; without the ambient-alpha reset before nested draws it
        // would inherit the run's translucency.
        BufferedImage image = render(session -> session.add(new ParagraphBuilder().name("DotLine")
                .inlineText("GHOST ", DocumentTextStyle.builder().size(16)
                        .color(DocumentColor.rgba(0, 0, 0, 40)).build())
                .dot(8, DocumentColor.rgb(0, 0, 0))
                .build()));
        assertThat(minLuminance(image))
                .as("the opaque dot must not inherit the translucent run's alpha")
                .isLessThan(60);
    }

    @Test
    void translucentTableBordersRenderLighterThanOpaque() throws Exception {
        BufferedImage opaque = render(session -> session.add(new TableBuilder()
                .name("Borders")
                .defaultCellStyle(DocumentTableStyle.builder()
                        .stroke(com.demcha.compose.document.style.DocumentStroke.of(
                                DocumentColor.rgb(0, 0, 0), 3))
                        .build())
                .row("")
                .build()));
        BufferedImage translucent = render(session -> session.add(new TableBuilder()
                .name("Borders")
                .defaultCellStyle(DocumentTableStyle.builder()
                        .stroke(com.demcha.compose.document.style.DocumentStroke.of(
                                DocumentColor.rgba(0, 0, 0, 100), 3))
                        .build())
                .row("")
                .build()));
        assertThat(minLuminance(opaque)).isLessThan(60);
        // Perpendicular borders double-blend where they cross at the cell
        // corners (1−(1−α)² ≈ 0.63), so the darkest translucent pixel sits
        // lower than a single pass — still far from opaque.
        assertThat(minLuminance(translucent)).isGreaterThan(80);
    }

    @Test
    void translucentTableCellTextRendersLighterThanOpaque() throws Exception {
        BufferedImage opaque = render(session -> session.add(new TableBuilder()
                .name("CellText")
                .defaultCellStyle(DocumentTableStyle.builder()
                        .textStyle(DocumentTextStyle.builder().size(14)
                                .color(DocumentColor.rgb(0, 0, 0)).build())
                        .stroke(com.demcha.compose.document.style.DocumentStroke.of(
                                DocumentColor.WHITE, 0))
                        .build())
                .row("INK")
                .build()));
        BufferedImage translucent = render(session -> session.add(new TableBuilder()
                .name("CellText")
                .defaultCellStyle(DocumentTableStyle.builder()
                        .textStyle(DocumentTextStyle.builder().size(14)
                                .color(DocumentColor.rgba(0, 0, 0, 100)).build())
                        .stroke(com.demcha.compose.document.style.DocumentStroke.of(
                                DocumentColor.WHITE, 0))
                        .build())
                .row("INK")
                .build()));
        assertThat(minLuminance(opaque)).isLessThan(60);
        assertThat(minLuminance(translucent)).isGreaterThan(100);
    }

    @Test
    void translucentSideBordersRenderLighterThanOpaque() throws Exception {
        // White text keeps the section non-empty without adding dark ink,
        // so the left accent border is the only measurable paint.
        DocumentTextStyle invisible = DocumentTextStyle.builder().size(12)
                .color(DocumentColor.WHITE).build();
        BufferedImage opaque = render(session -> session.dsl().pageFlow().name("Flow")
                .addSection("Card", section -> section
                        .borders(com.demcha.compose.document.style.DocumentBorders.left(
                                com.demcha.compose.document.style.DocumentStroke.of(
                                        DocumentColor.rgb(0, 0, 0), 5)))
                        .addParagraph(p -> p.text("HOLD").textStyle(invisible)))
                .build());
        BufferedImage translucent = render(session -> session.dsl().pageFlow().name("Flow")
                .addSection("Card", section -> section
                        .borders(com.demcha.compose.document.style.DocumentBorders.left(
                                com.demcha.compose.document.style.DocumentStroke.of(
                                        DocumentColor.rgba(0, 0, 0, 100), 5)))
                        .addParagraph(p -> p.text("HOLD").textStyle(invisible)))
                .build());
        assertThat(minLuminance(opaque)).isLessThan(60);
        assertThat(minLuminance(translucent)).isGreaterThan(100);
    }

    @Test
    void underlinedTableCellTextDrawsMarks() throws Exception {
        Consumer<DocumentSession> plainTable = session -> session.add(new TableBuilder()
                .name("Cells")
                .row("SPEED")
                .build());
        BufferedImage plain = render(plainTable);
        BufferedImage underlined = render(session -> session.add(new TableBuilder()
                .name("Cells")
                .defaultCellStyle(DocumentTableStyle.builder()
                        .textStyle(DocumentTextStyle.builder()
                                .decoration(DocumentTextDecoration.UNDERLINE).build())
                        .build())
                .row("SPEED")
                .build()));
        assertThat(darkPixels(underlined))
                .as("cell text decoration adds mark ink")
                .isGreaterThan(darkPixels(plain));
    }
}
