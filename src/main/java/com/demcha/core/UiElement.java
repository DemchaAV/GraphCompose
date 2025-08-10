package com.demcha.core;

import com.demcha.components.*;
import com.demcha.layout.AnchorType;
import com.demcha.layout.Offsettable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.ToString;

/**
 * ┌───────────────────────────────────────────┐   ← контейнер (list, canvas)
 * │   margin                                  │
 * │   ┌───────────────┐                       │
 * │   │   padding     │                       │
 * │   │   ● (x,y)     │ ← offset влияет здесь │
 * │   │               │                       │
 * │   └───────────────┘                       │
 * └───────────────────────────────────────────┘
 */
@Builder
@AllArgsConstructor
@ToString
public class UiElement{

    private double x, y;                  // базовая позиция
    private double offsetX, offsetY;      // смещение (скролл/анимации)
    private double scaleX = 1.0, scaleY = 1.0;
    private double rotation;              // угол
    private int margin, padding;          // отступы
    private int layer;                    // z-index
    private AnchorType anchor = AnchorType.TOP_LEFT;

    private double width, height;         // полезно для якоря


    public UiElement() {
        // Положение по умолчанию
        this.x = 0;
        this.y = 0;
        this.offsetX = 0;
        this.offsetY = 0;
        this.scaleX = 1.0;
        this.scaleY = 1.0;
        this.rotation = 0;
        this.margin = 0;
        this.padding = 0;
        this.layer = 0;
        this.anchor = AnchorType.TOP_LEFT;
        this.width = 0;
        this.height = 0;
    }

}

