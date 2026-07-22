package com.demcha.compose.document.backend.fixed.pptx.handlers;

import com.demcha.compose.document.backend.fixed.pptx.PptxCoordinates;
import com.demcha.compose.document.backend.fixed.pptx.PptxFragmentRenderHandler;
import com.demcha.compose.document.backend.fixed.pptx.PptxRenderEnvironment;
import com.demcha.compose.document.image.DocumentImageFitMode;
import com.demcha.compose.document.layout.PlacedFragment;
import com.demcha.compose.document.layout.payloads.ImageFragmentPayload;
import org.apache.poi.xslf.usermodel.XSLFPictureData;
import org.apache.poi.xslf.usermodel.XSLFPictureShape;
import org.openxmlformats.schemas.drawingml.x2006.main.CTRelativeRect;
import org.openxmlformats.schemas.presentationml.x2006.main.CTPicture;

import java.awt.Dimension;
import java.awt.geom.Rectangle2D;

/**
 * Renders resolved image fragments as pictures with the PDF handler's fit
 * modes: STRETCH fills the box, CONTAIN letter-boxes the centered image
 * inside it, and COVER crops via the picture's source rectangle — DrawingML's
 * native crop, so no clipping is needed.
 *
 * @since 2.1.0
 */
public final class PptxImageFragmentRenderHandler
        implements PptxFragmentRenderHandler<ImageFragmentPayload> {

    /**
     * Creates the image fragment renderer.
     */
    public PptxImageFragmentRenderHandler() {
    }

    @Override
    public Class<ImageFragmentPayload> payloadType() {
        return ImageFragmentPayload.class;
    }

    @Override
    public void render(PlacedFragment fragment,
                       ImageFragmentPayload payload,
                       PptxRenderEnvironment environment) {
        if (fragment.width() <= 0 || fragment.height() <= 0) {
            return;
        }
        XSLFPictureData data = environment.resolvePicture(payload.imageData());
        Dimension size = environment.pictureSize(payload.imageData());
        if (size == null) {
            size = data.getImageDimensionInPixels();
        }
        double sourceWidth = Math.max(1, size.getWidth());
        double sourceHeight = Math.max(1, size.getHeight());
        double boxWidth = fragment.width();
        double boxHeight = fragment.height();
        double boxTop = PptxCoordinates.topY(
                environment.canvasHeight(), fragment.y(), fragment.height());

        XSLFPictureShape picture = environment.surface(fragment.pageIndex()).createPicture(data);
        if (payload.fitMode() == DocumentImageFitMode.STRETCH) {
            picture.setAnchor(new Rectangle2D.Double(fragment.x(), boxTop, boxWidth, boxHeight));
            return;
        }
        if (payload.fitMode() == DocumentImageFitMode.COVER) {
            // The picture anchors on the full fragment box and the overflow is
            // cropped away in source space: the same centered-crop geometry the
            // PDF handler produces with a clip, expressed as srcRect fractions.
            picture.setAnchor(new Rectangle2D.Double(fragment.x(), boxTop, boxWidth, boxHeight));
            double scale = Math.max(boxWidth / sourceWidth, boxHeight / sourceHeight);
            double horizontalCrop = (sourceWidth * scale - boxWidth) / (sourceWidth * scale) / 2.0;
            double verticalCrop = (sourceHeight * scale - boxHeight) / (sourceHeight * scale) / 2.0;
            CTRelativeRect srcRect = ((CTPicture) picture.getXmlObject())
                    .getBlipFill().addNewSrcRect();
            srcRect.setL(toThousandthPercent(horizontalCrop));
            srcRect.setR(toThousandthPercent(horizontalCrop));
            srcRect.setT(toThousandthPercent(verticalCrop));
            srcRect.setB(toThousandthPercent(verticalCrop));
            return;
        }
        // CONTAIN: centered letter-box inside the fragment, no crop.
        double scale = Math.min(boxWidth / sourceWidth, boxHeight / sourceHeight);
        double drawWidth = sourceWidth * scale;
        double drawHeight = sourceHeight * scale;
        picture.setAnchor(new Rectangle2D.Double(
                fragment.x() + (boxWidth - drawWidth) / 2.0,
                boxTop + (boxHeight - drawHeight) / 2.0,
                drawWidth,
                drawHeight));
    }

    private static int toThousandthPercent(double fraction) {
        return (int) Math.round(Math.max(0, fraction) * 100000.0);
    }
}
