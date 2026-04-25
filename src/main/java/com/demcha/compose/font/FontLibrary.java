package com.demcha.compose.font;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Document-scoped font registry and resolver.
 *
 * <p>{@code FontLibrary} maps logical {@link FontName} values to concrete
 * backend font objects. Public authoring code deals only with logical names;
 * measurement and rendering phases resolve those names through typed backend
 * lookups such as {@code getFont(name, PdfFont.class)}.</p>
 *
 * <p>The library supports both eagerly registered fonts and lazily created
 * fonts through factories.</p>
 */
public class FontLibrary {

    private static final Map<FontName, FontName> FONT_ALIASES = Map.ofEntries(
            Map.entry(FontName.HELVETICA_BOLD, FontName.HELVETICA),
            Map.entry(FontName.HELVETICA_OBLIQUE, FontName.HELVETICA),
            Map.entry(FontName.HELVETICA_BOLD_OBLIQUE, FontName.HELVETICA),
            Map.entry(FontName.TIMES_BOLD, FontName.TIMES_ROMAN),
            Map.entry(FontName.TIMES_ITALIC, FontName.TIMES_ROMAN),
            Map.entry(FontName.TIMES_BOLD_ITALIC, FontName.TIMES_ROMAN),
            Map.entry(FontName.COURIER_BOLD, FontName.COURIER),
            Map.entry(FontName.COURIER_OBLIQUE, FontName.COURIER),
            Map.entry(FontName.COURIER_BOLD_OBLIQUE, FontName.COURIER));

    private final Map<FontName, Map<Class<?>, Object>> fonts = new ConcurrentHashMap<>();
    private final Map<FontName, Map<Class<?>, Supplier<?>>> fontFactories = new ConcurrentHashMap<>();

    /**
     * Resolves a font family to a concrete backend font object.
     *
     * @param fontName logical family requested by styles or templates
     * @param fontClass backend font type to resolve
     * @param <F> backend font type
     * @return the matching backend font when available
     */
    public <F> Optional<F> getFont(FontName fontName, Class<F> fontClass) {
        Objects.requireNonNull(fontClass, "fontClass");
        FontName resolvedName = resolveBaseFont(fontName);

        Map<Class<?>, Object> fontRegistry = fonts.get(resolvedName);
        if (fontRegistry != null) {
            Object registered = fontRegistry.get(fontClass);
            if (fontClass.isInstance(registered)) {
                return Optional.of(fontClass.cast(registered));
            }
        }

        Map<Class<?>, Supplier<?>> factoryRegistry = fontFactories.get(resolvedName);
        if (factoryRegistry != null) {
            Supplier<?> factory = factoryRegistry.get(fontClass);
            if (factory != null) {
                Object created = Objects.requireNonNull(
                        factory.get(),
                        "Font factory returned null for " + resolvedName + " and " + fontClass.getName());
                F typed = fontClass.cast(created);
                addFont(resolvedName, fontClass, typed);
                return Optional.of(typed);
            }
        }

        return Optional.empty();
    }

    /**
     * Registers an already created backend font object under a logical family.
     *
     * @param name logical font family
     * @param fontClass backend font type
     * @param font backend font instance
     * @param <F> backend font type
     */
    public <F> void addFont(FontName name, Class<F> fontClass, F font) {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(fontClass, "fontClass");
        Objects.requireNonNull(font, "font");
        if (!fontClass.isInstance(font)) {
            throw new IllegalArgumentException("Font instance does not match the provided class type.");
        }
        fonts.computeIfAbsent(name, key -> new ConcurrentHashMap<>())
                .put(fontClass, font);
    }

    /**
     * Registers a lazy factory for a backend font object.
     *
     * @param name logical font family
     * @param fontClass backend font type
     * @param factory lazy factory that creates the backend font object
     * @param <F> backend font type
     */
    public <F> void addFontFactory(FontName name, Class<F> fontClass, Supplier<? extends F> factory) {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(fontClass, "fontClass");
        Objects.requireNonNull(factory, "factory");
        fontFactories.computeIfAbsent(name, key -> new ConcurrentHashMap<>())
                .put(fontClass, factory);
    }

    /**
     * Convenience overload that registers a font under its runtime class.
     *
     * @param name logical font family
     * @param font backend font object
     * @param <F> backend font type
     */
    @SuppressWarnings("unchecked")
    public <F> void addFont(FontName name, F font) {
        Objects.requireNonNull(font, "font");
        addFont(name, (Class<F>) font.getClass(), font);
    }

    /**
     * Registers a font from a logical family plus concrete font pair.
     *
     * @param set font registration tuple
     * @param <F> backend font type
     */
    public <F> void addFont(FontSet<F> set) {
        Objects.requireNonNull(set, "set");
        addFont(set.name(), set.fontClass(), set.font());
    }

    /**
     * Returns the set of logical families currently known to this library.
     *
     * @return available logical font names
     */
    public Set<FontName> availableFonts() {
        Set<FontName> all = new LinkedHashSet<>();
        all.addAll(fonts.keySet());
        all.addAll(fontFactories.keySet());
        return Collections.unmodifiableSet(all);
    }

    private FontName resolveBaseFont(FontName fontName) {
        if (fontName == null || FontName.DEFAULT.equals(fontName)) {
            return FontName.HELVETICA;
        }
        return FONT_ALIASES.getOrDefault(fontName, fontName);
    }
}
