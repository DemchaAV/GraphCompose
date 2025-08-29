package com.demcha.legacy.components.data.text;

import com.demcha.components.content.text.TextStyle;
import com.demcha.components.geometry.OuterBoxSize;
import com.demcha.components.core.Component;
import com.demcha.components.content.Element;

import java.io.IOException;


public record TextData(String value, TextStyle style) implements Component {
   public static void autoMeasureText(Element e) throws IOException {
        e.get(TextData.class).ifPresent(td -> {
            double textWidth = td.style.getTextWidth(td.value);
            double textHeight = td.style.font().getFontDescriptor().getCapHeight();

            e.add(new OuterBoxSize(textWidth, textHeight));

        });
    }



}

