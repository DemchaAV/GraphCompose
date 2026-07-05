package com.demcha.compose.document.backend.fixed.pdf;

import com.demcha.compose.document.node.DocumentLinkOptions;
import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSInteger;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionGoTo;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionURI;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDBorderStyleDictionary;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageXYZDestination;

import java.io.IOException;

/**
 * Internal helper that writes canonical hyperlink annotations into rendered PDF
 * pages — external URI links ({@link PDActionURI}) and in-document navigation
 * links ({@link PDActionGoTo}).
 */
final class PdfLinkAnnotationWriter {
    private PdfLinkAnnotationWriter() {
    }

    /**
     * Adds an external-URI link annotation covering {@code rectangle}.
     */
    static void addUriLink(PDPage page, PlacedPdfRect rectangle, DocumentLinkOptions options) throws IOException {
        PDAnnotationLink link = newBorderlessLink(rectangle);
        PDActionURI action = new PDActionURI();
        action.setURI(options.uri());
        link.setAction(action);
        page.getAnnotations().add(link);
    }

    /**
     * Adds an in-document go-to link on {@code sourcePage} covering
     * {@code rectangle} that jumps to {@code (left, top)} on {@code targetPage}.
     * Zoom {@code 0} keeps the viewer's current magnification.
     */
    static void addInternalLink(PDPage sourcePage,
                                PlacedPdfRect rectangle,
                                PDPage targetPage,
                                double left,
                                double top) throws IOException {
        PDAnnotationLink link = newBorderlessLink(rectangle);

        PDPageXYZDestination destination = new PDPageXYZDestination();
        destination.setPage(targetPage);
        destination.setLeft((int) left);
        destination.setTop((int) top);
        destination.setZoom(0);

        PDActionGoTo action = new PDActionGoTo();
        action.setDestination(destination);
        link.setAction(action);

        sourcePage.getAnnotations().add(link);
    }

    private static PDAnnotationLink newBorderlessLink(PlacedPdfRect rectangle) {
        PDAnnotationLink link = new PDAnnotationLink();
        PDRectangle position = new PDRectangle();
        position.setLowerLeftX((float) rectangle.x());
        position.setLowerLeftY((float) rectangle.y());
        position.setUpperRightX((float) (rectangle.x() + rectangle.width()));
        position.setUpperRightY((float) (rectangle.y() + rectangle.height()));
        link.setRectangle(position);

        PDBorderStyleDictionary border = new PDBorderStyleDictionary();
        border.setWidth(0);
        link.setBorderStyle(border);
        link.setHighlightMode(PDAnnotationLink.HIGHLIGHT_MODE_NONE);

        COSArray borderArray = new COSArray();
        borderArray.add(COSInteger.ZERO);
        borderArray.add(COSInteger.ZERO);
        borderArray.add(COSInteger.ZERO);
        link.getCOSObject().setItem(COSName.BORDER, borderArray);
        return link;
    }

    record PlacedPdfRect(double x, double y, double width, double height) {
    }
}
