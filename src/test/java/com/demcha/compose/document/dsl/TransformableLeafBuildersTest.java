package com.demcha.compose.document.dsl;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.image.DocumentImageData;
import com.demcha.compose.document.layout.BuiltInNodeDefinitions;
import com.demcha.compose.document.layout.PlacedFragment;
import com.demcha.compose.document.layout.payloads.TransformBeginPayload;
import com.demcha.compose.document.layout.payloads.TransformEndPayload;
import com.demcha.compose.document.node.BarcodeNode;
import com.demcha.compose.document.node.DocumentBarcodeOptions;
import com.demcha.compose.document.node.DocumentBarcodeType;
import com.demcha.compose.document.node.EllipseNode;
import com.demcha.compose.document.node.ImageNode;
import com.demcha.compose.document.node.LineNode;
import com.demcha.compose.document.node.ShapeNode;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTransform;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Pins the {@link Transformable} contract on the five leaf builders that
 * opt in alongside {@link ShapeContainerBuilder} in v1.5: {@link ShapeBuilder},
 * {@link LineBuilder}, {@link EllipseBuilder}, {@link ImageBuilder}, and
 * {@link BarcodeBuilder}. The contract has two halves — builder ergonomics
 * (default identity transform; {@code rotate(...)} / {@code scale(...)}
 * propagate to the built node) and fragment ordering (identity transform
 * emits no markers; non-identity transform brackets the leaf payload with
 * a {@link TransformBeginPayload} ahead and a
 * {@link TransformEndPayload} behind, sharing the
 * same owner path).
 */
class TransformableLeafBuildersTest {

    private static final double EPS = 1e-3;

    // ---------------------------------------------------------------------
    // Builder ergonomics — default identity, rotate / scale propagation
    // ---------------------------------------------------------------------

    @Test
    void shapeBuilderDefaultsToIdentityTransform() {
        ShapeNode node = new ShapeBuilder()
                .name("S")
                .size(40, 40)
                .build();
        assertThat(node.transform()).isEqualTo(DocumentTransform.NONE);
    }

    @Test
    void shapeBuilderPropagatesRotateAndScale() {
        ShapeNode node = new ShapeBuilder()
                .name("S")
                .size(40, 40)
                .rotate(30)
                .scale(0.75, 1.25)
                .build();
        assertThat(node.transform().rotationDegrees()).isEqualTo(30.0, within(EPS));
        assertThat(node.transform().scaleX()).isEqualTo(0.75, within(EPS));
        assertThat(node.transform().scaleY()).isEqualTo(1.25, within(EPS));
    }

    @Test
    void lineBuilderDefaultsToIdentityTransform() {
        LineNode node = new LineBuilder()
                .name("L")
                .horizontal(60)
                .build();
        assertThat(node.transform()).isEqualTo(DocumentTransform.NONE);
    }

    @Test
    void lineBuilderPropagatesRotate() {
        LineNode node = new LineBuilder()
                .name("L")
                .horizontal(60)
                .rotate(45)
                .build();
        assertThat(node.transform().rotationDegrees()).isEqualTo(45.0, within(EPS));
    }

    @Test
    void ellipseBuilderDefaultsToIdentityTransform() {
        EllipseNode node = new EllipseBuilder()
                .name("E")
                .circle(50)
                .build();
        assertThat(node.transform()).isEqualTo(DocumentTransform.NONE);
    }

    @Test
    void ellipseBuilderPropagatesScale() {
        EllipseNode node = new EllipseBuilder()
                .name("E")
                .circle(50)
                .scale(2.0)
                .build();
        assertThat(node.transform().scaleX()).isEqualTo(2.0, within(EPS));
        assertThat(node.transform().scaleY()).isEqualTo(2.0, within(EPS));
    }

    @Test
    void imageBuilderPropagatesRotate() throws Exception {
        ImageNode node = new ImageBuilder()
                .name("I")
                .source(DocumentImageData.fromBytes(onePixelPng()))
                .size(40, 40)
                .rotate(90)
                .build();
        assertThat(node.transform().rotationDegrees()).isEqualTo(90.0, within(EPS));
    }

    @Test
    void barcodeBuilderPropagatesRotate() {
        BarcodeNode node = new BarcodeBuilder()
                .name("B")
                .data("HELLO")
                .qrCode()
                .size(40, 40)
                .rotate(-15)
                .build();
        assertThat(node.transform().rotationDegrees()).isEqualTo(-15.0, within(EPS));
    }

    // ---------------------------------------------------------------------
    // Identity transform → no transform markers in the fragment list
    // ---------------------------------------------------------------------

    @Test
    void shapeWithoutTransformEmitsSingleFragment() {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(400, 300)
                .margin(DocumentInsets.of(20))
                .create()) {
            session.add(new ShapeBuilder()
                    .name("PlainShape")
                    .size(40, 40)
                    .fillColor(DocumentColor.ORANGE)
                    .build());
            assertNoTransformMarkers(session.layoutGraph().fragments());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void ellipseWithoutTransformEmitsSingleFragment() {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(400, 300)
                .margin(DocumentInsets.of(20))
                .create()) {
            session.add(new EllipseBuilder()
                    .name("PlainEllipse")
                    .circle(50)
                    .fillColor(DocumentColor.ROYAL_BLUE)
                    .build());
            assertNoTransformMarkers(session.layoutGraph().fragments());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void lineWithoutTransformEmitsSingleFragment() {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(400, 300)
                .margin(DocumentInsets.of(20))
                .create()) {
            session.add(new LineBuilder()
                    .name("PlainLine")
                    .horizontal(120)
                    .build());
            assertNoTransformMarkers(session.layoutGraph().fragments());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // ---------------------------------------------------------------------
    // Non-identity transform → leaf is bracketed with transform begin/end
    // markers carrying the same owner path
    // ---------------------------------------------------------------------

    @Test
    void rotatedShapeBracketsLeafWithTransformBeginAndEnd() {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(400, 300)
                .margin(DocumentInsets.of(20))
                .create()) {
            session.add(new ShapeBuilder()
                    .name("RotatedShape")
                    .size(40, 40)
                    .fillColor(DocumentColor.ORANGE)
                    .rotate(30)
                    .build());
            assertTransformBracketsLeaf(
                    session.layoutGraph().fragments(),
                    BuiltInNodeDefinitions.ShapeFragmentPayload.class,
                    30.0);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void rotatedEllipseBracketsLeafWithTransformBeginAndEnd() {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(400, 300)
                .margin(DocumentInsets.of(20))
                .create()) {
            session.add(new EllipseBuilder()
                    .name("RotatedEllipse")
                    .circle(50)
                    .fillColor(DocumentColor.ROYAL_BLUE)
                    .rotate(20)
                    .build());
            assertTransformBracketsLeaf(
                    session.layoutGraph().fragments(),
                    BuiltInNodeDefinitions.EllipseFragmentPayload.class,
                    20.0);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void scaledLineBracketsLeafWithTransformBeginAndEnd() {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(400, 300)
                .margin(DocumentInsets.of(20))
                .create()) {
            session.add(new LineBuilder()
                    .name("ScaledLine")
                    .horizontal(120)
                    .scale(2.0)
                    .build());
            List<PlacedFragment> fragments = session.layoutGraph().fragments();
            int begin = indexOfPayload(fragments, TransformBeginPayload.class);
            int leaf = indexOfPayload(fragments, BuiltInNodeDefinitions.LineFragmentPayload.class);
            int end = indexOfPayload(fragments, TransformEndPayload.class);
            assertThat(begin).isGreaterThanOrEqualTo(0);
            assertThat(leaf).isGreaterThan(begin);
            assertThat(end).isGreaterThan(leaf);
            TransformBeginPayload payload =
                    (TransformBeginPayload) fragments.get(begin).payload();
            assertThat(payload.transform().scaleX()).isEqualTo(2.0, within(EPS));
            assertThat(payload.transform().scaleY()).isEqualTo(2.0, within(EPS));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void rotatedImageBracketsLeafWithTransformBeginAndEnd() throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(400, 300)
                .margin(DocumentInsets.of(20))
                .create()) {
            session.add(new ImageBuilder()
                    .name("RotatedImage")
                    .source(DocumentImageData.fromBytes(onePixelPng()))
                    .size(40, 40)
                    .rotate(90)
                    .build());
            assertTransformBracketsLeaf(
                    session.layoutGraph().fragments(),
                    BuiltInNodeDefinitions.ImageFragmentPayload.class,
                    90.0);
        }
    }

    @Test
    void rotatedBarcodeBracketsLeafWithTransformBeginAndEnd() {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(400, 300)
                .margin(DocumentInsets.of(20))
                .create()) {
            session.add(new BarcodeBuilder()
                    .name("RotatedBarcode")
                    .data("HELLO")
                    .qrCode()
                    .size(60, 60)
                    .rotate(-15)
                    .build());
            assertTransformBracketsLeaf(
                    session.layoutGraph().fragments(),
                    BuiltInNodeDefinitions.BarcodeFragmentPayload.class,
                    -15.0);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void transformBeginAndEndShareTheSameOwnerPath() {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(400, 300)
                .margin(DocumentInsets.of(20))
                .create()) {
            session.add(new ShapeBuilder()
                    .name("Owner")
                    .size(40, 40)
                    .fillColor(DocumentColor.ORANGE)
                    .rotate(10)
                    .build());

            List<PlacedFragment> fragments = session.layoutGraph().fragments();
            int begin = indexOfPayload(fragments, TransformBeginPayload.class);
            int end = indexOfPayload(fragments, TransformEndPayload.class);
            assertThat(begin).isGreaterThanOrEqualTo(0);
            assertThat(end).isGreaterThan(begin);

            String beginOwner = ((TransformBeginPayload) fragments.get(begin).payload()).ownerPath();
            String endOwner = ((TransformEndPayload) fragments.get(end).payload()).ownerPath();
            assertThat(beginOwner).isNotEmpty();
            assertThat(beginOwner).isEqualTo(endOwner);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    private static void assertTransformBracketsLeaf(List<PlacedFragment> fragments,
                                                    Class<?> leafPayloadType,
                                                    double expectedRotationDegrees) {
        int begin = indexOfPayload(fragments, TransformBeginPayload.class);
        int leaf = indexOfPayload(fragments, leafPayloadType);
        int end = indexOfPayload(fragments, TransformEndPayload.class);
        assertThat(begin)
                .as("transform-begin must be present for non-identity transform")
                .isGreaterThanOrEqualTo(0);
        assertThat(leaf)
                .as("leaf payload must follow transform-begin in render order")
                .isGreaterThan(begin);
        assertThat(end)
                .as("transform-end must close after the leaf")
                .isGreaterThan(leaf);
        TransformBeginPayload beginPayload =
                (TransformBeginPayload) fragments.get(begin).payload();
        assertThat(beginPayload.transform().rotationDegrees())
                .isEqualTo(expectedRotationDegrees, within(EPS));
    }

    private static void assertNoTransformMarkers(List<PlacedFragment> fragments) {
        assertThat(indexOfPayload(fragments, TransformBeginPayload.class))
                .as("identity transform must not emit transform-begin")
                .isEqualTo(-1);
        assertThat(indexOfPayload(fragments, TransformEndPayload.class))
                .as("identity transform must not emit transform-end")
                .isEqualTo(-1);
    }

    private static int indexOfPayload(List<PlacedFragment> fragments, Class<?> payloadType) {
        for (int i = 0; i < fragments.size(); i++) {
            if (payloadType.isInstance(fragments.get(i).payload())) {
                return i;
            }
        }
        return -1;
    }

    private static byte[] onePixelPng() throws Exception {
        BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        image.setRGB(0, 0, java.awt.Color.WHITE.getRGB());
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return output.toByteArray();
    }
}
