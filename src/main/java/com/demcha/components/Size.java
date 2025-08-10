package com.demcha.components;

import com.demcha.components.data.text.TextData;
import com.demcha.core.Component;
import com.demcha.core.Element;

public record Size(double width, double height) implements Component {

    public static Size textAutoSize(Element element) {


        return element.get(TextData.class).map((textData) -> {

            float textWidth = textData.style().getTextWidth(textData.value());
            float textHigh = textData.style().font().getFontDescriptor().getCapHeight();
            return new Size(textWidth, textHigh);
        }).orElse(null);

    }
}
