package com.demcha.compose.engine.components.content.text;

import com.demcha.compose.font.FontName;
import lombok.Builder;

import java.awt.*;

@Builder
public record TextStyle(FontName fontName, double size, TextDecoration decoration, Color color) {
public static  TextStyle DEFAULT_STYLE = new TextStyle(FontName.HELVETICA, 14, TextDecoration.DEFAULT, Color.BLACK);
}

