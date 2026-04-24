package com.demcha.compose.font;

import java.util.Objects;

/**
 * Logical font family identifier used throughout GraphCompose.
 * <p>
 * {@code FontName} is the stable name that flows through public APIs such as
 * {@code TextStyle}, themes, and custom font registration. The same identifier
 * can be resolved to backend-specific font implementations by the active
 * {@code FontLibrary}.
 * </p>
 *
 * <p>The class preserves enum-like constants for bundled families while still
 * allowing callers to create document-specific custom names through {@link #of(String)}.</p>
 */
public final class FontName {

    public static final FontName TIMES_ROMAN = of("Times-Roman");
    public static final FontName HELVETICA = of("Helvetica");
    public static final FontName HELVETICA_BOLD = of("Helvetica-Bold");
    public static final FontName HELVETICA_OBLIQUE = of("Helvetica-Oblique");
    public static final FontName HELVETICA_BOLD_OBLIQUE = of("Helvetica-BoldOblique");
    public static final FontName COURIER = of("Courier");
    public static final FontName COURIER_BOLD = of("Courier-Bold");
    public static final FontName COURIER_OBLIQUE = of("Courier-Oblique");
    public static final FontName COURIER_BOLD_OBLIQUE = of("Courier-BoldOblique");
    public static final FontName TIMES_BOLD = of("Times-Bold");
    public static final FontName TIMES_ITALIC = of("Times-Italic");
    public static final FontName TIMES_BOLD_ITALIC = of("Times-BoldItalic");
    public static final FontName SYMBOL = of("Symbol");
    public static final FontName DEFAULT = of("default");
    public static final FontName ZAPF_DINGBATS = of("ZapfDingbats");

    public static final FontName LATO = of("Lato");
    public static final FontName PT_SANS = of("PT Sans");
    public static final FontName PT_SERIF = of("PT Serif");
    public static final FontName FIRA_SANS = of("Fira Sans");
    public static final FontName UBUNTU = of("Ubuntu");
    public static final FontName ALEGREYA_SANS = of("Alegreya Sans");
    public static final FontName CARLITO = of("Carlito");
    public static final FontName POPPINS = of("Poppins");
    public static final FontName BARLOW = of("Barlow");
    public static final FontName BARLOW_CONDENSED = of("Barlow Condensed");
    public static final FontName ASAP_CONDENSED = of("Asap Condensed");
    public static final FontName ARSENAL = of("Arsenal");
    public static final FontName IBM_PLEX_SERIF = of("IBM Plex Serif");
    public static final FontName IBM_PLEX_MONO = of("IBM Plex Mono");
    public static final FontName CRIMSON_TEXT = of("Crimson Text");
    public static final FontName SPECTRAL = of("Spectral");
    public static final FontName ZILLA_SLAB = of("Zilla Slab");
    public static final FontName GENTIUM_PLUS = of("Gentium Plus");
    public static final FontName TINOS = of("Tinos");
    public static final FontName COUSINE = of("Cousine");
    public static final FontName FIRA_SANS_CONDENSED = of("Fira Sans Condensed");
    public static final FontName KANIT = of("Kanit");
    public static final FontName VOLKHOV = of("Volkhov");
    public static final FontName TAVIRAJ = of("Taviraj");
    public static final FontName TRIRONG = of("Trirong");
    public static final FontName SARABUN = of("Sarabun");
    public static final FontName PROMPT = of("Prompt");
    public static final FontName ANDIKA = of("Andika");
    public static final FontName BAI_JAMJUREE = of("Bai Jamjuree");

    private final String name;
    private final String normalizedName;

    private FontName(String name) {
        String normalizedInput = normalize(name);
        if (normalizedInput.isBlank()) {
            throw new IllegalArgumentException("Font name must not be blank");
        }
        this.name = name.trim();
        this.normalizedName = normalizedInput;
    }

    /**
     * Creates a logical font family identifier from a user-supplied name.
     *
     * <p>The returned value is suitable for custom font registration and later
     * style lookup.</p>
     */
    public static FontName of(String name) {
        return new FontName(name);
    }

    /**
     * Returns the original display name.
     */
    public String name() {
        return name;
    }

    /**
     * Returns the canonical normalized key used for equality and lookup.
     */
    public String normalizedName() {
        return normalizedName;
    }

    /**
     * Returns whether both identifiers refer to the same logical family.
     */
    public boolean sameFamily(FontName other) {
        return equals(other);
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof FontName fontName))
            return false;
        return normalizedName.equals(fontName.normalizedName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(normalizedName);
    }

    private static String normalize(String name) {
        Objects.requireNonNull(name, "name");
        return name.trim().toLowerCase();
    }
}
