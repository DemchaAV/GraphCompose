package com.demcha.compose.testsupport;

import com.demcha.compose.font_library.DefaultFonts;
import com.demcha.compose.font_library.FontFamilyDefinition;
import com.demcha.compose.font_library.FontName;
import com.demcha.compose.layout_core.components.components_builders.ComponentBuilder;
import com.demcha.compose.layout_core.components.content.header_footer.HeaderFooterConfig;
import com.demcha.compose.layout_core.components.content.header_footer.HeaderFooterZone;
import com.demcha.compose.layout_core.components.content.metadata.DocumentMetadata;
import com.demcha.compose.layout_core.components.content.protection.PdfProtectionConfig;
import com.demcha.compose.layout_core.components.content.watermark.WatermarkConfig;
import com.demcha.compose.layout_core.components.style.Margin;
import com.demcha.compose.layout_core.core.Canvas;
import com.demcha.compose.layout_core.core.EntityManager;
import com.demcha.compose.layout_core.debug.LayoutSnapshot;
import com.demcha.compose.layout_core.debug.LayoutSnapshotExtractor;
import com.demcha.compose.layout_core.system.LayoutSystem;
import com.demcha.compose.layout_core.system.implemented_systems.pdf_systems.PdfCanvas;
import com.demcha.compose.layout_core.system.implemented_systems.pdf_systems.PdfFileManagerSystem;
import com.demcha.compose.layout_core.system.implemented_systems.pdf_systems.PdfFont;
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
import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Test-only low-level engine harness used while production authoring stays
 * canonical through {@code DocumentSession}.
 */
public final class EngineComposerHarness implements Closeable {
    private final PDDocument doc;
    private final EntityManager entityManager;
    private final ComponentBuilder componentBuilder;
    private final Canvas canvas;
    private final LayoutSystem<PdfRenderingSystemECS> layoutSystem;
    private final PdfRenderingSystemECS renderingSystem;
    @Nullable
    private final Path outputFile;
    @Nullable
    private final PdfFileManagerSystem fileManagerSystem;
    private final List<HeaderFooterConfig> headerFooterConfigs = new ArrayList<>();
    @Nullable
    private WatermarkConfig watermarkConfig;
    @Nullable
    private DocumentMetadata documentMetadata;
    @Nullable
    private PdfProtectionConfig protectionConfig;
    private boolean postProcessed;
    private boolean layoutResolved;
    private boolean rendered;
    private long layoutConfigVersion;
    private long postProcessConfigVersion;
    private long resolvedLayoutVersion = -1;
    private long renderedVersion = -1;
    private long postProcessedVersion = -1;

    private EngineComposerHarness(Path outputFile,
                                  boolean markdown,
                                  boolean guideLines,
                                  PDRectangle pageSize,
                                  Margin margin,
                                  Collection<FontFamilyDefinition> customFontFamilies) {
        this.doc = new PDDocument();
        this.entityManager = new EntityManager(DefaultFonts.library(doc, customFontFamilies), markdown);
        this.entityManager.setGuideLines(guideLines);
        this.componentBuilder = createComponentBuilder(entityManager);
        this.canvas = new PdfCanvas(pageSize, 0.0f, 0.0f);
        if (margin != null) {
            this.canvas.addMargin(margin);
        }
        this.renderingSystem = new PdfRenderingSystemECS(doc, canvas);
        this.layoutSystem = new LayoutSystem<>(canvas, renderingSystem);
        this.outputFile = outputFile;
        this.fileManagerSystem = outputFile == null ? null : new PdfFileManagerSystem(outputFile, doc);
        setupSystems();
    }

    /**
     * Starts a test harness builder for in-memory output.
     *
     * @return harness builder
     */
    public static Builder pdf() {
        return new Builder(null);
    }

    /**
     * Starts a test harness builder for file output.
     *
     * @param outputFile target PDF path
     * @return harness builder
     */
    public static Builder pdf(Path outputFile) {
        return new Builder(outputFile);
    }

    /**
     * Returns the low-level engine builder facade.
     *
     * @return component builder
     */
    public ComponentBuilder componentBuilder() {
        return componentBuilder;
    }

    /**
     * Returns the active test canvas.
     *
     * @return canvas
     */
    public Canvas canvas() {
        return canvas;
    }

    /**
     * Enables or disables markdown parsing in the test entity manager.
     *
     * @param enabled whether markdown should be enabled
     */
    public void markdown(boolean enabled) {
        entityManager.setMarkdown(enabled);
    }

    /**
     * Enables or disables guide-line rendering in tests.
     *
     * @param enabled whether guides should be rendered
     */
    public void guideLines(boolean enabled) {
        entityManager.setGuideLines(enabled);
    }

    /**
     * Adds an extra canvas margin.
     *
     * @param margin margin to add
     */
    public void margin(Margin margin) {
        canvas.addMargin(margin);
        markLayoutDirty();
    }

    /**
     * Lists font families available to this harness.
     *
     * @return available font names
     */
    public List<FontName> availableFonts() {
        return List.copyOf(entityManager.getFonts().availableFonts());
    }

    /**
     * Adds a repeating header.
     *
     * @param config header config
     * @return this harness
     */
    public EngineComposerHarness header(HeaderFooterConfig config) {
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
        markPostProcessDirty();
        return this;
    }

    /**
     * Adds a simple repeating header.
     *
     * @return this harness
     */
    public EngineComposerHarness header(String leftText, String centerText, String rightText) {
        this.headerFooterConfigs.add(HeaderFooterConfig.builder()
                .zone(HeaderFooterZone.HEADER)
                .leftText(leftText)
                .centerText(centerText)
                .rightText(rightText)
                .showSeparator(true)
                .build());
        markPostProcessDirty();
        return this;
    }

    /**
     * Adds a repeating footer.
     *
     * @param config footer config
     * @return this harness
     */
    public EngineComposerHarness footer(HeaderFooterConfig config) {
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
        markPostProcessDirty();
        return this;
    }

    /**
     * Adds a simple repeating footer.
     *
     * @return this harness
     */
    public EngineComposerHarness footer(String leftText, String centerText, String rightText) {
        this.headerFooterConfigs.add(HeaderFooterConfig.builder()
                .zone(HeaderFooterZone.FOOTER)
                .leftText(leftText)
                .centerText(centerText)
                .rightText(rightText)
                .showSeparator(true)
                .build());
        markPostProcessDirty();
        return this;
    }

    /**
     * Adds a document-wide watermark.
     *
     * @param config watermark config
     * @return this harness
     */
    public EngineComposerHarness watermark(WatermarkConfig config) {
        this.watermarkConfig = config;
        markPostProcessDirty();
        return this;
    }

    /**
     * Adds a text watermark.
     *
     * @param text watermark text
     * @return this harness
     */
    public EngineComposerHarness watermark(String text) {
        this.watermarkConfig = WatermarkConfig.builder().text(text).build();
        markPostProcessDirty();
        return this;
    }

    /**
     * Adds document metadata.
     *
     * @param metadata metadata config
     * @return this harness
     */
    public EngineComposerHarness metadata(DocumentMetadata metadata) {
        this.documentMetadata = metadata;
        markPostProcessDirty();
        return this;
    }

    /**
     * Adds protection settings.
     *
     * @param config protection config
     * @return this harness
     */
    public EngineComposerHarness protect(PdfProtectionConfig config) {
        this.protectionConfig = config;
        markPostProcessDirty();
        return this;
    }

    /**
     * Returns a deterministic layout snapshot.
     *
     * @return layout snapshot
     * @throws Exception if layout fails
     */
    public LayoutSnapshot layoutSnapshot() throws Exception {
        buildComponents();
        ensureLayoutResolved();
        return LayoutSnapshotExtractor.extract(entityManager, canvas);
    }

    /**
     * Builds and saves the document when configured with an output path.
     *
     * @throws Exception if rendering or writing fails
     */
    public void build() throws Exception {
        buildComponents();
        ensureRendered();
        applyPostProcessing();
        if (fileManagerSystem != null) {
            fileManagerSystem.process(entityManager);
        }
    }

    /**
     * Builds and returns PDF bytes.
     *
     * @return rendered bytes
     * @throws Exception if rendering fails
     */
    public byte[] toBytes() throws Exception {
        buildComponents();
        ensureRendered();
        applyPostProcessing();
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            doc.save(baos);
            return baos.toByteArray();
        }
    }

    /**
     * Builds and returns the underlying PDFBox document for focused tests.
     *
     * @return rendered document owned by this harness
     * @throws Exception if rendering fails
     */
    public PDDocument toPDDocument() throws Exception {
        buildComponents();
        ensureRendered();
        applyPostProcessing();
        return doc;
    }

    @Override
    public void close() throws IOException {
        doc.close();
    }

    private void setupSystems() {
        entityManager.getSystems().addSystem(new FontLibraryTextMeasurementSystem(entityManager.getFonts(), PdfFont.class));
        entityManager.getSystems().addSystem(layoutSystem);
        entityManager.getSystems().addSystem(renderingSystem);
    }

    private void buildComponents() {
        componentBuilder.buildsComponents();
    }

    private void ensureLayoutResolved() throws Exception {
        long currentVersion = currentLayoutInputsVersion();
        if (layoutResolved && currentVersion == resolvedLayoutVersion) {
            return;
        }
        if (rendered) {
            throw new IllegalStateException("Harness was mutated after rendering. Create a new harness for a second render pass.");
        }
        layoutResolved = false;
        rendered = false;
        postProcessed = false;
        layoutSystem.process(entityManager);
        layoutResolved = true;
        resolvedLayoutVersion = currentLayoutInputsVersion();
    }

    private void ensureRendered() throws Exception {
        ensureLayoutResolved();
        long currentVersion = currentRenderInputsVersion();
        if (rendered && currentVersion == renderedVersion) {
            return;
        }
        if (postProcessed) {
            throw new IllegalStateException("Harness was mutated after post-processing. Create a new harness for a second export.");
        }
        renderingSystem.process(entityManager);
        rendered = true;
        renderedVersion = currentRenderInputsVersion();
    }

    private void applyPostProcessing() throws IOException {
        long currentVersion = currentPostProcessInputsVersion();
        if (postProcessed && currentVersion == postProcessedVersion) {
            return;
        }
        if (postProcessed) {
            throw new IllegalStateException("Harness post-processing has already been applied for a previous state.");
        }
        postProcessed = true;
        postProcessedVersion = currentVersion;

        if (watermarkConfig != null) {
            PdfWatermarkRenderer.apply(doc, watermarkConfig);
        }
        if (!headerFooterConfigs.isEmpty()) {
            Margin canvasMargin = canvas.margin();
            float marginLeft = canvasMargin != null ? (float) canvasMargin.left() : 24f;
            float marginRight = canvasMargin != null ? (float) canvasMargin.right() : 24f;
            PdfHeaderFooterRenderer.apply(doc, headerFooterConfigs, marginLeft, marginRight);
        }
        PdfBookmarkBuilder.buildOutline(doc, entityManager);
        if (documentMetadata != null) {
            PDDocumentInformation info = doc.getDocumentInformation();
            if (documentMetadata.getTitle() != null) {
                info.setTitle(documentMetadata.getTitle());
            }
            if (documentMetadata.getAuthor() != null) {
                info.setAuthor(documentMetadata.getAuthor());
            }
            if (documentMetadata.getSubject() != null) {
                info.setSubject(documentMetadata.getSubject());
            }
            if (documentMetadata.getKeywords() != null) {
                info.setKeywords(documentMetadata.getKeywords());
            }
            if (documentMetadata.getCreator() != null) {
                info.setCreator(documentMetadata.getCreator());
            }
            if (documentMetadata.getProducer() != null) {
                info.setProducer(documentMetadata.getProducer());
            }
        }
        if (protectionConfig != null) {
            applyProtection();
        }
    }

    private void applyProtection() throws IOException {
        AccessPermission permission = new AccessPermission();
        permission.setCanPrint(protectionConfig.isCanPrint());
        permission.setCanExtractContent(protectionConfig.isCanCopyContent());
        permission.setCanModify(protectionConfig.isCanModify());
        permission.setCanFillInForm(protectionConfig.isCanFillForms());
        permission.setCanExtractForAccessibility(protectionConfig.isCanExtractForAccessibility());
        permission.setCanAssembleDocument(protectionConfig.isCanAssemble());
        permission.setCanPrintFaithful(protectionConfig.isCanPrintHighQuality());

        StandardProtectionPolicy policy = new StandardProtectionPolicy(
                protectionConfig.getOwnerPassword(),
                protectionConfig.getUserPassword(),
                permission);
        policy.setEncryptionKeyLength(protectionConfig.getKeyLength());
        doc.protect(policy);
    }

    private long currentLayoutInputsVersion() {
        return entityManager.getMutationVersion() * 31L + layoutConfigVersion;
    }

    private long currentRenderInputsVersion() {
        return currentLayoutInputsVersion();
    }

    private long currentPostProcessInputsVersion() {
        return currentRenderInputsVersion() * 31L + postProcessConfigVersion;
    }

    private void markLayoutDirty() {
        layoutConfigVersion++;
        layoutResolved = false;
        rendered = false;
        postProcessed = false;
    }

    private void markPostProcessDirty() {
        postProcessConfigVersion++;
        postProcessed = false;
    }

    private static ComponentBuilder createComponentBuilder(EntityManager entityManager) {
        try {
            Constructor<ComponentBuilder> constructor = ComponentBuilder.class.getDeclaredConstructor(EntityManager.class);
            constructor.setAccessible(true);
            return constructor.newInstance(entityManager);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to create test component builder", e);
        }
    }

    /**
     * Builder for test harness instances.
     */
    public static final class Builder {
        private final Path outputFile;
        private PDRectangle pageSize = PDRectangle.A4;
        private Margin margin;
        private boolean markdown = true;
        private boolean guideLines;
        private final List<FontFamilyDefinition> customFontFamilies = new ArrayList<>();

        private Builder(Path outputFile) {
            this.outputFile = outputFile;
        }

        public Builder pageSize(PDRectangle pageSize) {
            this.pageSize = Objects.requireNonNull(pageSize, "pageSize");
            return this;
        }

        public Builder margin(Margin margin) {
            this.margin = margin;
            return this;
        }

        public Builder margin(float top, float right, float bottom, float left) {
            this.margin = new Margin(top, right, bottom, left);
            return this;
        }

        public Builder markdown(boolean enabled) {
            this.markdown = enabled;
            return this;
        }

        public Builder guideLines(boolean enabled) {
            this.guideLines = enabled;
            return this;
        }

        public Builder registerFontFamily(FontFamilyDefinition definition) {
            this.customFontFamilies.add(Objects.requireNonNull(definition, "definition"));
            return this;
        }

        public Builder registerFontFamily(FontName familyName, Path regular) {
            return registerFontFamily(FontFamilyDefinition.files(familyName, regular).build());
        }

        public Builder registerFontFamily(String familyName, Path regular) {
            return registerFontFamily(FontName.of(familyName), regular);
        }

        public Builder registerFontFamily(FontName familyName, Path regular, Path bold, Path italic) {
            return registerFontFamily(FontFamilyDefinition.files(familyName, regular)
                    .boldPath(bold)
                    .italicPath(italic)
                    .build());
        }

        public Builder registerFontFamily(String familyName, Path regular, Path bold, Path italic) {
            return registerFontFamily(FontName.of(familyName), regular, bold, italic);
        }

        public Builder registerFontFamily(FontName familyName, Path regular, Path bold, Path italic, Path boldItalic) {
            return registerFontFamily(FontFamilyDefinition.files(familyName, regular)
                    .boldPath(bold)
                    .italicPath(italic)
                    .boldItalicPath(boldItalic)
                    .build());
        }

        public Builder registerFontFamily(String familyName, Path regular, Path bold, Path italic, Path boldItalic) {
            return registerFontFamily(FontName.of(familyName), regular, bold, italic, boldItalic);
        }

        public EngineComposerHarness create() {
            return new EngineComposerHarness(
                    outputFile,
                    markdown,
                    guideLines,
                    pageSize,
                    margin,
                    List.copyOf(customFontFamilies));
        }
    }
}
