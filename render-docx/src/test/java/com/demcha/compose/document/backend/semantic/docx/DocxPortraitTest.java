package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.ImageBuilder;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.dsl.ShapeContainerBuilder;
import com.demcha.compose.document.image.DocumentImageData;
import com.demcha.compose.document.layout.PlacedNode;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.LayerAlign;
import com.demcha.compose.document.style.ClipPolicy;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentRowColumn;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A portrait in a ring: the photo the size the page draws it, standing as far inside the ring as
 * the page sets it.
 *
 * <p>{@code NavySidebar}'s portrait is a 127pt ring round a 123.8pt photo, in a column 123.8pt
 * wide. Sized from the width left inside the ring, the photo came out 120.6pt; written flush with
 * the ring's top, it lost the 1.6pt the ring holds round it above and below.</p>
 */
class DocxPortraitTest {

    private static final double PHOTO = 123.8;
    private static final double RING = 1.6;
    /** What the default text's line under the frame is raised into the space above it. */
    private static final long RAISE = DocxExports.DEFAULT_LINE_RAISE;

    @Test
    void aPhotoInARingWiderThanItsColumnKeepsItsSize() throws Exception {
        try (XWPFDocument document = portrait()) {
            XWPFParagraph photo = photoParagraph(document);
            long width = photo.getCTP().getRList().get(0).getDrawingArray(0).getInlineArray(0).getExtent().getCx();

            assertThat(width).as("the photo the page draws, not the width left inside the ring")
                    .isEqualTo(Math.round(PHOTO * 12700));
        }
    }

    @Test
    void aPaddedPictureIsTheSizeItAskedFor() throws Exception {
        // Its placement holds its padding, which is written as the space around its paragraph.
        try (XWPFDocument document = DocxExports.withLayout(400, 400, 20, page -> page
                .addImage(image -> image.name("Logo").source(DocumentImageData.fromBytes(pngBytes()))
                        .size(100, 50).padding(DocumentInsets.of(10))))) {
            XWPFParagraph logo = pictureIn(document.getParagraphs());
            var extent = logo.getRuns().get(0).getCTR().getDrawingArray(0).getInlineArray(0).getExtent();

            assertThat(extent.getCx()).isEqualTo(100L * 12700);
            assertThat(extent.getCy()).isEqualTo(50L * 12700);
            assertThat(beforeOf(logo)).as("its top padding, as the space above its paragraph").isEqualTo(10 * 20L);
        }
    }

    @Test
    void aLayersOwnMarginIsWrittenWithItAndNotTwice() throws Exception {
        // The layer's top margin is part of the space the frame holds above it: written once,
        // the picture stands where the page puts it.
        Consumer<SectionBuilder> frame = sidebar -> sidebar.addContainer(box -> box.name("Frame")
                .rectangle(100, 60)
                .center(picture(new DocumentInsets(6, 0, 0, 0))));
        double[] gaps = gapsAround(frame);
        try (XWPFDocument document = inSidebar(frame)) {
            List<XWPFParagraph> paragraphs = sidebar(document);
            XWPFParagraph picture = pictureIn(paragraphs);

            assertThat(beforeOf(picture)).isEqualTo(Math.round((25 + gaps[0]) * 20));
            assertThat(beforeOf(paragraphs.get(paragraphs.indexOf(picture) + 1)))
                    .isEqualTo(Math.round((gaps[1] + 22) * 20) - RAISE);
        }
    }

    @Test
    void aLayerMovedPastTheBottomLeavesTheFrameNoTallerThanItIs() throws Exception {
        Consumer<SectionBuilder> frame = sidebar -> sidebar.addContainer(box -> box.name("Frame")
                .rectangle(100, 60)
                .position(picture(DocumentInsets.zero()), 0, 30, LayerAlign.CENTER));
        double[] gaps = gapsAround(frame);
        assertThat(gaps[1]).as("the picture reaches past the frame's foot").isNegative();
        try (XWPFDocument document = inSidebar(frame)) {
            List<XWPFParagraph> paragraphs = sidebar(document);
            XWPFParagraph picture = pictureIn(paragraphs);
            // Less the 22pt above the heading under it, raised into that space.
            long below = beforeOf(paragraphs.get(paragraphs.indexOf(picture) + 1)) + RAISE - 22 * 20L;

            assertThat(beforeOf(picture) - 25 * 20L + 40 * 20L + below)
                    .as("the space above the picture, the picture and the space under it: the frame's 60pt")
                    .isEqualTo(60 * 20L);
            assertThat(below).as("all of what the frame holds is above the picture").isZero();
            assertThat(beforeOf(picture)).isEqualTo(Math.round((25 + gaps[0] + gaps[1]) * 20));
        }
    }

    @Test
    void aLayerMovedPastTheTopLeavesTheFrameNoTallerThanItIs() throws Exception {
        Consumer<SectionBuilder> frame = sidebar -> sidebar.addContainer(box -> box.name("Frame")
                .rectangle(100, 60)
                .position(picture(DocumentInsets.zero()), 0, -30, LayerAlign.CENTER));
        double[] gaps = gapsAround(frame);
        assertThat(gaps[0]).as("the picture reaches past the frame's top").isNegative();
        try (XWPFDocument document = inSidebar(frame)) {
            List<XWPFParagraph> paragraphs = sidebar(document);
            XWPFParagraph picture = pictureIn(paragraphs);

            assertThat(beforeOf(picture)).as("none of the frame above the picture").isEqualTo(25 * 20L);
            assertThat(beforeOf(paragraphs.get(paragraphs.indexOf(picture) + 1)))
                    .as("all of what it holds under it")
                    .isEqualTo(Math.round((gaps[0] + gaps[1] + 22) * 20) - RAISE);
        }
    }

    @Test
    void aLineMovedPastTheFootStandsWhereThePagePutsIt() throws Exception {
        // Its overhang is taken from the gap under the frame, so the space above it is all of it.
        Consumer<SectionBuilder> frame = sidebar -> sidebar.addContainer(box -> box.name("Frame")
                .rectangle(100, 40)
                .position(new ParagraphBuilder().name("Pic").text("Lead").build(), 0, 20, LayerAlign.CENTER));
        double[] gaps = gapsAround(frame);
        assertThat(gaps[1]).as("the line reaches past the frame's foot").isNegative();
        try (XWPFDocument document = inSidebar(frame)) {
            XWPFParagraph line = sidebar(document).stream().filter(p -> p.getText().equals("Lead"))
                    .findFirst().orElseThrow();

            assertThat(beforeOf(line)).isEqualTo(Math.round((25 + gaps[0]) * 20));
        }
    }

    @Test
    void aPhotoStandsAsFarInsideItsRingAsThePageSetsIt() throws Exception {
        try (XWPFDocument document = portrait()) {
            List<XWPFParagraph> paragraphs = sidebar(document);
            XWPFParagraph photo = photoParagraph(document);
            XWPFParagraph heading = paragraphs.get(paragraphs.indexOf(photo) + 1);

            assertThat(beforeOf(photo)).as("the sidebar's 25pt and the ring above the photo")
                    .isEqualTo(Math.round((25 + RING) * 20));
            assertThat(beforeOf(heading)).as("the ring below the photo, and the heading's 22pt")
                    .isEqualTo(Math.round((RING + 22) * 20) - RAISE);
        }
    }

    private static XWPFDocument portrait() throws Exception {
        // As the template lays it: a sidebar column of a row, padded to 123.8pt, the ring in a
        // section of its own at its top.
        double pad = (180 - PHOTO) / 2;
        return DocxExports.withLayout(400, 400, 20, page -> page.addRow("Page", row -> row
                .columns(DocumentRowColumn.fixed(180),
                        DocumentRowColumn.weight(1))
                .addSection("Sidebar", sidebar -> sidebar.padding(new DocumentInsets(25, pad, 0, pad))
                .addSection("Avatar", avatar -> avatar.addContainer(ring -> ring.name("AvatarRing")
                        .circle(PHOTO + 2 * RING)
                        .fillColor(DocumentColor.rgb(220, 224, 230))
                        .center(new ShapeContainerBuilder().name("AvatarCircle").circle(PHOTO)
                                .clipPolicy(ClipPolicy.CLIP_PATH)
                                .fillColor(DocumentColor.rgb(20, 40, 70))
                                .center(new ImageBuilder().name("AvatarPhoto")
                                        .source(DocumentImageData.fromBytes(pngBytes()))
                                        .size(PHOTO, PHOTO).build())
                                .build())))
                .addParagraph(p -> p.text("CONTACT").margin(new DocumentInsets(22, 0, 0, 0))))
                .addParagraph(p -> p.text("Main"))));
    }

    /** A 40pt picture with the margin given. */
    private static DocumentNode picture(DocumentInsets margin) {
        return new ImageBuilder().name("Pic").source(DocumentImageData.fromBytes(pngBytes()))
                .size(40, 40).margin(margin).build();
    }

    /** The section, with a CONTACT heading 22pt under it, as the sidebar of a page's row. */
    private static XWPFDocument inSidebar(Consumer<SectionBuilder> content) throws Exception {
        return DocxExports.withLayout(400, 400, 20, page -> sidebarRow(page, content));
    }

    private static void sidebarRow(PageFlowBuilder page, Consumer<SectionBuilder> content) {
        page.addRow("Page", row -> row
                .columns(DocumentRowColumn.fixed(180), DocumentRowColumn.weight(1))
                .addSection("Sidebar", sidebar -> {
                    sidebar.padding(new DocumentInsets(25, 20, 0, 20));
                    content.accept(sidebar);
                    sidebar.addParagraph(p -> p.text("CONTACT").margin(new DocumentInsets(22, 0, 0, 0)));
                })
                .addParagraph(p -> p.text("Main")));
    }

    /** Where the page puts the layer named Pic in its frame: the space above it and under it. */
    private static double[] gapsAround(Consumer<SectionBuilder> content) {
        try (DocumentSession session = GraphCompose.document().pageSize(400, 400)
                .margin(DocumentInsets.of(20)).create()) {
            session.pageFlow(page -> sidebarRow(page, content));
            PlacedNode frame = placed(session, "Frame");
            PlacedNode picture = placed(session, "Pic");
            return new double[]{
                    frame.placementY() + frame.placementHeight() - picture.placementY() - picture.placementHeight(),
                    picture.placementY() - frame.placementY()};
        }
    }

    private static PlacedNode placed(DocumentSession session, String name) {
        return session.layoutGraph().nodes().stream()
                .filter(node -> name.equals(node.semanticName()))
                .findFirst().orElseThrow();
    }

    private static XWPFParagraph pictureIn(List<XWPFParagraph> paragraphs) {
        return paragraphs.stream()
                .filter(p -> !p.getRuns().isEmpty() && p.getRuns().get(0).getCTR().sizeOfDrawingArray() > 0
                             && p.getRuns().get(0).getCTR().getDrawingArray(0).sizeOfInlineArray() > 0)
                .findFirst().orElseThrow();
    }

    /** The sidebar cell's paragraphs. */
    private static List<XWPFParagraph> sidebar(XWPFDocument document) {
        return document.getTables().get(0).getRow(0).getCell(0).getParagraphs();
    }

    private static XWPFParagraph photoParagraph(XWPFDocument document) {
        return pictureIn(sidebar(document));
    }

    private static long beforeOf(XWPFParagraph paragraph) {
        var spacing = paragraph.getCTP().getPPr().getSpacing();
        return spacing.isSetBefore() ? DocxTwips.of(spacing.getBefore()) : 0;
    }

    private static byte[] pngBytes() {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(new BufferedImage(64, 64, BufferedImage.TYPE_INT_RGB), "png", out);
            return out.toByteArray();
        } catch (Exception failure) {
            throw new IllegalStateException(failure);
        }
    }
}
