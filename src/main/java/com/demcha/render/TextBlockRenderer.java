package com.demcha.render;

import com.demcha.components.data.text.TextBlock;
import com.demcha.components.data.text.TextLines;
import com.demcha.core.Element;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import java.io.IOException;

public final class TextBlockRenderer {
    public static boolean supports(Element e) {
        return e.has(TextBlock.class) && e.has(TextLines.class) && e.has(com.demcha.components.Position.class);
    }

    public static void render(Element e, PdfRenderContext ctx) throws IOException {
        var tb = e.get(TextBlock.class).orElseThrow();
        var lines = e.get(TextLines.class).orElseThrow();
        var pos = e.get(com.demcha.components.Position.class).orElseThrow();

        var cs = ctx.getContentStream();
        PDFont font = tb.style().font();
        float fontSize = tb.style().size();

        float x = (float) pos.x();
        float yTop = (float) pos.y(); // предполагаем, что Position — baseline первой строки ИЛИ верх (ниже отдельный вариант)

        cs.setNonStrokingColor(tb.style().color());
        cs.setFont(font, fontSize);

        // ВАЖНО: если твой Arrange даёт top-left, а PDF — bottom-left,
        // то тут переведи Y: float y = ctx.toPdfY(yTop);
        float y = yTop;

        for (int i = 0; i < lines.lines().size(); i++) {
            String line = lines.lines().get(i);

            float baseline = y - i * lines.lineHeight(); // если Position — верх блока
            // Если Position — baseline первой строки, то baseline = y + lines.ascent() - i*lineHeight

            cs.beginText();
            cs.newLineAtOffset(x, baseline);
            cs.showText(line);
            cs.endText();
        }
    }
}

