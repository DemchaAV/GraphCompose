package com.demcha.compose.font_library;

import com.demcha.compose.loyaut_core.system.implemented_systems.pdf_systems.PdfFont;
import com.demcha.compose.loyaut_core.system.implemented_systems.pdf_systems.PdfFontGetter;
import com.demcha.compose.loyaut_core.system.interfaces.Font;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class FontLibrary implements PdfFontGetter {

    private final Map<FontName, Map<Class<?>, Font<?>>> fonts = new HashMap<>();

    /**
     * Получаем конкретный тип шрифта: PdfFont, WordFont и т.д.
     */
    public <F extends Font<?>> Optional<F> getFont(FontName fontName, Class<F> fontClass) {
        FontName resolvedName = resolveBaseFont(fontName);
        var fontRegistry = fonts.get(resolvedName);
        if (fontRegistry == null) {
            return Optional.empty();
        }


        Font<?> genericFont = fontRegistry.get(fontClass);
        if (fontClass.isInstance(genericFont)) {
            return Optional.of(fontClass.cast(genericFont));
        }

        return Optional.empty();
    }

    private FontName resolveBaseFont(FontName fontName) {
        if (fontName == null || FontName.DEFAULT.equals(fontName)) {
            return FontName.HELVETICA;
        }
        return switch (fontName) {
            case HELVETICA, HELVETICA_BOLD, HELVETICA_OBLIQUE, HELVETICA_BOLD_OBLIQUE -> FontName.HELVETICA;
            case TIMES_ROMAN, TIMES_BOLD, TIMES_ITALIC, TIMES_BOLD_ITALIC -> FontName.TIMES_ROMAN;
            case COURIER, COURIER_BOLD, COURIER_OBLIQUE, COURIER_BOLD_OBLIQUE -> FontName.COURIER;
            default -> fontName;
        };
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
        fonts.computeIfAbsent(name, k -> new HashMap<>())
                .put(fontClass, font);
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

    /**
     * @param fontName
     * @return
     */
    @Override
    public PdfFont getPdfFont(FontName fontName) {
        return getFont(fontName, PdfFont.class).orElseThrow();
    }
}
