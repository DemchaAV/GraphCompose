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
 * Factory class for creating document composers.
 * <p>
 * This is the main entry point for the GraphCompose engine.
 * Use the static factory methods to create document composers for supported
 * output formats.
 * </p>
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
 *     template.moduleBuilder("Profile", composer.canvas())
 *             .addChild(template.blockText(
 *                     "Analytical engineer focused on reliable platform design.",
 *                     composer.canvas().innerWidth()))
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
     * Creates a PDF composer builder that will save to the specified file.
     *
     * @param outputFile The path where the generated PDF will be saved.
     * @return A builder to configure the PDF composer.
     */
    public static PdfBuilder pdf(Path outputFile) {
        return new PdfBuilder(outputFile);
    }

    /**
     * Creates a PDF composer builder for in-memory generation (use
     * {@code toBytes()}).
     *
     * @return A builder to configure the PDF composer.
     */
    public static PdfBuilder pdf() {
        return new PdfBuilder(null);
    }

    /**
     * Returns bundled font families available out of the box.
     */
    public static List<FontName> availableFonts() {
        return DefaultFonts.bundledFontNames();
    }

    /**
     * Generates a PDF preview that shows all bundled font families.
     */
    public static void renderAvailableFontsPreview(Path outputFile) throws Exception {
        FontShowcase.renderAvailableFontsPreview(outputFile);
    }

    /**
     * Generates a PDF preview as bytes that shows all bundled font families.
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
     * Fluent builder for configuring and creating a {@link PdfComposer}.
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
         * Sets the page size for the PDF document.
         *
         * @param pageSize The page size (e.g., {@link PDRectangle#A4},
         *                 {@link PDRectangle#LETTER}).
         * @return This builder for method chaining.
         */
        public PdfBuilder pageSize(PDRectangle pageSize) {
            this.pageSize = Objects.requireNonNull(pageSize, "pageSize");
            return this;
        }

        /**
         * Sets the margin for the PDF document.
         *
         * @param margin The margin configuration.
         * @return This builder for method chaining.
         */
        public PdfBuilder margin(Margin margin) {
            this.margin = margin;
            return this;
        }

        /**
         * Convenience method for setting margin with individual values.
         *
         * @param top    Top margin in points.
         * @param right  Right margin in points.
         * @param bottom Bottom margin in points.
         * @param left   Left margin in points.
         * @return This builder for method chaining.
         */
        public PdfBuilder margin(float top, float right, float bottom, float left) {
            this.margin = new Margin(top, right, bottom, left);
            return this;
        }

        /**
         * Enables or disables Markdown parsing for text components.
         *
         * @param enabled true to enable markdown (default), false to disable.
         * @return This builder for method chaining.
         */
        public PdfBuilder markdown(boolean enabled) {
            this.markdown = enabled;
            return this;
        }

        /**
         * Enables or disables visual guide lines for debugging layout.
         *
         * @param enabled true to render guide lines, false to hide them (default).
         * @return This builder for method chaining.
         */
        public PdfBuilder guideLines(boolean enabled) {
            this.guideLines = enabled;
            return this;
        }

        /**
         * Registers a custom font family so it is available to the current PDF
         * document and future backends reusing the shared font catalog.
         */
        public PdfBuilder registerFontFamily(FontFamilyDefinition definition) {
            this.customFontFamilies.add(Objects.requireNonNull(definition, "definition"));
            return this;
        }

        /**
         * Registers a custom font family from local TTF/OTF files.
         */
        public PdfBuilder registerFontFamily(FontName familyName, Path regular) {
            return registerFontFamily(FontFamilyDefinition.files(familyName, regular).build());
        }

        /**
         * Registers a custom font family from local TTF/OTF files.
         */
        public PdfBuilder registerFontFamily(String familyName, Path regular) {
            return registerFontFamily(FontName.of(familyName), regular);
        }

        /**
         * Registers a custom font family from local TTF/OTF files.
         */
        public PdfBuilder registerFontFamily(FontName familyName, Path regular, Path bold, Path italic) {
            return registerFontFamily(FontFamilyDefinition.files(familyName, regular)
                    .boldPath(bold)
                    .italicPath(italic)
                    .build());
        }

        /**
         * Registers a custom font family from local TTF/OTF files.
         */
        public PdfBuilder registerFontFamily(String familyName, Path regular, Path bold, Path italic) {
            return registerFontFamily(FontName.of(familyName), regular, bold, italic);
        }

        /**
         * Registers a custom font family from local TTF/OTF files.
         */
        public PdfBuilder registerFontFamily(FontName familyName, Path regular, Path bold, Path italic, Path boldItalic) {
            return registerFontFamily(FontFamilyDefinition.files(familyName, regular)
                    .boldPath(bold)
                    .italicPath(italic)
                    .boldItalicPath(boldItalic)
                    .build());
        }

        /**
         * Registers a custom font family from local TTF/OTF files.
         */
        public PdfBuilder registerFontFamily(String familyName, Path regular, Path bold, Path italic, Path boldItalic) {
            return registerFontFamily(FontName.of(familyName), regular, bold, italic, boldItalic);
        }

        /**
         * Creates the configured PDF composer.
         *
         * @return A new {@link PdfComposer} instance ready for use.
         */
        public PdfComposer create() {
            return new PdfComposer(outputFile, markdown, guideLines, pageSize, margin, List.copyOf(customFontFamilies));
        }
    }
}
