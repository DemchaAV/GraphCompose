package com.demcha.compose.document.backend.fixed.pptx;

import com.demcha.compose.document.layout.PlacedFragment;
import com.demcha.compose.document.layout.payloads.EllipseFragmentPayload;
import com.demcha.compose.document.layout.payloads.LineFragmentPayload;
import com.demcha.compose.document.layout.payloads.PathFragmentPayload;
import com.demcha.compose.document.layout.payloads.PolygonFragmentPayload;
import com.demcha.compose.document.layout.payloads.ShapeClipBeginPayload;
import com.demcha.compose.document.layout.payloads.ShapeClipEndPayload;
import com.demcha.compose.document.layout.payloads.ShapeFragmentPayload;
import com.demcha.compose.document.layout.payloads.SideBorders;
import com.demcha.compose.document.layout.payloads.TransformBeginPayload;
import com.demcha.compose.document.layout.payloads.TransformEndPayload;
import com.demcha.compose.document.style.ClipPolicy;
import com.demcha.compose.document.style.DocumentCornerRadius;
import com.demcha.compose.document.style.DocumentPathSegment;
import com.demcha.compose.document.style.DocumentTransform;
import com.demcha.compose.document.style.ShapePoint;
import com.demcha.compose.document.style.ShapeOutline;
import com.demcha.compose.engine.components.content.shape.Stroke;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The no-op-clip proof: children safely inside the outline keep the clip out
 * of the raster fallback, and every unprovable situation — outline kinds
 * without a cheap containment test, transforms, edge-hugging ink, wide
 * strokes near the boundary — conservatively keeps it.
 */
class PptxClipSafetyTest {

    private static final double CLIP_X = 100;
    private static final double CLIP_Y = 50;
    private static final double CLIP_W = 80;
    private static final double CLIP_H = 60;

    private static PlacedFragment clipFragment(ShapeOutline outline, ClipPolicy policy) {
        return new PlacedFragment("root/card", 0, 0, CLIP_X, CLIP_Y, CLIP_W, CLIP_H,
                null, null, new ShapeClipBeginPayload(outline, policy, "root/card"));
    }

    /** Child box in clip-local coordinates, with an arbitrary payload. */
    private static PlacedFragment child(double x, double y, double width, double height, Object payload) {
        return new PlacedFragment("root/card/child", 0, 0,
                CLIP_X + x, CLIP_Y + y, width, height, null, null, payload);
    }

    private static boolean verdict(PlacedFragment clip, PlacedFragment... children) {
        List<PlacedFragment> fragments = new ArrayList<>();
        fragments.add(clip);
        fragments.addAll(List.of(children));
        fragments.add(new PlacedFragment("root/card", 1, 0, CLIP_X, CLIP_Y, CLIP_W, CLIP_H,
                null, null, new ShapeClipEndPayload("root/card")));
        return PptxClipSafety.clipCannotRemoveInk(
                (ShapeClipBeginPayload) clip.payload(), clip,
                fragments, 0, fragments.size() - 1);
    }

    private static Object ellipsePayload(double strokeWidth) {
        return new EllipseFragmentPayload(Color.BLUE,
                strokeWidth > 0 ? new Stroke(Color.BLACK, strokeWidth) : null, null, null);
    }

    @Test
    void paddedChildrenInsideARectangleAreProvablySafe() {
        PlacedFragment clip = clipFragment(new ShapeOutline.Rectangle(CLIP_W, CLIP_H), ClipPolicy.CLIP_PATH);
        assertThat(verdict(clip, child(10, 10, 40, 30, ellipsePayload(0)))).isTrue();
    }

    @Test
    void exactFitVectorInkIsSafeButOverflowAndStrokeBleedAreNot() {
        PlacedFragment clip = clipFragment(new ShapeOutline.Rectangle(CLIP_W, CLIP_H), ClipPolicy.CLIP_PATH);
        // Unstroked vector ink stays inside its box by construction, so even
        // an exact-fit child (an SVG path spanning its clip box) is provably
        // safe; overflowing ink and boundary-crossing stroke bleed are not.
        assertThat(verdict(clip, child(0, 0, CLIP_W, CLIP_H, ellipsePayload(0)))).isTrue();
        assertThat(verdict(clip, child(-1, 10, 40, 30, ellipsePayload(0)))).isFalse();
        assertThat(verdict(clip, child(50, 10, 40, 30, ellipsePayload(0)))).isFalse();
        assertThat(verdict(clip, child(0, 10, 40, 30, ellipsePayload(2)))).isFalse();
    }

    @Test
    void unknownPayloadInkIsUnprovableAnywhere() {
        record MysteryPayload() {
        }
        PlacedFragment clip = clipFragment(new ShapeOutline.Rectangle(CLIP_W, CLIP_H), ClipPolicy.CLIP_PATH);
        // Unknown ink extents cannot be modelled — even a deeply padded
        // placement keeps the raster path.
        assertThat(verdict(clip, child(20, 15, 40, 30, new MysteryPayload()))).isFalse();
    }

    @Test
    void mitreProneAndUnclampedInkIsUnprovable() {
        PlacedFragment clip = clipFragment(new ShapeOutline.Rectangle(CLIP_W, CLIP_H), ClipPolicy.CLIP_PATH);
        // Stroked polygons and paths can spike past half the stroke width at
        // mitred joins; path segments are not clamped to the unit box, so an
        // out-of-box coordinate draws outside the fragment bounds.
        PolygonFragmentPayload strokedDiamond = new PolygonFragmentPayload(
                List.of(new ShapePoint(0.5, 0), new ShapePoint(1, 0.5),
                        new ShapePoint(0.5, 1), new ShapePoint(0, 0.5)),
                Color.ORANGE, new Stroke(Color.BLACK, 1), null, null);
        assertThat(verdict(clip, child(20, 15, 30, 20, strokedDiamond))).isFalse();

        PolygonFragmentPayload filledDiamond = new PolygonFragmentPayload(
                List.of(new ShapePoint(0.5, 0), new ShapePoint(1, 0.5),
                        new ShapePoint(0.5, 1), new ShapePoint(0, 0.5)),
                Color.ORANGE, null, null, null);
        assertThat(verdict(clip, child(20, 15, 30, 20, filledDiamond))).isTrue();

        PathFragmentPayload inBoxFill = new PathFragmentPayload(
                List.of(new DocumentPathSegment.MoveTo(0, 0),
                        new DocumentPathSegment.LineTo(1, 0),
                        new DocumentPathSegment.LineTo(1, 1),
                        new DocumentPathSegment.Close()),
                Color.ORANGE, null, null, null, null, null, null, null, null);
        assertThat(verdict(clip, child(20, 15, 30, 20, inBoxFill))).isTrue();

        PathFragmentPayload overflowingFill = new PathFragmentPayload(
                List.of(new DocumentPathSegment.MoveTo(0, 0),
                        new DocumentPathSegment.LineTo(1.5, 0),
                        new DocumentPathSegment.LineTo(1, 1),
                        new DocumentPathSegment.Close()),
                Color.ORANGE, null, null, null, null, null, null, null, null);
        assertThat(verdict(clip, child(20, 15, 30, 20, overflowingFill))).isFalse();

        PathFragmentPayload strokedFill = new PathFragmentPayload(
                List.of(new DocumentPathSegment.MoveTo(0, 0),
                        new DocumentPathSegment.LineTo(1, 0),
                        new DocumentPathSegment.LineTo(1, 1),
                        new DocumentPathSegment.Close()),
                Color.ORANGE, null, new Stroke(Color.BLACK, 1), null,
                null, null, null, null, null);
        assertThat(verdict(clip, child(20, 15, 30, 20, strokedFill))).isFalse();
    }

    @Test
    void sideBordersWidenTheShapeInset() {
        PlacedFragment clip = clipFragment(new ShapeOutline.Rectangle(CLIP_W, CLIP_H), ClipPolicy.CLIP_PATH);
        ShapeFragmentPayload accented = new ShapeFragmentPayload(
                Color.LIGHT_GRAY, null, null, null, null,
                new SideBorders(null, null, null, new Stroke(Color.BLACK, 6)), null);
        // 1pt from the edge: the 6pt left accent border bleeds 3pt out.
        assertThat(verdict(clip, child(1, 10, 40, 30, accented))).isFalse();
        assertThat(verdict(clip, child(4, 10, 40, 30, accented))).isTrue();
    }

    @Test
    void aWideStrokeNearTheBoundaryWidensTheInset() {
        PlacedFragment clip = clipFragment(new ShapeOutline.Rectangle(CLIP_W, CLIP_H), ClipPolicy.CLIP_PATH);
        // 3pt from the edge: a 6pt stroke reaches exactly the boundary (still
        // uncut), an 8pt stroke crosses it.
        assertThat(verdict(clip, child(3, 10, 40, 30, ellipsePayload(0)))).isTrue();
        assertThat(verdict(clip, child(3, 10, 40, 30, ellipsePayload(6)))).isTrue();
        assertThat(verdict(clip, child(3, 10, 40, 30, ellipsePayload(8)))).isFalse();
    }

    @Test
    void roundedCornersOnlyForbidTheCornerSquares() {
        PlacedFragment clip = clipFragment(
                new ShapeOutline.RoundedRectangle(CLIP_W, CLIP_H, 8), ClipPolicy.CLIP_PATH);
        // Full-width middle band clears every corner square.
        assertThat(verdict(clip, child(3, 20, CLIP_W - 6, 20, ellipsePayload(0)))).isTrue();
        // Bottom-left corner square is cut territory.
        assertThat(verdict(clip, child(3, 3, 10, 10, ellipsePayload(0)))).isFalse();
    }

    @Test
    void perCornerRadiiForbidOnlyTheRoundedCorners() {
        PlacedFragment clip = clipFragment(
                new ShapeOutline.RoundedRectanglePerCorner(CLIP_W, CLIP_H,
                        DocumentCornerRadius.right(12)), ClipPolicy.CLIP_PATH);
        // Square left corners admit corner-adjacent ink; rounded right ones don't.
        assertThat(verdict(clip, child(3, 3, 10, 10, ellipsePayload(0)))).isTrue();
        assertThat(verdict(clip, child(CLIP_W - 13, 3, 10, 10, ellipsePayload(0)))).isFalse();
    }

    @Test
    void perCornerRadiiMapTopAndBottomToTheYUpConvention() {
        // Only the visual TOP-left corner is rounded: in the engine's y-up
        // space that is the high-y corner. A top/bottom swap in the corner
        // mapping would flip both verdicts.
        PlacedFragment clip = clipFragment(
                new ShapeOutline.RoundedRectanglePerCorner(CLIP_W, CLIP_H,
                        DocumentCornerRadius.of(12, 0, 0, 0)), ClipPolicy.CLIP_PATH);
        assertThat(verdict(clip, child(3, CLIP_H - 13, 10, 10, ellipsePayload(0))))
                .as("ink in the rounded visual top-left corner").isFalse();
        assertThat(verdict(clip, child(3, 3, 10, 10, ellipsePayload(0))))
                .as("ink in the square visual bottom-left corner").isTrue();
    }

    @Test
    void ellipseContainmentChecksTheCorners() {
        PlacedFragment clip = clipFragment(new ShapeOutline.Ellipse(CLIP_W, CLIP_H), ClipPolicy.CLIP_PATH);
        // Small centered box: corners inside the ellipse.
        assertThat(verdict(clip, child(30, 22, 20, 16, ellipsePayload(0)))).isTrue();
        // Corner-region box: inside the bounding box, outside the ellipse.
        assertThat(verdict(clip, child(4, 4, 12, 10, ellipsePayload(0)))).isFalse();
    }

    @Test
    void boundsPolicyIgnoresTheOutlineShape() {
        // CLIP_BOUNDS clips to the rectangle even for an ellipse outline, so
        // the corner-region box that fails CLIP_PATH is safe here.
        PlacedFragment clip = clipFragment(new ShapeOutline.Ellipse(CLIP_W, CLIP_H), ClipPolicy.CLIP_BOUNDS);
        assertThat(verdict(clip, child(4, 4, 12, 10, ellipsePayload(0)))).isTrue();
    }

    @Test
    void unprovableOutlinesAndTransformsStayOnTheRasterPath() {
        PlacedFragment polygonClip = clipFragment(
                ShapeOutline.diamond(CLIP_W, CLIP_H), ClipPolicy.CLIP_PATH);
        assertThat(verdict(polygonClip, child(30, 25, 10, 8, ellipsePayload(0)))).isFalse();

        PlacedFragment rectClip = clipFragment(
                new ShapeOutline.Rectangle(CLIP_W, CLIP_H), ClipPolicy.CLIP_PATH);
        assertThat(verdict(rectClip,
                child(20, 20, 10, 8, new TransformBeginPayload(
                        DocumentTransform.rotate(30), "root/card/spin")),
                child(22, 22, 6, 4, ellipsePayload(0)),
                child(20, 20, 10, 8, new TransformEndPayload("root/card/spin"))))
                .isFalse();
    }

    @Test
    void aNestedClipConfinesItsUnprovableChildrenToItsOwnBox() {
        PlacedFragment clip = clipFragment(new ShapeOutline.Rectangle(CLIP_W, CLIP_H), ClipPolicy.CLIP_PATH);
        // The nested icon clip contains an unprovable child (a stroked
        // diamond), but the nested region confines its ink to its own box —
        // the outer verdict judges just that box, so the outer region stays
        // native and only the icon rasters at its own 16x16 granularity.
        PlacedFragment nestedBegin = child(20, 20, 16, 16, new ShapeClipBeginPayload(
                new ShapeOutline.Rectangle(16, 16), ClipPolicy.CLIP_PATH, "root/card/icon"));
        PlacedFragment unprovableIcon = child(20, 20, 16, 16, new PolygonFragmentPayload(
                List.of(new ShapePoint(0.5, 0), new ShapePoint(1, 0.5),
                        new ShapePoint(0.5, 1), new ShapePoint(0, 0.5)),
                Color.ORANGE, new Stroke(Color.BLACK, 1), null, null));
        PlacedFragment nestedEnd = child(20, 20, 0, 0, new ShapeClipEndPayload("root/card/icon"));
        assertThat(verdict(clip, nestedBegin, unprovableIcon, nestedEnd)).isTrue();

        // A nested clip box that itself leaves the outer outline still fails.
        PlacedFragment overflowingBegin = child(CLIP_W - 8, 20, 16, 16, new ShapeClipBeginPayload(
                new ShapeOutline.Rectangle(16, 16), ClipPolicy.CLIP_PATH, "root/card/icon"));
        assertThat(verdict(clip, overflowingBegin, unprovableIcon, nestedEnd)).isFalse();
    }

    @Test
    void markersThemselvesDoNotBlockTheProof() {
        PlacedFragment clip = clipFragment(new ShapeOutline.Rectangle(CLIP_W, CLIP_H), ClipPolicy.CLIP_PATH);
        // A nested clip whose markers sit at the region edge draws nothing
        // itself; only its renderable children are judged.
        assertThat(verdict(clip,
                child(0, 0, CLIP_W, CLIP_H, new ShapeClipBeginPayload(
                        new ShapeOutline.Rectangle(CLIP_W, CLIP_H), ClipPolicy.CLIP_PATH, "root/card/inner")),
                child(10, 10, 20, 15, ellipsePayload(0)),
                child(0, 0, CLIP_W, CLIP_H, new ShapeClipEndPayload("root/card/inner"))))
                .isTrue();
    }

    @Test
    void lineThicknessCountsAsStrokeBleed() {
        PlacedFragment clip = clipFragment(new ShapeOutline.Rectangle(CLIP_W, CLIP_H), ClipPolicy.CLIP_PATH);
        LineFragmentPayload thin = new LineFragmentPayload(
                new Stroke(Color.BLACK, 1), 0, 0, CLIP_W - 8, 0, null, null);
        LineFragmentPayload fat = new LineFragmentPayload(
                new Stroke(Color.BLACK, 10), 0, 0, CLIP_W - 8, 0, null, null);
        assertThat(verdict(clip, child(4, 30, CLIP_W - 8, 0, thin))).isTrue();
        assertThat(verdict(clip, child(4, 30, CLIP_W - 8, 0, fat))).isFalse();
    }
}
