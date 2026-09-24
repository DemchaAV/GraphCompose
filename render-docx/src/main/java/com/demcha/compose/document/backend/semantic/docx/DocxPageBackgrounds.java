package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.document.layout.LayoutGraph;
import com.demcha.compose.document.layout.PlacedFragment;
import com.demcha.compose.document.layout.payloads.ShapeFragmentPayload;
import org.apache.poi.util.Units;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlOptions;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTDrawing;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * A session's page backgrounds as shapes behind the text of every page.
 *
 * <p>{@code DocumentSession.pageBackgrounds(...)} paints fills under everything on every page —
 * a sidebar CV's dark column, an invoice's tinted band — and the export wrote none of them: the
 * sidebar's white text stood on a white page, all but invisible. Word paints a page only in one
 * colour, but a shape anchored to the page in a header is drawn on every page that header is
 * shown on, behind the body's text. Each fill is such a shape: a rectangle positioned from the
 * page's edges, with no outline, in the order the page paints them.</p>
 *
 * <p>The fills are read from the layout, where they are fragments of every page under a path of
 * their own; every page carries the same set, so the first page's is the set.</p>
 */
final class DocxPageBackgrounds {

    /** The path prefix the layout gives the first page's background fragments. */
    private static final String FIRST_PAGE = "@page-background[0][";

    private DocxPageBackgrounds() {
    }

    /**
     * One page background, measured from the page's top-left corner.
     *
     * @param x      from the page's left edge, in points
     * @param top    from the page's top edge, in points
     * @param width  in points
     * @param height in points
     * @param color  the fill, its alpha included
     */
    record Fill(double x, double top, double width, double height, Color color) {
    }

    /**
     * The page backgrounds a layout paints, in the order it paints them.
     *
     * @param layout the section's layout, or {@code null}
     * @return the fills, empty when there are none
     */
    static List<Fill> of(LayoutGraph layout) {
        if (layout == null) {
            return List.of();
        }
        double pageHeight = layout.canvas().height();
        List<Fill> fills = new ArrayList<>();
        for (PlacedFragment fragment : layout.fragments()) {
            if (fragment.path() != null && fragment.path().startsWith(FIRST_PAGE)
                && fragment.payload() instanceof ShapeFragmentPayload shape
                && shape.fillColor() != null && fragment.width() > 0 && fragment.height() > 0) {
                // A fragment's y is its bottom edge, measured up from the page's foot.
                fills.add(new Fill(fragment.x(), pageHeight - fragment.y() - fragment.height(),
                        fragment.width(), fragment.height(), shape.fillColor()));
            }
        }
        return fills;
    }

    /**
     * A fill as a drawing anchored to the page, behind the text.
     *
     * @param fill  the fill
     * @param id    an identifier for the shape, unique in the document
     * @param order its place among the page's fills: a later fill is drawn over an earlier one
     * @return the drawing, to be added to a run in a header
     */
    static CTDrawing drawing(Fill fill, long id, int order) {
        long x = Units.toEMU(fill.x());
        long y = Units.toEMU(fill.top());
        long cx = Units.toEMU(fill.width());
        long cy = Units.toEMU(fill.height());
        Color color = fill.color();
        String rgb = String.format(Locale.ROOT, "%02X%02X%02X", color.getRed(), color.getGreen(), color.getBlue());
        String alpha = color.getAlpha() == 255 ? ""
                : "<a:alpha val=\"" + Math.round(color.getAlpha() * 100000.0 / 255.0) + "\"/>";
        String xml = "<w:drawing"
                + " xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\""
                + " xmlns:wp=\"http://schemas.openxmlformats.org/drawingml/2006/wordprocessingDrawing\""
                + " xmlns:a=\"http://schemas.openxmlformats.org/drawingml/2006/main\""
                + " xmlns:wps=\"http://schemas.microsoft.com/office/word/2010/wordprocessingShape\">"
                + "<wp:anchor distT=\"0\" distB=\"0\" distL=\"0\" distR=\"0\" simplePos=\"0\""
                + " relativeHeight=\"" + (order + 1) + "\" behindDoc=\"1\" locked=\"1\""
                + " layoutInCell=\"1\" allowOverlap=\"1\">"
                + "<wp:simplePos x=\"0\" y=\"0\"/>"
                + "<wp:positionH relativeFrom=\"page\"><wp:posOffset>" + x + "</wp:posOffset></wp:positionH>"
                + "<wp:positionV relativeFrom=\"page\"><wp:posOffset>" + y + "</wp:posOffset></wp:positionV>"
                + "<wp:extent cx=\"" + cx + "\" cy=\"" + cy + "\"/>"
                + "<wp:effectExtent l=\"0\" t=\"0\" r=\"0\" b=\"0\"/>"
                + "<wp:wrapNone/>"
                + "<wp:docPr id=\"" + id + "\" name=\"Page background " + (order + 1) + "\"/>"
                + "<wp:cNvGraphicFramePr/>"
                + "<a:graphic><a:graphicData uri=\"http://schemas.microsoft.com/office/word/2010/wordprocessingShape\">"
                + "<wps:wsp><wps:cNvSpPr/><wps:spPr>"
                + "<a:xfrm><a:off x=\"0\" y=\"0\"/><a:ext cx=\"" + cx + "\" cy=\"" + cy + "\"/></a:xfrm>"
                + "<a:prstGeom prst=\"rect\"><a:avLst/></a:prstGeom>"
                + "<a:solidFill><a:srgbClr val=\"" + rgb + "\">" + alpha + "</a:srgbClr></a:solidFill>"
                + "<a:ln><a:noFill/></a:ln>"
                + "</wps:spPr><wps:bodyPr/></wps:wsp>"
                + "</a:graphicData></a:graphic>"
                + "</wp:anchor></w:drawing>";
        // Parsed as the drawing's content rather than a document around it: set onto a run's
        // new w:drawing, a parsed document would nest a second w:drawing inside the first, and
        // the editors draw nothing.
        XmlOptions options = new XmlOptions();
        options.setLoadReplaceDocumentElement(null);
        try {
            return CTDrawing.Factory.parse(xml, options);
        } catch (XmlException failure) {
            throw new IllegalStateException("could not build a page background shape", failure);
        }
    }
}
