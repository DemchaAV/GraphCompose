package com.demcha.components.content.text;

import com.demcha.components.content.Box;
import com.demcha.components.core.Component;
import com.demcha.components.core.Entity;
import com.demcha.components.geometry.Size;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.io.IOException;
import java.util.Optional;

public record Text(String text, TextStyle textStyle) implements Box, Component {

    public static Text of(String text) {
        return  new Text(text, TextStyle.standard14("HELVETICA", 15, TextDecoration.DEFAULT));
    }


    public static <T extends Text> Size autoMeasureText(T textComponent) throws IOException {
        double width = textComponent.textStyle().getTextWidth(textComponent.text());
        double height = textComponent.textStyle().font().getFontDescriptor().getCapHeight();
        return new Size(width, height);
    }

    public Optional<Size> autoMeasureText() throws IOException {
        double width = textStyle.getTextWidth(text);
        double height = textStyle.getTextHeight(text);
        return Optional.of(new Size(width, height));
    }
}
