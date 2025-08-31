package com.demcha.components.layout;

import com.demcha.components.core.Component;

/**
 * Align = like Anchor, but used when local Position is absent.
 * If Position is present, Align acts as a base; Position is an offset from that base.
 */
public record Align(HAnchor h, VAnchor v, double spacing) implements Component {
    public static Align middle(double spacing){
        var v = VAnchor.MIDDLE;
        var h = HAnchor.CENTER;
        return new Align(h,v,spacing);
    }
    public static Align top(double spacing){
        var v = VAnchor.TOP;
        var h = HAnchor.CENTER;
        return new Align(h,v,spacing);
    }
    public static Align bottom(double spacing){
        var v = VAnchor.BOTTOM;
        var h = HAnchor.CENTER;
        return new Align(h,v,spacing);
    }
    public static Align defaultAlign(double spacing){
        var v = VAnchor.DEFAULT;
        var h = HAnchor.DEFAULT;
        return new Align(h,v,spacing);
    }
}

