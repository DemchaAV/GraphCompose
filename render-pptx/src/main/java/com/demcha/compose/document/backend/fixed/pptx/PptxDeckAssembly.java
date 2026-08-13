package com.demcha.compose.document.backend.fixed.pptx;

import com.demcha.compose.document.backend.fixed.FixedLayoutRenderContext;
import com.demcha.compose.document.backend.fixed.SectionUnit;
import com.demcha.compose.document.backend.fixed.pdf.PdfMeasurementResources;
import com.demcha.compose.document.backend.fixed.pptx.handlers.PptxChromeRenderer;
import com.demcha.compose.document.layout.LayoutCanvas;
import com.demcha.compose.document.layout.LayoutGraph;
import com.demcha.compose.document.layout.PlacedFragment;
import com.demcha.compose.document.layout.payloads.AnchorMarkerPayload;
import com.demcha.compose.document.layout.payloads.PdfSemanticFragmentPayload;
import com.demcha.compose.document.output.DocumentMetadata;
import com.demcha.compose.font.FontFamilyDefinition;
import com.demcha.compose.font.FontName;
import org.apache.poi.ooxml.POIXMLProperties;
import org.apache.poi.sl.usermodel.PictureData;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFPictureData;
import org.apache.poi.xslf.usermodel.XSLFPictureShape;

import javax.imageio.ImageIO;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Deck-level assembly shared by {@link PptxFixedLayoutBackend}'s output
 * paths: the multi-section render loop with combined-deck navigation, OPC
 * metadata application, and full-slide raster page placement. Extracted so
 * the backend class keeps only fragment dispatch, the single-document
 * pipelines, and its builder.
 *
 * @author Artem Demchyshyn
 */
final class PptxDeckAssembly {

    private PptxDeckAssembly() {
        // Utility class, no instantiation.
    }

    /**
     * Renders validated same-canvas sections into one deck written to
     * {@code output} (not normalized — the caller owns the deterministic zip
     * pass). Each section renders through its own chrome backend against its
     * window of the combined deck; navigation, deck metadata (first section
     * that declares it), and the invoked backend's deterministic pinning
     * apply once at the end.
     */
    static void renderSections(List<SectionUnit> sections,
                               LayoutCanvas canvas,
                               PptxFixedLayoutBackend invoked,
                               OutputStream output) throws Exception {
        int totalPages = sections.stream()
                .mapToInt(section -> Math.max(section.graph().totalPages(), 1))
                .sum();
        List<FontFamilyDefinition> unionFonts = unionCustomFonts(sections);
        try (XMLSlideShow show = new XMLSlideShow();
             PdfMeasurementResources measurement = PdfMeasurementResources.open(unionFonts)) {
            PptxRenderSession session = new PptxRenderSession(
                    show, canvas.width(), canvas.height(), totalPages);
            PptxRenderEnvironment environment = new PptxRenderEnvironment(
                    show, session, 0, canvas.height(), measurement.fontLibrary(), unionFonts);
            // A combined deck has one presentation and one font table, so one answer has
            // to govern: the backend renderSections was invoked on, as deterministic
            // output already does. A per-section chrome that declines is not consulted.
            environment.embedsBundledFonts(invoked.embedBundledFonts);
            int pageOffset = 0;
            for (SectionUnit section : sections) {
                // Each section renders with its OWN backend's handlers and
                // chrome, but records navigation against the combined deck
                // through the environment's section window.
                PptxFixedLayoutBackend chrome = (PptxFixedLayoutBackend) section.chrome();
                LayoutGraph graph = section.graph();
                int pages = Math.max(graph.totalPages(), 1);
                environment.beginSection(pageOffset);
                FixedLayoutRenderContext sectionContext = new FixedLayoutRenderContext(
                        section.canvas(), section.customFonts(), null, null);
                if (chrome.rasterSlidesDpi > 0) {
                    placeRasterPages(environment,
                            chrome.pdfBackendWithVisibleChrome().renderToImages(
                                    graph, sectionContext, chrome.rasterSlidesDpi, false, -1),
                            section.canvas());
                    // The pixels bake this section's outgoing navigation, but
                    // its anchors and bookmarks still exist as destinations
                    // for the other sections' links and for slide names.
                    recordRasterSectionDestinations(graph, environment);
                } else {
                    PptxChromeRenderer.applyWatermarkBehindContent(
                            environment, chrome.watermarkOptions, section.canvas(), pages);
                    chrome.renderGraph(graph, environment, sectionContext);
                    PptxChromeRenderer.applyWatermarkAboveContent(
                            environment, chrome.watermarkOptions, section.canvas(), pages);
                    PptxChromeRenderer.applyHeadersAndFooters(
                            environment, chrome.headerFooterOptions, section.canvas(), pages);
                }
                pageOffset += pages;
            }
            environment.beginSection(0);
            PptxNavigationWriter.apply(environment);
            environment.logRasterizedClipSummary();
            // Metadata is deck-global: the first section that declares it wins,
            // matching the PDF backend's combined-document rule.
            sections.stream()
                    .map(section -> ((PptxFixedLayoutBackend) section.chrome()).metadataOptions)
                    .filter(Objects::nonNull)
                    .findFirst()
                    .ifPresent(metadata -> applyMetadata(show, metadata));
            if (invoked.embedBundledFonts) {
                // After every section is drawn, so the deck carries the union of what its
                // sections used rather than one section's share.
                environment.embedBundledFontsUsed();
            }
            if (invoked.deterministicTimestamp != null) {
                PptxDeterminismWriter.pinCoreProperties(show, invoked.deterministicTimestamp);
            }
            show.write(output);
        }
    }

    /**
     * Registers a raster section's anchor destinations and bookmarks so
     * incoming cross-section links and slide names still resolve even though
     * the section's content is one picture.
     */
    private static void recordRasterSectionDestinations(LayoutGraph graph,
                                                        PptxRenderEnvironment environment) {
        for (PlacedFragment fragment : graph.fragments()) {
            if (fragment.payload() instanceof AnchorMarkerPayload anchor) {
                environment.registerAnchor(fragment, anchor.anchor());
            } else if (fragment.payload() instanceof PdfSemanticFragmentPayload semanticPayload
                    && semanticPayload.bookmarkOptions() != null) {
                environment.registerBookmark(fragment, semanticPayload.bookmarkOptions());
            }
        }
    }

    private static List<FontFamilyDefinition> unionCustomFonts(List<SectionUnit> sections) {
        Map<FontName, FontFamilyDefinition> byName = new LinkedHashMap<>();
        for (SectionUnit section : sections) {
            for (FontFamilyDefinition family : section.customFonts()) {
                byName.putIfAbsent(family.name(), family);
            }
        }
        return List.copyOf(byName.values());
    }

    /**
     * Places one full-slide picture per rendered page onto the environment's
     * active section window.
     */
    static void placeRasterPages(PptxRenderEnvironment environment,
                                 List<BufferedImage> pages,
                                 LayoutCanvas canvas) throws IOException {
        for (int pageIndex = 0; pageIndex < pages.size(); pageIndex++) {
            XSLFPictureData data = environment.slideShow().addPicture(
                    encodePng(pages.get(pageIndex)), PictureData.PictureType.PNG);
            XSLFPictureShape picture = environment.slide(pageIndex).createPicture(data);
            picture.setAnchor(new Rectangle2D.Double(0, 0, canvas.width(), canvas.height()));
        }
    }

    /**
     * Writes the canonical metadata into the deck's OPC properties: title,
     * author, subject, and keywords map onto the Dublin Core fields, the
     * creating application onto the extended {@code Application} property.
     * OPC has no producer field, so that value is not representable in .pptx.
     */
    static void applyMetadata(XMLSlideShow show, DocumentMetadata metadata) {
        POIXMLProperties.CoreProperties core = show.getProperties().getCoreProperties();
        if (metadata.getTitle() != null) {
            core.setTitle(metadata.getTitle());
        }
        if (metadata.getAuthor() != null) {
            core.setCreator(metadata.getAuthor());
        }
        if (metadata.getSubject() != null) {
            core.setSubjectProperty(metadata.getSubject());
        }
        if (metadata.getKeywords() != null) {
            core.setKeywords(metadata.getKeywords());
        }
        if (metadata.getCreator() != null) {
            show.getProperties().getExtendedProperties().setApplication(metadata.getCreator());
        }
    }

    private static byte[] encodePng(BufferedImage image) throws IOException {
        try (ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", buffer);
            return buffer.toByteArray();
        }
    }
}
