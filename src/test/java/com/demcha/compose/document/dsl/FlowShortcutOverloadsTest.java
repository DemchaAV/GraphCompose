package com.demcha.compose.document.dsl;

import com.demcha.compose.document.image.DocumentImageData;
import com.demcha.compose.document.node.ContainerNode;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.EllipseNode;
import com.demcha.compose.document.node.ImageNode;
import com.demcha.compose.document.node.ShapeNode;
import com.demcha.compose.document.style.DocumentColor;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class FlowShortcutOverloadsTest {

    private static final double EPS = 1e-6;
    private static final DocumentColor BRAND = DocumentColor.rgb(180, 40, 40);

    @Test
    void addShapeWithDimensionsAndFill() {
        ContainerNode flow = new PageFlowBuilder()
                .name("Flow")
                .addShape(50.0, 30.0, BRAND)
                .build();

        ShapeNode shape = (ShapeNode) firstChild(flow);
        assertThat(shape.width()).isEqualTo(50.0, within(EPS));
        assertThat(shape.height()).isEqualTo(30.0, within(EPS));
        assertThat(shape.fillColor()).isEqualTo(BRAND);
    }

    @Test
    void addEllipseWithDiameterAndFill() {
        ContainerNode flow = new PageFlowBuilder()
                .addEllipse(40.0, BRAND)
                .build();

        EllipseNode ellipse = (EllipseNode) firstChild(flow);
        assertThat(ellipse.width()).isEqualTo(40.0, within(EPS));
        assertThat(ellipse.height()).isEqualTo(40.0, within(EPS));
        assertThat(ellipse.fillColor()).isEqualTo(BRAND);
    }

    @Test
    void addEllipseWithSizeAndFill() {
        ContainerNode flow = new PageFlowBuilder()
                .addEllipse(80.0, 40.0, BRAND)
                .build();

        EllipseNode ellipse = (EllipseNode) firstChild(flow);
        assertThat(ellipse.width()).isEqualTo(80.0, within(EPS));
        assertThat(ellipse.height()).isEqualTo(40.0, within(EPS));
        assertThat(ellipse.fillColor()).isEqualTo(BRAND);
    }

    @Test
    void addCircleWithFill() {
        ContainerNode flow = new PageFlowBuilder()
                .addCircle(60.0, BRAND)
                .build();

        EllipseNode circle = (EllipseNode) firstChild(flow);
        assertThat(circle.width()).isEqualTo(60.0, within(EPS));
        assertThat(circle.height()).isEqualTo(60.0, within(EPS));
        assertThat(circle.fillColor()).isEqualTo(BRAND);
    }

    @Test
    void addImageWithExplicitDimensions() throws Exception {
        DocumentImageData data = DocumentImageData.fromBytes(onePixelPng());

        ContainerNode flow = new PageFlowBuilder()
                .addImage(data, 96.0, 64.0)
                .build();

        ImageNode image = (ImageNode) firstChild(flow);
        assertThat(image.imageData()).isSameAs(data);
        assertThat(image.width()).isNotNull();
        assertThat(image.width()).isEqualTo(96.0, within(EPS));
        assertThat(image.height()).isNotNull();
        assertThat(image.height()).isEqualTo(64.0, within(EPS));
    }

    private static DocumentNode firstChild(ContainerNode container) {
        List<DocumentNode> children = container.children();
        assertThat(children).hasSize(1);
        return children.get(0);
    }

    private static byte[] onePixelPng() throws Exception {
        BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        image.setRGB(0, 0, java.awt.Color.WHITE.getRGB());
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return output.toByteArray();
    }
}
