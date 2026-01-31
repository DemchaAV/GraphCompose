package com.demcha.compose;

import com.demcha.compose.loyaut_core.components.style.Margin;
import com.demcha.compose.loyaut_core.core.PdfComposer;
import org.apache.pdfbox.pdmodel.common.PDRectangle;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Factory class for creating document composers.
 * <p>
 * This is the main entry point for the GraphCompose framework.
 * Use the static factory methods to create composers for different document
 * formats.
 * </p>
 *
 * <h3>PDF Example:</h3>
 * 
 * <pre>
 * try (var composer = GraphCompose.pdf(Paths.get("output.pdf"))
 *         .pageSize(PDRectangle.A4)
 *         .margin(new Margin(15, 10, 15, 15))
 *         .markdown(true)
 *         .create()) {
 *
 *     var builder = composer.componentBuilder();
 *     // ... build your document
 *     composer.build();
 * }
 * </pre>
 *
 * <h3>Get bytes instead of file:</h3>
 * 
 * <pre>
 * try (var composer = GraphCompose.pdf()
 *         .pageSize(PDRectangle.A4)
 *         .create()) {
 *
 *     // ... build your document
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
         * Creates the configured PDF composer.
         *
         * @return A new {@link PdfComposer} instance ready for use.
         */
        public PdfComposer create() {
            return new PdfComposer(outputFile, markdown, guideLines, pageSize, margin);
        }
    }
}
