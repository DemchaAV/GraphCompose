package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.document.layout.PlacedFragment;
import com.demcha.compose.document.layout.payloads.EllipseFragmentPayload;
import com.demcha.compose.document.layout.payloads.ImageFragmentPayload;
import com.demcha.compose.document.layout.payloads.LineFragmentPayload;
import com.demcha.compose.document.layout.payloads.ParagraphFragmentPayload;
import com.demcha.compose.document.layout.payloads.ParagraphLine;
import com.demcha.compose.document.layout.payloads.PathFragmentPayload;
import com.demcha.compose.document.layout.payloads.PolygonFragmentPayload;
import com.demcha.compose.document.layout.payloads.ShapeClipBeginPayload;
import com.demcha.compose.document.layout.payloads.ShapeClipEndPayload;
import com.demcha.compose.document.layout.payloads.ShapeFragmentPayload;
import com.demcha.compose.document.layout.payloads.SideBorders;
import com.demcha.compose.document.layout.payloads.TransformBeginPayload;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.ClipPolicy;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentCornerRadius;
import com.demcha.compose.document.style.DocumentPaint;
import com.demcha.compose.document.style.DocumentLineCap;
import com.demcha.compose.document.style.DocumentLineJoin;
import com.demcha.compose.document.style.DocumentPathSegment;
import com.demcha.compose.document.style.DocumentTransform;
import com.demcha.compose.document.style.ShapeOutline;
import com.demcha.compose.document.style.ShapePoint;
import com.demcha.compose.engine.components.content.shape.Stroke;
import com.demcha.compose.engine.components.content.text.TextDecoration;
import com.demcha.compose.engine.components.content.text.TextStyle;
import com.demcha.compose.engine.components.style.Padding;
import com.demcha.compose.font.FontName;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Whether a clip cuts what is painted inside it, measured from the layout's fragments as the
 * Word file draws them: a clip that cuts nothing loses nothing in a file that has no clip a
 * container can set round its layers.
 */
class DocxClipInkTest {

    private static final double X = 100;
    private static final double Y = 50;
    private static final double W = 80;
    private static final double H = 60;

    @Test
    void aBoxInsideItsClipIsNotCutAndOnePastItIs() {
        PlacedFragment clip = clip(new ShapeOutline.Rectangle(W, H), ClipPolicy.CLIP_BOUNDS);

        assertThat(cuts(clip, at(10, 10, 40, 30, shape(0)))).isFalse();
        assertThat(cuts(clip, at(0, 0, W, H, shape(0)))).as("exactly its box").isFalse();
        assertThat(cuts(clip, at(50, 10, 40, 30, shape(0)))).as("10pt past its right side").isTrue();
        assertThat(cuts(clip, at(W - 39.7, 10, 40, 30, shape(0)))).as("a hair past it").isFalse();
    }

    @Test
    void aStrokeReachesHalfItsWidthPastItsBox() {
        PlacedFragment clip = clip(new ShapeOutline.Rectangle(W, H), ClipPolicy.CLIP_BOUNDS);

        assertThat(cuts(clip, at(0, 0, W, H, shape(0.8)))).as("0.4pt past").isFalse();
        assertThat(cuts(clip, at(0, 0, W, H, shape(4)))).as("2pt past").isTrue();
        assertThat(cuts(clip, at(2, 2, W - 4, H - 4, shape(4)))).isFalse();
    }

    @Test
    void anEllipseCutsTheCornersOfASquareButNotTheCircleInsideIt() {
        PlacedFragment clip = clip(new ShapeOutline.Ellipse(W, H), ClipPolicy.CLIP_PATH);

        assertThat(cuts(clip, at(0, 0, W, H, shape(0)))).as("the box's corners").isTrue();
        assertThat(cuts(clip, at(0, 0, W, H, ellipse(0)))).as("the same ellipse").isFalse();
        assertThat(cuts(clip, at(W / 2 - 10, H / 2 - 10, 20, 20, shape(0)))).as("a box at its middle").isFalse();
        assertThat(cuts(clip, at(0, 0, W, H, ellipse(4)))).as("its ring stroked 2pt past it").isTrue();
    }

    @Test
    void aPictureCroppedToTheEllipseItFillsIsNotCut() {
        PlacedFragment clip = clip(new ShapeOutline.Ellipse(W, H), ClipPolicy.CLIP_PATH);
        PlacedFragment picture = at(0, 0, W, H, new ImageFragmentPayload(null, null, null, null));

        assertThat(DocxClipInk.cuts(clip, List.of(picture), fragment -> false, UNKNOWN))
                .as("square, as written").isTrue();
        assertThat(DocxClipInk.cuts(clip, List.of(picture), fragment -> fragment == picture, UNKNOWN))
                .as("cropped to the ellipse").isFalse();
    }

    @Test
    void aRoundedClipCutsOnlyPastItsArcs() {
        PlacedFragment clip = clip(new ShapeOutline.RoundedRectangle(W, H, 20), ClipPolicy.CLIP_PATH);

        assertThat(cuts(clip, at(0, 0, W, H, shape(0)))).as("a square fill's corners").isTrue();
        assertThat(cuts(clip, at(0, 0, W, H, roundedShape(DocumentCornerRadius.of(20))))).isFalse();
        assertThat(cuts(clip, at(20, 0, W - 40, H, shape(0)))).as("a band between the arcs").isFalse();
        assertThat(cuts(clip, at(0, 20, W, H - 40, shape(0)))).as("a band across them").isFalse();
    }

    @Test
    void eachCornerKeepsItsOwnRadius() {
        PlacedFragment clip = clip(new ShapeOutline.RoundedRectanglePerCorner(W, H,
                DocumentCornerRadius.of(20, 0, 0, 0)), ClipPolicy.CLIP_PATH);

        assertThat(cuts(clip, at(W - 10, H - 10, 10, 10, shape(0)))).as("the square top right").isFalse();
        assertThat(cuts(clip, at(0, H - 10, 10, 10, shape(0)))).as("the rounded top left").isTrue();
    }

    @Test
    void aPathIsMeasuredWhereItRunsNotByItsBox() {
        PlacedFragment clip = clip(new ShapeOutline.Rectangle(W, H), ClipPolicy.CLIP_BOUNDS);
        List<DocumentPathSegment> inside = List.of(new DocumentPathSegment.MoveTo(0.2, 0.2),
                new DocumentPathSegment.LineTo(0.8, 0.2), new DocumentPathSegment.LineTo(0.5, 0.8),
                new DocumentPathSegment.Close());
        List<DocumentPathSegment> parkedOutside = List.of(new DocumentPathSegment.MoveTo(1.2, 0.2),
                new DocumentPathSegment.LineTo(1.8, 0.2), new DocumentPathSegment.LineTo(1.5, 0.8),
                new DocumentPathSegment.Close());

        assertThat(cuts(clip, at(0, 0, W, H, path(inside, 2, DocumentLineJoin.ROUND))))
                .as("an icon's stroked art inside its box").isFalse();
        assertThat(cuts(clip, at(0, 0, W, H, path(parkedOutside, 0, DocumentLineJoin.MITER))))
                .as("art parked outside the box").isTrue();
    }

    @Test
    void aCurveIsMeasuredAlongItsLengthNotByItsControlPoints() {
        PlacedFragment clip = clip(new ShapeOutline.Rectangle(W, H), ClipPolicy.CLIP_BOUNDS);
        // Control points well above the box; the curve itself peaks at three quarters of their height.
        List<DocumentPathSegment> arch = List.of(new DocumentPathSegment.MoveTo(0.1, 0.1),
                new DocumentPathSegment.CubicTo(0.1, 1.2, 0.9, 1.2, 0.9, 0.1));

        assertThat(cuts(clip, at(0, 0, W, H, path(arch, 0, DocumentLineJoin.ROUND)))).isFalse();
    }

    @Test
    void aMitredCornerRunsPastItsStrokeWhereARoundOneDoesNot() {
        PlacedFragment clip = clip(new ShapeOutline.Rectangle(W, H), ClipPolicy.CLIP_BOUNDS);
        // A spike whose tip stands 2pt below the top: a 2pt stroke's round join reaches 1pt above
        // it, its miter more than three.
        List<DocumentPathSegment> spike = List.of(new DocumentPathSegment.MoveTo(0.3, 0.1),
                new DocumentPathSegment.LineTo(0.5, (H - 2) / H), new DocumentPathSegment.LineTo(0.7, 0.1));

        assertThat(cuts(clip, at(0, 0, W, H, path(spike, 2, DocumentLineJoin.ROUND)))).isFalse();
        assertThat(cuts(clip, at(0, 0, W, H, path(spike, 2, DocumentLineJoin.MITER)))).isTrue();
    }

    @Test
    void aStrokedPolygonRunsToItsMitredPoint() {
        // A spike whose point stands 2pt below the top: its lines' edges stay a point under it,
        // the mitred point of its 2pt stroke more than three past it.
        PlacedFragment clip = clip(new ShapeOutline.Rectangle(W, H), ClipPolicy.CLIP_BOUNDS);
        List<ShapePoint> spike = List.of(new ShapePoint(0.3, 0.1), new ShapePoint(0.5, (H - 2) / H),
                new ShapePoint(0.7, 0.1));

        assertThat(cuts(clip, at(0, 0, W, H, new PolygonFragmentPayload(spike, Color.ORANGE, null, null, null))))
                .as("filled").isFalse();
        assertThat(cuts(clip, at(0, 0, W, H,
                new PolygonFragmentPayload(spike, Color.ORANGE, new Stroke(Color.BLACK, 2), null, null))))
                .as("stroked").isTrue();
    }

    @Test
    void aBoxsSideBordersAreEachALineEndedFlat() {
        // A band flush with the clip's top, ruled 2pt along its foot only: nothing passes the clip.
        PlacedFragment clip = clip(new ShapeOutline.Rectangle(W, H), ClipPolicy.CLIP_BOUNDS);
        ShapeFragmentPayload ruled = new ShapeFragmentPayload(null, null, DocumentCornerRadius.ZERO, null, null,
                new SideBorders(null, null, new Stroke(Color.BLACK, 2), null), null);
        ShapeFragmentPayload framed = new ShapeFragmentPayload(null, null, DocumentCornerRadius.ZERO, null, null,
                new SideBorders(new Stroke(Color.BLACK, 2), null, null, null), null);

        assertThat(cuts(clip, at(0, H - 20, W, 20, ruled))).isFalse();
        assertThat(cuts(clip, at(0, H - 20, W, 20, framed))).as("its top rule half past the clip's top").isTrue();
    }

    @Test
    void aLineEndsAsItsCapsEndIt() {
        // A 4pt rule across the whole box: a butt cap ends at its points, a round or a square one
        // runs 2pt past them.
        PlacedFragment clip = clip(new ShapeOutline.Rectangle(W, H), ClipPolicy.CLIP_BOUNDS);

        assertThat(cuts(clip, at(0, 30, W, 0, line(4, DocumentLineCap.BUTT, W)))).isFalse();
        assertThat(cuts(clip, at(0, 30, W, 0, line(4, DocumentLineCap.ROUND, W)))).isTrue();
        assertThat(cuts(clip, at(0, 30, W, 0, line(4, DocumentLineCap.SQUARE, W)))).isTrue();
        assertThat(cuts(clip, at(10, 30, 60, 0, line(4, DocumentLineCap.ROUND, 60)))).as("well inside").isFalse();
        assertThat(cuts(clip, at(0, H - 1, W, 0, line(4, DocumentLineCap.BUTT, W)))).as("its side past the top")
                .isTrue();
    }

    @Test
    void aStrokedBoxIsSquareAtItsCorners() {
        // A box's stroke is mitred: its corner runs out to a point, past where a round one ends.
        PlacedFragment clip = clip(new ShapeOutline.RoundedRectangle(W, H, 10), ClipPolicy.CLIP_PATH);

        assertThat(cuts(clip, at(4.2, 4.2, W - 8.4, H - 8.4, shape(4)))).isTrue();
        assertThat(cuts(clip, at(4.2, 4.2, W - 8.4, H - 8.4,
                new ShapeFragmentPayload(Color.ORANGE, new Stroke(Color.BLACK, 4), DocumentCornerRadius.of(6),
                        null, null, null, null)))).as("rounded, its stroke round its arcs").isFalse();
    }

    @Test
    void aGradientAloneIsNotDrawnAndCutsNothing() {
        PlacedFragment clip = clip(new ShapeOutline.Rectangle(W, H), ClipPolicy.CLIP_BOUNDS);

        assertThat(cuts(clip, at(-20, -20, 200, 200, new ShapeFragmentPayload(null, null, DocumentCornerRadius.ZERO,
                null, null, null, DocumentPaint.linear(DocumentColor.BLACK, DocumentColor.WHITE))))).isFalse();
    }

    @Test
    void aPathOutlineCutsWhatCrossesItsSlantedSide() {
        // A triangle: what stands under its point is kept, what reaches its slanted side is cut.
        PlacedFragment clip = clip(new ShapeOutline.Path(W, H, List.of(new DocumentPathSegment.MoveTo(0, 0),
                new DocumentPathSegment.LineTo(1, 0), new DocumentPathSegment.LineTo(0.5, 1),
                new DocumentPathSegment.Close())), ClipPolicy.CLIP_PATH);

        assertThat(cuts(clip, at(W / 2 - 5, 5, 10, 10, shape(0)))).isFalse();
        assertThat(cuts(clip, at(0, 0, W, H / 2, shape(0)))).isTrue();
    }

    @Test
    void markersPaintNothingAndATransformIsNotApplied() {
        PlacedFragment clip = clip(new ShapeOutline.Rectangle(W, H), ClipPolicy.CLIP_BOUNDS);

        // The Word file draws what a transform turns upright, at its box: that is what is measured.
        assertThat(cuts(clip,
                at(0, 0, W, H, new TransformBeginPayload(DocumentTransform.rotate(45), "root/card/turned")),
                at(10, 10, 20, 20, shape(0)),
                at(0, 0, 0, 0, new ShapeClipEndPayload("root/card/inner")))).isFalse();
        assertThat(cuts(clip, at(-20, -20, 200, 200, new ShapeClipBeginPayload(
                new ShapeOutline.Rectangle(200, 200), ClipPolicy.CLIP_BOUNDS, "root/card/inner")))).isFalse();
    }

    @Test
    void aLineOfTextIsMeasuredFromItsLettersTopsToTheirFeet() {
        // A label on a 12pt line, its baseline 2pt up it, centred in a chip 10pt tall: its line
        // stands a point past the chip at the top and the foot, its letters inside it.
        PlacedFragment clip = clip(new ShapeOutline.RoundedRectangle(W, 10, 5), ClipPolicy.CLIP_PATH);
        PlacedFragment label = at(0, -1, W, 12, paragraph(40, 12, Padding.zero()));
        DocxClipInk.LetterReach digits = (paragraph, line) -> new double[]{7, 0};
        DocxClipInk.LetterReach tall = (paragraph, line) -> new double[]{10, 0};

        assertThat(DocxClipInk.cuts(clip, List.of(label), fragment -> false, digits)).isFalse();
        assertThat(DocxClipInk.cuts(clip, List.of(label), fragment -> false, tall)).as("letters past the top")
                .isTrue();
        assertThat(DocxClipInk.cuts(clip, List.of(label), fragment -> false, UNKNOWN))
                .as("its whole line, where its letters' reach is not known").isTrue();
        assertThat(DocxClipInk.cuts(clip, List.of(at(0, 0.5, W, 9, paragraph(40, 9, Padding.zero()))),
                fragment -> false, (paragraph, line) -> new double[]{20, 0}))
                .as("its whole line, inside the chip, where its letters are said to run past it").isFalse();
        assertThat(DocxClipInk.cuts(clip, List.of(at(0, -1, W, 12, paragraph(W + 10, 12, Padding.zero()))),
                fragment -> false, digits)).as("a label set wider than its chip").isTrue();
        assertThat(DocxClipInk.cuts(clip, List.of(at(0, -1, W, 12, paragraph(40, 12, null))),
                fragment -> false, digits)).as("a paragraph with no padding given").isFalse();
    }

    @Test
    void aPolygonWithNoPointsPaintsNothing() {
        PlacedFragment clip = clip(new ShapeOutline.Rectangle(W, H), ClipPolicy.CLIP_BOUNDS);

        assertThat(cuts(clip, at(-20, -20, 200, 200,
                new PolygonFragmentPayload(List.of(), Color.ORANGE, new Stroke(Color.BLACK, 2), null, null)))).isFalse();
    }

    @Test
    void anUnpaintedShapePaintsNothing() {
        PlacedFragment clip = clip(new ShapeOutline.Rectangle(W, H), ClipPolicy.CLIP_BOUNDS);

        assertThat(cuts(clip, at(-20, -20, 200, 200,
                new ShapeFragmentPayload(null, null, DocumentCornerRadius.ZERO, null, null, null, null)))).isFalse();
        assertThat(cuts(clip, at(-20, -20, 200, 200, new EllipseFragmentPayload(null, null, null, null)))).isFalse();
    }

    /** Letters whose reach the fonts do not say, measured over their whole line. */
    private static final DocxClipInk.LetterReach UNKNOWN = (paragraph, line) -> null;

    private static boolean cuts(PlacedFragment clip, PlacedFragment... painted) {
        return DocxClipInk.cuts(clip, List.of(painted), fragment -> false, UNKNOWN);
    }

    private static PlacedFragment clip(ShapeOutline outline, ClipPolicy policy) {
        return new PlacedFragment("root/card", 1, 0, X, Y, outline.width(), outline.height(), null, null,
                new ShapeClipBeginPayload(outline, policy, "root/card"));
    }

    private static PlacedFragment at(double x, double y, double width, double height, Object payload) {
        return new PlacedFragment("root/card/layer", 0, 0, X + x, Y + y, width, height, null, null, payload);
    }

    private static ShapeFragmentPayload shape(double strokeWidth) {
        return new ShapeFragmentPayload(Color.ORANGE, strokeWidth > 0 ? new Stroke(Color.BLACK, strokeWidth) : null,
                DocumentCornerRadius.ZERO, null, null, null, null);
    }

    private static ShapeFragmentPayload roundedShape(DocumentCornerRadius radius) {
        return new ShapeFragmentPayload(Color.ORANGE, null, radius, null, null, null, null);
    }

    private static EllipseFragmentPayload ellipse(double strokeWidth) {
        return new EllipseFragmentPayload(Color.BLUE, strokeWidth > 0 ? new Stroke(Color.BLACK, strokeWidth) : null,
                null, null);
    }

    private static PathFragmentPayload path(List<DocumentPathSegment> segments, double strokeWidth, DocumentLineJoin join) {
        return new PathFragmentPayload(segments, strokeWidth > 0 ? null : Color.ORANGE, null,
                strokeWidth > 0 ? new Stroke(Color.BLACK, strokeWidth) : null, null, null, null, null,
                DocumentLineCap.BUTT, join);
    }

    private static ParagraphFragmentPayload paragraph(double lineWidth, double lineHeight, Padding padding) {
        ParagraphLine line = new ParagraphLine("Label", lineWidth, lineHeight, lineHeight, 8, 2, List.of(), List.of());
        return new ParagraphFragmentPayload(new TextStyle(FontName.HELVETICA, 10, TextDecoration.DEFAULT, Color.BLACK),
                TextAlign.CENTER, padding, lineHeight, 0, 0, List.of(line), null, null, null);
    }

    private static LineFragmentPayload line(double width, DocumentLineCap cap, double length) {
        return new LineFragmentPayload(new Stroke(Color.BLACK, width), 0, 0, length, 0, null, null, null, cap);
    }
}
