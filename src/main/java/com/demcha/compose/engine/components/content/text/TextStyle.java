package com.demcha.compose.engine.components.content.text;

import com.demcha.compose.font.FontName;
import com.demcha.compose.engine.components.core.Component;
import lombok.Builder;

import java.awt.*;

@Builder
public record TextStyle(FontName fontName, double size, TextDecoration decoration, Color color) implements Component {
public static  TextStyle DEFAULT_STYLE = new TextStyle(FontName.HELVETICA, 14, TextDecoration.DEFAULT, Color.BLACK);
}

