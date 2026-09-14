package com.demcha.compose.document.backend.fixed.pdf;

import com.demcha.compose.document.api.Beta;
import com.demcha.compose.document.layout.PlacedFragment;
import com.demcha.compose.document.node.DocumentBookmarkOptions;
import com.demcha.compose.engine.components.content.ImageData;
import com.demcha.compose.font.FontLibrary;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Shared per-render-pass state for the canonical PDF backend.
 *
 * <p>The environment owns immutable document-wide dependencies such as the
 * resolved font library and mutable caches such as decoded images. It is
 * created once per {@link PdfFixedLayoutBackend#render} invocation and is not
 * shared across render passes.</p>
 *
 * <p><b>Thread-safety:</b> mutable and confined to one render pass.</p>
 */
public final class PdfRenderEnvironment {
    private static final Logger LOG = LoggerFactory.getLogger("com.demcha.compose.engine.render");

    private final PDDocument document;
    private final FontLibrary fonts;
    private final PdfRenderSession session;
    private final int pageIndexOffset;
    private final Map<String, PDImageXObject> imageCache = new HashMap<>();
    private final Map<Float, PDExtendedGraphicsState> fillAlphaStates = new HashMap<>();
    private final Map<Float, PDExtendedGraphicsState> strokeAlphaStates = new HashMap<>();
    private final List<BookmarkRecord> bookmarkRecords = new ArrayList<>();
    private final Map<String, AnchorDestination> anchorDestinations = new LinkedHashMap<>();
    private final List<DeferredInternalLink> deferredInternalLinks = new ArrayList<>();
    private final PdfTrackedFontResources letterSpacedFonts;
    private boolean reorderedText;

    PdfRenderEnvironment(PDDocument document, FontLibrary fonts, PdfRenderSession session,
                         PdfTrackedFontResources letterSpacedFonts) {
        this(document, fonts, session, 0, letterSpacedFonts);
    }

    /**
     * Creates an environment whose recorded anchors, links, and bookmarks are
     * stamped with a page index shifted by {@code pageIndexOffset}.
     *
     * <p>Drawing stays local: {@link #pageSurface(int)} indexes the section's own
     * page list (offset {@code 0}). Only the destinations that resolve against the
     * combined document — anchors, deferred links, and bookmarks — are rebased by
     * the offset, so a section appended at page {@code N} navigates correctly. A
     * single-section render uses offset {@code 0} and is unaffected.</p>
     *
     * @param document        live combined document
     * @param fonts           shared font library
     * @param session         page-scoped drawing surface for this section
     * @param pageIndexOffset number of pages already placed before this section
     * @param letterSpacedFonts the document's letter-spaced font resources, shared by all its sections
     */
    PdfRenderEnvironment(PDDocument document, FontLibrary fonts, PdfRenderSession session, int pageIndexOffset,
                         PdfTrackedFontResources letterSpacedFonts) {
        this.document = document;
        this.fonts = fonts;
        this.session = session;
        this.pageIndexOffset = pageIndexOffset;
        this.letterSpacedFonts = letterSpacedFonts;
    }

    /**
     * Returns the face a letter-spaced run should be drawn with so that its spacing lives in the
     * glyph widths instead of between the glyph boxes.
     *
     * <p>A run drawn with {@code Tc} is placed correctly, but readers that ignore
     * {@code ActualText} find word breaks by the gaps between glyph boxes, and tracking is exactly
     * such a gap: pdf.js and pdfminer read a heading tracked at 0.18em as single letters. The
     * returned face shares the embedded program of {@code font} and states every width raised by
     * the tracking, so the glyphs land where {@code Tc} would put them and no gap is left. Draw the
     * run with {@link LetterSpacedFont#font()}, and with {@link LetterSpacedFont#characterSpacing()}
     * as its {@code Tc}.</p>
     *
     * <p>Only positive tracking of at least half a thousandth of an em and at most a hundred ems,
     * drawn with an embedded, subset, horizontal Type 0 font, qualifies, and only for non-empty
     * text the font's GSUB substitutions, if it keeps any, leave unchanged. For anything else
     * &mdash; and when the resource cannot be registered &mdash; this returns {@code null}, and the
     * run keeps drawing with {@code font} and a {@code Tc} equal to its letter spacing, exactly as
     * before.</p>
     *
     * <p>The returned face belongs to the document being rendered and to the size and spacing
     * asked for; ask again for another size. {@code font} must belong to the same document.
     * Asking is not free: once any face has been returned, the document is saved twice, because
     * the resource can only be completed after its base font has been subset. Text a handler
     * draws in visual order, such as reordered right-to-left text, should not be passed; the
     * built-in handlers keep {@code Tc} for it. A face this method returned may be passed back,
     * and stands for its base font.</p>
     *
     * <p><b>Experimental.</b> The glyph positions and text layer this produces are settled; the
     * shape of the call &mdash; a nullable result, a face bound to one size &mdash; may still change
     * in a minor release.</p>
     *
     * @param font          the face the run would otherwise be drawn with
     * @param fontSize      font size in points
     * @param letterSpacing the run's resolved letter spacing in points
     * @param text          the text the returned face would draw, after sanitizing: one run, or
     *                      several lines joined on line breaks when one decision covers them
     * @return the face and character spacing to draw the run with, or {@code null} to keep
     *         drawing with {@code font} and {@code Tc}
     * @since 2.4.0
     */
    @Beta
    public LetterSpacedFont letterSpacedFont(org.apache.pdfbox.pdmodel.font.PDFont font,
                                             double fontSize,
                                             double letterSpacing,
                                             String text) {
        return letterSpacedFonts.resolve(font, fontSize, letterSpacing, text);
    }

    /**
     * A face whose glyph widths carry a run's letter spacing, and the character spacing still owed.
     *
     * <p><b>Experimental</b>, like {@link #letterSpacedFont}: its shape may still change in a minor
     * release.</p>
     *
     * @param font             the font resource to select for the run
     * @param characterSpacing the {@code Tc} to draw the run with: the part of the letter spacing a
     *                         whole thousandth of an em cannot state, at most half a thousandth of
     *                         the font size, or zero
     * @since 2.4.0
     */
    @Beta
    public record LetterSpacedFont(org.apache.pdfbox.pdmodel.font.PDFont font, float characterSpacing) {
    }

    /**
     * The document's letter-spaced font resources, which the backend completes when it saves.
     *
     * @return the shared registry
     */
    PdfTrackedFontResources letterSpacedFonts() {
        return letterSpacedFonts;
    }

    /**
     * Returns the live PDFBox document for the current render pass.
     *
     * @return mutable PDFBox document owned by the backend
     */
    public PDDocument document() {
        return document;
    }

    /**
     * Returns the render-pass font library shared by all handlers.
     *
     * @return resolved font library
     */
    public FontLibrary fonts() {
        return fonts;
    }

    /**
     * Returns the page-scoped drawing surface for one resolved page.
     *
     * @param pageIndex zero-based page index
     * @return reusable page content stream owned by the current render session
     * @throws IOException if the page surface cannot be opened
     */
    public PDPageContentStream pageSurface(int pageIndex) throws IOException {
        return session.pageSurface(pageIndex);
    }

    /**
     * Resolves a section-local page index to the physical page in the combined
     * document, applying the section's page-index offset. Used by link
     * annotations, which attach to the document page rather than the section's
     * drawing surface.
     *
     * @param localPageIndex zero-based page index within the current section
     * @return the physical page in the combined document
     */
    public org.apache.pdfbox.pdmodel.PDPage documentPage(int localPageIndex) {
        return document.getPage(localPageIndex + pageIndexOffset);
    }

    /**
     * Returns the shared graphics state carrying a non-stroking alpha
     * constant, minting one per distinct value on first use.
     *
     * <p>PDFBox maps repeated writes of the <em>same</em>
     * {@link PDExtendedGraphicsState} instance to one {@code /ExtGState}
     * resource entry per page, so sharing instances keeps a page's resource
     * dictionary bounded by the number of distinct alpha values instead of
     * growing with every translucent draw. Callers must treat the returned
     * state as immutable.</p>
     *
     * @param alpha non-stroking alpha constant in {@code [0, 1]}
     * @return shared graphics state owned by the current render pass
     * @since 2.1.0
     */
    public PDExtendedGraphicsState fillAlphaState(float alpha) {
        return fillAlphaStates.computeIfAbsent(alpha, value -> {
            PDExtendedGraphicsState state = new PDExtendedGraphicsState();
            state.setNonStrokingAlphaConstant(value);
            return state;
        });
    }

    /**
     * Returns the shared graphics state carrying a stroking alpha constant,
     * minting one per distinct value on first use. Sharing semantics match
     * {@link #fillAlphaState(float)}.
     *
     * @param alpha stroking alpha constant in {@code [0, 1]}
     * @return shared graphics state owned by the current render pass
     * @since 2.1.0
     */
    public PDExtendedGraphicsState strokeAlphaState(float alpha) {
        return strokeAlphaStates.computeIfAbsent(alpha, value -> {
            PDExtendedGraphicsState state = new PDExtendedGraphicsState();
            state.setStrokingAlphaConstant(value);
            return state;
        });
    }

    /**
     * Resolves an image XObject through the render-pass image cache.
     *
     * @param imageData semantic image payload
     * @return decoded PDFBox image object shared by matching fragments
     */
    public PDImageXObject resolveImage(ImageData imageData) {
        return imageCache.computeIfAbsent(
                imageData.getFingerprint(),
                ignored -> createImage(document, imageData));
    }

    private PDImageXObject createImage(PDDocument document, ImageData imageData) {
        try {
            return PDImageXObject.createFromByteArray(document, imageData.getBytes(), imageData.getSourceKey());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to decode image '" + imageData.getSourceKey() + "'", e);
        }
    }

    void registerBookmark(PlacedFragment fragment, DocumentBookmarkOptions bookmarkOptions) {
        bookmarkRecords.add(new BookmarkRecord(
                bookmarkOptions.title(),
                bookmarkOptions.level(),
                fragment.pageIndex() + pageIndexOffset,
                fragment.y() + fragment.height()));
    }

    List<BookmarkRecord> bookmarkRecords() {
        return List.copyOf(bookmarkRecords);
    }

    /**
     * Records that a run was drawn in an order other than the one it was written in.
     *
     * <p>Reordering is the only way text reaches the page in a form that is not what an
     * author typed — Arabic is shaped into its joined forms on the same path — so this is
     * what tells the save whether the document's glyph maps are worth reading back. It is
     * set from the paragraph, chip and table-cell paths.</p>
     *
     * @since 2.2.0
     */
    public void markReorderedText() {
        reorderedText = true;
    }

    /**
     * Whether any run was drawn in an order other than the one it was written in.
     *
     * <p>Package-private like the other end-of-render readers ({@code bookmarkRecords()},
     * {@code anchorDestinations()}): the backend is the only caller.</p>
     *
     * @return {@code true} once {@link #markReorderedText()} has been called
     */
    boolean reorderedText() {
        return reorderedText;
    }

    /**
     * Records the resolved page and top-left of a named anchor declared by an
     * {@code AnchorMarkerPayload} fragment. A duplicate name keeps the last
     * registration (and logs a warning), matching the documented contract.
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
                fragment.y() + fragment.height());
        AnchorDestination previous = anchorDestinations.put(anchor, destination);
        if (previous != null) {
            LOG.warn("render.pdf.anchor.duplicate name={} — last registration wins", anchor);
        }
    }

    /**
     * Defers an internal (anchor-targeting) link for resolution in the post-pass,
     * once every anchor's position is known (supports forward references).
     *
     * @param pageIndex source page index where the clickable rectangle lives
     * @param rectangle clickable rectangle on the source page
     * @param anchor    target anchor name
     */
    void deferInternalLink(int pageIndex, PdfLinkAnnotationWriter.PlacedPdfRect rectangle, String anchor) {
        deferredInternalLinks.add(new DeferredInternalLink(pageIndex + pageIndexOffset, rectangle, anchor));
    }

    Map<String, AnchorDestination> anchorDestinations() {
        return Map.copyOf(anchorDestinations);
    }

    List<DeferredInternalLink> deferredInternalLinks() {
        return List.copyOf(deferredInternalLinks);
    }

    record BookmarkRecord(String title, int level, int pageIndex, double y) {
    }

    /**
     * Resolved page and top-left of a named anchor destination.
     *
     * @param pageIndex zero-based page index the anchor resolved to
     * @param left      left edge in PDF user space
     * @param top       top edge in PDF user space
     */
    record AnchorDestination(int pageIndex, double left, double top) {
    }

    /**
     * A clickable internal-link rectangle awaiting anchor resolution.
     *
     * @param pageIndex source page index
     * @param rectangle clickable rectangle on the source page
     * @param anchor    target anchor name
     */
    record DeferredInternalLink(int pageIndex, PdfLinkAnnotationWriter.PlacedPdfRect rectangle, String anchor) {
    }
}
