package com.demcha.compose.font_library;

import com.demcha.compose.loyaut_core.system.implemented_systems.pdf_systems.PdfFont;
import com.demcha.compose.loyaut_core.system.implemented_systems.word_sustems.WordFont;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Describes a reusable font family that can be materialized for different
 * renderers.
 * <p>
 * PDF fonts are loaded into the current {@link PDDocument}, while the Word
 * backend keeps only the family name so it can be reused later when the Word
 * renderer is implemented.
 */
public final class FontFamilyDefinition {

    private final FontName name;
    private final String wordFamily;
    private final PdfFontFactory pdfFontFactory;

    private FontFamilyDefinition(FontName name, String wordFamily, PdfFontFactory pdfFontFactory) {
        this.name = Objects.requireNonNull(name, "name");
        this.wordFamily = Objects.requireNonNull(wordFamily, "wordFamily");
        this.pdfFontFactory = Objects.requireNonNull(pdfFontFactory, "pdfFontFactory");
    }

    public FontName name() {
        return name;
    }

    public void register(FontLibrary library, PDDocument document) {
        library.addFontFactory(name, PdfFont.class, () -> {
            try {
                return pdfFontFactory.create(document);
            } catch (IOException e) {
                throw new IllegalStateException("Unable to register pdf font family " + name, e);
            }
        });
        library.addFontFactory(name, WordFont.class, () -> new WordFont(wordFamily));
    }

    public static FontFamilyDefinition standard14(FontName name,
            Standard14Fonts.FontName regular,
            Standard14Fonts.FontName bold,
            Standard14Fonts.FontName italic,
            Standard14Fonts.FontName boldItalic) {

        return new FontFamilyDefinition(name, name.name(), doc -> new PdfFont(
                new PDType1Font(regular),
                new PDType1Font(bold),
                new PDType1Font(italic),
                new PDType1Font(boldItalic)));
    }

    public static Builder classpath(FontName name, String regularResource) {
        return new Builder(name, new ClasspathFontSource(regularResource));
    }

    public static Builder classpath(String name, String regularResource) {
        return classpath(FontName.of(name), regularResource);
    }

    public static Builder files(FontName name, Path regularPath) {
        return new Builder(name, new FileFontSource(regularPath));
    }

    public static Builder files(String name, Path regularPath) {
        return files(FontName.of(name), regularPath);
    }

    public static final class Builder {
        private final FontName name;
        private final FontBinarySource regular;
        private FontBinarySource bold;
        private FontBinarySource italic;
        private FontBinarySource boldItalic;
        private String wordFamily;

        private Builder(FontName name, FontBinarySource regular) {
            this.name = Objects.requireNonNull(name, "name");
            this.regular = Objects.requireNonNull(regular, "regular");
            this.wordFamily = name.name();
        }

        public Builder wordFamily(String wordFamily) {
            this.wordFamily = Objects.requireNonNull(wordFamily, "wordFamily").trim();
            return this;
        }

        public Builder boldResource(String resourcePath) {
            this.bold = new ClasspathFontSource(resourcePath);
            return this;
        }

        public Builder italicResource(String resourcePath) {
            this.italic = new ClasspathFontSource(resourcePath);
            return this;
        }

        public Builder boldItalicResource(String resourcePath) {
            this.boldItalic = new ClasspathFontSource(resourcePath);
            return this;
        }

        public Builder boldPath(Path path) {
            this.bold = new FileFontSource(path);
            return this;
        }

        public Builder italicPath(Path path) {
            this.italic = new FileFontSource(path);
            return this;
        }

        public Builder boldItalicPath(Path path) {
            this.boldItalic = new FileFontSource(path);
            return this;
        }

        public FontFamilyDefinition build() {
            FontBinarySource resolvedBold = bold != null ? bold : regular;
            FontBinarySource resolvedItalic = italic != null ? italic : regular;
            FontBinarySource resolvedBoldItalic = boldItalic != null
                    ? boldItalic
                    : (bold != null ? bold : (italic != null ? italic : regular));

            return new FontFamilyDefinition(name, wordFamily, doc -> new PdfFont(
                    Pdf_FontLoader.loadFont(doc, regular.openStream(), regular.description()),
                    Pdf_FontLoader.loadFont(doc, resolvedBold.openStream(), resolvedBold.description()),
                    Pdf_FontLoader.loadFont(doc, resolvedItalic.openStream(), resolvedItalic.description()),
                    Pdf_FontLoader.loadFont(doc, resolvedBoldItalic.openStream(), resolvedBoldItalic.description())));
        }
    }

    @FunctionalInterface
    private interface PdfFontFactory {
        PdfFont create(PDDocument document) throws IOException;
    }

    private interface FontBinarySource {
        InputStream openStream() throws IOException;

        String description();
    }

    private record ClasspathFontSource(String resourcePath) implements FontBinarySource {
        private ClasspathFontSource {
            Objects.requireNonNull(resourcePath, "resourcePath");
        }

        @Override
        public InputStream openStream() {
            String normalizedPath = resourcePath.startsWith("/") ? resourcePath : "/" + resourcePath;
            InputStream inputStream = FontFamilyDefinition.class.getResourceAsStream(normalizedPath);
            if (inputStream == null) {
                throw new IllegalArgumentException("Classpath font resource not found: " + normalizedPath);
            }
            return inputStream;
        }

        @Override
        public String description() {
            return resourcePath;
        }
    }

    private record FileFontSource(Path path) implements FontBinarySource {
        private FileFontSource {
            Objects.requireNonNull(path, "path");
        }

        @Override
        public InputStream openStream() throws IOException {
            return Files.newInputStream(path);
        }

        @Override
        public String description() {
            return path.toAbsolutePath().toString();
        }
    }
}
