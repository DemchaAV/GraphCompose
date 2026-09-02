package com.demcha.compose.document.templates.proposal.presets;

import com.demcha.compose.document.dsl.LineBuilder;
import com.demcha.compose.document.dsl.RowBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.HorizontalAlign;
import com.demcha.compose.document.node.LayerAlign;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;

import java.util.List;
import java.util.function.Consumer;

import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.RULE_SOFT;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.RULE_THIN;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.px;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.toPx;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.topBearing;

/**
 * A running cursor down a column, and the small shapes the sheet repeats.
 *
 * <p>The design is a map of cap tops rather than a stack of gaps. A cursor
 * remembers how far down the column the layout has reached, and each block asks
 * for the margin that puts its own cap top where the map says — so a block moves
 * by changing one number in {@link IndigoStyles}, and the blocks around it do
 * not have to be re-derived.</p>
 *
 * <p>A block whose content ran past where the map expected it leaves the cursor
 * below the next block's stated position, and the margin that would put that
 * block back where the map says is a negative one — which would draw it over
 * what overran. Every margin is therefore floored at nothing: heavier content
 * pushes what follows down, onto a second page if it comes to that, instead of
 * printing on top of it.</p>
 */
final class IndigoFlow {

    private double bottomPx;

    /**
     * A cursor starting at a design y.
     *
     * @param originPx where the column begins, in design pixels
     */
    IndigoFlow(double originPx) {
        this.bottomPx = originPx;
    }

    /**
     * The top margin that puts a text box's caps at a design y.
     *
     * <p>A text box starts above its own first cap by the face's top bearing,
     * which is why a cap top and a box top are not the same number.</p>
     *
     * @param capTopPx where the design puts the cap top
     * @param size     the type size
     * @param bold     whether the type is bold, which has its own bearing
     * @return the margin to set on the block, never less than nothing
     */
    double capAt(double capTopPx, double size, boolean bold) {
        double boxTopPx = capTopPx - toPx(topBearing(size, bold));
        double margin = px(boxTopPx - bottomPx);
        bottomPx = boxTopPx + toPx(size);
        return Math.max(0, margin);
    }

    /**
     * The top margin for a box with no bearing: a rule, a tile, an image, or a
     * nested block that measures its own height.
     *
     * @param topPx    where the design puts the box's top
     * @param heightPx how tall the box is, in design pixels
     * @return the margin to set on the block, never less than nothing
     */
    double boxAt(double topPx, double heightPx) {
        double margin = px(topPx - bottomPx);
        bottomPx = topPx + heightPx;
        return Math.max(0, margin);
    }

    /**
     * Declares where a block that measured its own height has reached — a
     * wrapped paragraph, whose line count the map states rather than the engine.
     *
     * @param reachedPx the design y the block ends at
     */
    void advanceTo(double reachedPx) {
        this.bottomPx = reachedPx;
    }

    // ------------------------------------------------------------------
    // The shapes the sheet repeats
    // ------------------------------------------------------------------

    /**
     * A row wrapped in a layer, so it can sit inside a row cell.
     *
     * <p>A row nested in a row cell is refused by the layout compiler, and
     * through a section too. A row wrapped in a LayerStack layer lays out
     * horizontally there.</p>
     *
     * @param parent the section the row belongs to
     * @param name   the row's name, which its holder and layer extend
     * @param spec   the row itself
     */
    static void layeredRow(SectionBuilder parent, String name, Consumer<RowBuilder> spec) {
        SectionBuilder holder = new SectionBuilder();
        holder.name(name + "Holder");
        holder.addRow(name, spec);
        DocumentNode node = holder.build();
        parent.addLayerStack(stack -> stack
                .name(name + "Layer")
                .layer(node, LayerAlign.TOP_LEFT, 0));
    }

    /**
     * A horizontal rule of the sheet's own weight.
     *
     * @param line  the line being built
     * @param name  the rule's name
     * @param width how wide it runs
     * @param color its colour
     * @return the line, for chaining
     */
    static LineBuilder rule(LineBuilder line, String name, double width, DocumentColor color) {
        return line.name(name).horizontal(width).thickness(RULE_THIN).color(color);
    }

    /**
     * One of the two hairlines that split the sheet's halves.
     *
     * @param row                the row the hairline is a cell of
     * @param name               the hairline's name
     * @param offsetFromRowTopPx how far below the row's top it starts
     * @param heightPx           how far it runs
     */
    static void columnDivider(RowBuilder row, String name, double offsetFromRowTopPx,
                              double heightPx) {
        row.addSection(name, cell -> {
            cell.spacing(0);
            cell.addLine(line -> line
                    .name(name + "Rule")
                    .vertical(px(heightPx))
                    .thickness(RULE_THIN)
                    .color(RULE_SOFT)
                    .margin(new DocumentInsets(px(offsetFromRowTopPx), 0, 0, 0)));
        });
    }

    /**
     * A packaged mark, boxed so it can be centred inside a disc or a tile.
     *
     * @param token   the mark's token
     * @param allowed the set the block draws from
     * @param size    how large to draw it
     * @return the boxed mark
     */
    static DocumentNode glyph(String token, List<String> allowed, double size) {
        SectionBuilder holder = new SectionBuilder();
        holder.name(token + "Glyph").spacing(0);
        holder.addSvgIcon(IndigoIcons.icon(token, allowed), size, HorizontalAlign.CENTER);
        return holder.build();
    }
}
