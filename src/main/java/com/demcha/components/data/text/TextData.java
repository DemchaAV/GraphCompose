package com.demcha.components.data.text;

import com.demcha.components.Size;
import com.demcha.core.Component;
import com.demcha.core.Element;

import java.io.IOException;


public record TextData(String value, TextStyle style) implements Component {
   public static void autoMeasureText(Element e) throws IOException {
        e.get(TextData.class).ifPresent(td -> {
            float textWidth = td.style.getTextWidth(td.value);
            float textHeight = td.style.font().getFontDescriptor().getCapHeight();

            e.add(new Size(textWidth, textHeight));

        });
    }



}

