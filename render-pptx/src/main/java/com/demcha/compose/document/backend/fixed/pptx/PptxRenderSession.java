package com.demcha.compose.document.backend.fixed.pptx;

import org.apache.poi.util.Units;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.openxmlformats.schemas.presentationml.x2006.main.CTSlideSize;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;

/**
 * Page-scoped PPTX render session: one {@link XSLFSlide} per resolved page,
 * created up front so handlers can draw on any page in fragment order.
 *
 * <p>The slide size is stamped twice: once through POI's integer-point
 * {@code setPageSize} (which creates the size element), then overwritten with
 * exact EMU values so fractional page sizes (A4 is 595.27563 pt wide) keep
 * sub-point precision instead of rounding to whole points. Note the OOXML
 * schema floor for a slide dimension is 914400 EMU (72 pt) — page sizes below
 * that are not representable as slides.</p>
 *
 * <p><b>Thread-safety:</b> mutable and not thread-safe.</p>
 */
final class PptxRenderSession {

    private final List<XSLFSlide> slides;

    PptxRenderSession(XMLSlideShow show, double width, double height, int totalPages) {
        show.setPageSize(new Dimension(
                (int) Math.round(width),
                (int) Math.round(height)));
        CTSlideSize slideSize = show.getCTPresentation().getSldSz();
        slideSize.setCx(Units.toEMU(width));
        slideSize.setCy(Units.toEMU(height));

        int pageCount = Math.max(totalPages, 1);
        List<XSLFSlide> created = new ArrayList<>(pageCount);
        for (int pageIndex = 0; pageIndex < pageCount; pageIndex++) {
            created.add(show.createSlide());
        }
        this.slides = List.copyOf(created);
    }

    XSLFSlide slide(int pageIndex) {
        if (pageIndex < 0 || pageIndex >= slides.size()) {
            throw new IllegalArgumentException(
                    "Page index " + pageIndex + " is outside the render session.");
        }
        return slides.get(pageIndex);
    }
}
