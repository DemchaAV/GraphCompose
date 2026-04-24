package com.demcha.compose.font;

import com.demcha.compose.engine.render.pdf.PdfFont;
import com.demcha.compose.engine.render.pdf.PdfFontGetter;
import com.demcha.compose.engine.font.Font;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Document-scoped font registry and resolver.
 * <p>
 * {@code FontLibrary} maps logical {@link FontName} values to concrete backend
 * font implementations such as {@code PdfFont}. Builders and styles only deal
 * with logical names; measurement and rendering phases resolve those names
 * through this library when they need backend-specific font objects.
 * </p>
 *
 * <p>The library supports both eagerly registered fonts and lazily created
 * fonts through factories.</p>
 */
public class FontLibrary implements PdfFontGetter {

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

    // 1. ЗАМЕНА: Используем высокопроизводительную ConcurrentHashMap вместо LinkedHashMap
    private final Map<FontName, Map<Class<?>, Font<?>>> fonts = new ConcurrentHashMap<>();
    private final Map<FontName, Map<Class<?>, Supplier<Font<?>>>> fontFactories = new ConcurrentHashMap<>();

    /**
     * Resolves a font family to a concrete backend font implementation.
     *
     * @param fontName logical family requested by styles or templates
     * @param fontClass backend font type to resolve
     * @return the matching backend font when available
     */
    @SuppressWarnings("unchecked")
    public <F extends Font<?>> Optional<F> getFont(FontName fontName, Class<F> fontClass) {
        FontName resolvedName = resolveBaseFont(fontName);
        
        // 1. Check instantiated fonts
        var fontRegistry = fonts.get(resolvedName);
        if (fontRegistry != null) {
            Font<?> genericFont = fontRegistry.get(fontClass);
            if (genericFont != null && fontClass.isInstance(genericFont)) {
                return Optional.of(fontClass.cast(genericFont));
            }
        }

        // 2. Check font factories and evaluate lazily if present
        var factoryRegistry = fontFactories.get(resolvedName);
        if (factoryRegistry != null) {
            Supplier<Font<?>> factory = factoryRegistry.get(fontClass);
            if (factory != null) {
                Font<?> newFont = factory.get();
                addFont(resolvedName, fontClass, (F) newFont);
                return Optional.of(fontClass.cast(newFont));
            }
        }

        return Optional.empty();
    }

    private FontName resolveBaseFont(FontName fontName) {
        if (fontName == null || FontName.DEFAULT.equals(fontName)) {
            return FontName.HELVETICA;
        }
        return FONT_ALIASES.getOrDefault(fontName, fontName);
    }

    /**
     * Registers an already created backend font instance under a logical family.
     */
    public <F extends Font<?>> void addFont(FontName name, Class<F> fontClass, F font) {
        if (!fontClass.isInstance(font)) {
            throw new IllegalArgumentException(
                    "Font instance does not match the provided class type"
            );
        }
        fonts.computeIfAbsent(name, k -> new ConcurrentHashMap<>())
                .put(fontClass, font);
    }

    /**
     * Registers a lazy factory for a backend font implementation.
     */
    public <F extends Font<?>> void addFontFactory(FontName name, Class<F> fontClass, Supplier<F> factory) {
        // Safe cast in factory wrapping since we expect supplier returning F
        fontFactories.computeIfAbsent(name, k -> new ConcurrentHashMap<>())
                .put(fontClass, (Supplier<Font<?>>) (Supplier<?>) factory);
    }

    /**
     * Convenience overload that registers a font under its runtime class.
     */
    @SuppressWarnings("unchecked")
    public <F extends Font<?>> void addFont(FontName name, F font) {
        addFont(name, (Class<F>) font.getClass(), font);
    }

    /**
     * Registers a font from a logical family plus concrete font pair.
     */
    @SuppressWarnings("unchecked")
    public <F extends Font<?>> void addFont(FontSet set) {
        addFont(set.name(), (Class<F>) set.font().getClass(), (F) set.font());
    }

    /**
     * Returns the set of logical families currently known to this library.
     */
    public Set<FontName> availableFonts() {
        Set<FontName> all = new LinkedHashSet<>();
        all.addAll(fonts.keySet());
        all.addAll(fontFactories.keySet());
        return Collections.unmodifiableSet(all);
    }

    /**
     * Resolves the PDF font implementation for a logical family.
     *
     * @param fontName logical family requested by the renderer
     * @return the PDF font implementation
     */
    @Override
    public PdfFont getPdfFont(FontName fontName) {
        return getFont(fontName, PdfFont.class).orElseThrow();
    }
}
