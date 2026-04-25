package com.demcha.compose.font;

import com.demcha.compose.document.backend.fixed.pdf.PdfFontLibraryFactory;
import org.apache.pdfbox.pdmodel.PDDocument;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Catalog of built-in GraphCompose font families.
 */
public class DefaultFonts {

    private static final List<FontFamilyDefinition> STANDARD_FONT_FAMILIES = List.of(
            FontFamilyDefinition.standard14(
                    FontName.HELVETICA,
                    "HELVETICA",
                    "HELVETICA_BOLD",
                    "HELVETICA_OBLIQUE",
                    "HELVETICA_BOLD_OBLIQUE"),
            FontFamilyDefinition.standard14(
                    FontName.TIMES_ROMAN,
                    "TIMES_ROMAN",
                    "TIMES_BOLD",
                    "TIMES_ITALIC",
                    "TIMES_BOLD_ITALIC"),
            FontFamilyDefinition.standard14(
                    FontName.COURIER,
                    "COURIER",
                    "COURIER_BOLD",
                    "COURIER_OBLIQUE",
                    "COURIER_BOLD_OBLIQUE"));

    private static final List<FontFamilyDefinition> GOOGLE_FONT_FAMILIES = List.of(
            google(FontName.LATO, "lato", "Lato"),
            google(FontName.PT_SANS, "ptsans", "PT_Sans-Web"),
            google(FontName.PT_SERIF, "ptserif", "PT_Serif-Web"),
            google(FontName.FIRA_SANS, "firasans", "FiraSans"),
            google(FontName.UBUNTU, "ubuntu", "Ubuntu"),
            google(FontName.ALEGREYA_SANS, "alegreyasans", "AlegreyaSans"),
            google(FontName.CARLITO, "carlito", "Carlito"),
            google(FontName.POPPINS, "poppins", "Poppins"),
            google(FontName.BARLOW, "barlow", "Barlow"),
            google(FontName.BARLOW_CONDENSED, "barlowcondensed", "BarlowCondensed"),
            google(FontName.ASAP_CONDENSED, "asapcondensed", "AsapCondensed"),
            google(FontName.ARSENAL, "arsenal", "Arsenal"),
            google(FontName.IBM_PLEX_SERIF, "ibmplexserif", "IBMPlexSerif"),
            google(FontName.IBM_PLEX_MONO, "ibmplexmono", "IBMPlexMono"),
            google(FontName.CRIMSON_TEXT, "crimsontext", "CrimsonText"),
            google(FontName.SPECTRAL, "spectral", "Spectral"),
            google(FontName.ZILLA_SLAB, "zillaslab", "ZillaSlab"),
            google(FontName.GENTIUM_PLUS, "gentiumplus", "GentiumPlus"),
            google(FontName.TINOS, "tinos", "Tinos"),
            google(FontName.COUSINE, "cousine", "Cousine"),
            google(FontName.FIRA_SANS_CONDENSED, "firasanscondensed", "FiraSansCondensed"),
            google(FontName.KANIT, "kanit", "Kanit"),
            google(FontName.VOLKHOV, "volkhov", "Volkhov"),
            google(FontName.TAVIRAJ, "taviraj", "Taviraj"),
            google(FontName.TRIRONG, "trirong", "Trirong"),
            google(FontName.SARABUN, "sarabun", "Sarabun"),
            google(FontName.PROMPT, "prompt", "Prompt"),
            google(FontName.ANDIKA, "andika", "Andika"),
            google(FontName.BAI_JAMJUREE, "baijamjuree", "BaiJamjuree"));

    private DefaultFonts() {
    }

    /**
     * Creates a PDF-backed font library containing the standard 14 families.
     *
     * @return font library with standard fonts
     */
    public static FontLibrary standardLibrary() {
        return PdfFontLibraryFactory.standardLibrary();
    }

    /**
     * Creates a PDF-backed font library containing bundled families.
     *
     * @param document PDF document that owns loaded fonts
     * @return font library
     */
    public static FontLibrary library(PDDocument document) {
        return PdfFontLibraryFactory.library(document);
    }

    /**
     * Creates a PDF-backed font library containing bundled and custom families.
     *
     * @param document PDF document that owns loaded fonts
     * @param customFamilies document-local custom font families
     * @return font library
     */
    public static FontLibrary library(PDDocument document, Collection<FontFamilyDefinition> customFamilies) {
        return PdfFontLibraryFactory.library(document, customFamilies);
    }

    /**
     * Returns bundled font family definitions.
     *
     * @return standard and bundled Google font definitions
     */
    public static List<FontFamilyDefinition> bundledFamilies() {
        List<FontFamilyDefinition> result = new ArrayList<>();
        result.addAll(STANDARD_FONT_FAMILIES);
        result.addAll(GOOGLE_FONT_FAMILIES);
        return List.copyOf(result);
    }

    /**
     * Returns standard 14 font family definitions.
     *
     * @return standard family definitions
     */
    public static List<FontFamilyDefinition> standardFamilies() {
        return STANDARD_FONT_FAMILIES;
    }

    /**
     * Returns bundled Google font family definitions.
     *
     * @return Google font family definitions
     */
    public static List<FontFamilyDefinition> googleFamilies() {
        return GOOGLE_FONT_FAMILIES;
    }

    /**
     * Returns logical names for all bundled font families.
     *
     * @return bundled logical font names
     */
    public static List<FontName> bundledFontNames() {
        List<FontName> result = new ArrayList<>();
        bundledFamilies().stream().map(FontFamilyDefinition::name).forEach(result::add);
        return List.copyOf(result);
    }

    private static FontFamilyDefinition google(FontName fontName, String folder, String prefix) {
        return FontFamilyDefinition.classpath(fontName, resource(folder, prefix + "-Regular.ttf"))
                .boldResource(resource(folder, prefix + "-Bold.ttf"))
                .italicResource(resource(folder, prefix + "-Italic.ttf"))
                .boldItalicResource(resource(folder, prefix + "-BoldItalic.ttf"))
                .build();
    }

    private static String resource(String folder, String fileName) {
        return "fonts/google/" + folder + "/" + fileName;
    }
}
