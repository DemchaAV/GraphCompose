package com.demcha.compose.document.dsl;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.layout.PlacedFragment;
import com.demcha.compose.document.layout.payloads.ShapeClipBeginPayload;
import com.demcha.compose.document.layout.payloads.ShapeClipEndPayload;
import com.demcha.compose.document.node.LayerStackNode;
import com.demcha.compose.document.node.SpacerNode;
import com.demcha.compose.document.style.ClipPolicy;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.svg.SvgIcon;
import com.demcha.testing.VisualTestOutputs;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Render coverage for block SVG icons ({@code addSvgIcon} → {@link SvgIcon#node}
 * → {@code LayerStackNode}): the block path must clip its layers to the icon box
 * the way the inline path clips to the glyph box, so off-{@code viewBox} icon
 * art does not bleed onto the page. The fragment-level half of the contract
 * (the paired clip markers) is asserted directly; the pixel-level half is
 * proved by rasterizing the page.
 *
 * @see InlineSvgRenderTest#offCanvasSvgGeometryIsClippedToTheViewBox the inline
 * sibling this mirrors
 */
class BlockSvgRenderTest {

    /** A crimson square inside the 24×24 viewBox — paints within the icon box. */
    private static SvgIcon inBoxCrimson() {
        return SvgIcon.parse("""
                <svg viewBox="0 0 24 24">
                  <path d="M2 2 H22 V22 H2 Z" fill="rgb(196, 30, 58)"/>
                </svg>
                """);
    }

    /**
     * A crimson square drawn entirely OUTSIDE the 10×10 viewBox (x 30..40 →
     * three-to-four icon-widths to the right). A browser clips it away at the
     * viewBox; unclipped it would smear across the page — the block-path twin
     * of the inline :package: duplicate-box bug (Noto's working file parks
     * off-canvas copies outside the viewBox).
     */
    private static SvgIcon offCanvasCrimson() {
        return SvgIcon.parse("""
                <svg viewBox="0 0 10 10">
                  <path d="M30 0 H40 V10 H30 Z" fill="rgb(196, 30, 58)"/>
                </svg>
                """);
    }

    @Test
    void inBoxBlockSvgPaintsItsFillColor() throws Exception {
        try (PDDocument document = Loader.loadPDF(renderBlockIcon(inBoxCrimson(), 24))) {
            assertThat(document.getNumberOfPages()).isEqualTo(1);
            BufferedImage image = new PDFRenderer(document).renderImageWithDPI(0, 144);
            // The crimson only reaches the page through the block icon, so a
            // crimson pixel proves the vector layer was drawn (positive control
            // for the clip test below: the clip must not eat in-box art).
            assertThat(containsColorNear(image, 196, 30, 58, 45))
                    .as("in-box block SVG must paint its fill colour")
                    .isTrue();
        }
    }

    @Test
    void offCanvasBlockSvgGeometryIsClippedToTheViewBox() throws Exception {
        byte[] pdf = renderBlockIcon(offCanvasCrimson(), 24);
        try (PDDocument document = Loader.loadPDF(pdf)) {
            BufferedImage image = new PDFRenderer(document).renderImageWithDPI(0, 144);
            // The off-canvas square would land well within the page if unclipped
            // (≈3× the icon width to the right of the box). Finding no crimson
            // proves the block layer stack clips to the icon box.
            assertThat(containsColorNear(image, 196, 30, 58, 45))
                    .as("off-canvas block SVG geometry must be clipped to the viewBox, not bleed onto the page")
                    .isFalse();
        }
    }

    @Test
    void blockSvgIconEmitsBalancedViewBoxClipPair() throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(200, 200)
                .margin(DocumentInsets.of(16))
                .create()) {
            session.pageFlow(page -> page.addSvgIcon(inBoxCrimson(), 48));

            List<PlacedFragment> fragments = session.layoutGraph().fragments();
            List<PlacedFragment> begins = fragments.stream()
                    .filter(f -> f.payload() instanceof ShapeClipBeginPayload)
                    .toList();
            List<PlacedFragment> ends = fragments.stream()
                    .filter(f -> f.payload() instanceof ShapeClipEndPayload)
                    .toList();

            assertThat(begins).as("one viewBox clip opens around the icon").hasSize(1);
            assertThat(ends).as("one viewBox clip closes around the icon").hasSize(1);

            PlacedFragment begin = begins.get(0);
            PlacedFragment end = ends.get(0);
            ShapeClipBeginPayload beginPayload = (ShapeClipBeginPayload) begin.payload();
            ShapeClipEndPayload endPayload = (ShapeClipEndPayload) end.payload();

            assertThat(beginPayload.policy()).isEqualTo(ClipPolicy.CLIP_BOUNDS);
            assertThat(begin.path()).contains("SvgIcon");
            assertThat(beginPayload.ownerPath()).isEqualTo(endPayload.ownerPath());
            assertThat(begin.pageIndex())
                    .as("the clip pair must restore on the same page it saves")
                    .isEqualTo(end.pageIndex());
            assertThat(fragments.indexOf(begin))
                    .as("clip-begin must precede the layers and the clip-end")
                    .isLessThan(fragments.indexOf(end));

            // The clip rectangle is exactly the icon box (square 24×24 viewBox
            // at width 48 → 48×48), so it clips to the viewBox, nothing wider.
            assertThat(begin.width()).isCloseTo(48.0, within(1e-6));
            assertThat(begin.height()).isCloseTo(48.0, within(1e-6));
        }
    }

    @Test
    void plainLayerStackDoesNotClip() throws Exception {
        // A hand-built stack defaults to clipToBounds=false (the historical
        // four-arg shape), so it must emit no clip markers — the SVG opt-in
        // does not leak onto every layer stack.
        LayerStackNode plain = new LayerStackNode(
                "PlainStack",
                List.of(new LayerStackNode.Layer(
                        new SpacerNode("Inner", 20, 20, DocumentInsets.zero(), DocumentInsets.zero()))),
                null,
                null);

        try (DocumentSession session = GraphCompose.document()
                .pageSize(200, 200)
                .margin(DocumentInsets.of(16))
                .create()) {
            session.add(plain);

            assertThat(plain.clipToBounds()).isFalse();
            assertThat(session.layoutGraph().fragments())
                    .as("a non-clipping stack emits no clip markers")
                    .noneMatch(f -> f.payload() instanceof ShapeClipBeginPayload
                            || f.payload() instanceof ShapeClipEndPayload);
        }
    }

    @Test
    void writesVisualArtifact() throws Exception {
        // An off-canvas icon next to an in-box icon, for eyeballing: neither
        // the off-canvas art nor the off-canvas icon's empty box should bleed.
        try (DocumentSession session = GraphCompose.document()
                .pageSize(280, 160)
                .margin(DocumentInsets.of(20))
                .create()) {
            session.pageFlow(page -> {
                page.addParagraph(p -> p.text("Block SVG viewBox clip"));
                page.addSvgIcon(inBoxCrimson(), 48);
                page.addSvgIcon(offCanvasCrimson(), 48);
            });
            byte[] pdf = session.toPdfBytes();
            Path out = VisualTestOutputs.preparePdf("block-svg-viewbox-clip", "svg");
            Files.write(out, pdf);
            assertThat(new String(pdf, 0, 5, StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
            assertThat(Files.size(out)).isPositive();
        }
    }

    private static byte[] renderBlockIcon(SvgIcon icon, double width) throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(320, 160)
                .margin(DocumentInsets.of(16))
                .create()) {
            session.pageFlow(page -> page.addSvgIcon(icon, width));
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
