package com.demcha.compose.document.backend.fixed.pptx.handlers;

import com.demcha.compose.document.api.Internal;
import com.demcha.compose.document.backend.fixed.pptx.PptxRenderEnvironment;
import com.demcha.compose.document.layout.LayoutCanvas;
import com.demcha.compose.document.output.DocumentHeaderFooter;
import com.demcha.compose.document.output.DocumentHeaderFooterZone;
import com.demcha.compose.document.output.DocumentPageNumbering;
import com.demcha.compose.document.output.DocumentPageNumberStyle;
import com.demcha.compose.document.output.DocumentWatermark;
import com.demcha.compose.document.output.DocumentWatermarkLayer;
import com.demcha.compose.document.output.DocumentWatermarkPosition;
import com.demcha.compose.engine.components.content.header_footer.HeaderFooterConfig;
import com.demcha.compose.engine.components.content.header_footer.HeaderFooterZone;
import com.demcha.compose.engine.components.content.header_footer.PageNumberStyle;
import com.demcha.compose.engine.components.content.text.TextDecoration;
import com.demcha.compose.engine.components.content.text.TextStyle;
import com.demcha.compose.engine.components.style.Margin;
import com.demcha.compose.engine.render.pdf.GlyphFallbackLogger;
import com.demcha.compose.engine.render.pdf.PdfFont;
import com.demcha.compose.font.FontLibrary;
import com.demcha.compose.font.FontName;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.poi.sl.usermodel.PictureData;
import org.apache.poi.xslf.usermodel.XSLFConnectorShape;
import org.apache.poi.xslf.usermodel.XSLFPictureShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextBox;
import org.openxmlformats.schemas.presentationml.x2006.main.CTPicture;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

/**
 * Renders document-level chrome — watermarks and repeating headers/footers —
 * as slide shapes, reproducing the PDF backend's post-processing geometry:
 * the same position math, the same {@code {page}} / {@code {pages}} /
 * {@code {date}} token resolution with section-local numbering windows, and
 * the same Helvetica(-Bold) measurement so text lands where the PDF's does.
 *
 * <p>Layering needs no z-order surgery: the backend applies behind-content
 * watermarks before fragment rendering and above-content chrome after it, so
 * document order produces the PDF's prepend/append stacking.</p>
 *
 * <p>All slide lookups go through the environment's section-local page index,
 * so a multi-section render applies each section's chrome to that section's
 * slide window.</p>
 *
 * @since 2.1.0
 */
@Internal
public final class PptxChromeRenderer {

    /** Chrome text frames seat their baseline like every other PPTX text frame. */
    private static final double LINE_HEIGHT_FACTOR = 1.3;

    private PptxChromeRenderer() {
    }

    /**
     * Renders the watermark on every page of the active section window when it
     * layers behind the content. Must run before fragment rendering so the
     * slide's document order keeps the watermark underneath.
     *
     * @param environment active render environment
     * @param watermark   watermark chrome, may be {@code null}
     * @param canvas      section page canvas
     * @param pageCount   pages in the active section window
     * @throws IOException if watermark text measurement or image decoding fails
     */
    public static void applyWatermarkBehindContent(PptxRenderEnvironment environment,
                                                   DocumentWatermark watermark,
                                                   LayoutCanvas canvas,
                                                   int pageCount) throws IOException {
        applyWatermark(environment, watermark, canvas, pageCount, DocumentWatermarkLayer.BEHIND_CONTENT);
    }

    /**
     * Renders the watermark on every page of the active section window when it
     * layers above the content. Must run after fragment rendering.
     *
     * @param environment active render environment
     * @param watermark   watermark chrome, may be {@code null}
     * @param canvas      section page canvas
     * @param pageCount   pages in the active section window
     * @throws IOException if watermark text measurement or image decoding fails
     */
    public static void applyWatermarkAboveContent(PptxRenderEnvironment environment,
                                                  DocumentWatermark watermark,
                                                  LayoutCanvas canvas,
                                                  int pageCount) throws IOException {
        applyWatermark(environment, watermark, canvas, pageCount, DocumentWatermarkLayer.ABOVE_CONTENT);
    }

    private static void applyWatermark(PptxRenderEnvironment environment,
                                       DocumentWatermark watermark,
                                       LayoutCanvas canvas,
                                       int pageCount,
                                       DocumentWatermarkLayer targetLayer) throws IOException {
        if (watermark == null) {
            return;
        }
        DocumentWatermarkLayer layer = watermark.getLayer() == null
                ? DocumentWatermarkLayer.BEHIND_CONTENT
                : watermark.getLayer();
        if (layer != targetLayer) {
            return;
        }
        // Measurement, sanitization, and image decoding are page-invariant:
        // prepare once, place per page.
        if (watermark.isTextBased()) {
            PDFont font = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            float fontSize = watermark.getFontSize();
            String text = GlyphFallbackLogger.sanitize(font, watermark.getText());
            float textWidth = font.getStringWidth(text) / 1000f * fontSize;
            Color color = withOpacity(
                    watermark.getColor() == null ? Color.LIGHT_GRAY : watermark.getColor().color(),
                    watermark.getOpacity());
            TextStyle style = new TextStyle(
                    FontName.HELVETICA_BOLD, fontSize, TextDecoration.DEFAULT, color);
            for (int local = 0; local < pageCount; local++) {
                renderTextWatermark(environment, watermark, canvas, local, text, style, textWidth);
            }
        } else if (watermark.isImageBased()) {
            byte[] bytes = watermark.getImagePath() != null
                    ? Files.readAllBytes(watermark.getImagePath())
                    : watermark.getImageBytes();
            BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(bytes));
            if (decoded == null) {
                throw new IOException("The watermark image could not be decoded.");
            }
            for (int local = 0; local < pageCount; local++) {
                renderImageWatermark(environment, watermark, canvas, local,
                        bytes, decoded.getWidth(), decoded.getHeight());
            }
        }
    }

    private static void renderTextWatermark(PptxRenderEnvironment environment,
                                            DocumentWatermark watermark,
                                            LayoutCanvas canvas,
                                            int localPageIndex,
                                            String text,
                                            TextStyle style,
                                            float textWidth) {
        float fontSize = watermark.getFontSize();
        DocumentWatermarkPosition position = watermark.getPosition() == null
                ? DocumentWatermarkPosition.CENTER
                : watermark.getPosition();
        if (position == DocumentWatermarkPosition.TILE) {
            float spacingX = textWidth + 80;
            float spacingY = fontSize + 120;
            // Rotation can carry glyphs at most this far from the baseline
            // start; tiles that cannot reach the slide are skipped instead of
            // stored as invisible off-slide shapes (the PDF just overdraws).
            float reach = textWidth + fontSize;
            for (float y = (float) -canvas.height(); y < canvas.height() * 2; y += spacingY) {
                for (float x = (float) -canvas.width(); x < canvas.width() * 2; x += spacingX) {
                    if (x + reach < 0 || x - reach > canvas.width()
                            || y + reach < 0 || y - reach > canvas.height()) {
                        continue;
                    }
                    placeWatermarkText(environment, localPageIndex, text, style,
                            textWidth, x, y, watermark.getRotation());
                }
            }
        } else {
            float[] baseline = resolvePosition(position, canvas, textWidth, fontSize);
            placeWatermarkText(environment, localPageIndex, text, style,
                    textWidth, baseline[0], baseline[1], watermark.getRotation());
        }
    }

    /**
     * Places one watermark text frame whose baseline start sits at the given
     * bottom-left-space point, rotated about that point like the PDF's text
     * matrix. PowerPoint rotates a shape about its own centre and clockwise,
     * so the frame centre is orbited to where the PDF rotation would carry it
     * and the shape rotation carries the negated angle.
     */
    private static void placeWatermarkText(PptxRenderEnvironment environment,
                                           int localPageIndex,
                                           String text,
                                           TextStyle style,
                                           double textWidth,
                                           double baselineX,
                                           double baselineY,
                                           double rotationDegrees) {
        double canvasHeight = environment.canvasHeight();
        double ascent = environment.viewerAscent(style, style.size() * 0.9);
        double frameWidth = textWidth + PptxTextFrames.FRAME_EPSILON;
        double frameHeight = style.size() * LINE_HEIGHT_FACTOR;
        double frameLeft = baselineX;
        double frameTop = canvasHeight - baselineY - ascent;

        if (rotationDegrees != 0) {
            double pivotX = baselineX;
            double pivotY = canvasHeight - baselineY;
            double centerX = frameLeft + frameWidth / 2.0;
            double centerY = frameTop + frameHeight / 2.0;
            double theta = Math.toRadians(rotationDegrees);
            double dx = centerX - pivotX;
            double dy = centerY - pivotY;
            // Visually counter-clockwise orbit in the slide's top-down space,
            // matching the PDF's counter-clockwise rotation in y-up space.
            double orbitedX = pivotX + dx * Math.cos(theta) + dy * Math.sin(theta);
            double orbitedY = pivotY - dx * Math.sin(theta) + dy * Math.cos(theta);
            frameLeft = orbitedX - frameWidth / 2.0;
            frameTop = orbitedY - frameHeight / 2.0;
        }

        XSLFTextBox box = PptxTextFrames.singleRunBox(
                environment.slide(localPageIndex),
                "GraphCompose Watermark",
                new Rectangle2D.Double(frameLeft, frameTop, frameWidth, frameHeight),
                text, style, environment);
        if (rotationDegrees != 0) {
            box.setRotation(-rotationDegrees);
        }
    }

    private static void renderImageWatermark(PptxRenderEnvironment environment,
                                             DocumentWatermark watermark,
                                             LayoutCanvas canvas,
                                             int localPageIndex,
                                             byte[] bytes,
                                             float imageWidth,
                                             float imageHeight) {
        DocumentWatermarkPosition position = watermark.getPosition() == null
                ? DocumentWatermarkPosition.CENTER
                : watermark.getPosition();
        if (position == DocumentWatermarkPosition.TILE) {
            float spacingX = imageWidth + 60;
            float spacingY = imageHeight + 60;
            for (float y = 0; y < canvas.height(); y += spacingY) {
                for (float x = 0; x < canvas.width(); x += spacingX) {
                    placeWatermarkImage(environment, localPageIndex, bytes,
                            x, y, imageWidth, imageHeight, watermark.getOpacity());
                }
            }
        } else {
            float[] bottomLeft = resolvePosition(position, canvas, imageWidth, imageHeight);
            placeWatermarkImage(environment, localPageIndex, bytes,
                    bottomLeft[0], bottomLeft[1], imageWidth, imageHeight, watermark.getOpacity());
        }
    }

    private static void placeWatermarkImage(PptxRenderEnvironment environment,
                                            int localPageIndex,
                                            byte[] bytes,
                                            double x,
                                            double y,
                                            double width,
                                            double height,
                                            float opacity) {
        XSLFSlide slide = environment.slide(localPageIndex);
        XSLFPictureShape picture = slide.createPicture(
                environment.slideShow().addPicture(bytes, sniffPictureType(bytes)));
        picture.setAnchor(new Rectangle2D.Double(
                x, environment.canvasHeight() - y - height, width, height));
        CTPicture xml = (CTPicture) picture.getXmlObject();
        xml.getNvPicPr().getCNvPr().setName("GraphCompose Watermark");
        if (opacity < 1f) {
            xml.getBlipFill().getBlip().addNewAlphaModFix()
                    .setAmt(Math.round(Math.min(1f, Math.max(0f, opacity)) * 100000));
        }
    }

    private static PictureData.PictureType sniffPictureType(byte[] bytes) {
        if (bytes.length >= 2 && (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8) {
            return PictureData.PictureType.JPEG;
        }
        if (bytes.length >= 3 && bytes[0] == 'G' && bytes[1] == 'I' && bytes[2] == 'F') {
            return PictureData.PictureType.GIF;
        }
        if (bytes.length >= 2 && bytes[0] == 'B' && bytes[1] == 'M') {
            return PictureData.PictureType.BMP;
        }
        return PictureData.PictureType.PNG;
    }

    /**
     * Bottom-left anchor of a watermark object in the engine's y-up page
     * space — the same 20pt-inset placement grid the PDF watermark uses.
     */
    private static float[] resolvePosition(DocumentWatermarkPosition position,
                                           LayoutCanvas canvas,
                                           float objectWidth,
                                           float objectHeight) {
        float pageWidth = (float) canvas.width();
        float pageHeight = (float) canvas.height();
        return switch (position) {
            case CENTER -> new float[]{(pageWidth - objectWidth) / 2f, (pageHeight - objectHeight) / 2f};
            case TOP_LEFT -> new float[]{20, pageHeight - objectHeight - 20};
            case TOP_RIGHT -> new float[]{pageWidth - objectWidth - 20, pageHeight - objectHeight - 20};
            case BOTTOM_LEFT -> new float[]{20, 20};
            case BOTTOM_RIGHT -> new float[]{pageWidth - objectWidth - 20, 20};
            case TILE -> new float[]{0, 0}; // handled by the tiling loops
        };
    }

    /**
     * Renders every header/footer entry on the active section window, with
     * {@code {page}} / {@code {pages}} tokens and the show-on-page rules
     * evaluated against the section-local window — the multi-section numbering
     * contract the PDF backend applies.
     *
     * @param environment active render environment
     * @param entries     header/footer chrome entries
     * @param canvas      section page canvas (supplies the text margins)
     * @param pageCount   pages in the active section window
     * @throws IOException if header/footer text measurement fails
     */
    public static void applyHeadersAndFooters(PptxRenderEnvironment environment,
                                              List<DocumentHeaderFooter> entries,
                                              LayoutCanvas canvas,
                                              int pageCount) throws IOException {
        if (entries == null || entries.isEmpty()) {
            return;
        }
        Margin margin = canvas.margin();
        float marginLeft = margin != null ? (float) margin.left() : 24f;
        float marginRight = margin != null ? (float) margin.right() : 24f;
        List<HeaderFooterConfig> configs = entries.stream()
                .map(PptxChromeRenderer::toEngine)
                .toList();
        for (int local = 0; local < pageCount; local++) {
            for (HeaderFooterConfig config : configs) {
                if (!config.appliesTo(local + 1)) {
                    continue;
                }
                // Resolved per zone rather than once for the run: two zones may ask
                // for two families, and the font the slots are measured against has
                // to be the one the slide is typeset in or they land off-centre.
                PDFont font = resolveFont(environment, config);
                renderZone(environment, config, font, canvas, local, pageCount, marginLeft, marginRight);
            }
        }
    }

    private static void renderZone(PptxRenderEnvironment environment,
                                   HeaderFooterConfig config,
                                   PDFont font,
                                   LayoutCanvas canvas,
                                   int localPageIndex,
                                   int pageCount,
                                   float marginLeft,
                                   float marginRight) throws IOException {
        float fontSize = config.getFontSize();
        float pageWidth = (float) canvas.width();
        float usableWidth = pageWidth - marginLeft - marginRight;
        int currentPage = localPageIndex + 1;

        // Baseline in the engine's y-up space — identical to the PDF zones.
        float baseY = config.getZone() == HeaderFooterZone.HEADER
                ? (float) canvas.height() - config.getHeight() + fontSize / 2f
                : config.getHeight() - fontSize;
        String shapeName = config.getZone() == HeaderFooterZone.HEADER
                ? "GraphCompose Header"
                : "GraphCompose Footer";
        TextStyle style = config.textStyle();

        String leftText = GlyphFallbackLogger.sanitize(font,
                config.resolveTokens(config.getLeftText(), currentPage, pageCount));
        if (!leftText.isEmpty()) {
            placeZoneText(environment, localPageIndex, shapeName, leftText, style, marginLeft, baseY,
                    font.getStringWidth(leftText) / 1000f * fontSize);
        }
        String centerText = GlyphFallbackLogger.sanitize(font,
                config.resolveTokens(config.getCenterText(), currentPage, pageCount));
        if (!centerText.isEmpty()) {
            float textWidth = font.getStringWidth(centerText) / 1000f * fontSize;
            placeZoneText(environment, localPageIndex, shapeName, centerText, style,
                    marginLeft + (usableWidth - textWidth) / 2f, baseY, textWidth);
        }
        String rightText = GlyphFallbackLogger.sanitize(font,
                config.resolveTokens(config.getRightText(), currentPage, pageCount));
        if (!rightText.isEmpty()) {
            float textWidth = font.getStringWidth(rightText) / 1000f * fontSize;
            placeZoneText(environment, localPageIndex, shapeName, rightText, style,
                    pageWidth - marginRight - textWidth, baseY, textWidth);
        }

        if (config.isShowSeparator()) {
            float separatorY = config.getZone() == HeaderFooterZone.HEADER
                    ? (float) canvas.height() - config.getHeight()
                    : config.getHeight();
            XSLFConnectorShape separator = environment.slide(localPageIndex).createConnector();
            separator.setAnchor(new Rectangle2D.Double(
                    marginLeft, environment.canvasHeight() - separatorY,
                    pageWidth - marginRight - marginLeft, 0));
            separator.setLineColor(config.getSeparatorColor());
            separator.setLineWidth(config.getSeparatorThickness());
            PptxTextFrames.setShapeName(separator, shapeName + " Separator");
        }
    }

    private static void placeZoneText(PptxRenderEnvironment environment,
                                      int localPageIndex,
                                      String shapeName,
                                      String text,
                                      TextStyle style,
                                      double x,
                                      double baseY,
                                      double textWidth) {
        double top = environment.canvasHeight() - baseY
                - environment.viewerAscent(style, style.size() * 0.9);
        PptxTextFrames.singleRunBox(
                environment.slide(localPageIndex),
                shapeName,
                new Rectangle2D.Double(x, top,
                        textWidth + PptxTextFrames.FRAME_EPSILON,
                        style.size() * LINE_HEIGHT_FACTOR),
                text, style, environment);
    }

    /**
     * Resolves the zone's font family through the deck's own font library, so a
     * header or footer is typeset in the family the author asked for rather than
     * in a font this renderer picked. Falls back to standard-14 Helvetica when the
     * library cannot supply the family — chrome must not cost the caller the whole
     * deck over a font name.
     */
    private static PDFont resolveFont(PptxRenderEnvironment environment, HeaderFooterConfig config) {
        TextStyle style = config.textStyle();
        FontLibrary fonts = environment == null ? null : environment.fonts();
        if (fonts != null) {
            PdfFont resolved = fonts.getFont(style.fontName(), PdfFont.class).orElse(null);
            if (resolved != null) {
                return resolved.fontType(style.decoration());
            }
        }
        return new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    }

    /**
     * Maps the canonical header/footer entry onto the engine config that owns
     * token resolution and the numbering window rules — the same mapping the
     * PDF options adapter applies.
     */
    private static HeaderFooterConfig toEngine(DocumentHeaderFooter entry) {
        DocumentPageNumbering numbering = entry.getNumbering() == null
                ? DocumentPageNumbering.DEFAULT
                : entry.getNumbering();
        return HeaderFooterConfig.builder()
                .zone(entry.getZone() == DocumentHeaderFooterZone.FOOTER
                        ? HeaderFooterZone.FOOTER
                        : HeaderFooterZone.HEADER)
                .height(entry.getHeight())
                .leftText(entry.getLeftText())
                .centerText(entry.getCenterText())
                .rightText(entry.getRightText())
                .fontSize(entry.getFontSize())
                .fontName(entry.getFontName())
                .textColor(entry.getTextColor() == null ? null : entry.getTextColor().color())
                .showSeparator(entry.isShowSeparator())
                .separatorColor(entry.getSeparatorColor() == null ? null : entry.getSeparatorColor().color())
                .separatorThickness(entry.getSeparatorThickness())
                .numberStartAt(numbering.getStartAt())
                .numberCountFrom(numbering.getCountFrom())
                .numberShowOnFirstPage(numbering.isShowOnFirstPage())
                .numberStyle(toEngine(numbering.getStyle()))
                .build();
    }

    private static PageNumberStyle toEngine(DocumentPageNumberStyle style) {
        if (style == null) {
            return PageNumberStyle.DECIMAL;
        }
        return switch (style) {
            case DECIMAL -> PageNumberStyle.DECIMAL;
            case LOWER_ROMAN -> PageNumberStyle.LOWER_ROMAN;
            case UPPER_ROMAN -> PageNumberStyle.UPPER_ROMAN;
            case LOWER_ALPHA -> PageNumberStyle.LOWER_ALPHA;
            case UPPER_ALPHA -> PageNumberStyle.UPPER_ALPHA;
        };
    }

    private static Color withOpacity(Color color, float opacity) {
        int alpha = Math.round(Math.min(1f, Math.max(0f, opacity)) * 255);
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
    }
}
