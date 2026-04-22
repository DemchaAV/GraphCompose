package com.demcha.compose.font_library;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.DocumentDsl;
import com.demcha.compose.layout_core.components.content.text.TextDecoration;
import com.demcha.compose.layout_core.components.content.text.TextStyle;
import com.demcha.compose.layout_core.components.style.Margin;
import org.apache.pdfbox.pdmodel.common.PDRectangle;

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

    public static void renderAvailableFontsPreview(Path outputFile) throws Exception {
        try (DocumentSession document = GraphCompose.document(outputFile)
                .pageSize(PDRectangle.A4)
                .margin(28, 28, 28, 28)
                .markdown(false)
                .create()) {

            buildShowcase(document, DefaultFonts.bundledFontNames());
            document.buildPdf();
        }
    }

    public static byte[] renderAvailableFontsPreview() throws Exception {
        try (DocumentSession document = GraphCompose.document()
                .pageSize(PDRectangle.A4)
                .margin(28, 28, 28, 28)
                .markdown(false)
                .create()) {

            buildShowcase(document, DefaultFonts.bundledFontNames());
            return document.toPdfBytes();
        }
    }

    public static byte[] renderFontsPreview(Collection<FontName> fonts) throws Exception {
        try (DocumentSession document = GraphCompose.document()
                .pageSize(PDRectangle.A4)
                .margin(28, 28, 28, 28)
                .markdown(false)
                .create()) {

            buildShowcase(document, fonts);
            return document.toPdfBytes();
        }
    }

    public static void buildShowcase(DocumentSession document, Collection<FontName> fonts) {
        Objects.requireNonNull(document, "document").pageFlow(flow -> {
            flow.name("AvailableFontsPreview")
                    .spacing(16)
                    .addParagraph(paragraph -> paragraph
                            .name("AvailableFontsPreviewTitle")
                            .text("Available Fonts Preview")
                            .textStyle(style(FontName.HELVETICA, 22, TextDecoration.BOLD, new Color(34, 34, 34)))
                            .margin(Margin.bottom(6)))
                    .addParagraph(paragraph -> paragraph
                            .name("AvailableFontsPreviewSubtitle")
                            .text("Each section shows the family name and sample lines in regular, bold, italic and bold-italic styles.")
                            .textStyle(style(FontName.HELVETICA, 10, TextDecoration.DEFAULT, new Color(90, 90, 90)))
                            .margin(Margin.bottom(12)));

            Collection<FontName> safeFonts = fonts == null ? DefaultFonts.bundledFontNames() : fonts;
            for (FontName fontName : safeFonts) {
                addFontSection(flow, fontName);
            }
        });
    }

    private static void addFontSection(DocumentDsl.PageFlowBuilder flow, FontName fontName) {
        flow.module(fontName.name(), module -> {
            module.name("FontSection_" + fontName.name())
                    .spacing(2)
                    .margin(Margin.bottom(8))
                    .titleStyle(style(FontName.HELVETICA, 12, TextDecoration.BOLD, new Color(25, 25, 25)))
                    .titleMargin(Margin.bottom(2));
            sampleLine(module, "Regular: ", fontName, TextDecoration.DEFAULT, Margin.bottom(1));
            sampleLine(module, "Bold: ", fontName, TextDecoration.BOLD, Margin.bottom(1));
            sampleLine(module, "Italic: ", fontName, TextDecoration.ITALIC, Margin.bottom(1));
            sampleLine(module, "Bold Italic: ", fontName, TextDecoration.BOLD_ITALIC, Margin.bottom(4));
        });
    }

    private static void sampleLine(DocumentDsl.ModuleBuilder module,
                                   String label,
                                   FontName fontName,
                                   TextDecoration decoration,
                                   Margin margin) {
        module.paragraph(paragraph -> paragraph
                .text(label + SAMPLE_TEXT)
                .textStyle(style(fontName, 11, decoration, Color.BLACK))
                .margin(margin));
    }

    private static TextStyle style(FontName fontName, double size, TextDecoration decoration, Color color) {
        return new TextStyle(fontName, size, decoration, color);
    }
}
