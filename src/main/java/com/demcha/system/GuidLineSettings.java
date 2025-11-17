package com.demcha.system;

import com.demcha.components.content.shape.Stroke;
import lombok.Data;
import lombok.experimental.Accessors;

import java.awt.*;
@Data
@Accessors(fluent = true)
public class GuidLineSettings {
    /**
     * The opacity for all guide elements. Value between 0.0f (transparent) and 1.0f (opaque).
     */
    float GUIDES_OPACITY = 0.8f;
    // --- Margin Guide ---
    Color MARGIN_COLOR = new Color(0, 110, 255);
    com.demcha.components.content.shape.Stroke MARGIN_STROKE = new com.demcha.components.content.shape.Stroke(0.5);
    // --- Padding Guide ---
    Color PADDING_COLOR = new Color(255, 140, 0);
    com.demcha.components.content.shape.Stroke PADDING_STROKE = new com.demcha.components.content.shape.Stroke(0.5);
    // --- Content Box Guide ---
    Color BOX_COLOR = new Color(150, 150, 150); // Using a slightly lighter gray
    com.demcha.components.content.shape.Stroke BOX_STROKE = new Stroke(1.0);
    //viability
    boolean showOnlySetGuide = true;
}
