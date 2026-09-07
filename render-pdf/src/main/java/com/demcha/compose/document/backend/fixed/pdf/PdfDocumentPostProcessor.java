package com.demcha.compose.document.backend.fixed.pdf;

import com.demcha.compose.document.backend.fixed.pdf.options.PdfHeaderFooterOptions;
import com.demcha.compose.document.backend.fixed.pdf.options.PdfMetadataOptions;
import com.demcha.compose.document.backend.fixed.pdf.options.PdfProtectionOptions;
import com.demcha.compose.document.backend.fixed.pdf.options.PdfViewerPreferencesOptions;
import com.demcha.compose.document.backend.fixed.pdf.options.PdfWatermarkOptions;
import com.demcha.compose.document.layout.LayoutCanvas;
import com.demcha.compose.document.style.DocumentPageLayout;
import com.demcha.compose.document.style.DocumentPageMode;
import com.demcha.compose.engine.components.style.Margin;
import com.demcha.compose.engine.render.pdf.helpers.PdfHeaderFooterRenderer;
import com.demcha.compose.engine.render.pdf.helpers.PdfWatermarkRenderer;
import com.demcha.compose.font.FontLibrary;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PageLayout;
import org.apache.pdfbox.pdmodel.PageMode;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.apache.pdfbox.pdmodel.interactive.viewerpreferences.PDViewerPreferences;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

/**
 * Applies canonical document-level PDF chrome after the semantic backend has
 * finished fragment rendering.
 *
 * <p>The processor reuses the production-grade PDF backend helpers internally
 * while exposing only canonical {@code document.*} DTOs to callers.</p>
 */
public final class PdfDocumentPostProcessor {
    private PdfDocumentPostProcessor() {
    }

    /**
     * Applies canonical document-level PDF options to an already rendered
     * PDFBox document in place.
     *
     * @param document            target PDFBox document
     * @param canvas              semantic layout canvas used to derive content margins
     * @param metadataOptions     canonical metadata options, or {@code null}
     * @param watermarkOptions    canonical watermark options, or {@code null}
     * @param protectionOptions   canonical protection options, or {@code null}
     * @param headerFooterOptions repeating header/footer options
     * @param fonts               the document's font library, used to resolve each
     *                            header/footer zone's font family
     * @throws IOException if PDFBox post-processing fails
     */
    public static void apply(PDDocument document,
                             LayoutCanvas canvas,
                             PdfMetadataOptions metadataOptions,
                             PdfWatermarkOptions watermarkOptions,
                             PdfProtectionOptions protectionOptions,
                             Collection<PdfHeaderFooterOptions> headerFooterOptions,
                             FontLibrary fonts) throws IOException {
        if (watermarkOptions != null) {
            PdfWatermarkRenderer.apply(document, PdfOptionsAdapter.toEngine(watermarkOptions));
        }

        if (headerFooterOptions != null && !headerFooterOptions.isEmpty()) {
            Margin canvasMargin = canvas.margin();
            float marginLeft = canvasMargin != null ? (float) canvasMargin.left() : 24f;
            float marginRight = canvasMargin != null ? (float) canvasMargin.right() : 24f;
            List<com.demcha.compose.engine.components.content.header_footer.HeaderFooterConfig> configs =
                    headerFooterOptions.stream()
                            .map(PdfOptionsAdapter::toEngine)
                            .toList();
            PdfHeaderFooterRenderer.apply(document, configs, fonts, marginLeft, marginRight);
        }

        if (metadataOptions != null) {
            applyMetadata(document, metadataOptions);
        }

        if (protectionOptions != null) {
            applyProtection(document, protectionOptions);
        }
    }

    /**
     * Applies the per-section visible chrome — watermark and repeating
     * header/footer — to one contiguous window of pages in a combined
     * multi-section document, numbering each section from its own first page.
     *
     * <p>Metadata and protection are document-global in PDF and are NOT applied
     * here; see {@link #applyDocumentMetadataAndProtection}.</p>
     *
     * @param document            target combined PDFBox document
     * @param canvas              section layout canvas used to derive content margins
     * @param watermarkOptions    section watermark options, or {@code null}
     * @param headerFooterOptions section repeating header/footer options
     * @param basePageOffset      zero-based index of the section's first page
     * @param sectionPageCount    number of pages in the section
     * @param fonts               the combined document's font library, used to
     *                            resolve each header/footer zone's font family
     * @throws IOException if PDFBox post-processing fails
     */
    public static void applySectionChrome(PDDocument document,
                                          LayoutCanvas canvas,
                                          PdfWatermarkOptions watermarkOptions,
                                          Collection<PdfHeaderFooterOptions> headerFooterOptions,
                                          int basePageOffset,
                                          int sectionPageCount,
                                          FontLibrary fonts) throws IOException {
        if (watermarkOptions != null) {
            PdfWatermarkRenderer.apply(
                    document, PdfOptionsAdapter.toEngine(watermarkOptions), basePageOffset, sectionPageCount);
        }

        if (headerFooterOptions != null && !headerFooterOptions.isEmpty()) {
            Margin canvasMargin = canvas.margin();
            float marginLeft = canvasMargin != null ? (float) canvasMargin.left() : 24f;
            float marginRight = canvasMargin != null ? (float) canvasMargin.right() : 24f;
            List<com.demcha.compose.engine.components.content.header_footer.HeaderFooterConfig> configs =
                    headerFooterOptions.stream()
                            .map(PdfOptionsAdapter::toEngine)
                            .toList();
            PdfHeaderFooterRenderer.apply(
                    document, configs, fonts, marginLeft, marginRight, basePageOffset, sectionPageCount);
        }
    }

    /**
     * Applies document-global metadata and protection to a combined document.
     * Unlike watermark and header/footer, these cannot be scoped per section, so
     * a multi-section document applies them once.
     *
     * @param document          target PDFBox document
     * @param metadataOptions   metadata options, or {@code null}
     * @param protectionOptions protection options, or {@code null}
     * @throws IOException if PDFBox post-processing fails
     */
    public static void applyDocumentMetadataAndProtection(PDDocument document,
                                                          PdfMetadataOptions metadataOptions,
                                                          PdfProtectionOptions protectionOptions) throws IOException {
        if (metadataOptions != null) {
            applyMetadata(document, metadataOptions);
        }
        if (protectionOptions != null) {
            applyProtection(document, protectionOptions);
        }
    }

    /**
     * Writes document-global viewer preferences to the PDF catalog: page mode,
     * page layout, and the window-chrome flags. Each unset field leaves the
     * reader's default in place. A no-op when {@code options} is {@code null}.
     *
     * @param document target PDFBox document
     * @param options  viewer-preference options, or {@code null}
     * @since 1.9.0
     */
    public static void applyViewerPreferences(PDDocument document, PdfViewerPreferencesOptions options) {
        if (options == null) {
            return;
        }
        PDDocumentCatalog catalog = document.getDocumentCatalog();
        if (options.getPageMode() != null) {
            catalog.setPageMode(toPageMode(options.getPageMode()));
        }
        if (options.getPageLayout() != null) {
            catalog.setPageLayout(toPageLayout(options.getPageLayout()));
        }

        PDViewerPreferences prefs = catalog.getViewerPreferences();
        if (prefs == null) {
            prefs = new PDViewerPreferences(new COSDictionary());
        }
        boolean anyFlag = false;
        if (options.getDisplayDocTitle() != null) {
            prefs.setDisplayDocTitle(options.getDisplayDocTitle());
            anyFlag = true;
        }
        if (options.getHideToolbar() != null) {
            prefs.setHideToolbar(options.getHideToolbar());
            anyFlag = true;
        }
        if (options.getHideMenubar() != null) {
            prefs.setHideMenubar(options.getHideMenubar());
            anyFlag = true;
        }
        if (options.getFitWindow() != null) {
            prefs.setFitWindow(options.getFitWindow());
            anyFlag = true;
        }
        if (options.getCenterWindow() != null) {
            prefs.setCenterWindow(options.getCenterWindow());
            anyFlag = true;
        }
        if (anyFlag) {
            catalog.setViewerPreferences(prefs);
        }
    }

    private static PageMode toPageMode(DocumentPageMode mode) {
        return switch (mode) {
            case USE_NONE -> PageMode.USE_NONE;
            case USE_OUTLINES -> PageMode.USE_OUTLINES;
            case USE_THUMBNAILS -> PageMode.USE_THUMBS;
            case FULL_SCREEN -> PageMode.FULL_SCREEN;
            case USE_ATTACHMENTS -> PageMode.USE_ATTACHMENTS;
        };
    }

    private static PageLayout toPageLayout(DocumentPageLayout layout) {
        return switch (layout) {
            case SINGLE_PAGE -> PageLayout.SINGLE_PAGE;
            case ONE_COLUMN -> PageLayout.ONE_COLUMN;
            case TWO_COLUMN_LEFT -> PageLayout.TWO_COLUMN_LEFT;
            case TWO_COLUMN_RIGHT -> PageLayout.TWO_COLUMN_RIGHT;
            case TWO_PAGE_LEFT -> PageLayout.TWO_PAGE_LEFT;
            case TWO_PAGE_RIGHT -> PageLayout.TWO_PAGE_RIGHT;
        };
    }

    private static void applyMetadata(PDDocument document, PdfMetadataOptions metadataOptions) {
        PDDocumentInformation info = document.getDocumentInformation();
        if (metadataOptions.getTitle() != null) {
            info.setTitle(metadataOptions.getTitle());
        }
        if (metadataOptions.getAuthor() != null) {
            info.setAuthor(metadataOptions.getAuthor());
        }
        if (metadataOptions.getSubject() != null) {
            info.setSubject(metadataOptions.getSubject());
        }
        if (metadataOptions.getKeywords() != null) {
            info.setKeywords(metadataOptions.getKeywords());
        }
        if (metadataOptions.getCreator() != null) {
            info.setCreator(metadataOptions.getCreator());
        }
        if (metadataOptions.getProducer() != null) {
            info.setProducer(metadataOptions.getProducer());
        }
    }

    /**
     * Applies canonical document-level PDF options to already rendered PDF bytes
     * and returns a new byte array.
     *
     * @param pdfBytes            rendered PDF bytes
     * @param canvas              semantic layout canvas used to derive content margins
     * @param metadataOptions     canonical metadata options, or {@code null}
     * @param watermarkOptions    canonical watermark options, or {@code null}
     * @param protectionOptions   canonical protection options, or {@code null}
     * @param headerFooterOptions repeating header/footer options
     * @return post-processed PDF bytes
     * @throws IOException if the PDF cannot be loaded or post-processed
     */
    public static byte[] apply(byte[] pdfBytes,
                               LayoutCanvas canvas,
                               PdfMetadataOptions metadataOptions,
                               PdfWatermarkOptions watermarkOptions,
                               PdfProtectionOptions protectionOptions,
                               Collection<PdfHeaderFooterOptions> headerFooterOptions) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            // This entry point receives finished bytes rather than a live render, so
            // there is no session font library to inherit. Build one over the loaded
            // document: it covers the standard-14 and bundled families a zone can name.
            // A family registered only for that render as a custom font is not among
            // them and falls back to Helvetica, as it did before zones could name one.
            apply(document, canvas, metadataOptions, watermarkOptions, protectionOptions,
                    headerFooterOptions, PdfFontLibraryFactory.library(document));
            try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                document.save(output);
                return output.toByteArray();
            }
        }
    }

    /**
     * Returns {@code true} when any document-level post-processing option is set.
     *
     * @param metadataOptions     canonical metadata options, or {@code null}
     * @param watermarkOptions    canonical watermark options, or {@code null}
     * @param protectionOptions   canonical protection options, or {@code null}
     * @param headerFooterOptions repeating header/footer options
     * @return {@code true} when post-processing work is required
     */
    public static boolean hasPostProcessing(PdfMetadataOptions metadataOptions,
                                            PdfWatermarkOptions watermarkOptions,
                                            PdfProtectionOptions protectionOptions,
                                            Collection<PdfHeaderFooterOptions> headerFooterOptions) {
        return metadataOptions != null
               || watermarkOptions != null
               || protectionOptions != null
               || (headerFooterOptions != null && !headerFooterOptions.isEmpty());
    }

    /**
     * Applies password protection to a built document.
     *
     * <p>Package-visible because protection is the one post-processing step whose timing
     * matters: encryption happens while a document is being <em>saved</em> and writes the
     * ciphertext back into the streams it encrypted, so anything that needs to read a
     * stream the save produced — the glyph-map correction — has to run before this
     * does. {@link PdfShapedGlyphUnicode#save} defers it for exactly that reason.</p>
     *
     * @param document target PDFBox document
     * @param options  protection options
     * @throws IOException if the protection policy cannot be applied
     */
    static void applyProtection(PDDocument document, PdfProtectionOptions options) throws IOException {
        AccessPermission permission = new AccessPermission();
        permission.setCanPrint(options.isCanPrint());
        permission.setCanExtractContent(options.isCanCopyContent());
        permission.setCanModify(options.isCanModify());
        permission.setCanFillInForm(options.isCanFillForms());
        permission.setCanExtractForAccessibility(options.isCanExtractForAccessibility());
        permission.setCanAssembleDocument(options.isCanAssemble());
        permission.setCanPrintFaithful(options.isCanPrintHighQuality());

        StandardProtectionPolicy policy = new StandardProtectionPolicy(
                options.getOwnerPassword(),
                options.getUserPassword(),
                permission);
        policy.setEncryptionKeyLength(options.getKeyLength());
        document.protect(policy);
    }
}
