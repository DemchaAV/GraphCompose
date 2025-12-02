package com.demcha.loyaut_core.components.content.text;

import com.demcha.loyaut_core.components.core.Component;

// Кэш разложенных строк + метрики
public record TextLines(
        java.util.List<String> lines,
        float lineHeight,     // высота строки (cap/descent/leading)
        float ascent,         // cap
        float descent,        // |descent|
        float maxLineWidth    // максимальная ширина среди всех строк
) implements Component {
    public static TextLines empty() {
        return new TextLines(java.util.List.of(), 0, 0, 0, 0);
    }
}
