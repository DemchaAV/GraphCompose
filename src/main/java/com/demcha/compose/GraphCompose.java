package com.demcha.compose;

import com.demcha.compose.font_library.FontFamilyDefinition;
import com.demcha.compose.font_library.FontName;
import com.demcha.compose.font_library.FontShowcase;
import com.demcha.compose.font_library.DefaultFonts;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.backend.fixed.pdf.options.PdfHeaderFooterOptions;
import com.demcha.compose.document.backend.fixed.pdf.options.PdfHeaderFooterZone;
import com.demcha.compose.document.backend.fixed.pdf.options.PdfMetadataOptions;
import com.demcha.compose.document.backend.fixed.pdf.options.PdfProtectionOptions;
import com.demcha.compose.document.backend.fixed.pdf.options.PdfWatermarkOptions;
import com.demcha.compose.layout_core.components.style.Margin;
import com.demcha.compose.layout_core.core.DocumentComposer;
import com.demcha.compose.layout_core.core.PdfComposer;
import org.apache.pdfbox.pdmodel.common.PDRectangle;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Main entry point for GraphCompose document authoring.
 * <p>
 * Application code starts here, chooses either the canonical semantic document
 * session through {@link #document()} or the deprecated legacy PDF composer
 * through {@link #pdf()}, and then lets GraphCompose turn author intent into a
 * paginated document.
 * </p>
 *
 * <p>The typical runtime flow is:</p>
 * <ol>
 *   <li>create a semantic session with {@link #document(Path)} or {@link #document()}</li>
 *   <li>author content with {@link DocumentSession#dsl()}</li>
 *   <li>inspect {@link DocumentSession#layoutGraph()} or {@link DocumentSession#layoutSnapshot()} if needed</li>
 *   <li>call {@link DocumentSession#buildPdf()} or {@link DocumentSession#toPdfBytes()}</li>
 * </ol>
 *
 * @author Artem Demchyshyn
 *
 * <h3>Build a PDF file with the canonical DSL</h3>
 * <pre>
 * try (DocumentSession document = GraphCompose.document(outputFile)
 *         .pageSize(PDRectangle.A4)
 *         .margin(new Margin(24, 24, 24, 24))
 *         .create()) {
 *
 *     document.dsl()
 *             .pageFlow()
 *             .spacing(8)
 *             .addParagraph(p -&gt; p
 *                     .name("Greeting")
 *                     .text("Hello GraphCompose")
 *                     .textStyle(TextStyle.DEFAULT_STYLE))
 *             .build();
 *
 *     document.buildPdf();
 * }
 * </pre>
 *
 * <h3>Get bytes instead of writing to disk</h3>
 * <pre>
 * try (DocumentSession document = GraphCompose.document()
 *         .pageSize(PDRectangle.A4)
 *         .create()) {
 *
 *     document.dsl()
 *             .pageFlow()
 *             .addParagraph(p -&gt; p.text("In-memory PDF"))
 *             .build();
 *
 *     byte[] pdfBytes = document.toPdfBytes();
 * }
 * </pre>
 *
 * <h3>Low-level PDF composer</h3>
 * <pre>
 * try (DocumentComposer composer = GraphCompose.pdf().create()) {
 *     var cb = composer.componentBuilder();
 *     var summary = cb.blockText(Align.left(4), TextStyle.DEFAULT_STYLE)
 *             .size(composer.canvas().innerWidth(), 2)
 *             .text(cb.text()
 *                     .textWithAutoSize("Analytical engineer focused on reliable platform design.")
 *                     .textStyle(TextStyle.DEFAULT_STYLE))
 *             .anchor(Anchor.topLeft())
 *             .build();
 *
 *     cb.vContainer(Align.middle(8))
 *             .size(composer.canvas().innerWidth(), 0)
 *             .anchor(Anchor.topLeft())
 *             .addChild(summary)
 *             .build();
 *
 *     byte[] pdfBytes = composer.toBytes();
 * }
 * </pre>
 */
public final class GraphCompose {

    private GraphCompose() {
        // Factory class, no instantiation
    }

    // ===== PDF Factory =====

    /**
     * Starts a legacy PDF composition flow that writes the rendered document to a file.
     *
     * <p>The returned builder only captures configuration. No layout or rendering
     * work happens until {@link PdfBuilder#create()} creates the composer and the
     * caller later invokes {@code build()} or {@code toBytes()}.</p>
     *
     * @param outputFile the target file path for the generated PDF
     * @return a fluent builder for configuring the {@link PdfComposer}
     */
    @Deprecated(forRemoval = false)
    public static PdfBuilder pdf(Path outputFile) {
        return new PdfBuilder(outputFile);
    }

    /**
     * Starts a legacy PDF composition flow for in-memory generation.
     *
     * <p>Use this overload when the document should be consumed as bytes or as a
     * {@code PDDocument} instead of being written directly to disk.</p>
     *
     * @return a fluent builder for configuring the {@link PdfComposer}
     */
    @Deprecated(forRemoval = false)
    public static PdfBuilder pdf() {
        return new PdfBuilder(null);
    }

    /**
     * Starts the canonical semantic document composition flow.
     *
     * @return builder for creating an in-memory semantic document session
     */
    public static DocumentBuilder document() {
        return new DocumentBuilder(null);
    }

    /**
     * Starts the canonical semantic document composition flow with a default output target
     * used by {@link DocumentSession#buildPdf()}.
     *
     * @param outputFile default PDF output path for {@link DocumentSession#buildPdf()}
     * @return builder for creating a semantic document session
     */
    public static DocumentBuilder document(Path outputFile) {
        return new DocumentBuilder(outputFile);
    }

    /**
     * Returns the logical font families bundled with GraphCompose out of the box.
     *
     * <p>The returned names are the identifiers used by {@code TextStyle},
     * {@code CvTheme}, and the font library. They describe what can be referenced
     * immediately without registering custom font families.</p>
     */
    public static List<FontName> availableFonts() {
        return DefaultFonts.bundledFontNames();
    }

    /**
     * Renders a PDF showcase of all bundled font families to disk.
     *
     * <p>This is primarily a discovery helper for developers choosing a family for
     * {@code TextStyle}, templates, or themes.</p>
     */
    public static void renderAvailableFontsPreview(Path outputFile) throws Exception {
        FontShowcase.renderAvailableFontsPreview(outputFile);
    }

    /**
     * Renders a PDF showcase of all bundled font families in memory.
     *
     * @return the generated preview document as PDF bytes
     */
    public static byte[] renderAvailableFontsPreview() throws Exception {
        return FontShowcase.renderAvailableFontsPreview();
    }

    // ===== Word Factory (placeholder for future) =====

    // public static WordBuilder word(Path outputFile) {
    // return new WordBuilder(outputFile);
    // }

    // ===== PDF Builder =====

    /**
     * Fluent configuration builder for {@link PdfComposer}.
     *
     * <p>This builder captures document-wide settings such as page size, canvas
     * margin, markdown handling, guideline rendering, and per-document custom
     * font registrations. It does not create any entities by itself; entity
     * creation starts after {@link #create()} returns a composer.</p>
     */
    public static final class PdfBuilder {
        private final Path outputFile;
        private PDRectangle pageSize = PDRectangle.A4;
        private Margin margin = null;
        private boolean markdown = true;
        private boolean guideLines = false;
        private final List<FontFamilyDefinition> customFontFamilies = new ArrayList<>();

        private PdfBuilder(Path outputFile) {
            this.outputFile = outputFile;
        }

        /**
         * Sets the physical page size used by the PDF canvas.
         *
         * <p>This defines the outer drawing area before canvas margins are applied.</p>
         *
         * @param pageSize the target page size, for example {@link PDRectangle#A4}
         *                 or {@link PDRectangle#LETTER}
         * @return this builder
         */
        public PdfBuilder pageSize(PDRectangle pageSize) {
            this.pageSize = Objects.requireNonNull(pageSize, "pageSize");
            return this;
        }

        /**
         * Sets the canvas margin used as the initial inner drawing area.
         *
         * <p>Root entities are positioned relative to the page after this margin
         * is subtracted from the page rectangle.</p>
         *
         * @param margin the document-wide canvas margin
         * @return this builder
         */
        public PdfBuilder margin(Margin margin) {
            this.margin = margin;
            return this;
        }

        /**
         * Convenience overload for setting the canvas margin in points.
         *
         * @param top top margin
         * @param right right margin
         * @param bottom bottom margin
         * @param left left margin
         * @return this builder
         */
        public PdfBuilder margin(float top, float right, float bottom, float left) {
            this.margin = new Margin(top, right, bottom, left);
            return this;
        }

        /**
         * Enables or disables markdown parsing for text-related builders in the
         * created composer.
         *
         * <p>When enabled, builders such as {@code BlockTextBuilder} may tokenize
         * and style supported markdown content instead of treating input as plain text.</p>
         *
         * @param enabled {@code true} to enable markdown parsing
         * @return this builder
         */
        public PdfBuilder markdown(boolean enabled) {
            this.markdown = enabled;
            return this;
        }

        /**
         * Enables or disables visual guide-line rendering for layout debugging.
         *
         * <p>Guide lines help inspect resolved placement and box geometry without
         * changing the actual layout calculations.</p>
         *
         * @param enabled {@code true} to render guide lines
         * @return this builder
         */
        public PdfBuilder guideLines(boolean enabled) {
            this.guideLines = enabled;
            return this;
        }

        /**
         * Registers a custom font family for the composer created by this builder.
         *
         * <p>The registration becomes part of the document-specific font catalog and
         * can then be referenced through {@code FontName} values in styles and themes.</p>
         */
        public PdfBuilder registerFontFamily(FontFamilyDefinition definition) {
            this.customFontFamilies.add(Objects.requireNonNull(definition, "definition"));
            return this;
        }

        /**
         * Registers a custom family backed by a single regular font file.
         */
        public PdfBuilder registerFontFamily(FontName familyName, Path regular) {
            return registerFontFamily(FontFamilyDefinition.files(familyName, regular).build());
        }

        /**
         * Registers a custom family backed by a single regular font file.
         */
        public PdfBuilder registerFontFamily(String familyName, Path regular) {
            return registerFontFamily(FontName.of(familyName), regular);
        }

        /**
         * Registers a custom family backed by regular, bold, and italic files.
         */
        public PdfBuilder registerFontFamily(FontName familyName, Path regular, Path bold, Path italic) {
            return registerFontFamily(FontFamilyDefinition.files(familyName, regular)
                    .boldPath(bold)
                    .italicPath(italic)
                    .build());
        }

        /**
         * Registers a custom family backed by regular, bold, and italic files.
         */
        public PdfBuilder registerFontFamily(String familyName, Path regular, Path bold, Path italic) {
            return registerFontFamily(FontName.of(familyName), regular, bold, italic);
        }

        /**
         * Registers a custom family backed by regular, bold, italic, and bold-italic files.
         */
        public PdfBuilder registerFontFamily(FontName familyName, Path regular, Path bold, Path italic, Path boldItalic) {
            return registerFontFamily(FontFamilyDefinition.files(familyName, regular)
                    .boldPath(bold)
                    .italicPath(italic)
                    .boldItalicPath(boldItalic)
                    .build());
        }

        /**
         * Registers a custom family backed by regular, bold, italic, and bold-italic files.
         */
        public PdfBuilder registerFontFamily(String familyName, Path regular, Path bold, Path italic, Path boldItalic) {
            return registerFontFamily(FontName.of(familyName), regular, bold, italic, boldItalic);
        }

        /**
         * Creates a fully configured {@link PdfComposer}.
         *
         * <p>After this point, application code can start building entities. The
         * composer remains empty until builders register entities into its
         * {@code EntityManager}.</p>
         *
         * @return a new {@link PdfComposer} ready for document composition
         */
        public PdfComposer create() {
            return new PdfComposer(outputFile, markdown, guideLines, pageSize, margin, List.copyOf(customFontFamilies));
        }
    }

    /**
     * Fluent configuration builder for the canonical semantic document session.
     *
     * <p>Unlike the legacy {@link PdfBuilder}, this builder produces a
     * {@link DocumentSession} that exposes the semantic DSL through
     * {@link DocumentSession#dsl()} and keeps authoring separate from the PDF
     * backend.</p>
     */
    public static final class DocumentBuilder {
        private final Path outputFile;
        private PDRectangle pageSize = PDRectangle.A4;
        private Margin margin = Margin.zero();
        private boolean guideLines = false;
        private PdfMetadataOptions metadataOptions;
        private PdfWatermarkOptions watermarkOptions;
        private PdfProtectionOptions protectionOptions;
        private final List<PdfHeaderFooterOptions> headerFooterOptions = new ArrayList<>();
        private final List<FontFamilyDefinition> customFontFamilies = new ArrayList<>();

        private DocumentBuilder(Path outputFile) {
            this.outputFile = outputFile;
        }

        /**
         * Sets the page size used by the semantic canvas.
         *
         * @param pageSize physical page rectangle
         * @return this builder
         */
        public DocumentBuilder pageSize(PDRectangle pageSize) {
            this.pageSize = Objects.requireNonNull(pageSize, "pageSize");
            return this;
        }

        /**
         * Sets the semantic document margin.
         *
         * @param margin outer page margin
         * @return this builder
         */
        public DocumentBuilder margin(Margin margin) {
            this.margin = margin == null ? Margin.zero() : margin;
            return this;
        }

        /**
         * Convenience overload for setting document margin in points.
         *
         * @param top top margin
         * @param right right margin
         * @param bottom bottom margin
         * @param left left margin
         * @return this builder
         */
        public DocumentBuilder margin(float top, float right, float bottom, float left) {
            this.margin = new Margin(top, right, bottom, left);
            return this;
        }

        /**
         * Enables or disables PDF guide-line overlays for debugging rendered
         * semantic fragment geometry.
         *
         * @param enabled {@code true} to draw guide lines in rendered PDFs
         * @return this builder
         */
        public DocumentBuilder guideLines(boolean enabled) {
            this.guideLines = enabled;
            return this;
        }

        /**
         * Configures document metadata for PDFs rendered from the created
         * semantic session.
         *
         * @param options canonical metadata options, or {@code null} to clear
         * @return this builder
         */
        public DocumentBuilder metadata(PdfMetadataOptions options) {
            this.metadataOptions = options;
            return this;
        }

        /**
         * Configures a document-wide watermark for PDFs rendered from the
         * created semantic session.
         *
         * @param options canonical watermark options, or {@code null} to clear
         * @return this builder
         */
        public DocumentBuilder watermark(PdfWatermarkOptions options) {
            this.watermarkOptions = options;
            return this;
        }

        /**
         * Configures PDF protection and permissions for the created semantic
         * session.
         *
         * @param options canonical protection options, or {@code null} to clear
         * @return this builder
         */
        public DocumentBuilder protect(PdfProtectionOptions options) {
            this.protectionOptions = options;
            return this;
        }

        /**
         * Registers a repeating page header.
         *
         * @param options canonical header options
         * @return this builder
         */
        public DocumentBuilder header(PdfHeaderFooterOptions options) {
            this.headerFooterOptions.add(Objects.requireNonNull(options, "options")
                    .withZone(PdfHeaderFooterZone.HEADER));
            return this;
        }

        /**
         * Registers a repeating page footer.
         *
         * @param options canonical footer options
         * @return this builder
         */
        public DocumentBuilder footer(PdfHeaderFooterOptions options) {
            this.headerFooterOptions.add(Objects.requireNonNull(options, "options")
                    .withZone(PdfHeaderFooterZone.FOOTER));
            return this;
        }

        /**
         * Registers a document-local font family available to text measurement and
         * PDF rendering.
         *
         * @param definition font family definition
         * @return this builder
         */
        public DocumentBuilder registerFontFamily(FontFamilyDefinition definition) {
            this.customFontFamilies.add(Objects.requireNonNull(definition, "definition"));
            return this;
        }

        /**
         * Registers a custom family backed by a single regular font file.
         *
         * @param familyName logical family identifier
         * @param regular path to the regular font file
         * @return this builder
         */
        public DocumentBuilder registerFontFamily(FontName familyName, Path regular) {
            return registerFontFamily(FontFamilyDefinition.files(familyName, regular).build());
        }

        /**
         * Registers a custom family backed by a single regular font file.
         *
         * @param familyName logical family identifier
         * @param regular path to the regular font file
         * @return this builder
         */
        public DocumentBuilder registerFontFamily(String familyName, Path regular) {
            return registerFontFamily(FontName.of(familyName), regular);
        }

        /**
         * Registers a custom family backed by regular, bold, and italic files.
         *
         * @param familyName logical family identifier
         * @param regular path to the regular font file
         * @param bold path to the bold font file
         * @param italic path to the italic font file
         * @return this builder
         */
        public DocumentBuilder registerFontFamily(FontName familyName, Path regular, Path bold, Path italic) {
            return registerFontFamily(FontFamilyDefinition.files(familyName, regular)
                    .boldPath(bold)
                    .italicPath(italic)
                    .build());
        }

        /**
         * Registers a custom family backed by regular, bold, and italic files.
         *
         * @param familyName logical family identifier
         * @param regular path to the regular font file
         * @param bold path to the bold font file
         * @param italic path to the italic font file
         * @return this builder
         */
        public DocumentBuilder registerFontFamily(String familyName, Path regular, Path bold, Path italic) {
            return registerFontFamily(FontName.of(familyName), regular, bold, italic);
        }

        /**
         * Registers a custom family backed by regular, bold, italic, and bold-italic files.
         *
         * @param familyName logical family identifier
         * @param regular path to the regular font file
         * @param bold path to the bold font file
         * @param italic path to the italic font file
         * @param boldItalic path to the bold-italic font file
         * @return this builder
         */
        public DocumentBuilder registerFontFamily(FontName familyName, Path regular, Path bold, Path italic, Path boldItalic) {
            return registerFontFamily(FontFamilyDefinition.files(familyName, regular)
                    .boldPath(bold)
                    .italicPath(italic)
                    .boldItalicPath(boldItalic)
                    .build());
        }

        /**
         * Registers a custom family backed by regular, bold, italic, and bold-italic files.
         *
         * @param familyName logical family identifier
         * @param regular path to the regular font file
         * @param bold path to the bold font file
         * @param italic path to the italic font file
         * @param boldItalic path to the bold-italic font file
         * @return this builder
         */
        public DocumentBuilder registerFontFamily(String familyName, Path regular, Path bold, Path italic, Path boldItalic) {
            return registerFontFamily(FontName.of(familyName), regular, bold, italic, boldItalic);
        }

        /**
         * Creates a mutable semantic document session.
         *
         * <pre>{@code
         * try (var document = GraphCompose.document(Path.of("output.pdf"))
         *         .pageSize(PDRectangle.A4)
         *         .margin(24, 24, 24, 24)
         *         .create()) {
         *     document.dsl()
         *             .pageFlow()
         *             .spacing(8)
         *             .addParagraph(p -> p.text("Hello GraphCompose"))
         *             .build();
         *
         *     document.buildPdf();
         * }
         * }</pre>
         *
         * @return a new semantic document session
         */
        public DocumentSession create() {
            return new DocumentSession(
                    outputFile,
                    pageSize,
                    margin,
                    List.copyOf(customFontFamilies),
                    guideLines,
                    metadataOptions,
                    watermarkOptions,
                    protectionOptions,
                    List.copyOf(headerFooterOptions));
        }
    }
}
