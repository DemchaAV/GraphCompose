package com.demcha.compose.document.backend.fixed.pdf;

import com.demcha.compose.engine.render.pdf.PdfFont;
import com.demcha.compose.engine.render.word.WordFont;
import com.demcha.compose.font.DefaultFonts;
import com.demcha.compose.font.FontFamilyDefinition;
import com.demcha.compose.font.FontLibrary;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Materializes public font descriptors into PDF-backed font library entries.
 *
 * <p>This class keeps PDFBox and engine font types behind the PDF backend
 * boundary while the public font package remains a backend-neutral descriptor
 * catalog.</p>
 *
 * @author Artem Demchyshyn
 */
public final class PdfFontLibraryFactory {

    private PdfFontLibraryFactory() {
    }

    /**
     * Creates a font library containing the standard 14 font families.
     *
     * @return PDF-backed font library
     */
    public static FontLibrary standardLibrary() {
        FontLibrary fontLibrary = new FontLibrary();
        DefaultFonts.standardFamilies().forEach(definition -> register(fontLibrary, null, definition));
        return fontLibrary;
    }

    /**
     * Creates a font library containing bundled font families.
     *
     * @param document PDF document that owns loaded fonts
     * @return PDF-backed font library
     */
    public static FontLibrary library(PDDocument document) {
        return library(document, List.of());
    }

    /**
     * Creates a font library containing bundled and custom font families.
     *
     * @param document PDF document that owns loaded fonts
     * @param customFamilies document-local custom font families
     * @return PDF-backed font library
     */
    public static FontLibrary library(PDDocument document, Collection<FontFamilyDefinition> customFamilies) {
        FontLibrary fontLibrary = new FontLibrary();

        for (FontFamilyDefinition definition : DefaultFonts.bundledFamilies()) {
            register(fontLibrary, document, definition);
        }
        for (FontFamilyDefinition definition : customFamilies) {
            register(fontLibrary, document, definition);
        }

        return fontLibrary;
    }

    private static void register(FontLibrary library, PDDocument document, FontFamilyDefinition definition) {
        Objects.requireNonNull(library, "library");
        Objects.requireNonNull(definition, "definition");

        definition.standard14Family().ifPresent(family ->
                library.addFontFactory(definition.name(), PdfFont.class, () -> new PdfFont(
                        new PDType1Font(standardFont(family.regular())),
                        new PDType1Font(standardFont(family.bold())),
                        new PDType1Font(standardFont(family.italic())),
                        new PDType1Font(standardFont(family.boldItalic())))));

        definition.fontSourceSet().ifPresent(sources ->
                library.addFontFactory(definition.name(), PdfFont.class, () -> {
                    PDDocument owner = Objects.requireNonNull(
                            document,
                            "A PDF document is required to load binary font family " + definition.name());
                    try {
                        return new PdfFont(
                                PdfFontLoader.loadFont(owner, sources.regular().openStream(), sources.regular().description()),
                                PdfFontLoader.loadFont(owner, sources.bold().openStream(), sources.bold().description()),
                                PdfFontLoader.loadFont(owner, sources.italic().openStream(), sources.italic().description()),
                                PdfFontLoader.loadFont(owner, sources.boldItalic().openStream(), sources.boldItalic().description()));
                    } catch (IOException e) {
                        throw new IllegalStateException("Unable to register PDF font family " + definition.name(), e);
                    }
                }));

        library.addFontFactory(definition.name(), WordFont.class, () -> new WordFont(definition.wordFamily()));
    }

    private static Standard14Fonts.FontName standardFont(String name) {
        return Standard14Fonts.FontName.valueOf(name);
    }
}
