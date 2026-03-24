package com.demcha.compose.font_library;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class DefaultFonts {

    private static final List<FontFamilyDefinition> STANDARD_FONT_FAMILIES = List.of(
            FontFamilyDefinition.standard14(
                    FontName.HELVETICA,
                    Standard14Fonts.FontName.HELVETICA,
                    Standard14Fonts.FontName.HELVETICA_BOLD,
                    Standard14Fonts.FontName.HELVETICA_OBLIQUE,
                    Standard14Fonts.FontName.HELVETICA_BOLD_OBLIQUE),
            FontFamilyDefinition.standard14(
                    FontName.TIMES_ROMAN,
                    Standard14Fonts.FontName.TIMES_ROMAN,
                    Standard14Fonts.FontName.TIMES_BOLD,
                    Standard14Fonts.FontName.TIMES_ITALIC,
                    Standard14Fonts.FontName.TIMES_BOLD_ITALIC),
            FontFamilyDefinition.standard14(
                    FontName.COURIER,
                    Standard14Fonts.FontName.COURIER,
                    Standard14Fonts.FontName.COURIER_BOLD,
                    Standard14Fonts.FontName.COURIER_OBLIQUE,
                    Standard14Fonts.FontName.COURIER_BOLD_OBLIQUE));

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
            google(FontName.COUSINE, "cousine", "Cousine"));

    private DefaultFonts() {
    }

    public static FontLibrary standardLibrary() {
        FontLibrary fontLibrary = new FontLibrary();
        STANDARD_FONT_FAMILIES.forEach(definition -> definition.register(fontLibrary, null));
        return fontLibrary;
    }

    public static FontLibrary library(PDDocument document) {
        return library(document, List.of());
    }

    public static FontLibrary library(PDDocument document, Collection<FontFamilyDefinition> customFamilies) {
        FontLibrary fontLibrary = new FontLibrary();

        for (FontFamilyDefinition definition : STANDARD_FONT_FAMILIES) {
            definition.register(fontLibrary, document);
        }
        for (FontFamilyDefinition definition : GOOGLE_FONT_FAMILIES) {
            definition.register(fontLibrary, document);
        }
        for (FontFamilyDefinition definition : customFamilies) {
            definition.register(fontLibrary, document);
        }

        return fontLibrary;
    }

    public static List<FontName> bundledFontNames() {
        List<FontName> result = new ArrayList<>();
        STANDARD_FONT_FAMILIES.stream().map(FontFamilyDefinition::name).forEach(result::add);
        GOOGLE_FONT_FAMILIES.stream().map(FontFamilyDefinition::name).forEach(result::add);
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
