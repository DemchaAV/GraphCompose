package com.demcha.compose.document.backend.fixed.pdf;

import com.demcha.compose.engine.render.pdf.PdfFont;
import com.demcha.compose.engine.render.word.WordFont;
import com.demcha.compose.font.DefaultFonts;
import com.demcha.compose.font.FontFamilyDefinition;
import com.demcha.compose.font.FontLibrary;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
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
        DefaultFonts.standardFamilies().forEach(definition -> register(fontLibrary, null, definition, false));
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
        return buildLibrary(document, customFamilies, false);
    }

    /**
     * Creates a measurement-only font library.
     *
     * <p>Binary families resolve to per-thread cached measurement fonts instead of
     * embedding a fresh subset into a throwaway {@link PDDocument} on every session
     * (Finding 4: the measurement document is discarded, so its embed was pure
     * waste). Standard-14 families are unaffected — they never embed. The resolved
     * font metrics are byte-identical to the render library, so layout geometry is
     * unchanged.</p>
     *
     * @param customFamilies document-local custom font families
     * @return measurement-backed font library that needs no owning document
     */
    public static FontLibrary measurementLibrary(Collection<FontFamilyDefinition> customFamilies) {
        return buildLibrary(null, customFamilies, true);
    }

    private static FontLibrary buildLibrary(PDDocument document,
                                            Collection<FontFamilyDefinition> customFamilies,
                                            boolean measurement) {
        FontLibrary fontLibrary = new FontLibrary();

        for (FontFamilyDefinition definition : DefaultFonts.bundledFamilies()) {
            register(fontLibrary, document, definition, measurement);
        }
        for (FontFamilyDefinition definition : customFamilies) {
            register(fontLibrary, document, definition, measurement);
        }

        return fontLibrary;
    }

    private static void register(FontLibrary library,
                                 PDDocument document,
                                 FontFamilyDefinition definition,
                                 boolean measurement) {
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
                    try {
                        return new PdfFont(
                                loadBinaryFace(measurement, document, definition, sources.regular()),
                                loadBinaryFace(measurement, document, definition, sources.bold()),
                                loadBinaryFace(measurement, document, definition, sources.italic()),
                                loadBinaryFace(measurement, document, definition, sources.boldItalic()));
                    } catch (IOException e) {
                        throw new IllegalStateException("Unable to register PDF font family " + definition.name(), e);
                    }
                }));

        library.addFontFactory(definition.name(), WordFont.class, () -> new WordFont(definition.wordFamily()));
    }

    /**
     * Loads a single binary face for either the render path (subset embedded into
     * {@code document}) or the measurement path (per-thread cached, doc-independent
     * metrics). Both observe the same parsed font program, so metrics match.
     */
    private static PDType0Font loadBinaryFace(boolean measurement,
                                              PDDocument document,
                                              FontFamilyDefinition definition,
                                              FontFamilyDefinition.FontBinarySource source) throws IOException {
        if (measurement) {
            return PdfFontLoader.loadMeasurementFont(source.openStream(), source.description());
        }
        PDDocument owner = Objects.requireNonNull(
                document,
                "A PDF document is required to load binary font family " + definition.name());
        return PdfFontLoader.loadFont(owner, source.openStream(), source.description());
    }

    private static Standard14Fonts.FontName standardFont(String name) {
        return Standard14Fonts.FontName.valueOf(name);
    }
}
