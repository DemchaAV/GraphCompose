package com.demcha.compose.engine.components.content.barcode;

import com.demcha.compose.engine.components.core.Component;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.awt.Color;
import java.util.Objects;

/**
 * Content component that carries the data needed to generate a barcode image.
 *
 * <p>This is a backend-neutral marker attached to an entity. The actual bitmap
 * generation is deferred to the PDF render handler at draw time.</p>
 *
 * @author Artem Demchyshyn
 */
@Getter
@ToString
@EqualsAndHashCode
public final class BarcodeData implements Component {

    private final String content;
    private final BarcodeType type;
    private final Color foreground;
    private final Color background;
    private final int margin;

    private BarcodeData(String content, BarcodeType type, Color foreground, Color background, int margin) {
        this.content = Objects.requireNonNull(content, "barcode content");
        this.type = Objects.requireNonNull(type, "barcode type");
        this.foreground = foreground;
        this.background = background;
        this.margin = margin;
    }

    /**
     * Creates barcode data with default black-on-white colors.
     */
    public static BarcodeData of(String content, BarcodeType type) {
        return new BarcodeData(content, type, Color.BLACK, Color.WHITE, 0);
    }

    /**
     * Creates barcode data with custom colors.
     */
    public static BarcodeData of(String content, BarcodeType type, Color foreground, Color background) {
        return new BarcodeData(content, type, foreground, background, 0);
    }

    /**
     * Creates barcode data with custom colors and quiet zone margin (in barcode modules).
     */
    public static BarcodeData of(String content, BarcodeType type, Color foreground, Color background, int margin) {
        return new BarcodeData(content, type, foreground, background, margin);
    }
}
