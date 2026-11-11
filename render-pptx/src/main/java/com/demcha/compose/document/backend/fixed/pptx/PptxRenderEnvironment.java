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
import com.demcha.compose.font.DefaultFonts;
import com.demcha.compose.font.FontFamilyDefinition;
import com.demcha.compose.font.FontLibrary;
import com.demcha.compose.font.FontName;
import org.apache.poi.sl.usermodel.PictureData;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFFontInfo;
import org.apache.poi.xslf.usermodel.XSLFPictureData;
import org.apache.poi.xslf.usermodel.XSLFGroupShape;
import org.apache.poi.xslf.usermodel.XSLFShapeContainer;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.geom.Rectangle2D;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

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
    private final Map<FontName, String> customFontFamilies;
    private final Map<FontName, PptxViewerMetrics.ViewerFontMetrics> viewerFontMetrics;
    private final Map<String, XSLFPictureData> pictureDataCache = new LinkedHashMap<>();
    private final Map<String, java.awt.Dimension> pictureSizeCache = new LinkedHashMap<>();
    private final List<BookmarkRecord> bookmarkRecords = new ArrayList<>();
    private final Map<String, AnchorDestination> anchorDestinations = new LinkedHashMap<>();
    private final List<FragmentLink> fragmentLinks = new ArrayList<>();
    private final Set<String> substitutionWarned = new LinkedHashSet<>();
    /**
     * Every face a run asked for: family to the bold/italic combinations drawn with it.
     *
     * <p>Kept per face rather than per family because embedding is whole-font. A deck
     * showing six Korean characters in one weight was carrying Gothic A1 twice — 4.4 MB
     * for a facet that is never on a slide.</p>
     */
    private final Map<FontName, Set<Face>> usedFaces = new LinkedHashMap<>();
    /** Bundled families already noted as carried, so the note is one line per family. */
    private final Set<String> bundledCarryNoted = new LinkedHashSet<>();

    /** A family's four faces, as the source set names them. */
    private enum Face {
        REGULAR, BOLD, ITALIC, BOLD_ITALIC;

        static Face of(boolean bold, boolean italic) {
            if (bold && italic) {
                return BOLD_ITALIC;
            }
            return bold ? BOLD : italic ? ITALIC : REGULAR;
        }

        FontFamilyDefinition.FontBinarySource in(FontFamilyDefinition.FontSourceSet sources) {
            return switch (this) {
                case BOLD -> sources.bold();
                case ITALIC -> sources.italic();
                case BOLD_ITALIC -> sources.boldItalic();
                case REGULAR -> sources.regular();
            };
        }
    }
    /** Bundled families that carry a binary, i.e. the ones the embedder can take. */
    private static final Map<FontName, FontFamilyDefinition> EMBEDDABLE_BUNDLED =
            DefaultFonts.bundledFamilies().stream()
                    .filter(family -> family.fontSourceSet().isPresent())
                    .collect(java.util.stream.Collectors.toMap(
                            FontFamilyDefinition::name, family -> family, (a, b) -> a,
                            LinkedHashMap::new));
    /** Whether this render will offer the families it used to the embedder. */
    private boolean embedsBundledFonts = true;
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
        Map<FontName, String> familyNames = new LinkedHashMap<>();
        Map<FontName, PptxViewerMetrics.ViewerFontMetrics> metrics = new LinkedHashMap<>();
        for (FontFamilyDefinition family : customFontFamilies) {
            familyNames.put(family.name(), family.wordFamily());
            if (family.fontSourceSet().isEmpty()) {
                // Said of the text, not of the deck: this runs before a single run is drawn,
                // so whether anything will use the family is not known here, and a family
                // registered this way and never drawn in is named nowhere in the package.
                // "No program" rather than "nothing": where text is drawn, the family's name
                // does travel, on the slide that drew it. The substitution is part of the same
                // clause and not a second sentence, so that it too says nothing about a deck
                // that draws no text in the family.
                LOG.warn("render.pptx.font.substitution family={} — registered without binary "
                                + "sources, so no program for \"{}\" travels with the deck; any "
                                + "text drawn with it is referenced by name only and "
                                + "substituted by viewers that do not have the font installed",
                        family.name().name(), family.wordFamily());
            }
            family.fontSourceSet().ifPresent(sources -> {
                if (embedFontFamily(show, family, sources)) {
                    metrics.put(family.name(), PptxViewerMetrics.load(family, sources));
                } else {
                    // Same key as every other substitution so one log filter
                    // catches them all; the embed-skip warning carries the cause.
                    // "Not carried whole" rather than "not carried" because the presentation
                    // refusing a face it has been handed keeps what it took: a read that fails
                    // leaves nothing of the family, that refusal can leave its declaration and
                    // an earlier face. The substitution is said of the text drawn in a face
                    // that did not travel, for the same reason from the other end — where a
                    // face did travel, a viewer without the font does not substitute its text.
                    LOG.warn("render.pptx.font.substitution family={} — could not be embedded, "
                                    + "so \"{}\" is not carried whole; text drawn in a face "
                                    + "that did not travel is referenced by name and "
                                    + "substituted by viewers that do not have the font "
                                    + "installed",
                            family.name().name(), family.wordFamily());
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
        if (fontName != null) {
            usedFaces.computeIfAbsent(fontName, name -> new LinkedHashSet<>())
                    .add(Face.of(bold, italic));
            String customFamily = customFontFamilies.get(fontName);
            if (customFamily != null) {
                return customFamily;
            }
        }
        // Dedupe on the resolved viewer family so facet-suffixed names
        // (Roboto-Regular, Roboto-Bold) warn once, not once per facet; the
        // message still names the source font the document asked for.
        String viewerFamily = PptxFontMapping.familyFor(fontName);
        String sourceName = (fontName == null ? FontName.HELVETICA : fontName).name();
        String replacement = PptxFontMapping.standardReplacementFor(fontName);
        if (replacement != null) {
            if (substitutionWarned.add(viewerFamily)) {
                LOG.warn("render.pptx.font.substitution family={} — a PDF built-in with no "
                                + "PPTX counterpart, rendered as metric-compatible \"{}\" "
                                + "(identical widths, slightly different letterforms); register "
                                + "a binary font family for identical glyphs in both formats",
                        sourceName, replacement);
            }
        } else if (embedsBundledFonts && EMBEDDABLE_BUNDLED.containsKey(fontName)) {
            // A bundled family this render carries itself. Warning here would tell the
            // reader the opposite of what the deck ends up containing; if the embed
            // fails, embedBundledFontsUsed says so at the point it knows. Noted once per
            // family, as the warning below is — an Arabic article draws sixteen hundred
            // runs in one family, and sixteen hundred identical lines say nothing more
            // than the first. Its own set, so a later decline still warns.
            if (bundledCarryNoted.add(viewerFamily)) {
                LOG.debug("render.pptx.font.bundled family={} — carried by this deck",
                        sourceName);
            }
        } else if (substitutionWarned.add(viewerFamily)) {
            // Nothing will carry it: either it was never registered, or this render was
            // asked not to carry bundled families. Either way the deck can only name it,
            // so a viewer without it installed substitutes.
            LOG.warn("render.pptx.font.substitution family={} — not registered with binary "
                            + "sources for this render, the deck references \"{}\" by name "
                            + "only; register the family (registerFontFamily) to embed it and "
                            + "guarantee identical glyphs on every machine",
                    sourceName, viewerFamily);
        }
        return viewerFamily;
    }

    /**
     * Returns the ascent, in points, that the viewer's font engine uses to seat
     * this run's first baseline below a top-anchored frame. PowerPoint positions
     * text by frame, not by baseline: the first baseline lands at
     * {@code frameTop + viewerAscent}, so frames anchor at
     * {@code baseline − viewerAscent}. Embedded fonts report their real font
     * program ascent per facet; standard-14 replacements use the known Arial /
     * Times New Roman / Courier New ratios; anything else assumes the PDF
     * descriptor ascent.
     *
     * <p>Deliberately never written to the run's DrawingML {@code baseline}
     * property: PowerPoint treats any non-zero baseline as super/subscript and
     * shrinks the glyphs.</p>
     *
     * @param style          resolved text style
     * @param fallbackAscent ascent to assume when the style cannot be resolved
     * @return viewer ascent in points
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
        return PptxViewerMetrics.ascentPoints(
                style, font, viewerFontMetrics.get(style.fontName()));
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
     * Embeds every distinct facet of one font family, returning whether the
     * family made it into the presentation. Embedding is best-effort: a font
     * whose license bits forbid embedding, or whose bytes cannot be read at
     * all — a missing file, a missing classpath resource, a program the writer
     * cannot parse — degrades to a plain family-name reference: page geometry
     * is unaffected, viewers simply substitute glyphs. Neither fails the export
     * here, however the refusal arrives; this answers {@code false} instead,
     * and names the family in the log.
     *
     * <p>That is as far as the promise goes. A family the page had to
     * <em>measure</em> fails the render before a slide is built; a family that
     * embeds goes on to {@link PptxViewerMetrics#load}, which reads the font
     * again with a different parser and can throw where this one did not; and
     * the presentation refusing a face it is handed is its own matter, below.</p>
     */
    private static boolean embedFontFamily(XMLSlideShow show,
                                           FontFamilyDefinition family,
                                           FontFamilyDefinition.FontSourceSet sources) {
        return embedFontFamily(show, family, sources, EnumSet.allOf(Face.class));
    }

    /**
     * Embeds the named faces of a family.
     *
     * <p>Faces rather than the whole family, because embedding carries a font program
     * whole: a deck drawing one weight of a CJK family was shipping the other at over two
     * megabytes for glyphs no slide contains. A family whose source set repeats a source
     * across faces — a regular-and-bold family asked for italic — embeds it once, since
     * the sources dedupe by description.</p>
     *
     * <p>Every face is <em>read</em> before any of them is handed to the presentation,
     * because the answer is one boolean for the whole family: it decides whether the family
     * keeps its viewer metrics and whether the reader is told to install the font. The
     * presentation is mutated at both steps — constructing the font info declares the
     * family, and adding a facet writes its part and sets the embedded-font flags — so
     * handing faces over as they were read left a family whose second face could not be
     * read with its first face in the deck, while the caller was told the deck carries
     * nothing of it. A family whose first face could not be read left the bare
     * declaration.</p>
     *
     * <p>That holds for everything that fails before the hand-over: a face that cannot be
     * read, and a face read whole whose licence bits forbid embedding, which
     * {@link PptxEmbeddedFont#wrap} refuses. So a family of one embeddable face and one
     * restricted face now carries neither, where it used to carry the first. It does not
     * hold for the presentation refusing a face it is handed, after the program is read and
     * parsed: that refusal happens inside the mutation, and what it
     * took stays — a part, a dataId, the embedded-font flags. Undoing all of those would
     * cover the presentation's refusal too, at the price of reaching that far into the
     * writer's own model for a
     * refusal it has not been seen to make; so this keeps the handling the presentation's
     * step already had, including which failures it raises rather than swallows. The cost
     * of buffering instead is that one family's programs are held twice while its facets
     * are added, since the parts keep their bytes as well.</p>
     */
    private static boolean embedFontFamily(XMLSlideShow show,
                                           FontFamilyDefinition family,
                                           FontFamilyDefinition.FontSourceSet sources,
                                           Set<Face> faces) {
        Map<String, byte[]> programs = new LinkedHashMap<>();
        for (FontFamilyDefinition.FontBinarySource source
                : faces.stream().map(face -> face.in(sources)).toList()) {
            // Named before it is read, so a source that cannot even say what it is still
            // warns rather than failing the render inside the warning. No source this
            // library ships can reach the sentinel; FontBinarySource is a caller's to
            // implement, and this answers for theirs.
            String description = "<unnamed>";
            try {
                description = source.description();
                if (programs.containsKey(description)) {
                    continue;
                }
                try (InputStream stream = source.openStream()) {
                    programs.put(description, PptxEmbeddedFont.wrap(stream));
                }
            } catch (IOException | RuntimeException exception) {
                // A source may fail unchecked: the classpath one reports a missing resource
                // with an IllegalArgumentException. Named here and the family degrades, as
                // for a missing file on disk — the render is not the caller's to lose.
                // toString rather than getMessage because an unchecked failure may carry none.
                LOG.warn("render.pptx.font.embed.skipped family={} source={} reason={}",
                        family.name().name(), description, exception.toString());
                return false;
            }
        }
        // From here the handling is the presentation's own, and exactly what it was before
        // the reads were pulled forward: a refusal while taking a face degrades the family,
        // and whatever the presentation took before it refused stays in it. Widening this
        // catch would extend that half-written state to failures it used to raise.
        XSLFFontInfo embedded = new XSLFFontInfo(show, family.wordFamily());
        for (Map.Entry<String, byte[]> program : programs.entrySet()) {
            try {
                embedded.addFacet(new ByteArrayInputStream(program.getValue()));
            } catch (IOException exception) {
                LOG.warn("render.pptx.font.embed.skipped family={} source={} reason={}",
                        family.name().name(), program.getKey(), exception.toString());
                return false;
            }
        }
        return true;
    }

    /**
     * Tells this render whether the families it uses will be carried.
     *
     * <p>Set before anything is drawn, because the answer changes what a run is warned
     * about: a family the deck is about to carry must not be reported as one the reader
     * has to install, and a render explicitly asked not to carry them must not be told to
     * register what it just declined.</p>
     *
     * @param enabled whether {@link #embedBundledFontsUsed()} will run for this render
     */
    void embedsBundledFonts(boolean enabled) {
        this.embedsBundledFonts = enabled;
    }

    /**
     * Embeds the bundled families this deck drew with, on the terms a registered one gets.
     *
     * <p>A family a caller registers themselves is embedded, and warned about when it
     * cannot be. A family this library <em>ships</em> — Amiri for Arabic, David Libre for
     * Hebrew, the Noto faces for Georgian and Armenian, Gothic A1 for Hangul — was neither:
     * the deck named it and embedded nothing, so a viewer without it installed substituted,
     * and for a script the substitute does not cover the slide showed boxes. The asymmetry
     * was the sharp edge, since the shipped families are the ones a deck reaches for
     * precisely when the reader is least likely to have the font.</p>
     *
     * <p>Only what was drawn is embedded, down to the face — the bundled set is dozens of
     * families, and embedding carries a font program whole with no subsetting, so a deck
     * offering all of them would be tens of megabytes and one offering a face it never
     * drew would pay megabytes for glyphs no slide contains.</p>
     *
     * <p>Metrics are deliberately untouched. A registered family also contributes viewer
     * metrics, which participate in layout; taking those from a bundled family now would
     * move text in every deck that already renders correctly. This changes what the file
     * carries, not where anything sits.</p>
     *
     * <p>Call after the last run is drawn — the recorded face set is only complete then.</p>
     */
    void embedBundledFontsUsed() {
        boolean embeddedAny = false;
        for (FontFamilyDefinition family : EMBEDDABLE_BUNDLED.values()) {
            // EMBEDDABLE_BUNDLED already excludes the standard-14 definitions: they carry
            // no binary to embed, and PPTX maps them to viewer-facing metric-compatible
            // replacements (Helvetica to Arial, and so on) rather than assuming the
            // viewer has the PDF built-in.
            Set<Face> faces = usedFaces.get(family.name());
            if (faces == null || customFontFamilies.containsKey(family.name())) {
                // Not drawn with, or the caller registered their own under this name — the
                // deck then references that family, so the bundled binary would be unused.
                continue;
            }
            Optional<FontFamilyDefinition.FontSourceSet> sources = family.fontSourceSet();
            if (embedFontFamily(show, family, sources.orElseThrow(), faces)) {
                embeddedAny = true;
            } else {
                // Reached on the same answer as the registered-family line above, and worded
                // the same way: being drawn in makes the deck's reference to the family real,
                // but neither that nor the substitution follows for a face that did travel.
                LOG.warn("render.pptx.font.substitution family={} — a bundled family that "
                                + "could not be embedded, so \"{}\" is not carried whole; text "
                                + "drawn in a face that did not travel is referenced by name "
                                + "and substituted by viewers that do not have the font "
                                + "installed",
                        family.name().name(), family.wordFamily());
            }
        }
        if (embeddedAny) {
            show.getCTPresentation().setSaveSubsetFonts(false);
        }
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
