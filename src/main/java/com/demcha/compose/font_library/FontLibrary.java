package com.demcha.compose.font_library;

import com.demcha.compose.loyaut_core.system.implemented_systems.pdf_systems.PdfFont;
import com.demcha.compose.loyaut_core.system.implemented_systems.pdf_systems.PdfFontGetter;
import com.demcha.compose.loyaut_core.system.interfaces.Font;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class FontLibrary implements PdfFontGetter {

    // Эта мапа неизменяемая (Map.ofEntries создает ImmutableMap),
    // поэтому она на 100% потокобезопасна изначально.
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
     * Получаем конкретный тип шрифта: PdfFont, WordFont и т.д.
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
     * Добавляем шрифт с явным указанием класса.
     */
    public <F extends Font<?>> void addFont(FontName name, Class<F> fontClass, F font) {
        if (!fontClass.isInstance(font)) {
            throw new IllegalArgumentException(
                    "Font instance does not match the provided class type"
            );
        }
        // 2. ЗАМЕНА: Внутренняя мапа тоже должна быть ConcurrentHashMap
        fonts.computeIfAbsent(name, k -> new ConcurrentHashMap<>())
                .put(fontClass, font);
    }

    /**
     * Добавляем фабрику для отложенного инстанцирования шрифта
     */
    public <F extends Font<?>> void addFontFactory(FontName name, Class<F> fontClass, Supplier<F> factory) {
        // Safe cast in factory wrapping since we expect supplier returning F
        fontFactories.computeIfAbsent(name, k -> new ConcurrentHashMap<>())
                .put(fontClass, (Supplier<Font<?>>) (Supplier<?>) factory);
    }

    /**
     * Удобный метод: кладём под runtime-класс.
     */
    @SuppressWarnings("unchecked")
    public <F extends Font<?>> void addFont(FontName name, F font) {
        addFont(name, (Class<F>) font.getClass(), font);
    }

    @SuppressWarnings("unchecked")
    public <F extends Font<?>> void addFont(FontSet set) {
        addFont(set.name(), (Class<F>) set.font().getClass(), (F) set.font());
    }

    public Set<FontName> availableFonts() {
        Set<FontName> all = new LinkedHashSet<>();
        all.addAll(fonts.keySet());
        all.addAll(fontFactories.keySet());
        return Collections.unmodifiableSet(all);
    }

    /**
     * @param fontName
     * @return
     */
    @Override
    public PdfFont getPdfFont(FontName fontName) {
        return getFont(fontName, PdfFont.class).orElseThrow();
    }
}