package com.demcha.compose.document.backend.fixed.pdf;

import com.demcha.compose.document.backend.fixed.pdf.options.PdfLinkOptions;
import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSInteger;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionURI;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDBorderStyleDictionary;

import java.io.IOException;

/**
 * Internal helper that writes canonical hyperlink annotations into rendered PDF
 * pages.
 */
final class PdfLinkAnnotationWriter {
    private PdfLinkAnnotationWriter() {
    }

    static void addUriLink(PDPage page, PlacedPdfRect rectangle, PdfLinkOptions options) throws IOException {
        PDAnnotationLink link = new PDAnnotationLink();
        PDRectangle position = new PDRectangle();
        position.setLowerLeftX((float) rectangle.x());
        position.setLowerLeftY((float) rectangle.y());
        position.setUpperRightX((float) (rectangle.x() + rectangle.width()));
        position.setUpperRightY((float) (rectangle.y() + rectangle.height()));
        link.setRectangle(position);

        PDActionURI action = new PDActionURI();
        action.setURI(options.uri());
        link.setAction(action);

        PDBorderStyleDictionary border = new PDBorderStyleDictionary();
        border.setWidth(0);
        link.setBorderStyle(border);
        link.setHighlightMode(PDAnnotationLink.HIGHLIGHT_MODE_NONE);

        COSArray borderArray = new COSArray();
        borderArray.add(COSInteger.ZERO);
        borderArray.add(COSInteger.ZERO);
        borderArray.add(COSInteger.ZERO);
        link.getCOSObject().setItem(COSName.BORDER, borderArray);

        page.getAnnotations().add(link);
    }

    record PlacedPdfRect(double x, double y, double width, double height) {
    }
}
