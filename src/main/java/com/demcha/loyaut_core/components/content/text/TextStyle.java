package com.demcha.loyaut_core.components.content.text;

import com.demcha.font_library.FontName;
import com.demcha.loyaut_core.components.core.Component;
import lombok.Builder;

import java.awt.*;

@Builder
public record TextStyle(FontName fontName, double size, TextDecoration decoration, Color color) implements Component {
public static  TextStyle DEFAULT_STYLE = new TextStyle(FontName.HELVETICA, 14, TextDecoration.DEFAULT, Color.BLACK);
}

