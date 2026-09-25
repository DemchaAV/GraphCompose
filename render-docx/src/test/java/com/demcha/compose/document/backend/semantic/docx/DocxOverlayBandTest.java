package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.EllipseBuilder;
import com.demcha.compose.document.dsl.LayerStackBuilder;
import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.layout.PlacedNode;
import com.demcha.compose.document.node.LayerAlign;
import com.demcha.compose.document.node.LayerStackNode;
import com.demcha.compose.document.node.SpacerNode;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.style.DocumentTextStyle;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.junit.jupiter.api.Test;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPPr;

import java.io.ByteArrayInputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A stack of overlapping layers is written as one band, as the page places its content.
 *
 * <p>A CV sidebar opens with a monogram: a spacer as tall as the badge keeps the badge's place
 * in the flow, and the badge — a ring, drawn, and the initials centred in it — is laid over the
 * spacer. Written layer after layer, the initials came after the spacer's full height, 60pt
 * below where the page draws them, and the sidebar under them with it.</p>
 */
class DocxOverlayBandTest {

    private static final double BADGE = 80;

    @Test
    void aBadgeOverItsPlaceHolderIsWrittenWhereThePageDrawsIt() throws Exception {
        try (Export export = export()) {
            List<XWPFParagraph> paragraphs = export.document().getParagraphs();
            List<String> texts = paragraphs.stream().map(XWPFParagraph::getText).toList();

            assertThat(texts).as("the spacer that holds the badge's place is not written")
                    .containsExactly("Above", "JR", "Below");
            PlacedNode stack = export.placed("Frame");
            PlacedNode initials = export.placed("Initials");
            double above = (stack.placementY() + stack.placementHeight())
                           - (initials.placementY() + initials.placementHeight());
            double below = initials.placementY() - stack.placementY();
            assertThat(before(paragraphs.get(1))).as("the initials sit where the ring centres them")
                    .isCloseTo(Math.round((6 + above) * 20), org.assertj.core.data.Offset.offset(2L));
            assertThat(before(paragraphs.get(2))).as("and the badge keeps its height below them")
                    .isCloseTo(Math.round((below + 12) * 20), org.assertj.core.data.Offset.offset(2L));
        }
    }

    @Test
    void aDrawingThatIsNotWrittenStillTakesItsRoom() throws Exception {
        // A portrait drawn as a path, in the flow and inside a layer stack alike: none of it
        // reaches the file, and its height is still space above what follows.
        DocumentSession session = GraphCompose.document().pageSize(300, 500).margin(DocumentInsets.of(20)).create();
        session.pageFlow(page -> page
                .addParagraph(p -> p.text("Above"))
                // An icon is itself a stack of paths, inside the portrait's stack: held once.
                .addLayerStack(stack -> stack.name("Portrait")
                        .layer(new LayerStackBuilder().name("Icon")
                                .layer(new com.demcha.compose.document.dsl.PathBuilder().name("Face").size(50, 50)
                                        .moveTo(0, 0).lineTo(1, 0).lineTo(0.5, 1).closePath().build())
                                .layer(new com.demcha.compose.document.dsl.PathBuilder().name("Hair").size(50, 20)
                                        .moveTo(0, 0).lineTo(1, 0).lineTo(0.5, 1).closePath().build())
                                .build()))
                .addParagraph(p -> p.text("Below")));
        try (session; XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(
                session.export(new DocxSemanticBackend())))) {
            XWPFParagraph below = document.getParagraphs().stream()
                    .filter(paragraph -> "Below".equals(paragraph.getText())).findFirst().orElseThrow();

            assertThat(before(below)).isEqualTo(50L * 20);
        }
    }

    @Test
    void aCellOfSpaceAndDrawingKeepsItsHeight() throws Exception {
        // A masthead's hairline column: padding round a vertical line. Nothing in it is
        // written, and the row is as tall as it only if its space is still there.
        try (XWPFDocument document = DocxExports.withLayout(300, 500, 20, page -> page
                .addRow(row -> row
                        .addSection("Hairline", cell -> cell.spacing(0)
                                .padding(new DocumentInsets(10, 0, 10, 0))
                                .addLine(line -> line.name("Rule").vertical(40).thickness(1)
                                        .color(DocumentColor.rgb(0, 0, 0))))
                        .addParagraph(p -> p.text("Beside"))))) {
            var cell = document.getTables().get(0).getRow(0).getCell(0);
            XWPFParagraph holder = cell.getParagraphs().get(0);

            assertThat(before(holder)).as("10 above, the line's 40, 10 below").isEqualTo(60L * 20);
        }
    }

    private static long before(XWPFParagraph paragraph) {
        CTPPr properties = paragraph.getCTP().getPPr();
        if (properties == null || !properties.isSetSpacing() || !properties.getSpacing().isSetBefore()) {
            return 0;
        }
        return DocxTwips.of(properties.getSpacing().getBefore());
    }

    private static Export export() throws Exception {
        LayerStackNode badge = new LayerStackBuilder()
                .name("Badge")
                .back(new EllipseBuilder().name("Ring").size(BADGE, BADGE)
                        .stroke(DocumentStroke.of(DocumentColor.rgb(0, 0, 0), 1)).build())
                .layer(new ParagraphBuilder().name("Initials").text("JR")
                        .textStyle(DocumentTextStyle.DEFAULT.withSize(28)).build(), LayerAlign.CENTER)
                .build();
        DocumentSession session = GraphCompose.document().pageSize(300, 500).margin(DocumentInsets.of(20)).create();
        session.pageFlow(page -> page
                .addParagraph(p -> p.text("Above").margin(DocumentInsets.bottom(6)))
                .addLayerStack(frame -> frame
                        .name("Frame")
                        .margin(DocumentInsets.bottom(12))
                        .back(new SpacerNode("Space", 200, BADGE, DocumentInsets.zero(), DocumentInsets.zero()))
                        .layer(badge, LayerAlign.TOP_CENTER))
                .addParagraph(p -> p.text("Below")));
        XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(session.export(new DocxSemanticBackend())));
        return new Export(session, document);
    }

    private record Export(DocumentSession session, XWPFDocument document) implements AutoCloseable {

        PlacedNode placed(String name) {
            return session.layoutGraph().nodes().stream()
                    .filter(node -> name.equals(node.semanticName()))
                    .findFirst()
                    .orElseThrow();
        }

        @Override
        public void close() throws Exception {
            document.close();
            session.close();
        }
    }
}
