package com.demcha.compose.document.backend.fixed.pptx;

import com.demcha.compose.document.api.Beta;

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
import org.apache.poi.xslf.usermodel.XSLFPictureData;
import org.apache.poi.xslf.usermodel.XSLFGroupShape;
import org.apache.poi.xslf.usermodel.XSLFShapeContainer;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.geom.Rectangle2D;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Shared per-render-pass state for the fixed-layout PPTX backend.
 *
 * <p>The environment owns the slide surfaces for one document render pass plus
 * the navigation bookkeeping that resolves after all fragments have been
 * painted: bookmark records, anchor destinations, and fragment-level link
 * rectangles. Handlers draw through {@link #surface(int)} - the innermost open
 * transform group or the page's slide - and never create slides themselves.</p>
 *
 * <p>Navigation records are collected in slide top-down space (rectangles are
 * top-left anchored, in points). Their emission — slide-jump hyperlinks and
 * the bookmark-to-slide-name mapping — lands together with the navigation
 * support; the records are collected from the first release so no handler
 * needs retrofitting.</p>
 *
 * <p><b>Thread-safety:</b> mutable and confined to one render pass.</p>
 *
 * <p><b>Experimental</b> ({@code @Beta}): first release of the PPTX backend
 * (2.1.0) — this type's contract may still change in a minor release; see
 * {@code docs/api-stability.md}.</p>
 *
 * @since 2.1.0
 */
@Beta
public final class PptxRenderEnvironment {

    private static final Logger LOG = LoggerFactory.getLogger("com.demcha.compose.engine.render");

    private final XMLSlideShow show;
    private final PptxRenderSession session;
    private int pageIndexOffset;
    private final double canvasHeight;
    private final FontLibrary fonts;
    private final PptxDeckFonts deckFonts;
    private final Map<String, XSLFPictureData> pictureDataCache = new LinkedHashMap<>();
    private final Map<String, java.awt.Dimension> pictureSizeCache = new LinkedHashMap<>();
    private final List<BookmarkRecord> bookmarkRecords = new ArrayList<>();
    private final Map<String, AnchorDestination> anchorDestinations = new LinkedHashMap<>();
    private final List<FragmentLink> fragmentLinks = new ArrayList<>();
    private final java.util.Deque<XSLFGroupShape> groupStack = new java.util.ArrayDeque<>();
    private int rasterizedClipCount;
    private long rasterizedClipPixels;

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
        this.deckFonts = new PptxDeckFonts(show, customFontFamilies);
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
     * Moves the environment to the next section window of a multi-section
     * render: section-local page indexes now resolve to slides (and record
     * navigation) at the given offset into the combined deck.
     */
    void beginSection(int pageIndexOffset) {
        this.pageIndexOffset = pageIndexOffset;
    }

    /**
     * Returns the drawing surface for one resolved page of the active section
     * window.
     *
     * @param pageIndex zero-based section-local page index
     * @return the slide backing that page
     */
    public XSLFSlide slide(int pageIndex) {
        return session.slide(pageIndex + pageIndexOffset);
    }

    /**
     * Returns a slide by its absolute index in the combined deck — the space
     * the navigation records live in. Used by the post-pass emission, which
     * runs after every section window has been rendered.
     */
    XSLFSlide globalSlide(int globalPageIndex) {
        return session.slide(globalPageIndex);
    }

    /**
     * Records that one clip region was rasterized rather than rendered as
     * native shapes. Each such region costs a full sub-render through the PDF
     * backend and loses text editability inside its bounds, so the cost of a
     * clip-heavy deck should be visible rather than inferred from render time.
     *
     * @param pixelWidth  rasterized picture width in pixels
     * @param pixelHeight rasterized picture height in pixels
     */
    void recordRasterizedClip(int pixelWidth, int pixelHeight) {
        rasterizedClipCount++;
        rasterizedClipPixels += (long) pixelWidth * pixelHeight;
    }

    /**
     * Emits the one-line clip-rasterization summary for the finished render
     * pass. Silent when nothing was rasterized, so an all-vector deck adds no
     * noise to the log.
     */
    void logRasterizedClipSummary() {
        if (rasterizedClipCount == 0) {
            return;
        }
        LOG.debug("render.pptx.fixed.clip-raster regions={} megapixels={}",
                rasterizedClipCount,
                String.format(java.util.Locale.ROOT, "%.2f", rasterizedClipPixels / 1_000_000.0));
    }

    /**
     * Returns the container new shapes must be created on: the innermost open
     * transform group when one is active, otherwise the page's slide. The
     * layout compiler guarantees begin/end markers stay balanced on one page,
     * so the stack never leaks across slides.
     *
     * @param pageIndex zero-based page index
     * @return active drawing container for that page
     */
    public XSLFShapeContainer surface(int pageIndex) {
        XSLFGroupShape group = groupStack.peek();
        return group != null ? group : slide(pageIndex);
    }

    /**
     * Opens a transform group as the active drawing container. Called by the
     * transform-begin handler; every push is popped by the matching end
     * marker.
     *
     * @param group group shape covering the transformed composite
     */
    public void pushGroup(XSLFGroupShape group) {
        groupStack.push(group);
    }

    /**
     * Closes the innermost transform group.
     *
     * @return the group that was active
     */
    public XSLFGroupShape popGroup() {
        XSLFGroupShape group = groupStack.poll();
        if (group == null) {
            throw new IllegalStateException(
                    "Transform end marker without a matching begin: the group stack is empty.");
        }
        return group;
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
        return fontFamily(fontName, false, false);
    }

    /**
     * As {@link #fontFamily(FontName)}, recording which face of the family was drawn.
     *
     * @param fontName logical document font name
     * @param bold     whether this run is bold
     * @param italic   whether this run is italic
     * @return viewer-facing PPTX family name
     */
    @Internal
    public String fontFamily(FontName fontName, boolean bold, boolean italic) {
        return deckFonts.fontFamily(fontName, bold, italic);
    }

    /**
     * Whether this run asks a viewer for a bold face.
     *
     * <p>The span's decoration, and the family's registered name only where the family's own faces
     * cannot name one — see {@code PptxDeckFonts.tokensTheNameMaySpeak}. Every writer that needs
     * the flag asks here, so the attribute a run carries, the face recorded as drawn and the
     * face whose seat the frame is placed by are one answer rather than three readings of the same
     * style.</p>
     *
     * @param style the span's style
     * @return whether the run is written bold
     */
    @Internal
    public boolean runIsBold(TextStyle style) {
        return PptxFontMapping.isBold(style, deckFonts.tokensTheNameMaySpeak(style.fontName()));
    }

    /**
     * Whether this run asks a viewer for an italic face, on the same terms as
     * {@link #runIsBold}.
     *
     * @param style the span's style
     * @return whether the run is written italic
     */
    @Internal
    public boolean runIsItalic(TextStyle style) {
        return PptxFontMapping.isItalic(style, deckFonts.tokensTheNameMaySpeak(style.fontName()));
    }

    /**
     * Returns how far below a top-anchored frame's top PowerPoint stands this
     * run's first baseline, in points. PowerPoint positions text by frame, not
     * by baseline: the first baseline lands at {@code frameTop + viewerAscent},
     * so frames anchor at {@code baseline − viewerAscent}. The distance is not
     * the font's ascent but PowerPoint's own seat, worked out per face from how
     * the face's OS/2 metrics divide (see {@code PptxViewerMetrics}): embedded
     * fonts from their own programs, standard-14 replacements from Arial's /
     * Times New Roman's / Courier New's, anything else from the TrueType program
     * the page measured it with, or the PDF descriptor's ascent and descent where
     * it is not one.
     *
     * <p>Deliberately never written to the run's DrawingML {@code baseline}
     * property: PowerPoint treats any non-zero baseline as super/subscript and
     * shrinks the glyphs.</p>
     *
     * @param style          resolved text style
     * @param fallbackAscent ascent to assume when the style cannot be resolved
     * @return the distance from the frame top to the first baseline, in points
     */
    @Internal
    public double viewerAscent(TextStyle style, double fallbackAscent) {
        if (style == null || style.size() <= 0) {
            return fallbackAscent;
        }
        PdfFont font = fonts.getFont(style.fontName(), PdfFont.class).orElse(null);
        if (font == null) {
            return fallbackAscent;
        }
        return PptxViewerMetrics.seatPoints(
                style, font, deckFonts.viewerMetrics(style.fontName()),
                runIsBold(style), runIsItalic(style));
    }

    /**
     * Returns the image's intrinsic pixel size, decoded once per fingerprint.
     * POI's header sniffing mis-reports some images (notably tiny PNGs), which
     * would break the CONTAIN/COVER aspect math the PDF backend computes from
     * the real bitmap — so both backends read the same decoded dimensions.
     *
     * @param imageData engine image payload
     * @return decoded pixel dimensions, or {@code null} when undecodable
     */
    public java.awt.Dimension pictureSize(ImageData imageData) {
        return pictureSizeCache.computeIfAbsent(imageData.getFingerprint(), ignored -> {
            try {
                java.awt.image.BufferedImage decoded = javax.imageio.ImageIO.read(
                        new ByteArrayInputStream(imageData.getBytes()));
                return decoded == null ? null
                        : new java.awt.Dimension(decoded.getWidth(), decoded.getHeight());
            } catch (IOException exception) {
                return null;
            }
        });
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
     * Tells this render whether the families it uses will be carried; see
     * {@link PptxDeckFonts#embedsBundledFonts(boolean)}.
     *
     * @param enabled whether {@link #embedBundledFontsUsed()} will run for this render
     */
    void embedsBundledFonts(boolean enabled) {
        deckFonts.embedsBundledFonts(enabled);
    }

    /**
     * Embeds the bundled families this deck drew with; see
     * {@link PptxDeckFonts#embedBundledFontsUsed()}. Call after the last run is drawn.
     */
    void embedBundledFontsUsed() {
        deckFonts.embedBundledFontsUsed();
    }

    /**
     * Records a bookmark for the navigation pass.
     *
     * @param fragment        placed fragment carrying the bookmark
     * @param bookmarkOptions resolved bookmark metadata
     */
    @Internal
    public void registerBookmark(PlacedFragment fragment, DocumentBookmarkOptions bookmarkOptions) {
        bookmarkRecords.add(new BookmarkRecord(
                bookmarkOptions.title(),
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

    /**
     * Records a clickable rectangle for deferred emission once every anchor is
     * placed — the path span- and line-level internal links take, since their
     * targets may still be unrendered forward references.
     *
     * @param pageIndex zero-based section-local page index
     * @param rectangle clickable rectangle in slide top-down points
     * @param target    resolved link target
     */
    @Internal
    public void deferLink(int pageIndex, Rectangle2D.Double rectangle, DocumentLinkTarget target) {
        fragmentLinks.add(new FragmentLink(pageIndex + pageIndexOffset, rectangle, target));
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

    /**
     * A recorded bookmark awaiting slide-name emission. PPTX has no outline
     * hierarchy, so the engine bookmark's nesting level is not carried.
     *
     * @param title     bookmark title
     * @param pageIndex zero-based slide index the bookmark resolved to
     */
    record BookmarkRecord(String title, int pageIndex) {
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
