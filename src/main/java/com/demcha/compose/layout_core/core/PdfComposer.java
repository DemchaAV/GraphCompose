package com.demcha.compose.layout_core.core;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.font_library.DefaultFonts;
import com.demcha.compose.font_library.FontFamilyDefinition;
import com.demcha.compose.layout_core.debug.LayoutSnapshot;
import com.demcha.compose.layout_core.debug.LayoutSnapshotExtractor;
import com.demcha.compose.layout_core.components.content.header_footer.HeaderFooterConfig;
import com.demcha.compose.layout_core.components.content.header_footer.HeaderFooterZone;
import com.demcha.compose.layout_core.components.content.metadata.DocumentMetadata;
import com.demcha.compose.layout_core.components.content.protection.PdfProtectionConfig;
import com.demcha.compose.layout_core.components.content.watermark.WatermarkConfig;
import com.demcha.compose.layout_core.components.style.Margin;
import com.demcha.compose.layout_core.system.LayoutSystem;
import com.demcha.compose.layout_core.system.implemented_systems.pdf_systems.PdfFont;
import com.demcha.compose.layout_core.system.implemented_systems.pdf_systems.PdfCanvas;
import com.demcha.compose.layout_core.system.implemented_systems.pdf_systems.PdfFileManagerSystem;
import com.demcha.compose.layout_core.system.implemented_systems.pdf_systems.PdfRenderingSystemECS;
import com.demcha.compose.layout_core.system.implemented_systems.pdf_systems.helpers.PdfBookmarkBuilder;
import com.demcha.compose.layout_core.system.implemented_systems.pdf_systems.helpers.PdfHeaderFooterRenderer;
import com.demcha.compose.layout_core.system.implemented_systems.pdf_systems.helpers.PdfWatermarkRenderer;
import com.demcha.compose.layout_core.system.measurement.FontLibraryTextMeasurementSystem;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * PDF-backed implementation of {@link DocumentComposer}.
 * <p>
 * This class assembles the concrete runtime pieces for the supported production
 * backend: a PDFBox document, an {@code EntityManager}, a canvas, the layout
 * system, the PDF rendering system, and an optional file output system.
 * </p>
 *
 * <p>In practice it is the bridge between the declarative entity graph and the
 * final PDF bytes:</p>
 * <ol>
 *   <li>builders register entities into the manager</li>
 *   <li>layout resolves geometry and pagination</li>
 *   <li>the PDF renderer draws resolved entities</li>
 *   <li>the result is written to a file or returned as bytes</li>
 * </ol>
 *
 * <h3>Build to file</h3>
 * <pre>
 * try (var composer = GraphCompose.pdf(outputPath).create()) {
 *     composer.componentBuilder()
 *             .text()
 *             .textWithAutoSize("Hello GraphCompose")
 *             .textStyle(TextStyle.DEFAULT_STYLE)
 *             .anchor(Anchor.topLeft())
 *             .build();
 *
 *     composer.build();
 * }
 * </pre>
 *
 * <h3>Or get bytes directly</h3>
 * <pre>
 * try (var composer = GraphCompose.pdf()
 *         .pageSize(PDRectangle.A4)
 *         .create()) {
 *
 *     composer.componentBuilder()
 *             .text()
 *             .textWithAutoSize("In-memory PDF")
 *             .textStyle(TextStyle.DEFAULT_STYLE)
 *             .anchor(Anchor.topLeft())
 *             .build();
 *
 *     byte[] pdfBytes = composer.toBytes();
 * }
 * </pre>
 */
public final class PdfComposer extends AbstractDocumentComposer {

    private final PDDocument doc;
    private final LayoutSystem<PdfRenderingSystemECS> layoutSystem;
    private final PdfRenderingSystemECS renderingSystem;
    @Nullable
    private final Path outputFile; // null if output to bytes only
    @Nullable
    private final PdfFileManagerSystem fileManagerSystem;

    // ===== Document-level feature configs =====
    private final List<HeaderFooterConfig> headerFooterConfigs = new ArrayList<>();
    @Nullable private WatermarkConfig watermarkConfig;
    @Nullable private DocumentMetadata documentMetadata;
    @Nullable private PdfProtectionConfig protectionConfig;
    private boolean postProcessed = false;

    /**
     * Internal constructor used by {@link GraphCompose.PdfBuilder}.
     *
     * <p>Callers normally obtain instances via {@link GraphCompose#pdf(Path)} or
     * {@link GraphCompose#pdf()} and never construct this type directly.</p>
     */
    public PdfComposer(Path outputFile, boolean markdown, boolean guideLines, PDRectangle pageSize, Margin margin,
            Collection<FontFamilyDefinition> customFontFamilies) {
        this(createState(markdown, guideLines, pageSize, margin, customFontFamilies), outputFile);
    }

    private PdfComposer(PdfComposerState state, @Nullable Path outputFile) {
        super(state.entityManager(), state.canvas());
        this.doc = state.doc();
        this.renderingSystem = state.renderingSystem();
        this.outputFile = outputFile;
        this.layoutSystem = new LayoutSystem<>(canvas(), renderingSystem);
        this.fileManagerSystem = outputFile == null ? null : new PdfFileManagerSystem(outputFile, doc);
        setupPdfSystems();
    }

    private static PdfComposerState createState(boolean markdown,
                                                boolean guideLines,
                                                PDRectangle pageSize,
                                                Margin margin,
                                                Collection<FontFamilyDefinition> customFontFamilies) {
        PDDocument doc = new PDDocument();
        EntityManager entityManager = new EntityManager(DefaultFonts.library(doc, customFontFamilies), markdown);
        entityManager.setGuideLines(guideLines);

        Canvas canvas = new PdfCanvas(pageSize, 0.0f, 0.0f);
        if (margin != null) {
            canvas.addMargin(margin);
        }

        PdfRenderingSystemECS renderingSystem = new PdfRenderingSystemECS(doc, canvas);
        return new PdfComposerState(doc, entityManager, canvas, renderingSystem);
    }

    private void setupPdfSystems() {
        entityManager().getSystems().addSystem(new FontLibraryTextMeasurementSystem(entityManager().getFonts(), PdfFont.class));
        entityManager().getSystems().addSystem(layoutSystem);
        entityManager().getSystems().addSystem(renderingSystem);
        // Note: PdfFileManagerSystem is NOT added to the system pipeline.
        // File saving is done explicitly in buildDocument() AFTER post-processing.
    }

    /**
     * Adds an additional margin to the current canvas.
     *
     * <p>This mutates the canvas configuration for subsequent root-level layout
     * calculations. It does not retroactively re-render the document until a build
     * operation is triggered.</p>
     *
     * @param margin the margin to apply to the canvas
     */
    public void margin(Margin margin) {
        canvas().addMargin(margin);
    }

    public List<com.demcha.compose.font_library.FontName> availableFonts() {
        return List.copyOf(entityManager().getFonts().availableFonts());
    }

    // ===== Document-level feature API =====

    /**
     * Configures a page header that repeats on every page.
     *
     * <p>Text may contain placeholders: {@code {page}}, {@code {pages}}, {@code {date}}.</p>
     *
     * @param config the header configuration
     * @return this composer
     */
    public PdfComposer header(HeaderFooterConfig config) {
        this.headerFooterConfigs.add(HeaderFooterConfig.builder()
                .zone(HeaderFooterZone.HEADER)
                .height(config.getHeight())
                .leftText(config.getLeftText())
                .centerText(config.getCenterText())
                .rightText(config.getRightText())
                .fontSize(config.getFontSize())
                .textColor(config.getTextColor())
                .showSeparator(config.isShowSeparator())
                .separatorColor(config.getSeparatorColor())
                .separatorThickness(config.getSeparatorThickness())
                .build());
        return this;
    }

    /**
     * Convenience: configure a simple header with left/center/right text.
     */
    public PdfComposer header(String leftText, String centerText, String rightText) {
        this.headerFooterConfigs.add(HeaderFooterConfig.builder()
                .zone(HeaderFooterZone.HEADER)
                .leftText(leftText)
                .centerText(centerText)
                .rightText(rightText)
                .showSeparator(true)
                .build());
        return this;
    }

    /**
     * Configures a page footer that repeats on every page.
     *
     * <p>Text may contain placeholders: {@code {page}}, {@code {pages}}, {@code {date}}.</p>
     *
     * @param config the footer configuration
     * @return this composer
     */
    public PdfComposer footer(HeaderFooterConfig config) {
        this.headerFooterConfigs.add(HeaderFooterConfig.builder()
                .zone(HeaderFooterZone.FOOTER)
                .height(config.getHeight())
                .leftText(config.getLeftText())
                .centerText(config.getCenterText())
                .rightText(config.getRightText())
                .fontSize(config.getFontSize())
                .textColor(config.getTextColor())
                .showSeparator(config.isShowSeparator())
                .separatorColor(config.getSeparatorColor())
                .separatorThickness(config.getSeparatorThickness())
                .build());
        return this;
    }

    /**
     * Convenience: configure a simple footer with page numbers.
     */
    public PdfComposer footer(String leftText, String centerText, String rightText) {
        this.headerFooterConfigs.add(HeaderFooterConfig.builder()
                .zone(HeaderFooterZone.FOOTER)
                .leftText(leftText)
                .centerText(centerText)
                .rightText(rightText)
                .showSeparator(true)
                .build());
        return this;
    }

    /**
     * Configures a document-wide watermark.
     *
     * @param config the watermark configuration
     * @return this composer
     */
    public PdfComposer watermark(WatermarkConfig config) {
        this.watermarkConfig = config;
        return this;
    }

    /**
     * Convenience: configure a text watermark with default settings.
     */
    public PdfComposer watermark(String text) {
        this.watermarkConfig = WatermarkConfig.builder().text(text).build();
        return this;
    }

    /**
     * Sets document metadata (title, author, subject, keywords).
     *
     * @param metadata the metadata configuration
     * @return this composer
     */
    public PdfComposer metadata(DocumentMetadata metadata) {
        this.documentMetadata = metadata;
        return this;
    }

    /**
     * Configures PDF encryption and access permissions.
     *
     * @param config the protection configuration
     * @return this composer
     */
    public PdfComposer protect(PdfProtectionConfig config) {
        this.protectionConfig = config;
        return this;
    }

    // ===== Debug / Snapshot =====

    /**
     * Resolves the document layout and returns a deterministic debug snapshot of
     * the layout tree after pagination.
     *
     * <p>This method does not render the PDF stream. It is intended for layout
     * regression tests, debugging, and dev tooling that needs geometry rather
     * than pixels.</p>
     *
     * <p>This debug API is intentionally isolated from the normal production
     * pipeline. Calling {@link #build()}, {@link #toBytes()}, or
     * {@link #toPDDocument()} does not depend on this method and does not reuse
     * any debug snapshot state.</p>
     *
     * @return resolved layout snapshot for the current document
     * @throws Exception if layout resolution fails
     */
    public LayoutSnapshot layoutSnapshot() throws Exception {
        buildComponents();
        layoutSystem.process(entityManager());
        return LayoutSnapshotExtractor.extract(entityManager(), canvas());
    }

    @Override
    protected void buildDocument() throws Exception {
        entityManager().processSystems();
        applyPostProcessing();
        // Save to file AFTER post-processing (metadata, watermarks, etc.)
        if (fileManagerSystem != null) {
            fileManagerSystem.process(entityManager());
        }
    }

    @Override
    protected byte[] exportBytes() throws Exception {
        processInMemoryRender();
        applyPostProcessing();
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            doc.save(baos);
            return baos.toByteArray();
        }
    }

    /**
     * Builds the document and exposes the underlying {@link PDDocument}.
     * <p>
     * Use this when a caller needs to continue working with PDFBox directly after
     * GraphCompose has completed entity materialization, layout, and PDF rendering.
     * </p>
     *
     * <p><b>Lifecycle note:</b> the returned document is still owned by this
     * composer. Keep normal resource handling in mind and close the composer when
     * the PDF document is no longer needed.</p>
     *
     * @return the processed {@link PDDocument}
     * @throws Exception if layout or rendering fails
     */
    public PDDocument toPDDocument() throws Exception {
        buildComponents();
        processInMemoryRender();
        applyPostProcessing();
        return doc;
    }

    private void processInMemoryRender() throws Exception {
        entityManager().getSystems().getSystem(LayoutSystem.class)
                .ifPresent(sys -> sys.process(entityManager()));
        if (renderingSystem != null) {
            renderingSystem.process(entityManager());
        }
    }

    /**
     * Applies document-level post-processing features after all entities have
     * been laid out and rendered: watermarks, headers/footers, bookmarks,
     * metadata, and protection.
     */
    private void applyPostProcessing() throws IOException {
        if (postProcessed) return;
        postProcessed = true;
        // 1. Watermark (behind content is applied via prepend at rendering time)
        if (watermarkConfig != null) {
            PdfWatermarkRenderer.apply(doc, watermarkConfig);
        }

        // 2. Headers and footers
        if (!headerFooterConfigs.isEmpty()) {
            Margin canvasMargin = canvas().margin();
            float marginLeft = canvasMargin != null ? (float) canvasMargin.left() : 24f;
            float marginRight = canvasMargin != null ? (float) canvasMargin.right() : 24f;
            PdfHeaderFooterRenderer.apply(doc, headerFooterConfigs, marginLeft, marginRight);
        }

        // 3. Bookmarks
        PdfBookmarkBuilder.buildOutline(doc, entityManager());

        // 4. Document metadata
        if (documentMetadata != null) {
            PDDocumentInformation info = doc.getDocumentInformation();
            if (documentMetadata.getTitle() != null) info.setTitle(documentMetadata.getTitle());
            if (documentMetadata.getAuthor() != null) info.setAuthor(documentMetadata.getAuthor());
            if (documentMetadata.getSubject() != null) info.setSubject(documentMetadata.getSubject());
            if (documentMetadata.getKeywords() != null) info.setKeywords(documentMetadata.getKeywords());
            if (documentMetadata.getCreator() != null) info.setCreator(documentMetadata.getCreator());
            if (documentMetadata.getProducer() != null) info.setProducer(documentMetadata.getProducer());
        }

        // 5. PDF protection
        if (protectionConfig != null) {
            applyProtection();
        }
    }

    private void applyProtection() throws IOException {
        AccessPermission ap = new AccessPermission();
        ap.setCanPrint(protectionConfig.isCanPrint());
        ap.setCanExtractContent(protectionConfig.isCanCopyContent());
        ap.setCanModify(protectionConfig.isCanModify());
        ap.setCanFillInForm(protectionConfig.isCanFillForms());
        ap.setCanExtractForAccessibility(protectionConfig.isCanExtractForAccessibility());
        ap.setCanAssembleDocument(protectionConfig.isCanAssemble());
        ap.setCanPrintFaithful(protectionConfig.isCanPrintHighQuality());

        StandardProtectionPolicy policy = new StandardProtectionPolicy(
                protectionConfig.getOwnerPassword(),
                protectionConfig.getUserPassword(),
                ap);
        policy.setEncryptionKeyLength(protectionConfig.getKeyLength());
        doc.protect(policy);
    }

    @Override
    public void close() throws IOException {
        doc.close();
    }

    private record PdfComposerState(
            PDDocument doc,
            EntityManager entityManager,
            Canvas canvas,
            PdfRenderingSystemECS renderingSystem) {
    }
}


