package com.demcha.compose;

import com.demcha.compose.font_library.FontFamilyDefinition;
import com.demcha.compose.font_library.FontName;
import com.demcha.compose.font_library.FontShowcase;
import com.demcha.compose.font_library.DefaultFonts;
import com.demcha.compose.layout_core.components.style.Margin;
import com.demcha.compose.layout_core.core.DocumentComposer;
import com.demcha.compose.layout_core.core.PdfComposer;
import org.apache.pdfbox.pdmodel.common.PDRectangle;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Main entry point for creating GraphCompose document composers.
 * <p>
 * This factory sits above the rest of the engine. Application code starts here,
 * obtains a composer, creates entities through {@code ComponentBuilder} or the
 * template layer, and then lets the layout and rendering systems process that
 * entity graph into a final document.
 * </p>
 *
 * <p>The typical runtime flow is:</p>
 * <ol>
 *   <li>create a composer with {@link #pdf(Path)} or {@link #pdf()}</li>
 *   <li>use {@code composer.componentBuilder()} to create entities</li>
 *   <li>optionally use {@code TemplateBuilder} on top of the builder layer</li>
 *   <li>call {@code build()} to write to disk or {@code toBytes()} to render in memory</li>
 * </ol>
 *
 * @author Artem Demchyshyn
 *
 * <h3>Build a PDF file</h3>
 * <pre>
 * try (DocumentComposer composer = GraphCompose.pdf(outputFile)
 *         .pageSize(PDRectangle.A4)
 *         .margin(new Margin(24, 24, 24, 24))
 *         .markdown(true)
 *         .create()) {
 *
 *     var cb = composer.componentBuilder();
 *     cb.text()
 *             .textWithAutoSize("Hello GraphCompose")
 *             .textStyle(TextStyle.DEFAULT_STYLE)
 *             .anchor(Anchor.topLeft())
 *             .build();
 *
 *     composer.build();
 * }
 * </pre>
 *
 * <h3>Get bytes instead of writing to disk</h3>
 * <pre>
 * try (DocumentComposer composer = GraphCompose.pdf()
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
 *
 * <h3>Optional template layer</h3>
 * <pre>
 * try (DocumentComposer composer = GraphCompose.pdf().create()) {
 *     var template = TemplateBuilder.from(
 *             composer.componentBuilder(),
 *             CvTheme.defaultTheme());
 *
 *     var profile = template.moduleBuilder("Profile", composer.canvas())
 *             .addChild(template.blockText(
 *                     "Analytical engineer focused on reliable platform design.",
 *                     composer.canvas().innerWidth()))
 *             .build();
 *
 *     template.pageFlow(composer.canvas())
 *             .addChild(profile)
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
     * Starts a PDF composition flow that writes the rendered document to a file.
     *
     * <p>The returned builder only captures configuration. No layout or rendering
     * work happens until {@link PdfBuilder#create()} creates the composer and the
     * caller later invokes {@code build()} or {@code toBytes()}.</p>
     *
     * @param outputFile the target file path for the generated PDF
     * @return a fluent builder for configuring the {@link PdfComposer}
     */
    public static PdfBuilder pdf(Path outputFile) {
        return new PdfBuilder(outputFile);
    }

    /**
     * Starts a PDF composition flow for in-memory generation.
     *
     * <p>Use this overload when the document should be consumed as bytes or as a
     * {@code PDDocument} instead of being written directly to disk.</p>
     *
     * @return a fluent builder for configuring the {@link PdfComposer}
     */
    public static PdfBuilder pdf() {
        return new PdfBuilder(null);
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
}
