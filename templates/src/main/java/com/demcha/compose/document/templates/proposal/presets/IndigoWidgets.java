package com.demcha.compose.document.templates.proposal.presets;

import com.demcha.compose.document.dsl.ShapeContainerBuilder;
import com.demcha.compose.document.dsl.SpacerBuilder;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.style.DocumentColor;

import java.util.List;

import static com.demcha.compose.document.templates.proposal.presets.IndigoFlow.glyph;

/**
 * The two filled shapes the Indigo proposal repeats: the disc a header tile
 * opens with and the rounded tile a feature opens with.
 *
 * <p>Both are drawn whether or not the document names a mark for them. The shape
 * is the design's; the mark inside it is the document's, and a tile that names
 * none is still the tile the design draws.</p>
 */
final class IndigoWidgets {

    private IndigoWidgets() {
    }

    /**
     * A filled disc carrying one of the header's marks.
     *
     * @param name     the disc's name
     * @param fill     the disc's colour
     * @param token    the mark's token, or blank for a disc that carries none
     * @param diameter the disc's diameter
     * @param markSize how large to draw the mark
     * @return the disc
     */
    static DocumentNode disc(String name, DocumentColor fill, String token,
                             double diameter, double markSize) {
        return new ShapeContainerBuilder()
                .name(name)
                .circle(diameter)
                .fillColor(fill)
                .center(mark(name, token, IndigoIcons.HEADER_TOKENS, markSize))
                .build();
    }

    /**
     * A filled rounded tile carrying one of the band's marks.
     *
     * @param name     the tile's name
     * @param fill     the tile's colour
     * @param token    the mark's token, or blank for a tile that carries none
     * @param size     the tile's side
     * @param radius   the tile's corner radius
     * @param markSize how large to draw the mark
     * @return the tile
     */
    static DocumentNode tile(String name, DocumentColor fill, String token,
                             double size, double radius, double markSize) {
        return new ShapeContainerBuilder()
                .name(name)
                .roundedRect(size, size, radius)
                .fillColor(fill)
                .center(mark(name, token, IndigoIcons.BAND_TOKENS, markSize))
                .build();
    }

    /**
     * What goes inside the shape: the mark the document names, or the space it
     * would have taken.
     *
     * <p>A shape with no layer at all is refused, so a shape that carries no
     * mark is centred on the mark's own space instead — the design's shape is
     * drawn either way, and a token this block cannot draw is still a data error
     * reported by name rather than an empty tile.</p>
     */
    private static DocumentNode mark(String name, String token, List<String> allowed,
                                     double markSize) {
        return IndigoIcons.has(token)
                ? glyph(token, allowed, markSize)
                : new SpacerBuilder().name(name + "Space").size(markSize, markSize).build();
    }
}
