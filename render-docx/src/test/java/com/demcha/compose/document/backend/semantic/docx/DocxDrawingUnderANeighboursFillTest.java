package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.LayerStackBuilder;
import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.dsl.ShapeBuilder;
import com.demcha.compose.document.dsl.ShapeContainerBuilder;
import com.demcha.compose.document.node.LayerAlign;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentStroke;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A panel is written as a shaded cell, and a cell's fill belongs to the text layer, which Word and
 * LibreOffice both paint over everything drawn behind that layer. So a drawing that stands beside
 * a painted panel — on a layer above it, not inside it — and is drawn behind the text is covered
 * by that panel's shading, and the reader never sees it.
 *
 * <p>Measured on the fidelity corpus in Word 16.0.20527 and LibreOffice 24.2, at 300 dpi: the one
 * document of the seventy-one that holds the case lays an 18pt icon over a card's top right corner
 * through a layer stack. Drawn behind the text, the engine's PDF draws 4308 pixels of the icon's
 * own ink, Word's draws none, and LibreOffice's draws none — not one pixel anywhere on the page,
 * the icon's whole box coming back the card's colour. With the card unpainted, the one difference,
 * Word draws every pixel the engine does.</p>
 *
 * <p>So the stack's order is carried into the document: a layer the page paints after a painted one
 * is drawn in front of the text ({@code laidOverAPaintedNeighbour}). What stays behind the text is
 * what the order or the flow keeps there — a drawing a painted layer is laid over in turn, and one
 * whose box holds a line of the flow, which in front it would cover — and that the export names,
 * rather than losing it in silence.</p>
 *
 * <p>A drawing standing <em>inside</em> a painted panel is a different case with its own note and
 * its own tests ({@code DocxDrawingsTest}): there the surface it stands on decides. Measured the
 * same way, that case has no instance at all in the corpus — its branch is taken 55 times over 14
 * documents and goes in front every one of them.</p>
 *
 * @author Artem Demchyshyn
 */
class DocxDrawingUnderANeighboursFillTest {

    private static final DocumentColor BAND = DocumentColor.rgb(90, 120, 200);
    private static final DocumentColor ACCENT = DocumentColor.rgb(26, 86, 148);
    private static final DocumentColor OVER = DocumentColor.rgb(10, 170, 90);

    /** The ink of the pin, as a shape's fill is written: what to pick its anchor out by. */
    private static final String PIN_INK = "1A5694";

    private static final String COVERED = "a panel painted beside it";

    @Test
    void aShapeOverAPaintedNeighbourStandsInFrontOfIt() throws Exception {
        Export export = export(session -> session.pageFlow(page -> page.add(pinOverABand(BAND, 150, 2))));

        assertThat(pinAnchor(export))
                .as("the stack paints the pin over the band, so the document draws it in front")
                .contains("behindDoc=\"0\"");
        assertThat(covered(export.report()))
                .as("nothing to name: the pin is not behind the text at all")
                .isEmpty();
        assertThat(detailsOf(export.report(), "/Pin"))
                .anySatisfy(detail -> assertThat(detail).contains(
                        "in front of the text, since a layer the page paints under it carries a fill"));
    }

    @Test
    void aShapeOverAnUnpaintedNeighbourStaysBehindTheText() throws Exception {
        // The one difference from the fixture above. With no fill beside it nothing is painted over
        // the pin, and Word draws all 3237 pixels of it against the engine's 3237 with the anchor
        // behind the text — which is why behindDoc alone says nothing about whether it is seen.
        Export export = export(session -> session.pageFlow(page -> page
                .add(new LayerStackBuilder().name("Stack")
                        .layer(new SectionBuilder().name("Band").fixedWidth(200)
                                        .addParagraph(p -> p.name("Stage").text("Stage two")).build(),
                                LayerAlign.TOP_LEFT, 0)
                        .position(new ShapeBuilder().name("Pin").size(30, 14).fillColor(ACCENT).build(),
                                150, 2, LayerAlign.TOP_LEFT, 1)
                        .build())));

        assertThat(pinAnchor(export)).contains("behindDoc=\"1\"");
        assertThat(covered(export.report())).isEmpty();
    }

    @Test
    void aShapeOverANeighbourHoldingAPaintedPanelStandsInFrontOfIt() throws Exception {
        // The fill is a card inside the neighbour rather than the layer itself: the cell written
        // for that card paints over the pin just the same, so the neighbour counts as painted by
        // what it holds and not only by what it is.
        Export export = export(session -> session.pageFlow(page -> page
                .add(new LayerStackBuilder().name("Stack")
                        .layer(new SectionBuilder().name("Band").fixedWidth(200)
                                        .addSection("Card", card -> card.fillColor(BAND)
                                                .addParagraph(p -> p.name("Stage").text("Stage two")))
                                        .build(),
                                LayerAlign.TOP_LEFT, 0)
                        .position(new ShapeBuilder().name("Pin").size(30, 14).fillColor(ACCENT).build(),
                                150, 2, LayerAlign.TOP_LEFT, 1)
                        .build())));

        assertThat(pinAnchor(export)).contains("behindDoc=\"0\"");
        assertThat(covered(export.report())).isEmpty();
    }

    @Test
    void aShapeOverANeighbourBearingBordersAloneStaysBehindTheText() throws Exception {
        // A neighbour covers only where a fill is written for it. One bearing borders alone writes
        // no shading, so there is nothing to paint over the pin: it keeps the place the overlays
        // round it give it, and nothing is named.
        Export export = export(session -> session.pageFlow(page -> page
                .add(new LayerStackBuilder().name("Stack")
                        .layer(new SectionBuilder().name("Band")
                                        .stroke(DocumentStroke.of(ACCENT, 1)).fixedWidth(200)
                                        .addParagraph(p -> p.name("Stage").text("Stage two")).build(),
                                LayerAlign.TOP_LEFT, 0)
                        .position(new ShapeBuilder().name("Pin").size(30, 14).fillColor(ACCENT).build(),
                                150, 2, LayerAlign.TOP_LEFT, 1)
                        .build())));

        assertThat(pinAnchor(export)).contains("behindDoc=\"1\"");
        assertThat(covered(export.report())).isEmpty();
    }

    @Test
    void aShapeAPaintedNeighbourIsLaidOverInTurnStaysBehindAndIsNamed() throws Exception {
        // The page paints a third layer, painted, over the pin. There the pin belongs behind that
        // layer's shading, and in front it would cover the shading instead — the same loss the
        // other way round. So it stays behind the text, where the layer under it covers it, and
        // the export says so.
        Export export = export(session -> session.pageFlow(page -> page
                .add(new LayerStackBuilder().name("Stack")
                        .layer(new SectionBuilder().name("Lower").fillColor(BAND).fixedWidth(200)
                                        .addParagraph(p -> p.name("Stage").text("Stage two")).build(),
                                LayerAlign.TOP_LEFT, 0)
                        .position(new ShapeBuilder().name("Pin").size(30, 14).fillColor(ACCENT).build(),
                                150, 2, LayerAlign.TOP_LEFT, 1)
                        .layer(new SectionBuilder().name("Upper").fillColor(OVER).fixedWidth(200)
                                        .addParagraph(p -> p.name("Over").text("Over it")).build(),
                                LayerAlign.TOP_LEFT, 2)
                        .build())));

        assertThat(pinAnchor(export)).contains("behindDoc=\"1\"");
        assertThat(covered(export.report())).singleElement().asString()
                .as("the pin, and the panel that covers it, both named")
                .contains("Pin").contains("Lower[0]").contains(COVERED);
    }

    @Test
    void aShapeOverAPaintedNeighbourFramingALineOfTheFlowStaysBehindAndIsNamed() throws Exception {
        // In front it would be painted over the band's own line of text. A drawing that frames a
        // line of the flow stays behind it, as it does inside a painted panel, and what the
        // neighbour's shading then takes of it is named.
        Export export = export(session -> session.pageFlow(page -> page
                .add(new LayerStackBuilder().name("Stack")
                        .layer(new SectionBuilder().name("Band").fillColor(BAND).fixedWidth(200)
                                        .addParagraph(p -> p.name("Stage").text("Stage two")).build(),
                                LayerAlign.TOP_LEFT, 0)
                        .position(new ShapeBuilder().name("Pin").size(190, 30).fillColor(ACCENT).build(),
                                2, 0, LayerAlign.TOP_LEFT, 1)
                        .build())));

        assertThat(pinAnchor(export)).contains("behindDoc=\"1\"");
        assertThat(covered(export.report())).singleElement().asString()
                .contains("Pin").contains("Band[0]").contains(COVERED);
    }

    @Test
    void aStackWhoseUpperShapeMustStayBehindKeepsEveryShapeOfItThere() throws Exception {
        // Two shapes over one painted band: the lower clear of the band's line of text, the upper
        // holding it. Word draws every shape in front over every shape behind, so lifting the
        // lower one alone would draw it over the upper one, which the page draws over it — the
        // loss this rule fixes, made worse. The stack's order is kept whole: neither is lifted,
        // and the panel beside them is named for both.
        Export export = export(session -> session.pageFlow(page -> page
                .add(new LayerStackBuilder().name("Stack")
                        .layer(new SectionBuilder().name("Band").fillColor(BAND).fixedWidth(200)
                                        .addParagraph(p -> p.name("Stage").text("Stage two")).build(),
                                LayerAlign.TOP_LEFT, 0)
                        .position(new ShapeBuilder().name("Pin").size(30, 14).fillColor(ACCENT).build(),
                                150, 2, LayerAlign.TOP_LEFT, 1)
                        .position(new ShapeBuilder().name("Over").size(190, 30).fillColor(OVER).build(),
                                2, 0, LayerAlign.TOP_LEFT, 2)
                        .build())));

        assertThat(pinAnchor(export))
                .as("the lower shape stays behind with the upper one, not over it")
                .contains("behindDoc=\"1\"");
        assertThat(covered(export.report()))
                .as("both named as covered by the band, since both are behind the text")
                .hasSize(2);
    }

    @Test
    void aLayerBearingTextOverThePaintedOneDoesNotKeepTheShapeBehind() throws Exception {
        // What keeps a shape behind is another DRAWING painted over it, not any layer that
        // happens to carry text. The words are a layer above the pin, so they are asked — and
        // they draw nothing, so they cover nothing. Counting their own fragment as one a shape is
        // drawn from refused the lift for stacks of the shape `[painted background, text, icons]`,
        // which is the ordinary layer stack, so the rule fired almost nowhere.
        Export export = export(session -> session.pageFlow(page -> page
                .add(new LayerStackBuilder().name("Stack")
                        .layer(new SectionBuilder().name("Band").fillColor(BAND).fixedWidth(200)
                                        .addParagraph(p -> p.name("Stage").text("Stage two")).build(),
                                LayerAlign.TOP_LEFT, 0)
                        .position(new ShapeBuilder().name("Pin").size(30, 14).fillColor(ACCENT).build(),
                                150, 2, LayerAlign.TOP_LEFT, 1)
                        .position(new SectionBuilder().name("Words").fixedWidth(90)
                                        .addParagraph(p -> p.name("Note").text("A line of words")).build(),
                                2, 40, LayerAlign.TOP_LEFT, 2)
                        .build())));

        assertThat(pinAnchor(export))
                .as("the words are painted over the pin but draw nothing, so they cover nothing "
                    + "and keep it behind nothing")
                .contains("behindDoc=\"0\"");
        assertThat(covered(export.report())).isEmpty();
    }

    @Test
    void anUnpaintedShapeContainerOverThePaintedLayerKeepsNothingBehindIt() throws Exception {
        // A shape container that paints nothing draws no shape, so it covers nothing and must not
        // refuse the lift. Its box is asked of nothing: only the fragments a shape is actually
        // drawn from are, which is what tells this container from the badge of the test above.
        Export export = export(session -> session.pageFlow(page -> page
                .add(new LayerStackBuilder().name("Stack")
                        .layer(new SectionBuilder().name("Band").fillColor(BAND).fixedWidth(200)
                                        .addParagraph(p -> p.name("Stage").text("Stage two")).build(),
                                LayerAlign.TOP_LEFT, 0)
                        .position(new ShapeContainerBuilder().name("Clear").rectangle(90, 20)
                                        .center(new ParagraphBuilder().name("Inside").text("Clear").build())
                                        .build(),
                                2, 40, LayerAlign.TOP_LEFT, 1)
                        .position(new ShapeBuilder().name("Pin").size(30, 14).fillColor(ACCENT).build(),
                                150, 2, LayerAlign.TOP_LEFT, 2)
                        .build())));

        assertThat(pinAnchor(export)).contains("behindDoc=\"0\"");
        assertThat(covered(export.report())).isEmpty();
    }

    @Test
    void aShapeOverABadgeAndAPaintedLayerIsStillLifted() throws Exception {
        // What the rule asks is whether anything is drawn OVER the shape, not whether anything is
        // drawn at all. The badge is painted before the pin, so the file keeps the pin above it
        // just as the page does, and the lift is safe whatever the badge's own place turns out to
        // be.
        Export export = export(session -> session.pageFlow(page -> page
                .add(new LayerStackBuilder().name("Stack")
                        .layer(new SectionBuilder().name("Band").fillColor(BAND).fixedWidth(200)
                                        .addParagraph(p -> p.name("Stage").text("Stage two")).build(),
                                LayerAlign.TOP_LEFT, 0)
                        .position(new ShapeContainerBuilder().name("Badge").circle(28).fillColor(OVER)
                                        .center(new ParagraphBuilder().text("JR").build()).build(),
                                40, 2, LayerAlign.TOP_LEFT, 1)
                        .position(new ShapeBuilder().name("Pin").size(30, 14).fillColor(ACCENT).build(),
                                150, 2, LayerAlign.TOP_LEFT, 2)
                        .build())));

        assertThat(pinAnchor(export))
                .as("the pin is lifted, the badge standing under it on the page and in the file")
                .contains("behindDoc=\"0\"");
        assertThat(covered(export.report()))
                .as("nothing covers the pin, so nothing is named for it")
                .noneMatch(named -> named.contains("/Pin"));
    }

    @Test
    void aShapeOverABorderedNeighbourAndAPaintedLayerIsStillLifted() throws Exception {
        // The bordered wrapper draws — the layout gives it a fragment a shape is drawn from — but
        // it is painted before the pin, so it is not something drawn over the pin and the lift
        // stands.
        Export export = export(session -> session.pageFlow(page -> page
                .add(new LayerStackBuilder().name("Stack")
                        .layer(new SectionBuilder().name("Band").fillColor(BAND).fixedWidth(200)
                                        .addParagraph(p -> p.name("Stage").text("Stage two")).build(),
                                LayerAlign.TOP_LEFT, 0)
                        .position(new SectionBuilder().name("Outlined").fixedWidth(90)
                                        .stroke(DocumentStroke.of(ACCENT, 1))
                                        .addParagraph(p -> p.name("Note").text("In a box")).build(),
                                2, 40, LayerAlign.TOP_LEFT, 1)
                        .position(new ShapeBuilder().name("Pin").size(30, 14).fillColor(ACCENT).build(),
                                150, 2, LayerAlign.TOP_LEFT, 2)
                        .build())));

        assertThat(pinAnchor(export)).contains("behindDoc=\"0\"");
        assertThat(covered(export.report())).noneMatch(named -> named.contains("/Pin"));
    }

    @Test
    void aShapeStandingInTwoStacksAtOnceIsNotLifted() throws Exception {
        // One instance used in two stacks: the layout keeps one path and one box for it, so an
        // answer taken at one of its places is given at both. Here the first place is alone over
        // a painted band, where a lift would be right, and the second is under a hood that the
        // page paints over it, where the same lift draws the shape over the hood.
        com.demcha.compose.document.node.DocumentNode shared =
                new ShapeBuilder().name("Pin").size(30, 14).fillColor(ACCENT).build();
        Export export = export(session -> session.pageFlow(page -> page
                .add(new LayerStackBuilder().name("Painted")
                        .layer(new SectionBuilder().name("Band").fillColor(BAND).fixedWidth(200)
                                        .addParagraph(p -> p.name("Stage").text("Stage two")).build(),
                                LayerAlign.TOP_LEFT, 0)
                        .position(shared, 150, 2, LayerAlign.TOP_LEFT, 1)
                        .build())
                .add(new LayerStackBuilder().name("Hooded")
                        .layer(new SectionBuilder().name("Band two").fillColor(BAND).fixedWidth(200)
                                        .addParagraph(p -> p.name("Other").text("Stage three")).build(),
                                LayerAlign.TOP_LEFT, 0)
                        .position(shared, 150, 2, LayerAlign.TOP_LEFT, 1)
                        .position(new ShapeBuilder().name("Hood").size(40, 20).fillColor(OVER).build(),
                                145, 0, LayerAlign.TOP_LEFT, 2)
                        .build())));

        assertThat(anchorsHolding(export, PIN_INK))
                .as("both places behind the text, neither answered from the other's")
                .hasSizeGreaterThan(1)
                .allSatisfy(anchor -> assertThat(anchor).contains("behindDoc=\"1\""));
    }

    @Test
    void aShapeOfANestedStackUnderAnOuterStacksDrawingIsNotLifted() throws Exception {
        // The painted layer is in the inner stack, and the drawing that would be inverted is a
        // layer of the OUTER one — painted after the inner stack whole. Asking only the overlay
        // that holds the painted layer left this lifted: measured, the pin came back
        // behindDoc="0" with relativeHeight below the hood's, so Word drew the pin over a hood
        // the page draws over it.
        Export export = export(session -> session.pageFlow(page -> page
                .add(new LayerStackBuilder().name("Outer")
                        .layer(new LayerStackBuilder().name("Inner")
                                        .layer(new SectionBuilder().name("Band").fillColor(BAND)
                                                        .fixedWidth(200)
                                                        .addParagraph(p -> p.name("Stage").text("Stage two"))
                                                        .build(),
                                                LayerAlign.TOP_LEFT, 0)
                                        .position(new ShapeBuilder().name("Pin").size(120, 30)
                                                .fillColor(ACCENT).build(), 60, 2, LayerAlign.TOP_LEFT, 1)
                                        .build(),
                                LayerAlign.TOP_LEFT, 0)
                        .position(new ShapeBuilder().name("Hood").size(70, 30).fillColor(OVER).build(),
                                0, 0, LayerAlign.TOP_LEFT, 1)
                        .build())));

        assertThat(pinAnchor(export))
                .as("the pin stays behind the hood the outer stack paints over it")
                .contains("behindDoc=\"1\"");
        assertThat(covered(export.report()))
                .as("and the band is named as covering it, so the pin was judged at all")
                .anySatisfy(named -> assertThat(named).contains("Pin").contains("Band[0]"));
    }

    @Test
    void aStackDrawingAPictureBesideItsLabelLiftsNothing() throws Exception {
        // A picture drawn beside the text it labels, in a nested layer of the stack, nowhere near
        // the pin. The picture is never lifted — the surface it stands on decides, not its
        // neighbours — so lifting the pin would paint the pin over a picture the page draws over
        // it. Measured on exactly this shape: the pin came back behindDoc="0" and the picture
        // behindDoc="1", and they overlap.
        Export export = export(session -> session.pageFlow(page -> page
                .add(new LayerStackBuilder().name("Stack")
                        .layer(new SectionBuilder().name("Band").fillColor(BAND).fixedWidth(200)
                                        .addParagraph(p -> p.name("Stage").text("Stage two")).build(),
                                LayerAlign.TOP_LEFT, 0)
                        .position(new ShapeBuilder().name("Pin").size(30, 14).fillColor(ACCENT).build(),
                                140, 2, LayerAlign.TOP_LEFT, 1)
                        .position(new LayerStackBuilder().name("Fact")
                                        .layer(new com.demcha.compose.document.dsl.ImageBuilder()
                                                        .name("Icon").source(pngBytes()).size(10, 10).build(),
                                                LayerAlign.CENTER_LEFT)
                                        .position(new ParagraphBuilder().name("Label").text("Label").build(),
                                                14, 0, LayerAlign.CENTER_LEFT, 1)
                                        .build(),
                                168, 0, LayerAlign.TOP_LEFT, 2)
                        .build())));

        // The icon's centre stands past the pin's right edge, so the pin does not frame it and
        // it is not that test keeping the pin behind — the boxes still overlap by two points,
        // which is where the file would have drawn the pin over the picture.
        assertThat(detailsOf(export.report(), "/Icon"))
                .as("the picture is drawn where the page puts it, which is why it cannot be lifted")
                .anySatisfy(detail -> assertThat(detail).contains("drawn beside the text it labels"));
        assertThat(pinAnchor(export))
                .as("the pin stays behind, the picture above it never being lifted")
                .contains("behindDoc=\"1\"");
        assertThat(covered(export.report()))
                .as("and the band is named as covering the pin, so the pin was judged at all")
                .anySatisfy(named -> assertThat(named).contains("Pin").contains("Band[0]"));
    }

    @Test
    void aShapeTheSameInstanceOfWhichStandsTwiceIsNotLifted() throws Exception {
        // One node used as two layers stands at two places, and the layout keeps one path and one
        // box for the instance. What lies under it at one place is not what lies under it at the
        // other, so an answer taken from whichever place the index kept would be given for both:
        // it is refused instead.
        com.demcha.compose.document.node.DocumentNode pin =
                new ShapeBuilder().name("Pin").size(30, 14).fillColor(ACCENT).build();
        Export export = export(session -> session.pageFlow(page -> page
                .add(new LayerStackBuilder().name("Stack")
                        .layer(new SectionBuilder().name("Band").fillColor(BAND).fixedWidth(200)
                                        .addParagraph(p -> p.name("Stage").text("Stage two")).build(),
                                LayerAlign.TOP_LEFT, 0)
                        .position(pin, 150, 2, LayerAlign.TOP_LEFT, 1)
                        .position(pin, 150, 30, LayerAlign.TOP_LEFT, 2)
                        .build())));

        assertThat(anchorsHolding(export, PIN_INK))
                .as("both places behind the text, neither answered from the other's")
                .isNotEmpty()
                .allSatisfy(anchor -> assertThat(anchor).contains("behindDoc=\"1\""));
    }

    @Test
    void aShapeBesideOneStandingTwiceIsNotLiftedOverIt() throws Exception {
        // The refusal above is asked of the node being lifted, so on its own it would let a
        // neighbour be lifted over the places it refuses — measured: the lone shape came back
        // behindDoc="0" and both of the repeated instance's places behindDoc="1", the lower of
        // them drawn over the higher. Being alone is what answers here: the repeated instance is
        // a second drawing of the stack.
        com.demcha.compose.document.node.DocumentNode shared =
                new ShapeBuilder().name("Shared").size(40, 10).fillColor(OVER).build();
        Export export = export(session -> session.pageFlow(page -> page
                .add(new LayerStackBuilder().name("Stack")
                        .layer(new SectionBuilder().name("Band").fillColor(BAND).fixedWidth(200)
                                        .addParagraph(p -> p.name("Stage").text("Stage two")).build(),
                                LayerAlign.TOP_LEFT, 0)
                        .position(new ShapeBuilder().name("Pin").size(30, 14).fillColor(ACCENT).build(),
                                150, 2, LayerAlign.TOP_LEFT, 1)
                        .position(shared, 140, 8, LayerAlign.TOP_LEFT, 2)
                        .position(shared, 140, 24, LayerAlign.TOP_LEFT, 3)
                        .build())));

        assertThat(pinAnchor(export))
                .as("the lone shape stays behind with the places it would have been drawn over")
                .contains("behindDoc=\"1\"");
    }

    @Test
    void twoPaintedLayersUnderAShapeStillLiftIt() throws Exception {
        // The topmost painted layer is the one the shapes above it are judged from; a second
        // painted layer below it changes nothing, the page covering it with the first.
        Export export = export(session -> session.pageFlow(page -> page
                .add(new LayerStackBuilder().name("Stack")
                        .layer(new SectionBuilder().name("Under").fillColor(OVER).fixedWidth(200)
                                        .addParagraph(p -> p.name("Deep").text("Under it all")).build(),
                                LayerAlign.TOP_LEFT, 0)
                        .layer(new SectionBuilder().name("Band").fillColor(BAND).fixedWidth(200)
                                        .addParagraph(p -> p.name("Stage").text("Stage two")).build(),
                                LayerAlign.TOP_LEFT, 1)
                        .position(new ShapeBuilder().name("Pin").size(30, 14).fillColor(ACCENT).build(),
                                150, 2, LayerAlign.TOP_LEFT, 2)
                        .build())));

        assertThat(pinAnchor(export)).contains("behindDoc=\"0\"");
        assertThat(covered(export.report())).isEmpty();
    }

    @Test
    void aShapeInAStackNestedOverAPaintedLayerStandsInFrontOfIt() throws Exception {
        // The climb reads every stack the shape stands in, not only the nearest: the pin is a
        // layer of an inner stack which is itself a layer over the painted band.
        Export export = export(session -> session.pageFlow(page -> page
                .add(new LayerStackBuilder().name("Stack")
                        .layer(new SectionBuilder().name("Band").fillColor(BAND).fixedWidth(200)
                                        .addParagraph(p -> p.name("Stage").text("Stage two")).build(),
                                LayerAlign.TOP_LEFT, 0)
                        .position(new LayerStackBuilder().name("Inner")
                                        .position(new ShapeBuilder().name("Pin").size(30, 14)
                                                .fillColor(ACCENT).build(), 0, 0, LayerAlign.TOP_LEFT, 0)
                                        .build(),
                                150, 2, LayerAlign.TOP_LEFT, 1)
                        .build())));

        assertThat(pinAnchor(export)).contains("behindDoc=\"0\"");
        assertThat(covered(export.report())).isEmpty();
    }

    @Test
    void aShapeDeclaredBeforeAPaintedLayerOfTheSameZIndexStaysBehindIt() throws Exception {
        // Layers sharing a z-index are painted in the order they were given, so the band declared
        // after the pin is painted over it: the page covers the pin, and in front the pin would
        // cover the band's shading instead.
        Export export = export(session -> session.pageFlow(page -> page
                .add(new LayerStackBuilder().name("Stack")
                        .position(new ShapeBuilder().name("Pin").size(30, 14).fillColor(ACCENT).build(),
                                150, 2, LayerAlign.TOP_LEFT, 0)
                        .layer(new SectionBuilder().name("Band").fillColor(BAND).fixedWidth(200)
                                        .addParagraph(p -> p.name("Stage").text("Stage two")).build(),
                                LayerAlign.TOP_LEFT, 0)
                        .build())));

        assertThat(pinAnchor(export)).contains("behindDoc=\"1\"");
        assertThat(covered(export.report())).singleElement().asString()
                .contains("Pin").contains("Band").contains(COVERED);
    }

    @Test
    void aShapeDeclaredAfterAPaintedLayerOfTheSameZIndexStandsInFrontOfIt() throws Exception {
        // The same two layers the other way round, both still at z-index 0: the band is painted
        // first, so the pin stands over it and goes in front. This and the test above are the pair
        // that pins the tie — the sort is stable, and which way it falls is the order given.
        Export export = export(session -> session.pageFlow(page -> page
                .add(new LayerStackBuilder().name("Stack")
                        .layer(new SectionBuilder().name("Band").fillColor(BAND).fixedWidth(200)
                                        .addParagraph(p -> p.name("Stage").text("Stage two")).build(),
                                LayerAlign.TOP_LEFT, 0)
                        .position(new ShapeBuilder().name("Pin").size(30, 14).fillColor(ACCENT).build(),
                                150, 2, LayerAlign.TOP_LEFT, 0)
                        .build())));

        assertThat(pinAnchor(export)).contains("behindDoc=\"0\"");
        assertThat(covered(export.report())).isEmpty();
    }

    @Test
    void aPictureOverAPaintedNeighbourStaysBehindItAndIsNamed() throws Exception {
        // A picture drawn where the page puts it is never lifted — the surface it stands on
        // decides, not its neighbours — so over a painted neighbour it is still covered. Not
        // silently: the naming pass names it where it stands.
        //
        // And the badge round it stays behind WITH it. Lifting the badge alone would paint its
        // own fill over the photo it frames, which the page draws over the badge: measured, that
        // is what happened before the picture was counted among the things that keep a stack
        // behind — the badge came back behindDoc="0" and the photo behindDoc="1". Both anchors
        // are read here for that reason, not the note alone.
        Export export = export(session -> session.pageFlow(page -> page
                .add(new LayerStackBuilder().name("Stack")
                        .layer(new SectionBuilder().name("Band").fillColor(BAND).fixedWidth(200)
                                        .addParagraph(p -> p.name("Stage").text("Stage two")).build(),
                                LayerAlign.TOP_LEFT, 0)
                        .position(new ShapeContainerBuilder().name("Frame").circle(40)
                                        .fillColor(ACCENT)
                                        .center(new com.demcha.compose.document.dsl.ImageBuilder()
                                                .name("Photo").source(pngBytes()).size(36, 36).build())
                                        .build(),
                                150, 2, LayerAlign.TOP_LEFT, 1)
                        .build())));

        assertThat(detailsOf(export.report(), "/Photo"))
                .as("the picture is drawn where the page puts it, which is the path in question")
                .isNotEmpty();
        assertThat(anchorsHolding(export, PIN_INK))
                .as("the badge's own outline, behind the text with the photo it frames")
                .singleElement().asString().contains("behindDoc=\"1\"");
        assertThat(anchorsHolding(export, "<pic:pic"))
                .as("and the photo, behind it too, so the badge is not painted over it")
                .singleElement().asString().contains("behindDoc=\"1\"");
        assertThat(covered(export.report()))
                .as("both named as covered by the band")
                .hasSize(2)
                .anySatisfy(named -> assertThat(named).contains("Photo").contains("Band[0]"))
                .anySatisfy(named -> assertThat(named).contains("Frame").contains("Band[0]"));
    }

    @Test
    void aShapeBesideAPaintedNeighbourItDoesNotReachStandsInFrontToo() throws Exception {
        // Set past the band's right edge, where the band's shading covers nothing of it. The order
        // is the tree's, not the geometry's: a layer the page paints over a painted one is drawn in
        // front whether the two boxes meet or not, and where they do not the reader sees the same
        // page either way.
        Export export = export(session -> session.pageFlow(page -> page.add(pinOverABand(BAND, 210, 2))));

        assertThat(pinAnchor(export)).contains("behindDoc=\"0\"");
        assertThat(covered(export.report())).isEmpty();
    }

    @Test
    void aShapeInsideAPaintedPanelAndBehindItsTextIsNotNamedAsCoveredByIt() throws Exception {
        // Inside the panel, not beside it. The track lies under the label laid over it, so it is
        // one of the few shapes a painted panel keeps behind the text — the export's own case,
        // which has a note of its own — and the box it lies in is that same panel's. Only the
        // test of which box holds which keeps it from being named here as well, so that test is
        // what this fixture exercises: a shape the panel puts in front would never reach it.
        Export export = export(session -> session.pageFlow(page -> page
                .addSection("Card", card -> card.fillColor(BAND)
                        .addLayerStack(stack -> stack
                                .layer(new ShapeBuilder().name("Track").size(200, 20)
                                        .fillColor(ACCENT).build(), LayerAlign.TOP_LEFT)
                                .layer(new ParagraphBuilder().name("Label").text("80%").build(),
                                        LayerAlign.CENTER)))));

        assertThat(detailsOf(export.report(), "/Track"))
                .as("the track is behind the text, so it does reach the test of which box holds it")
                .anySatisfy(detail -> assertThat(detail).contains("behind the text it frames"));
        assertThat(covered(export.report())).isEmpty();
    }

    @Test
    void aBadgeInFrontOverAPaintedNeighbourIsNotNamedAsCoveredByIt() throws Exception {
        // A badge holding its initials is handed to the queue already in front, with the flag for
        // the call saying otherwise. Standing over a painted neighbour it is seen plainly, so it
        // must not be named as hidden under it.
        Export export = export(session -> session.pageFlow(page -> page
                .add(new LayerStackBuilder().name("Stack")
                        .layer(new SectionBuilder().name("Band").fillColor(BAND).fixedWidth(200)
                                        .addParagraph(p -> p.name("Stage").text("Stage two")).build(),
                                LayerAlign.TOP_LEFT, 0)
                        .position(new ShapeContainerBuilder().name("Badge").circle(28)
                                        .fillColor(ACCENT)
                                        .center(new ParagraphBuilder().text("JR").build()).build(),
                                150, 2, LayerAlign.TOP_LEFT, 1)
                        .build())));

        assertThat(covered(export.report())).isEmpty();
    }

    @Test
    void aShapeOverAPaintedNeighbourInAnEarlierSectionIsNamedToo() throws Exception {
        // A section's pages are counted from its own first, and a page's height is its section's,
        // so a drawing and a panel are only ever judged against each other within one section. The
        // pin of each section stands under that section's upper band; neither may be missed, and
        // neither may be matched against the other section's.
        List<DocxExportReport.Note> notes = exportSections(
                page -> page.add(pinnedBetweenTwoBands("Band one", "Pin one")),
                page -> page.addParagraph(p -> p.text("A section of plain text, no panel in it.")),
                page -> page.add(pinnedBetweenTwoBands("Band two", "Pin two")));

        assertThat(notes.stream()
                .filter(note -> note.detail() != null && note.detail().contains(COVERED))
                .map(note -> note.path() + " -> " + note.detail().replaceAll(".*box of ([^,]+),.*", "$1")))
                .as("each section's pin named against its own section's band")
                .hasSize(2)
                .anySatisfy(named -> assertThat(named).contains("Pin one").contains("Band one"))
                .anySatisfy(named -> assertThat(named).contains("Pin two").contains("Band two"));
    }

    @Test
    void twoSectionsOfOnePresetAreEachNamed() throws Exception {
        // Each section's paths are built from its own roots, so two sections of the same preset
        // give their drawing one and the same path. Named once per section, not once per path:
        // the second section's loss is as real as the first's, and a reader of the report is
        // told which section each is in.
        List<DocxExportReport.Note> notes = exportSections(
                page -> page.add(pinnedBetweenTwoBands("Band", "Pin")),
                page -> page.add(pinnedBetweenTwoBands("Band", "Pin")));

        List<String> covered = notes.stream()
                .filter(note -> note.detail() != null && note.detail().contains(COVERED))
                .map(DocxExportReport.Note::detail).toList();
        assertThat(covered).as("one note for each section, each naming its own")
                .hasSize(2)
                .anySatisfy(detail -> assertThat(detail).contains("of section 1"))
                .anySatisfy(detail -> assertThat(detail).contains("of section 2"));
    }

    /** A band painted on the lower layer with a pin set over it on the layer above. */
    private static com.demcha.compose.document.node.DocumentNode pinOverABand(DocumentColor fill,
                                                                              double x, double y) {
        return new LayerStackBuilder().name("Stack")
                .layer(new SectionBuilder().name("Band").fillColor(fill).fixedWidth(200)
                        .addParagraph(p -> p.name("Stage").text("Stage two")).build(), LayerAlign.TOP_LEFT, 0)
                .position(new ShapeBuilder().name("Pin").size(30, 14).fillColor(ACCENT).build(),
                        x, y, LayerAlign.TOP_LEFT, 1)
                .build();
    }

    /**
     * A pin between two painted bands: the page paints one under it and one over it, so the pin
     * stays behind the text and the one under it covers it — the case the report still names.
     */
    private static com.demcha.compose.document.node.DocumentNode pinnedBetweenTwoBands(String band, String pin) {
        return new LayerStackBuilder().name("Stack")
                .layer(new SectionBuilder().name(band).fillColor(BAND).fixedWidth(200)
                        .addParagraph(p -> p.text("Stage")).build(), LayerAlign.TOP_LEFT, 0)
                .position(new ShapeBuilder().name(pin).size(30, 14).fillColor(ACCENT).build(),
                        150, 2, LayerAlign.TOP_LEFT, 1)
                .layer(new SectionBuilder().name("Over " + band).fillColor(OVER).fixedWidth(200)
                        .addParagraph(p -> p.text("Over")).build(), LayerAlign.TOP_LEFT, 2)
                .build();
    }

    /** The notes of a document of several sections, each a page of its own content. */
    @SafeVarargs
    private static List<DocxExportReport.Note> exportSections(
            Consumer<com.demcha.compose.document.dsl.PageFlowBuilder>... contents) throws Exception {
        List<DocxExportReport.Note> notes = new java.util.ArrayList<>();
        var documents = GraphCompose.documents();
        for (Consumer<com.demcha.compose.document.dsl.PageFlowBuilder> content : contents) {
            DocumentSession session = GraphCompose.document().pageSize(400, 400)
                    .margin(DocumentInsets.of(20)).create();
            session.pageFlow(content::accept);
            documents = documents.section(session);
        }
        try (com.demcha.compose.document.api.MultiSectionDocument document = documents.create()) {
            document.export(DocxSemanticBackend.builder()
                    .reportSink(report -> notes.addAll(report.notes())).build());
        }
        return notes;
    }

    /** The notes naming a drawing a panel beside it covers. */
    private static List<String> covered(DocxExportReport report) {
        return report.notes().stream()
                .filter(note -> note.detail() != null && note.detail().contains(COVERED))
                .map(note -> note.subject() + " " + note.path() + ": " + note.detail())
                .toList();
    }

    /** The details of every note on a node whose path ends in {@code path}. */
    private static List<String> detailsOf(DocxExportReport report, String path) {
        return report.notes().stream()
                .filter(note -> note.path() != null && note.path().contains(path) && note.detail() != null)
                .map(DocxExportReport.Note::detail)
                .toList();
    }

    /** The one anchor holding the pin's ink. */
    private static String pinAnchor(Export export) throws Exception {
        List<String> pins = anchorsHolding(export, PIN_INK);
        assertThat(pins).as("one anchor drawn in the pin's ink").hasSize(1);
        return pins.get(0);
    }

    /** Every anchor drawn in one ink. */
    private static List<String> anchorsHolding(Export export, String ink) throws Exception {
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(export.bytes()))) {
            List<String> found = new ArrayList<>();
            Matcher matcher = Pattern.compile("<wp:anchor.*?</wp:anchor>", Pattern.DOTALL)
                    .matcher(document.getDocument().xmlText());
            while (matcher.find()) {
                if (matcher.group().contains(ink)) {
                    found.add(matcher.group());
                }
            }
            return found;
        }
    }

    /** What one export gives: the file, to read the anchors out of, and the report. */
    private record Export(byte[] bytes, DocxExportReport report) {
    }

    private static byte[] pngBytes() {
        try (java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream()) {
            javax.imageio.ImageIO.write(
                    new java.awt.image.BufferedImage(40, 40, java.awt.image.BufferedImage.TYPE_INT_RGB),
                    "png", out);
            return out.toByteArray();
        } catch (Exception failure) {
            throw new IllegalStateException(failure);
        }
    }

    private static Export export(Consumer<DocumentSession> content) throws Exception {
        AtomicReference<DocxExportReport> report = new AtomicReference<>();
        byte[] bytes;
        try (DocumentSession session = GraphCompose.document().pageSize(400, 400)
                .margin(DocumentInsets.of(20)).create()) {
            content.accept(session);
            bytes = session.export(new DocxSemanticBackend(report::set));
        }
        return new Export(bytes, report.get());
    }
}
