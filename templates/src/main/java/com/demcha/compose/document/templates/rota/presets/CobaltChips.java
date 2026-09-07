package com.demcha.compose.document.templates.rota.presets;

import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.dsl.ShapeContainerBuilder;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.templates.data.rota.ShiftEmphasis;
import com.demcha.compose.document.templates.data.rota.ShiftStatus;

import static com.demcha.compose.document.templates.rota.presets.CobaltStyles.CHIP_CORNER_RADIUS;
import static com.demcha.compose.document.templates.rota.presets.CobaltStyles.CHIP_ON_LIGHT;
import static com.demcha.compose.document.templates.rota.presets.CobaltStyles.CHIP_OUTLINE_WIDTH;
import static com.demcha.compose.document.templates.rota.presets.CobaltStyles.CHIP_SINGLE_ON_LIGHT;
import static com.demcha.compose.document.templates.rota.presets.CobaltStyles.PAPER;
import static com.demcha.compose.document.templates.rota.presets.CobaltStyles.colorFor;
import static com.demcha.compose.document.templates.rota.presets.CobaltStyles.labelOnFillFor;

/**
 * The sheet's one repeated shape: the chip a cell of the rota is drawn as.
 *
 * <p>Three forms, and the third is the reason this is a shape at all.
 * {@link ShiftEmphasis#STRONG} fills with the status's colour and labels it in
 * that status's own contrast colour; {@link ShiftEmphasis#SOFT} keeps the paper
 * and hairlines it in the same colour; {@link ShiftEmphasis#PLAIN} draws
 * neither and is the same box carrying only text. A box rather than a bare
 * paragraph, so an unmarked entry occupies exactly the height a chip does and
 * the two seat alike in a row that mixes them.</p>
 */
final class CobaltChips {

    private CobaltChips() {
    }

    /**
     * One chip.
     *
     * @param text    what the chip prints
     * @param status  what the entry means, which decides the colour
     * @param emphasis how loudly it is drawn
     * @param width   how wide its column leaves it
     * @param height  how tall to draw it — a lone entry is taller than one of a
     *                stacked pair
     * @param lone    whether it is the only entry in its cell, which decides the
     *                face: a pair keeps the smaller one, because two chips and a
     *                gap have to fit the block a single chip fills
     * @param rowFill the row's own background, which a plain chip takes
     * @return the chip
     */
    static DocumentNode chip(String text, ShiftStatus status, ShiftEmphasis emphasis,
                             double width, double height, boolean lone,
                             DocumentColor rowFill) {
        boolean filled = emphasis == ShiftEmphasis.STRONG;
        boolean plain = emphasis == ShiftEmphasis.PLAIN;
        DocumentColor colour = colorFor(status);
        DocumentTextStyle label = filled
                ? labelOnFillFor(status, lone)
                : (lone ? CHIP_SINGLE_ON_LIGHT : CHIP_ON_LIGHT);

        // A plain entry is the absence of a chip, so it takes the row's own
        // background; a soft one keeps the paper, because a white box hairlined
        // in its status colour is what it is.
        ShapeContainerBuilder chip = new ShapeContainerBuilder()
                .name("Chip")
                .roundedRect(width, height, CHIP_CORNER_RADIUS)
                .fillColor(filled ? colour : (plain ? rowFill : PAPER));
        if (!filled && !plain) {
            chip.stroke(DocumentStroke.of(colour, CHIP_OUTLINE_WIDTH));
        }
        return chip.center(label(text, label, width)).build();
    }

    /**
     * A chip's label, inset inside the box.
     *
     * <p>The inset is not decoration: a shape container clamps rather than
     * centres a child wider than itself, so a label kept narrower than its chip
     * is what makes a long one centre instead of sitting against the left
     * edge.</p>
     *
     * @param text      what the label prints
     * @param style     the face it is set in
     * @param chipWidth the chip it has to fit inside
     * @return the label
     */
    static DocumentNode label(String text, DocumentTextStyle style, double chipWidth) {
        return new ParagraphBuilder()
                .name("ChipLabel")
                .text(text)
                .textStyle(style)
                .align(TextAlign.CENTER)
                .lineSpacing(0)
                .margin(new DocumentInsets(0, chipWidth * 0.04, 0, chipWidth * 0.04))
                .build();
    }
}
