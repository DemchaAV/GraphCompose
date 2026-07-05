package com.demcha.compose.document.backend.fixed.pdf;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.ModuleBuilder;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.font.DefaultFonts;
import com.demcha.compose.font.FontName;

import java.awt.Color;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Objects;

/**
 * Generates a preview PDF for bundled or custom font families so callers can
 * visually inspect the catalog.
 *
 * @author Artem Demchyshyn
 */
public final class FontShowcase {

    private static final String SAMPLE_TEXT = "The quick brown fox jumps over the lazy dog 0123456789";

    private FontShowcase() {
    }

    /**
     * Renders a preview of every bundled font family to a PDF file on disk.
     *
     * @param outputFile destination path for the generated preview PDF
     * @throws Exception if the document cannot be built or written
     */
    public static void renderAvailableFontsPreview(Path outputFile) throws Exception {
        try (DocumentSession document = GraphCompose.document(outputFile)
                .pageSize(DocumentPageSize.A4)
                .margin(28, 28, 28, 28)
                .markdown(false)
                .create()) {

            buildShowcase(document, DefaultFonts.bundledFontNames());
            document.buildPdf();
        }
    }

    /**
     * Renders a preview of every bundled font family and returns the PDF bytes.
     *
     * @return the generated preview document as PDF bytes
     * @throws Exception if the document cannot be built or rendered
     */
    public static byte[] renderAvailableFontsPreview() throws Exception {
        try (DocumentSession document = GraphCompose.document()
                .pageSize(DocumentPageSize.A4)
                .margin(28, 28, 28, 28)
                .markdown(false)
                .create()) {

            buildShowcase(document, DefaultFonts.bundledFontNames());
            return document.toPdfBytes();
        }
    }

    /**
     * Renders a preview of the given font families and returns the PDF bytes.
     *
     * @param fonts logical font names to include; when {@code null} the full
     *              bundled catalog is used
     * @return the generated preview document as PDF bytes
     * @throws Exception if the document cannot be built or rendered
     */
    public static byte[] renderFontsPreview(Collection<FontName> fonts) throws Exception {
        try (DocumentSession document = GraphCompose.document()
                .pageSize(DocumentPageSize.A4)
                .margin(28, 28, 28, 28)
                .markdown(false)
                .create()) {

            buildShowcase(document, fonts);
            return document.toPdfBytes();
        }
    }

    /**
     * Builds the font-preview content into an existing document session.
     *
     * @param document open document session to populate; must not be {@code null}
     * @param fonts    logical font names to showcase; when {@code null} the full
     *                 bundled catalog is used
     */
    public static void buildShowcase(DocumentSession document, Collection<FontName> fonts) {
        Objects.requireNonNull(document, "document").pageFlow(flow -> {
            flow.name("AvailableFontsPreview")
                    .spacing(16)
                    .addParagraph(paragraph -> paragraph
                            .name("AvailableFontsPreviewTitle")
                            .text("Available Fonts Preview")
                            .textStyle(style(FontName.HELVETICA, 22, DocumentTextDecoration.BOLD, new Color(34, 34, 34)))
                            .margin(DocumentInsets.bottom(6)))
                    .addParagraph(paragraph -> paragraph
                            .name("AvailableFontsPreviewSubtitle")
                            .text("Each section shows the family name and sample lines in regular, bold, italic and bold-italic styles.")
                            .textStyle(style(FontName.HELVETICA, 10, DocumentTextDecoration.DEFAULT, new Color(90, 90, 90)))
                            .margin(DocumentInsets.bottom(12)));

            Collection<FontName> safeFonts = fonts == null ? DefaultFonts.bundledFontNames() : fonts;
            for (FontName fontName : safeFonts) {
                addFontSection(flow, fontName);
            }
        });
    }

    private static void addFontSection(PageFlowBuilder flow, FontName fontName) {
        flow.module(fontName.name(), module -> {
            module.name("FontSection_" + fontName.name())
                    .spacing(2)
                    .margin(DocumentInsets.bottom(8))
                    .titleStyle(style(FontName.HELVETICA, 12, DocumentTextDecoration.BOLD, new Color(25, 25, 25)))
                    .titleMargin(DocumentInsets.bottom(2));
            sampleLine(module, "Regular: ", fontName, DocumentTextDecoration.DEFAULT, DocumentInsets.bottom(1));
            sampleLine(module, "Bold: ", fontName, DocumentTextDecoration.BOLD, DocumentInsets.bottom(1));
            sampleLine(module, "Italic: ", fontName, DocumentTextDecoration.ITALIC, DocumentInsets.bottom(1));
            sampleLine(module, "Bold Italic: ", fontName, DocumentTextDecoration.BOLD_ITALIC, DocumentInsets.bottom(4));
        });
    }

    private static void sampleLine(ModuleBuilder module,
                                   String label,
                                   FontName fontName,
                                   DocumentTextDecoration decoration,
                                   DocumentInsets margin) {
        module.paragraph(paragraph -> paragraph
                .text(label + SAMPLE_TEXT)
                .textStyle(style(fontName, 11, decoration, Color.BLACK))
                .margin(margin));
    }

    private static DocumentTextStyle style(FontName fontName, double size, DocumentTextDecoration decoration, Color color) {
        return new DocumentTextStyle(fontName, size, decoration, DocumentColor.of(color));
    }
}
