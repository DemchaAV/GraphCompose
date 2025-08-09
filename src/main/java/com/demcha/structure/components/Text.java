package com.demcha.structure.components;

import com.demcha.structure.Element;
import com.demcha.structure.TextDecoration;
import com.demcha.structure.interfaces.UiElement;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.font.PDFont;


@AllArgsConstructor
@RequiredArgsConstructor
public class Text implements Element {
    private final String key;
    private final String text;
    private boolean defaultSize = true;
    private TextDecoration decoration = TextDecoration.DEFAULT;
    private int fontSize;
    private PDFont font;


}
