package com.demcha.compose.document.backend.fixed.pptx;

import com.demcha.compose.document.api.Internal;
import com.demcha.compose.document.layout.PlacedFragment;
import com.demcha.compose.document.node.DocumentBookmarkOptions;
import com.demcha.compose.document.node.DocumentLinkTarget;
import com.demcha.compose.document.backend.fixed.pptx.handlers.PptxFontMapping;
import com.demcha.compose.engine.components.content.ImageData;
import com.demcha.compose.engine.components.content.text.TextStyle;
import com.demcha.compose.engine.render.pdf.PdfFont;
import com.demcha.compose.font.FontFamilyDefinition;
import com.demcha.compose.font.FontLibrary;
import com.demcha.compose.font.FontName;
import org.apache.poi.sl.usermodel.PictureData;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFFontInfo;
import org.apache.poi.xslf.usermodel.XSLFPictureData;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.font.FontRenderContext;
import java.awt.geom.Rectangle2D;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Shared per-render-pass state for the fixed-layout PPTX backend.
 *
 * <p>The environment owns the slide surfaces for one document render pass plus
 * the navigation bookkeeping that resolves after all fragments have been
 * painted: bookmark records, anchor destinations, and fragment-level link
 * rectangles. Handlers draw through {@link #slide(int)} and never create
 * slides themselves.</p>
 *
 * <p>Navigation records are collected in slide top-down space (rectangles are
 * top-left anchored, in points). Their emission — slide-jump hyperlinks and
 * the bookmark-to-slide-name mapping — lands together with the navigation
 * support; the records are collected from the first release so no handler
 * needs retrofitting.</p>
 *
 * <p><b>Thread-safety:</b> mutable and confined to one render pass.</p>
 *
 * @since 2.1.0
 */
public final class PptxRenderEnvironment {

    private static final Logger LOG = LoggerFactory.getLogger("com.demcha.compose.engine.render");
    private static final FontRenderContext FONT_RENDER_CONTEXT =
            new FontRenderContext(null, true, true);
    private static final float FONT_METRICS_SIZE = 1000.0f;

    private final XMLSlideShow show;
    private final PptxRenderSession session;
    private final int pageIndexOffset;
    private final double canvasHeight;
    private final FontLibrary fonts;
    private final Map<FontName, String> customFontFamilies;
    private final Map<FontName, ViewerFontMetrics> viewerFontMetrics;
    private final Map<String, XSLFPictureData> pictureDataCache = new LinkedHashMap<>();
    private final List<BookmarkRecord> bookmarkRecords = new ArrayList<>();
    private final Map<String, AnchorDestination> anchorDestinations = new LinkedHashMap<>();
    private final List<FragmentLink> fragmentLinks = new ArrayList<>();

    PptxRenderEnvironment(XMLSlideShow show,
                          PptxRenderSession session,
                          int pageIndexOffset,
                          double canvasHeight,
                          FontLibrary fonts,
                          Collection<FontFamilyDefinition> customFontFamilies) {
        this.show = show;
        this.session = session;
        this.pageIndexOffset = pageIndexOffset;
        this.canvasHeight = canvasHeight;
        this.fonts = fonts;
        Map<FontName, String> familyNames = new LinkedHashMap<>();
        Map<FontName, ViewerFontMetrics> metrics = new LinkedHashMap<>();
        for (FontFamilyDefinition family : customFontFamilies) {
            familyNames.put(family.name(), family.wordFamily());
            family.fontSourceSet().ifPresent(sources -> {
                if (embedFontFamily(show, family, sources)) {
                    metrics.put(family.name(), viewerMetrics(family, sources));
                }
            });
        }
        if (!metrics.isEmpty()) {
            show.getCTPresentation().setSaveSubsetFonts(false);
        }
        this.customFontFamilies = Map.copyOf(familyNames);
        this.viewerFontMetrics = Map.copyOf(metrics);
    }

    PptxRenderEnvironment(XMLSlideShow show,
                          PptxRenderSession session,
                          int pageIndexOffset,
                          double canvasHeight) {
        this(show, session, pageIndexOffset, canvasHeight, new FontLibrary(), List.of());
    }

    /**
     * Returns the live slide show for the current render pass.
     *
     * @return mutable POI slide show owned by the backend
     */
    public XMLSlideShow slideShow() {
        return show;
    }

    /**
     * Returns the drawing surface for one resolved page.
     *
     * @param pageIndex zero-based page index
     * @return the slide backing that page
     */
    public XSLFSlide slide(int pageIndex) {
        return session.slide(pageIndex);
    }

    /**
     * Returns the page height in points — the constant every vertical
     * coordinate flips against when moving from the engine's bottom-left page
     * space into the slide's top-down space.
     *
     * @return canvas height in points
     */
    public double canvasHeight() {
        return canvasHeight;
    }

    /**
     * Returns the same PDF-backed measurement font library that produced the
     * paragraph geometry, so emitted text can use identical glyph sanitization.
     *
     * @return resolved render-pass fonts
     */
    public FontLibrary fonts() {
        return fonts;
    }

    /**
     * Resolves the viewer-facing family registered for a document-local font,
     * falling back to the standard PPTX mapping for bundled/standard names.
     *
     * @param fontName logical document font name
     * @return viewer-facing PPTX family name
     */
    @Internal
    public String fontFamily(FontName fontName) {
        if (fontName != null) {
            String customFamily = customFontFamilies.get(fontName);
            if (customFamily != null) {
                return customFamily;
            }
        }
        return PptxFontMapping.familyFor(fontName);
    }

    /**
     * Returns the DrawingML baseline shift needed to place the viewer font on
     * the PDF-measured baseline. PowerPoint seats a run using the viewer font's
     * hhea ascent, while the layout graph carries the PDF descriptor ascent;
     * the percentage difference is written to the run's baseline property.
     *
     * @param style resolved text style
     * @return positive percentage that moves the run upward
     */
    @Internal
    public double baselineOffset(TextStyle style) {
        if (style == null || style.size() <= 0) {
            return 0;
        }
        PdfFont font = fonts.getFont(style.fontName(), PdfFont.class).orElse(null);
        if (font == null) {
            return 0;
        }
        double pdfAscentRatio = font.verticalMetrics(style).ascent() / style.size();
        ViewerFontMetrics customMetrics = viewerFontMetrics.get(style.fontName());
        double viewerAscentRatio = customMetrics == null
                ? PptxFontMapping.viewerAscentRatio(style.fontName(), pdfAscentRatio)
                : customMetrics.ascent(PptxFontMapping.isBold(style),
                        PptxFontMapping.isItalic(style));
        return Math.max(-100.0, Math.min(100.0,
                (viewerAscentRatio - pdfAscentRatio) * 100.0));
    }

    /**
     * Resolves picture data once per distinct image fingerprint.
     *
     * @param imageData engine image payload
     * @return PPTX picture data owned by the current slide show
     */
    public XSLFPictureData resolvePicture(ImageData imageData) {
        return pictureDataCache.computeIfAbsent(imageData.getFingerprint(), ignored ->
                show.addPicture(imageData.getBytes(), pictureType(imageData)));
    }

    private static PictureData.PictureType pictureType(ImageData imageData) {
        String format = imageData.getMetadata() == null ? "" : imageData.getMetadata().format();
        return switch (format == null ? "" : format.toLowerCase(java.util.Locale.ROOT)) {
            case "jpg", "jpeg" -> PictureData.PictureType.JPEG;
            case "gif" -> PictureData.PictureType.GIF;
            case "bmp" -> PictureData.PictureType.BMP;
            case "tif", "tiff" -> PictureData.PictureType.TIFF;
            default -> PictureData.PictureType.PNG;
        };
    }

    /**
     * Embeds every distinct facet of one font family, returning whether the
     * family made it into the presentation. Embedding is best-effort: a font
     * whose license bits forbid embedding (or whose bytes cannot be read)
     * degrades to a plain family-name reference — page geometry is unaffected,
     * viewers simply substitute glyphs — instead of failing a render that the
     * PDF backend would complete.
     */
    private static boolean embedFontFamily(XMLSlideShow show,
                                           FontFamilyDefinition family,
                                           FontFamilyDefinition.FontSourceSet sources) {
        XSLFFontInfo embedded = new XSLFFontInfo(show, family.wordFamily());
        Set<String> embeddedSources = new LinkedHashSet<>();
        for (FontFamilyDefinition.FontBinarySource source : List.of(
                sources.regular(), sources.bold(), sources.italic(), sources.boldItalic())) {
            if (!embeddedSources.add(source.description())) {
                continue;
            }
            try (InputStream stream = source.openStream()) {
                byte[] eot = PptxEmbeddedFont.wrap(stream);
                embedded.addFacet(new ByteArrayInputStream(eot));
            } catch (IOException exception) {
                LOG.warn("render.pptx.font.embed.skipped family={} source={} reason={}",
                        family.name().name(), source.description(), exception.getMessage());
                return false;
            }
        }
        return true;
    }

    private static ViewerFontMetrics viewerMetrics(
            FontFamilyDefinition family,
            FontFamilyDefinition.FontSourceSet sources) {
        return new ViewerFontMetrics(
                viewerAscent(family, sources.regular()),
                viewerAscent(family, sources.bold()),
                viewerAscent(family, sources.italic()),
                viewerAscent(family, sources.boldItalic()));
    }

    private static double viewerAscent(FontFamilyDefinition family,
                                       FontFamilyDefinition.FontBinarySource source) {
        try (InputStream stream = source.openStream()) {
            Font font = Font.createFont(Font.TRUETYPE_FONT, stream).deriveFont(FONT_METRICS_SIZE);
            return font.getLineMetrics("Hg", FONT_RENDER_CONTEXT).getAscent() / FONT_METRICS_SIZE;
        } catch (IOException | FontFormatException exception) {
            throw new IllegalStateException(
                    "Unable to read PPTX font metrics for " + family.name().name()
                            + " from " + source.description(), exception);
        }
    }

    private record ViewerFontMetrics(double regularAscent,
                                     double boldAscent,
                                     double italicAscent,
                                     double boldItalicAscent) {
        double ascent(boolean bold, boolean italic) {
            if (bold && italic) {
                return boldItalicAscent;
            }
            if (bold) {
                return boldAscent;
            }
            return italic ? italicAscent : regularAscent;
        }
    }

    void registerBookmark(PlacedFragment fragment, DocumentBookmarkOptions bookmarkOptions) {
        bookmarkRecords.add(new BookmarkRecord(
                bookmarkOptions.title(),
                bookmarkOptions.level(),
                fragment.pageIndex() + pageIndexOffset));
    }

    /**
     * Records the resolved slide and top-left corner of a named anchor declared
     * by an {@code AnchorMarkerPayload} fragment. A duplicate name keeps the
     * last registration (and logs a warning), matching the engine contract.
     *
     * @param fragment placed anchor marker fragment
     * @param anchor   non-blank anchor name
     */
    public void registerAnchor(PlacedFragment fragment, String anchor) {
        if (anchor == null || anchor.isBlank()) {
            return;
        }
        AnchorDestination destination = new AnchorDestination(
                fragment.pageIndex() + pageIndexOffset,
                fragment.x(),
                PptxCoordinates.topY(canvasHeight, fragment.y(), fragment.height()));
        AnchorDestination previous = anchorDestinations.put(anchor, destination);
        if (previous != null) {
            LOG.warn("render.pptx.anchor.duplicate name={} — last registration wins", anchor);
        }
    }

    void recordFragmentLink(PlacedFragment fragment, DocumentLinkTarget target) {
        fragmentLinks.add(new FragmentLink(
                fragment.pageIndex() + pageIndexOffset,
                PptxCoordinates.anchorOf(canvasHeight, fragment),
                target));
    }

    List<BookmarkRecord> bookmarkRecords() {
        return List.copyOf(bookmarkRecords);
    }

    Map<String, AnchorDestination> anchorDestinations() {
        return Map.copyOf(anchorDestinations);
    }

    List<FragmentLink> fragmentLinks() {
        return List.copyOf(fragmentLinks);
    }

    record BookmarkRecord(String title, int level, int pageIndex) {
    }

    /**
     * Resolved slide and top-left corner of a named anchor destination, in
     * slide top-down space.
     *
     * @param pageIndex zero-based slide index the anchor resolved to
     * @param left      left edge in points
     * @param top       top edge in points
     */
    record AnchorDestination(int pageIndex, double left, double top) {
    }

    /**
     * A fragment-level clickable rectangle awaiting link emission, in slide
     * top-down space.
     *
     * @param pageIndex zero-based slide index the rectangle lives on
     * @param rectangle clickable rectangle in points
     * @param target    resolved link target
     */
    record FragmentLink(int pageIndex, Rectangle2D.Double rectangle, DocumentLinkTarget target) {
    }
}
