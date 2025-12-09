package com.demcha.font_library;

import com.demcha.loyaut_core.system.implemented_systems.pdf_systems.PdfFont;
import com.demcha.loyaut_core.system.implemented_systems.pdf_systems.PdfFontGetter;
import com.demcha.loyaut_core.system.interfaces.Font;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class FontLibrary implements PdfFontGetter {

    private final Map<FontName, Map<Class<?>, Font<?>>> fonts = new HashMap<>();

    /**
     * Получаем конкретный тип шрифта: PdfFont, WordFont и т.д.
     */
    public <F extends Font<?>> Optional<F> getFont(FontName fontName, Class<F> fontClass) {
        if (FontName.DEFAULT.equals(fontName)){
            fontName = FontName.HELVETICA;
        }
        var fontRegistry = fonts.get(fontName);
        if (fontRegistry == null) {
            return Optional.empty();
        }


        Font<?> genericFont = fontRegistry.get(fontClass);
        if (fontClass.isInstance(genericFont)) {
            return Optional.of(fontClass.cast(genericFont));
        }

        return Optional.empty();
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
