package com.demcha.compose.font;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

/**
 * Describes a reusable font family in backend-neutral terms.
 *
 * <p>A definition records the logical family name plus either a standard-font
 * descriptor or binary font sources. Backends materialize those descriptors into
 * their own concrete font objects during measurement or rendering.</p>
 */
public final class FontFamilyDefinition {

    private final FontName name;
    private final String wordFamily;
    private final Standard14Family standard14Family;
    private final FontSourceSet fontSourceSet;

    private FontFamilyDefinition(FontName name,
                                 String wordFamily,
                                 Standard14Family standard14Family,
                                 FontSourceSet fontSourceSet) {
        this.name = Objects.requireNonNull(name, "name");
        this.wordFamily = Objects.requireNonNull(wordFamily, "wordFamily");
        this.standard14Family = standard14Family;
        this.fontSourceSet = fontSourceSet;
        if ((standard14Family == null) == (fontSourceSet == null)) {
            throw new IllegalArgumentException("Exactly one font descriptor must be provided.");
        }
    }

    /**
     * Returns the logical font family name.
     *
     * @return logical family
     */
    public FontName name() {
        return name;
    }

    /**
     * Returns the family name used by Word-style backends.
     *
     * @return word family name
     */
    public String wordFamily() {
        return wordFamily;
    }

    /**
     * Returns the standard 14 PDF font descriptor when this family maps to a
     * built-in font.
     *
     * @return optional standard font descriptor
     */
    public Optional<Standard14Family> standard14Family() {
        return Optional.ofNullable(standard14Family);
    }

    /**
     * Returns binary font sources when this family is backed by font files or
     * classpath resources.
     *
     * @return optional font sources
     */
    public Optional<FontSourceSet> fontSourceSet() {
        return Optional.ofNullable(fontSourceSet);
    }

    /**
     * Creates a standard 14 font family descriptor.
     *
     * @param name logical family name
     * @param regular standard regular face identifier
     * @param bold standard bold face identifier
     * @param italic standard italic face identifier
     * @param boldItalic standard bold-italic face identifier
     * @return font family descriptor
     */
    public static FontFamilyDefinition standard14(FontName name,
                                                  String regular,
                                                  String bold,
                                                  String italic,
                                                  String boldItalic) {
        return new FontFamilyDefinition(
                name,
                name.name(),
                new Standard14Family(regular, bold, italic, boldItalic),
                null);
    }

    /**
     * Starts a classpath-backed custom font family definition.
     *
     * @param name logical family name
     * @param regularResource classpath resource for the regular face
     * @return custom font builder
     */
    public static Builder classpath(FontName name, String regularResource) {
        return new Builder(name, new ClasspathFontSource(regularResource));
    }

    /**
     * Starts a classpath-backed custom font family definition.
     *
     * @param name logical family name
     * @param regularResource classpath resource for the regular face
     * @return custom font builder
     */
    public static Builder classpath(String name, String regularResource) {
        return classpath(FontName.of(name), regularResource);
    }

    /**
     * Starts a file-backed custom font family definition.
     *
     * @param name logical family name
     * @param regularPath regular font file
     * @return custom font builder
     */
    public static Builder files(FontName name, Path regularPath) {
        return new Builder(name, new FileFontSource(regularPath));
    }

    /**
     * Starts a file-backed custom font family definition.
     *
     * @param name logical family name
     * @param regularPath regular font file
     * @return custom font builder
     */
    public static Builder files(String name, Path regularPath) {
        return files(FontName.of(name), regularPath);
    }

    /**
     * Backend-neutral descriptor for a standard 14 font family.
     *
     * @param regular regular face identifier
     * @param bold bold face identifier
     * @param italic italic face identifier
     * @param boldItalic bold-italic face identifier
     */
    public record Standard14Family(String regular, String bold, String italic, String boldItalic) {
        /**
         * Creates a validated standard font descriptor.
         */
        public Standard14Family {
            Objects.requireNonNull(regular, "regular");
            Objects.requireNonNull(bold, "bold");
            Objects.requireNonNull(italic, "italic");
            Objects.requireNonNull(boldItalic, "boldItalic");
        }
    }

    /**
     * Backend-neutral binary sources for a four-face font family.
     *
     * @param regular regular face source
     * @param bold bold face source
     * @param italic italic face source
     * @param boldItalic bold-italic face source
     */
    public record FontSourceSet(FontBinarySource regular,
                                FontBinarySource bold,
                                FontBinarySource italic,
                                FontBinarySource boldItalic) {
        /**
         * Creates a validated source set.
         */
        public FontSourceSet {
            Objects.requireNonNull(regular, "regular");
            Objects.requireNonNull(bold, "bold");
            Objects.requireNonNull(italic, "italic");
            Objects.requireNonNull(boldItalic, "boldItalic");
        }
    }

    /**
     * Backend-neutral binary font source.
     */
    public interface FontBinarySource {
        /**
         * Opens a new stream for the font data.
         *
         * @return input stream owned by the caller
         * @throws IOException if the source cannot be opened
         */
        InputStream openStream() throws IOException;

        /**
         * Describes this source for diagnostics and cache keys.
         *
         * @return source description
         */
        String description();
    }

    /**
     * Builder for file- or classpath-backed custom font families.
     */
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

        /**
         * Sets the Word-style family name.
         *
         * @param wordFamily word family name
         * @return this builder
         */
        public Builder wordFamily(String wordFamily) {
            this.wordFamily = Objects.requireNonNull(wordFamily, "wordFamily").trim();
            return this;
        }

        /**
         * Sets the bold face from a classpath resource.
         *
         * @param resourcePath resource path
         * @return this builder
         */
        public Builder boldResource(String resourcePath) {
            this.bold = new ClasspathFontSource(resourcePath);
            return this;
        }

        /**
         * Sets the italic face from a classpath resource.
         *
         * @param resourcePath resource path
         * @return this builder
         */
        public Builder italicResource(String resourcePath) {
            this.italic = new ClasspathFontSource(resourcePath);
            return this;
        }

        /**
         * Sets the bold-italic face from a classpath resource.
         *
         * @param resourcePath resource path
         * @return this builder
         */
        public Builder boldItalicResource(String resourcePath) {
            this.boldItalic = new ClasspathFontSource(resourcePath);
            return this;
        }

        /**
         * Sets the bold face from a file path.
         *
         * @param path font file path
         * @return this builder
         */
        public Builder boldPath(Path path) {
            this.bold = new FileFontSource(path);
            return this;
        }

        /**
         * Sets the italic face from a file path.
         *
         * @param path font file path
         * @return this builder
         */
        public Builder italicPath(Path path) {
            this.italic = new FileFontSource(path);
            return this;
        }

        /**
         * Sets the bold-italic face from a file path.
         *
         * @param path font file path
         * @return this builder
         */
        public Builder boldItalicPath(Path path) {
            this.boldItalic = new FileFontSource(path);
            return this;
        }

        /**
         * Builds an immutable custom font family definition.
         *
         * @return font family definition
         */
        public FontFamilyDefinition build() {
            FontBinarySource resolvedBold = bold != null ? bold : regular;
            FontBinarySource resolvedItalic = italic != null ? italic : regular;
            FontBinarySource resolvedBoldItalic = boldItalic != null
                    ? boldItalic
                    : (bold != null ? bold : (italic != null ? italic : regular));

            return new FontFamilyDefinition(
                    name,
                    wordFamily,
                    null,
                    new FontSourceSet(regular, resolvedBold, resolvedItalic, resolvedBoldItalic));
        }
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
