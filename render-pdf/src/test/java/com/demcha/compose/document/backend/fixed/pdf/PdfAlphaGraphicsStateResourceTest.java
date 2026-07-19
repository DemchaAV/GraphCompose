package com.demcha.compose.document.backend.fixed.pdf;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.LineBuilder;
import com.demcha.compose.document.dsl.PageBreakBuilder;
import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.dsl.TableBuilder;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.table.DocumentTableStyle;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Alpha writes reuse one shared {@code PDExtendedGraphicsState} per distinct
 * (channel, alpha) pair for the whole render pass, so a page's
 * {@code /ExtGState} resources stay bounded by the number of distinct alpha
 * values instead of growing with every translucent draw — and fully opaque
 * documents carry no {@code /ExtGState} resources at all.
 */
class PdfAlphaGraphicsStateResourceTest {

    private static byte[] render(Consumer<DocumentSession> content) throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(400, 400)
                .margin(DocumentInsets.of(20))
                .create()) {
            content.accept(session);
            return session.toPdfBytes();
        }
    }

    private static List<PDExtendedGraphicsState> extGStates(PDPage page) {
        List<PDExtendedGraphicsState> states = new ArrayList<>();
        for (COSName name : page.getResources().getExtGStateNames()) {
            states.add(page.getResources().getExtGState(name));
        }
        return states;
    }

    @Test
    void alternatingTranslucentRunsShareOneStatePerDistinctAlpha() throws Exception {
        DocumentTextStyle opaque = DocumentTextStyle.builder().size(12).build();
        DocumentTextStyle faded = DocumentTextStyle.builder().size(12)
                .color(DocumentColor.rgba(0, 0, 0, 100)).build();
        byte[] pdf = render(session -> {
            ParagraphBuilder paragraph = new ParagraphBuilder().name("Runs");
            for (int i = 0; i < 8; i++) {
                paragraph.inlineText("DARK", opaque).inlineText(" dim ", faded);
            }
            session.add(paragraph.build());
        });
        try (PDDocument document = Loader.loadPDF(pdf)) {
            List<PDExtendedGraphicsState> states = extGStates(document.getPage(0));
            // 16 alpha flips reuse two constants: 100/255 and the 1.0 reset.
            assertThat(states)
                    .as("distinct fill alphas, not one resource per alpha flip")
                    .hasSize(2);
            assertThat(states)
                    .extracting(PDExtendedGraphicsState::getNonStrokingAlphaConstant)
                    .containsExactlyInAnyOrder(100f / 255f, 1f);
        }
    }

    @Test
    void fillAndStrokeChannelsKeepSeparateStates() throws Exception {
        byte[] pdf = render(session -> {
            session.add(new ParagraphBuilder().name("Fill")
                    .text("dim text")
                    .textStyle(DocumentTextStyle.builder().size(12)
                            .color(DocumentColor.rgba(0, 0, 0, 100)).build())
                    .build());
            session.add(new LineBuilder().name("Stroke")
                    .horizontal(200).thickness(3)
                    .color(DocumentColor.rgba(0, 0, 0, 100))
                    .build());
        });
        try (PDDocument document = Loader.loadPDF(pdf)) {
            List<PDExtendedGraphicsState> states = extGStates(document.getPage(0));
            // Same alpha value, but the non-stroking and stroking constants
            // live in separate shared states (plus the text run's 1.0 reset
            // before the line fragment is not emitted — the paragraph's q..Q
            // restores opacity, so only the two translucent states remain).
            assertThat(states).hasSize(2);
            assertThat(states)
                    .extracting(PDExtendedGraphicsState::getNonStrokingAlphaConstant)
                    .containsExactlyInAnyOrder(100f / 255f, null);
            assertThat(states)
                    .extracting(PDExtendedGraphicsState::getStrokingAlphaConstant)
                    .containsExactlyInAnyOrder(100f / 255f, null);
        }
    }

    @Test
    void repeatedTranslucentStrokesAcrossConsumersShareOneState() throws Exception {
        // Three lines and a bordered table all stroke with the same rgba —
        // the old per-emission minting registered one /ExtGState each.
        byte[] pdf = render(session -> {
            for (int i = 0; i < 3; i++) {
                session.add(new LineBuilder().name("Rule" + i)
                        .horizontal(200).thickness(3)
                        .color(DocumentColor.rgba(0, 0, 0, 100))
                        .build());
            }
            session.add(new TableBuilder().name("Borders")
                    .defaultCellStyle(DocumentTableStyle.builder()
                            .stroke(DocumentStroke.of(DocumentColor.rgba(0, 0, 0, 100), 3))
                            .build())
                    .row("")
                    .build());
        });
        try (PDDocument document = Loader.loadPDF(pdf)) {
            List<PDExtendedGraphicsState> states = extGStates(document.getPage(0));
            assertThat(states)
                    .as("one shared stroking state, not one per stroked consumer")
                    .hasSize(1);
            assertThat(states.get(0).getStrokingAlphaConstant()).isEqualTo(100f / 255f);
        }
    }

    @Test
    void repeatedTranslucentFillsAcrossRowsShareOneState() throws Exception {
        // Six cell fills with the same rgba exercise the colour-based
        // applyFillAlpha path (shapes, table paint, chips, marks) — the old
        // per-emission minting registered one /ExtGState per row.
        byte[] pdf = render(session -> {
            TableBuilder table = new TableBuilder().name("Fills")
                    .defaultCellStyle(DocumentTableStyle.builder()
                            .fillColor(DocumentColor.rgba(0, 0, 160, 100))
                            .stroke(DocumentStroke.of(DocumentColor.WHITE, 0))
                            .build());
            for (int i = 0; i < 6; i++) {
                table.row("");
            }
            session.add(table.build());
        });
        try (PDDocument document = Loader.loadPDF(pdf)) {
            List<PDExtendedGraphicsState> states = extGStates(document.getPage(0));
            assertThat(states)
                    .as("one shared fill state, not one per translucent cell fill")
                    .hasSize(1);
            assertThat(states.get(0).getNonStrokingAlphaConstant()).isEqualTo(100f / 255f);
        }
    }

    @Test
    void anOpaqueDocumentCarriesNoExtGStateResources() throws Exception {
        // Text, line, and table paint each have their own opaque early-return
        // — one document pins all of them at the resource level.
        byte[] pdf = render(session -> {
            session.add(new ParagraphBuilder().name("Opaque")
                    .text("plain opaque text")
                    .textStyle(DocumentTextStyle.builder().size(12).build())
                    .build());
            session.add(new LineBuilder().name("Rule")
                    .horizontal(200).thickness(3)
                    .color(DocumentColor.rgb(0, 0, 0))
                    .build());
            session.add(new TableBuilder().name("Table")
                    .defaultCellStyle(DocumentTableStyle.builder()
                            .fillColor(DocumentColor.rgb(230, 230, 240))
                            .stroke(DocumentStroke.of(DocumentColor.rgb(0, 0, 0), 1))
                            .build())
                    .row("cell")
                    .build());
        });
        try (PDDocument document = Loader.loadPDF(pdf)) {
            assertThat(extGStates(document.getPage(0)))
                    .as("opaque documents stay byte-identical — no alpha resources")
                    .isEmpty();
        }
    }

    @Test
    void sharedStatesRegisterPerPageInAMultiPageDocument() throws Exception {
        DocumentTextStyle faded = DocumentTextStyle.builder().size(12)
                .color(DocumentColor.rgba(0, 0, 0, 100)).build();
        byte[] pdf = render(session -> {
            session.add(new ParagraphBuilder().name("First")
                    .text("dim on page one").textStyle(faded).build());
            session.add(new PageBreakBuilder().name("Break").build());
            session.add(new ParagraphBuilder().name("Second")
                    .text("dim on page two").textStyle(faded).build());
            session.add(new PageBreakBuilder().name("Break2").build());
            session.add(new ParagraphBuilder().name("Third")
                    .text("opaque on page three")
                    .textStyle(DocumentTextStyle.builder().size(12).build())
                    .build());
        });
        try (PDDocument document = Loader.loadPDF(pdf)) {
            assertThat(document.getNumberOfPages()).isEqualTo(3);
            for (int pageIndex = 0; pageIndex < 2; pageIndex++) {
                assertThat(extGStates(document.getPage(pageIndex)))
                        .extracting(PDExtendedGraphicsState::getNonStrokingAlphaConstant)
                        .containsExactly(100f / 255f);
            }
            // Shared states register only on the pages that use them — an
            // eager cache that pre-registers every state would leave a stray
            // entry on the opaque page.
            assertThat(extGStates(document.getPage(2))).isEmpty();
        }
    }
}
