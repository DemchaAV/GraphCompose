package com.demcha.compose;

import com.demcha.compose.font.FontFamilyDefinition;
import com.demcha.compose.font.FontName;
import com.demcha.compose.font.FontShowcase;
import com.demcha.compose.font.DefaultFonts;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.backend.fixed.pdf.options.PdfHeaderFooterOptions;
import com.demcha.compose.document.backend.fixed.pdf.options.PdfHeaderFooterZone;
import com.demcha.compose.document.backend.fixed.pdf.options.PdfMetadataOptions;
import com.demcha.compose.document.backend.fixed.pdf.options.PdfProtectionOptions;
import com.demcha.compose.document.backend.fixed.pdf.options.PdfWatermarkOptions;
import com.demcha.compose.document.style.DocumentInsets;
import org.apache.pdfbox.pdmodel.common.PDRectangle;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Main entry point for GraphCompose document authoring.
 * <p>
 * Application code starts here and should use the canonical semantic document
 * session through {@link #document()} to turn author intent into a paginated
 * document.
 * </p>
 *
 * <p>The typical runtime flow is:</p>
 * <ol>
 *   <li>create a semantic session with {@link #document(Path)} or {@link #document()}</li>
 *   <li>author content with {@link DocumentSession#pageFlow()}, {@link DocumentSession#compose(java.util.function.Consumer)},
 *       or {@link DocumentSession#dsl()}</li>
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
 *         .margin(24, 24, 24, 24)
 *         .create()) {
 *
 *     document.pageFlow(page -> page
 *             .module("Summary", module -> module.paragraph("Hello GraphCompose")));
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
 *     document.pageFlow(page -> page
 *             .module("Summary", module -> module.paragraph("In-memory PDF")));
 *
 *     byte[] pdfBytes = document.toPdfBytes();
 * }
 * </pre>
 */
public final class GraphCompose {

    private GraphCompose() {
        // Factory class, no instantiation
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
     * <p>The returned names are the identifiers used by {@code DocumentTextStyle},
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

    /**
     * Fluent configuration builder for the canonical semantic document session.
     *
     * <p>This builder produces a {@link DocumentSession} that exposes the semantic DSL through
     * {@link DocumentSession#dsl()} and keeps authoring separate from the PDF
     * backend.</p>
     */
    public static final class DocumentBuilder {
        private final Path outputFile;
        private PDRectangle pageSize = PDRectangle.A4;
        private DocumentInsets margin = DocumentInsets.zero();
        private boolean markdown = true;
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
         * Sets the semantic document margin with the public canonical spacing value.
         *
         * @param margin outer page margin
         * @return this builder
         */
        public DocumentBuilder margin(DocumentInsets margin) {
            this.margin = margin == null ? DocumentInsets.zero() : margin;
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
            this.margin = new DocumentInsets(top, right, bottom, left);
            return this;
        }

        /**
         * Enables or disables markdown parsing for semantic paragraph blocks.
         *
         * <p>When enabled, canonical paragraph rendering understands the same
         * practical inline markdown subset used by the built-in template
         * flow, such as bold and italic spans.</p>
         *
         * @param enabled {@code true} to enable markdown-aware paragraph rendering
         * @return this builder
         */
        public DocumentBuilder markdown(boolean enabled) {
            this.markdown = enabled;
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
         *     document.pageFlow(page -> page
         *             .module("Summary", module -> module.paragraph("Hello GraphCompose")));
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
                    markdown,
                    guideLines,
                    metadataOptions,
                    watermarkOptions,
                    protectionOptions,
                    List.copyOf(headerFooterOptions));
        }
    }
}
