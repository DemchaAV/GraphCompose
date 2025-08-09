package com.demcha.structure.interfaces;

import com.demcha.structure.Anchor;
import com.demcha.structure.interfaces.ui.*;
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
public class UiElement
        implements Positionable, Offsettable, Transformable, BoxModel, Layered, Anchorable {

    private double x, y;                  // базовая позиция
    private double offsetX, offsetY;      // смещение (скролл/анимации)
    private double scaleX = 1.0, scaleY = 1.0;
    private double rotation;              // угол
    private int margin, padding;          // отступы
    private int layer;                    // z-index
    private Anchor anchor = Anchor.TOP_LEFT;

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
        this.anchor = Anchor.TOP_LEFT;
        this.width = 0;
        this.height = 0;
    }

    // ===== Positionable =====
    @Override
    public double getX() {
        return x;
    }

    @Override
    public double getY() {
        return y;
    }

    @Override
    public void setPosition(double x, double y) {
        this.x = x;
        this.y = y;
    }

    // ===== Offsettable =====
    @Override
    public double getOffsetX() {
        return offsetX;
    }

    @Override
    public double getOffsetY() {
        return offsetY;
    }

    @Override
    public void setOffset(double dx, double dy) {
        this.offsetX = dx;
        this.offsetY = dy;
    }

    // ===== Transformable =====
    @Override
    public double getScaleX() {
        return scaleX;
    }

    @Override
    public double getScaleY() {
        return scaleY;
    }

    @Override
    public double getRotation() {
        return rotation;
    }

    @Override
    public void setRotation(double angle) {
        this.rotation = angle;
    }

    @Override
    public void setScale(double sx, double sy) {
        this.scaleX = sx;
        this.scaleY = sy;
    }

    // ===== BoxModel =====
    @Override
    public int getMargin() {
        return margin;
    }

    @Override
    public void setMargin(int margin) {
        this.margin = margin;
    }

    @Override
    public int getPadding() {
        return padding;
    }

    @Override
    public void setPadding(int padding) {
        this.padding = padding;
    }

    // ===== Layered =====
    @Override
    public int getLayer() {
        return layer;
    }

    @Override
    public void setLayer(int layer) {
        this.layer = layer;
    }

    // ===== Anchorable =====
    @Override
    public Anchor getAnchor() {
        return anchor;
    }

    @Override
    public void setAnchor(Anchor anchor) {
        this.anchor = anchor;
    }

    // Размеры + хелперы
    public void setSize(double w, double h) {
        this.width = w;
        this.height = h;
    }

    public double getWidth() {
        return width;
    }

    public double getHeight() {
        return height;
    }

    /**
     * Финальная позиция с учётом anchor, margin и offset (без учёта скролла контейнера).
     */
    public double[] computeFinalPosition() {
        double ax = 0, ay = 0;
        switch (anchor) {
            case TOP_LEFT -> {
                ax = 0;
                ay = 0;
            }
            case TOP_CENTER -> {
                ax = -width / 2;
                ay = 0;
            }
            case TOP_RIGHT -> {
                ax = -width;
                ay = 0;
            }
            case CENTER_LEFT -> {
                ax = 0;
                ay = -height / 2;
            }
            case CENTER -> {
                ax = -width / 2;
                ay = -height / 2;
            }
            case CENTER_RIGHT -> {
                ax = -width;
                ay = -height / 2;
            }
            case BOTTOM_LEFT -> {
                ax = 0;
                ay = -height;
            }
            case BOTTOM_CENTER -> {
                ax = -width / 2;
                ay = -height;
            }
            case BOTTOM_RIGHT -> {
                ax = -width;
                ay = -height;
            }
        }
        double finalX = x + offsetX + ax + margin;
        double finalY = y + offsetY + ay + margin;
        return new double[]{finalX, finalY};
    }
}

