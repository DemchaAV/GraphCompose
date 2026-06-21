package com.demcha.compose.document.dsl;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.node.DocumentLinkOptions;
import com.demcha.compose.document.node.InlineImageAlignment;
import com.demcha.compose.document.node.InlineSvgRun;
import com.demcha.compose.document.svg.SvgIcon;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.contentstream.operator.Operator;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSNumber;
import org.apache.pdfbox.pdfparser.PDFStreamParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * End-to-end coverage for inline SVG-icon runs: the measure → tokenize → span →
 * PDF render pipeline must paint vector glyphs (colour emoji, marks) on the text
 * baseline without dropping them or substituting font glyphs.
 */
class InlineSvgRenderTest {

    /** A solid crimson square — distinctive against black text on a white page. */
    private static SvgIcon crimsonSquare() {
        return SvgIcon.parse("""
                <svg viewBox="0 0 24 24">
                  <path d="M2 2 H22 V22 H2 Z" fill="rgb(196, 30, 58)"/>
                </svg>
                """);
    }

    /** A gradient-filled square — exercises the inline gradient paint path. */
    private static SvgIcon gradientSquare() {
        return SvgIcon.parse("""
                <svg viewBox="0 0 10 10">
                  <defs>
                    <linearGradient id="g" gradientUnits="userSpaceOnUse" x1="0" y1="0" x2="10" y2="10">
                      <stop offset="0" stop-color="#A78BFA"/>
                      <stop offset="1" stop-color="#6128D9"/>
                    </linearGradient>
                  </defs>
                  <path d="M0 0 H10 V10 Z" fill="url(#g)"/>
                </svg>
                """);
    }

    @Test
    void inlineSvgRendersEndToEndKeepingTextWithoutGlyphSubstitution() throws Exception {
        byte[] pdf = renderIconRow(crimsonSquare());
        assertThat(pdf).isNotEmpty();

        try (PDDocument document = Loader.loadPDF(pdf)) {
            assertThat(document.getNumberOfPages()).isEqualTo(1);
            String text = new PDFTextStripper().getText(document);
            assertThat(text).contains("Ship it");
            assertThat(text).doesNotContain("?");
        }
    }

    @Test
    void inlineSvgPaintsItsFillColor() throws Exception {
        try (PDDocument document = Loader.loadPDF(renderIconRow(crimsonSquare()))) {
            BufferedImage image = new PDFRenderer(document).renderImageWithDPI(0, 144);
            // The crimson fill only enters the page through the inline icon — the
            // text is default black and the background white — so finding crimson
            // pixels proves the vector layer was drawn inline, not dropped.
            assertThat(containsColorNear(image, 196, 30, 58, 45))
                    .as("inline SVG icon must paint its fill colour")
                    .isTrue();
        }
    }

    @Test
    void gradientInlineSvgRendersAndPaints() throws Exception {
        try (PDDocument document = Loader.loadPDF(renderIconRow(gradientSquare()))) {
            assertThat(document.getNumberOfPages()).isEqualTo(1);
            BufferedImage image = new PDFRenderer(document).renderImageWithDPI(0, 144);
            // The violet gradient stops only reach the page through the inline
            // icon's shading, so a violet pixel proves the gradient paint path
            // runs inline as well as for block paths.
            assertThat(containsColorNear(image, 129, 80, 224, 60))
                    .as("inline gradient SVG must paint its shading")
                    .isTrue();
        }
    }

    @Test
    void offCanvasSvgGeometryIsClippedToTheViewBox() throws Exception {
        // A crimson square drawn entirely OUTSIDE the 10×10 viewBox (x 30..40 →
        // normalized x 3..4). SVG viewBox semantics clip it away; without the
        // glyph-box clip it would bleed several glyph-widths to the right and
        // smear onto neighbouring content (the :package: duplicate-box bug, where
        // Noto's working file parks off-canvas copies outside the viewBox).
        SvgIcon offCanvas = SvgIcon.parse("""
                <svg viewBox="0 0 10 10">
                  <path d="M30 0 H40 V10 H30 Z" fill="rgb(196, 30, 58)"/>
                </svg>
                """);
        try (PDDocument document = Loader.loadPDF(renderIconRow(offCanvas))) {
            BufferedImage image = new PDFRenderer(document).renderImageWithDPI(0, 144);
            assertThat(containsColorNear(image, 196, 30, 58, 45))
                    .as("off-canvas SVG geometry must be clipped to the viewBox, not bleed onto the page")
                    .isFalse();
        }
    }

    @Test
    void linkedInlineSvgEmitsClickableAnnotationSizedToTheIconBox() throws Exception {
        double iconSize = 6.0;
        byte[] pdf;
        try (DocumentSession session = GraphCompose.document()
                .pageSize(220, 120)
                .margin(14, 14, 14, 14)
                .create()) {
            session.dsl()
                    .pageFlow()
                    .name("Flow")
                    .addParagraph(paragraph -> paragraph
                            .inlineText("Home ")
                            .svgIcon(crimsonSquare(), iconSize, InlineImageAlignment.CENTER,
                                    0.0, new DocumentLinkOptions("https://example.com")))
                    .build();
            pdf = session.toPdfBytes();
        }

        try (PDDocument document = Loader.loadPDF(pdf)) {
            PDAnnotationLink link = (PDAnnotationLink) document.getPage(0).getAnnotations().stream()
                    .filter(PDAnnotationLink.class::isInstance)
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("no link annotation for the inline SVG"));
            // The clickable box must hug the icon (height == iconSize), not the
            // taller text line box — i.e. the SVG span takes the inline-graphic
            // rectangle path, not the text fallback.
            assertThat((double) link.getRectangle().getHeight())
                    .as("link rect hugs the icon, not the full line box")
                    .isCloseTo(iconSize, within(0.5));
        }
    }

    @Test
    void richTextSvgIconSizesByAspectRatio() {
        // A 20×10 viewBox is twice as wide as tall, so a size-of-10 icon measures
        // 20 wide × 10 tall — the run carries the aspect-correct box.
        SvgIcon wide = SvgIcon.parse("""
                <svg viewBox="0 0 20 10">
                  <path d="M0 0 H20 V10 H0 Z" fill="#c41e3a"/>
                </svg>
                """);
        InlineSvgRun run = onlySvgRun(RichText.text("x").svgIcon(wide, 10.0));

        assertThat(run.icon()).isSameAs(wide);
        assertThat(run.width()).isEqualTo(20.0, within(1e-6));
        assertThat(run.height()).isEqualTo(10.0, within(1e-6));
    }

    @Test
    void autoSizeReservesWidthForTheInlineSvgIcon() throws Exception {
        // Same auto-sized paragraph, with and without a wide (100×10) inline
        // icon. The icon must eat horizontal room, so the fitted text size is
        // strictly smaller with the icon present — proving the auto-size width
        // estimate counts inline SVG runs (not only text / image / shape).
        double withoutIcon = maxTextFontSize(renderAutoSized(false));
        double withIcon = maxTextFontSize(renderAutoSized(true));

        assertThat(withoutIcon).as("text alone fits at a readable size").isGreaterThan(0.0);
        assertThat(withIcon)
                .as("the inline icon shrinks the auto-sized text")
                .isLessThan(withoutIcon);
    }

    private static byte[] renderAutoSized(boolean withIcon) throws Exception {
        SvgIcon wideBar = SvgIcon.parse(
                "<svg viewBox='0 0 100 10'><path d='M0 0 H100 V10 H0 Z' fill='#c41e3a'/></svg>");
        try (DocumentSession session = GraphCompose.document()
                .pageSize(182, 120)
                .margin(16, 16, 16, 16)
                .create()) {
            session.dsl()
                    .pageFlow()
                    .name("Flow")
                    .addParagraph(p -> {
                        p.inlineText("Status complete now");
                        if (withIcon) {
                            p.svgIcon(wideBar, 10);
                        }
                        p.autoSize(24, 5);
                    })
                    .build();
            return session.toPdfBytes();
        }
    }

    /** Largest font size given to a {@code Tf} operator on page 0. */
    private static double maxTextFontSize(byte[] pdf) throws Exception {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            PDFStreamParser parser = new PDFStreamParser(document.getPage(0));
            double max = 0.0;
            List<COSBase> operands = new ArrayList<>();
            for (Object token = parser.parseNextToken(); token != null; token = parser.parseNextToken()) {
                if (token instanceof COSBase base) {
                    operands.add(base);
                } else if (token instanceof Operator op) {
                    if (op.getName().equals("Tf") && !operands.isEmpty()
                            && operands.get(operands.size() - 1) instanceof COSNumber size) {
                        max = Math.max(max, size.floatValue());
                    }
                    operands.clear();
                }
            }
            return max;
        }
    }

    private static InlineSvgRun onlySvgRun(RichText rich) {
        return rich.runs().stream()
                .filter(InlineSvgRun.class::isInstance)
                .map(InlineSvgRun.class::cast)
                .findFirst()
                .orElseThrow(() -> new AssertionError("no InlineSvgRun in " + rich.runs()));
    }

    private static byte[] renderIconRow(SvgIcon icon) throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(320, 160)
                .margin(16, 16, 16, 16)
                .create()) {
            session.dsl()
                    .pageFlow()
                    .name("Flow")
                    .addParagraph(paragraph -> paragraph
                            .name("IconRow")
                            .inlineText("Ship it ")
                            .svgIcon(icon, 12)
                            .inlineText(" now"))
                    .build();
            return session.toPdfBytes();
        }
    }

    private static boolean containsColorNear(BufferedImage image, int r, int g, int b, int tolerance) {
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int rgb = image.getRGB(x, y);
                int rr = (rgb >> 16) & 0xFF;
                int gg = (rgb >> 8) & 0xFF;
                int bb = rgb & 0xFF;
                if (Math.abs(rr - r) <= tolerance
                        && Math.abs(gg - g) <= tolerance
                        && Math.abs(bb - b) <= tolerance) {
                    return true;
                }
            }
        }
        return false;
    }
}
