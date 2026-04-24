package com.demcha.compose.engine.components.components_builders;

import com.demcha.compose.engine.components.containers.abstract_builders.EmptyBox;
import com.demcha.compose.engine.components.content.barcode.BarcodeData;
import com.demcha.compose.engine.components.content.barcode.BarcodeType;
import com.demcha.compose.engine.components.renderable.BarcodeComponent;
import com.demcha.compose.engine.components.style.ComponentColor;
import com.demcha.compose.engine.core.EntityManager;
import lombok.extern.slf4j.Slf4j;

import java.awt.Color;

/**
 * Builder for barcode/QR-code leaf entities.
 *
 * <p>Barcodes are fixed-size leaves rendered as bitmap images at draw time.
 * The builder follows the same contract as {@link ImageBuilder} and
 * {@link CircleBuilder}: extend {@link EmptyBox}, attach a renderable marker
 * and content, register via {@code build()}.</p>
 *
 * <h3>Usage</h3>
 * <pre>
 * cb.barcode()
 *     .data("https://example.com/invoice/12345")
 *     .type(BarcodeType.QR_CODE)
 *     .size(120, 120)
 *     .anchor(Anchor.topRight())
 *     .build();
 * </pre>
 *
 * @author Artem Demchyshyn
 */
@Slf4j
public class BarcodeBuilder extends EmptyBox<BarcodeBuilder> {

    private String content;
    private BarcodeType type = BarcodeType.QR_CODE;
    private Color foreground = Color.BLACK;
    private Color background = Color.WHITE;
    private int quietZoneMargin = 0;

    BarcodeBuilder(EntityManager entityManager) {
        super(entityManager);
    }

    /**
     * Sets the data content to encode in the barcode.
     *
     * @param content the string payload (URL, product code, etc.)
     * @return this builder
     */
    public BarcodeBuilder data(String content) {
        this.content = content;
        return this;
    }

    /**
     * Sets the barcode symbology type.
     *
     * @param type the barcode type, for example {@link BarcodeType#QR_CODE}
     *             or {@link BarcodeType#CODE_128}
     * @return this builder
     */
    public BarcodeBuilder type(BarcodeType type) {
        this.type = type;
        return this;
    }

    /**
     * Sets a QR Code barcode type. Convenience shorthand for
     * {@code type(BarcodeType.QR_CODE)}.
     */
    public BarcodeBuilder qrCode() {
        return type(BarcodeType.QR_CODE);
    }

    /**
     * Sets a Code 128 barcode type.
     */
    public BarcodeBuilder code128() {
        return type(BarcodeType.CODE_128);
    }

    /**
     * Sets a Code 39 barcode type.
     */
    public BarcodeBuilder code39() {
        return type(BarcodeType.CODE_39);
    }

    /**
     * Sets an EAN-13 barcode type.
     */
    public BarcodeBuilder ean13() {
        return type(BarcodeType.EAN_13);
    }

    /**
     * Sets an EAN-8 barcode type.
     */
    public BarcodeBuilder ean8() {
        return type(BarcodeType.EAN_8);
    }

    /**
     * Sets the foreground (bar) color.
     *
     * @param foreground the bar color
     * @return this builder
     */
    public BarcodeBuilder foreground(Color foreground) {
        this.foreground = foreground;
        return this;
    }

    /**
     * Sets the foreground (bar) color from a {@link ComponentColor}.
     */
    public BarcodeBuilder foreground(ComponentColor foreground) {
        return foreground(foreground.color());
    }

    /**
     * Sets the background (space) color.
     *
     * @param background the background color
     * @return this builder
     */
    public BarcodeBuilder background(Color background) {
        this.background = background;
        return this;
    }

    /**
     * Sets the background (space) color from a {@link ComponentColor}.
     */
    public BarcodeBuilder background(ComponentColor background) {
        return background(background.color());
    }

    /**
     * Sets the quiet zone margin in barcode modules.
     *
     * @param margin number of modules for the quiet zone (0 = no extra margin)
     * @return this builder
     */
    public BarcodeBuilder quietZone(int margin) {
        this.quietZoneMargin = Math.max(0, margin);
        return this;
    }

    @Override
    public void initialize() {
        entity.addComponent(new BarcodeComponent());
    }

    @Override
    public com.demcha.compose.engine.components.core.Entity build() {
        if (content == null || content.isEmpty()) {
            throw new IllegalStateException("BarcodeBuilder requires data content before build()");
        }
        entity.addComponent(BarcodeData.of(content, type, foreground, background, quietZoneMargin));
        return registerBuiltEntity();
    }
}
