package com.demcha.compose.document.backend.fixed.pptx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.image.DocumentImageData;
import com.demcha.compose.document.layout.LayoutGraph;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.svg.SvgIcon;
import com.demcha.compose.font.FontFamilyDefinition;
import com.demcha.compose.font.FontName;
import com.demcha.compose.testing.layout.LayoutSnapshotAssertions;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Produces the reviewable PDF/PPTX text-fidelity pair and advisory previews. */
class PptxTextParityDemoTest {

    private static final double SCALE = 2.0;
    private static final FontName DEMO_FONT = FontName.of("PptxParityLato");
    private static final FontFamilyDefinition DEMO_FONT_FAMILY = FontFamilyDefinition.classpath(
                    DEMO_FONT, "fonts/google/lato/Lato-Regular.ttf")
            .boldResource("fonts/google/lato/Lato-Bold.ttf")
            .italicResource("fonts/google/lato/Lato-Italic.ttf")
            .boldItalicResource("fonts/google/lato/Lato-BoldItalic.ttf")
            .wordFamily("Lato")
            .build();

    @Test
    void writesTextFidelityDemoPairAndLocksItsLayout() throws Exception {
        Path output = Path.of("target", "visual-tests", "pptx-parity", "text-fidelity");
        Files.createDirectories(output);

        try (DocumentSession session = composeDemo()) {
            LayoutSnapshotAssertions.assertMatches(session,
                    Path.of("src", "test", "resources", "layout-snapshots"),
                    Path.of("target", "visual-tests", "layout-snapshots"),
                    "text-fidelity", "pptx-parity");

            LayoutGraph graph = session.render(new GraphCapturingBackend());
            byte[] pdf = session.toPdfBytes();
            byte[] pptx = session.render(new PptxFixedLayoutBackend());
            Files.write(output.resolve("text-fidelity.pdf"), pdf);
            Files.write(output.resolve("text-fidelity.pptx"), pptx);
            PptxGeometryAssertions.assertTextGeometryMatches(
                    graph, pptx, List.of(DEMO_FONT_FAMILY));

            List<BufferedImage> pdfPages = session.toImages((int) Math.round(72 * SCALE));
            try (XMLSlideShow show = new XMLSlideShow(new ByteArrayInputStream(pptx))) {
                assertThat(show.getSlides()).hasSameSizeAs(pdfPages);
                for (int i = 0; i < pdfPages.size(); i++) {
                    ImageIO.write(pdfPages.get(i), "png",
                            output.resolve("text-fidelity-page-" + (i + 1) + ".pdf.png").toFile());
                    ImageIO.write(rasterize(show.getSlides().get(i),
                                    show.getPageSize().width, show.getPageSize().height),
                            "png", output.resolve("text-fidelity-page-" + (i + 1) + ".pptx.png").toFile());
                }
            }
        }
    }

    private static DocumentSession composeDemo() throws Exception {
        DocumentSession session = GraphCompose.document()
                .registerFontFamily(DEMO_FONT_FAMILY)
                .pageSize(540, 300)
                .margin(DocumentInsets.of(28))
                .create();
        DocumentTextStyle body = DocumentTextStyle.builder()
                .fontName(DEMO_FONT).size(14).build();
        SvgIcon icon = SvgIcon.parse("""
                <svg viewBox="0 0 10 10"><path fill="#2563eb" d="M0 0 L10 0 L5 10 Z"/></svg>
                """);
        byte[] png = onePixelPng();
        session.pageFlow().name("TextFidelity").spacing(13)
                .addParagraph(p -> p.text("PPTX fixed-layout text fidelity")
                        .textStyle(DocumentTextStyle.builder()
                                .fontName(DEMO_FONT).size(22)
                                .decoration(DocumentTextDecoration.BOLD)
                                .color(DocumentColor.rgb(15, 23, 42)).build()))
                .addParagraph(p -> p.rich(r -> r
                        .plain("One measured line: ")
                        .bold("bold")
                        .plain(" · ")
                        .italic("italic")
                        .plain(" · ")
                        .underline("underlined")
                        .plain(" · ")
                        .strikethrough("struck")
                        .plain(" · ")
                        .link("external link", "https://graphcompose.dev"))
                        .textStyle(body))
                .addParagraph(p -> p.rich(r -> r
                        .plain("Inline geometry ")
                        .chip(" READY ", DocumentColor.WHITE, DocumentColor.ROYAL_BLUE)
                        .plain("  ")
                        .image(DocumentImageData.fromBytes(png), 15, 15)
                        .plain("  ")
                        .checkbox(15, true, DocumentColor.DARK_GRAY, DocumentColor.ROYAL_BLUE)
                        .plain("  ")
                        .svgIcon(icon, 15)
                        .plain(" stays on the shared baseline."))
                        .textStyle(body))
                .addParagraph(p -> p.text("Right-aligned frame uses the resolved line width.")
                        .align(TextAlign.RIGHT)
                        .textStyle(DocumentTextStyle.builder().fontName(DEMO_FONT).size(13).build()))
                .addParagraph(p -> p.text("Unsupported glyph sanitizes exactly like PDF: 😀")
                        .align(TextAlign.CENTER)
                        .textStyle(DocumentTextStyle.builder().fontName(DEMO_FONT).size(11).build()))
                .build();
        return session;
    }

    private static byte[] onePixelPng() throws Exception {
        BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        image.setRGB(0, 0, Color.ORANGE.getRGB());
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", output);
            return output.toByteArray();
        }
    }

    private static BufferedImage rasterize(XSLFSlide slide, int width, int height) {
        BufferedImage image = new BufferedImage((int) Math.round(width * SCALE),
                (int) Math.round(height * SCALE), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
            graphics.scale(SCALE, SCALE);
            slide.draw(graphics);
        } finally {
            graphics.dispose();
        }
        return image;
    }
}
