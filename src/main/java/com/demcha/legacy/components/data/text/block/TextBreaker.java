package com.demcha.legacy.components.data.text.block;

import com.demcha.legacy.components.data.text.TextLines;
import org.apache.pdfbox.pdmodel.font.PDFont;

import java.io.IOException;

public final class TextBreaker {
    private TextBreaker() {}

    public static TextLines breakIntoLines(String text, PDFont font, float fontSize, double wrapWidth, double lineSpacingFactor) {
        if (text == null || text.isEmpty()) return TextLines.empty();
        String[] words = text.split("\\s+");
        java.util.List<String> lines = new java.util.ArrayList<>();
        StringBuilder cur = new StringBuilder();

        float ascent = font.getFontDescriptor().getCapHeight() / 1000f * fontSize;
        float descent = Math.abs(font.getFontDescriptor().getDescent()) / 1000f * fontSize;
        float baseLineHeight = ascent + descent; // базовая высота строки
        float lineHeight = Math.max(baseLineHeight, baseLineHeight * (float) lineSpacingFactor);

        float maxWidth = 0;
        float maxW = (float) wrapWidth;

        for (String w : words) {
            String candidate = cur.isEmpty() ? w : cur + " " + w;
            float candidateWidth = 0;
            try {
                candidateWidth = font.getStringWidth(candidate) / 1000f * fontSize;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            if (candidateWidth <= maxW) {
                cur.setLength(0);
                cur.append(candidate);
                maxWidth = Math.max(maxWidth, candidateWidth);
            } else {
                // Перенос: текущую строку завершаем, новая начинает с w
                if (!cur.isEmpty()) {
                    lines.add(cur.toString());
                    cur.setLength(0);
                }
                // Если слово само шире строки — (простой вариант) кладём целиком на строку
                // TODO: добавить переносы по слогам/дефисам при необходимости
                float wWidth = 0;
                try {
                    wWidth = font.getStringWidth(w) / 1000f * fontSize;
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                if (wWidth > maxW) {
                    lines.add(w); // грубо, но просто. можно хитрее: резать по символам с добавлением '-'
                    maxWidth = Math.max(maxWidth, wWidth);
                } else {
                    cur.append(w);
                    maxWidth = Math.max(maxWidth, wWidth);
                }
            }
        }
        if (!cur.isEmpty()) lines.add(cur.toString());

        return new TextLines(lines, lineHeight, ascent, descent, maxWidth);
    }
}
