package com.demcha.compose.document.templates.rota.presets;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.data.rota.RotaCovers;
import com.demcha.compose.document.templates.data.rota.RotaDay;
import com.demcha.compose.document.templates.data.rota.RotaGroup;
import com.demcha.compose.document.templates.data.rota.RotaLegend;
import com.demcha.compose.document.templates.data.rota.RotaShift;
import com.demcha.compose.document.templates.data.rota.RotaStaff;
import com.demcha.compose.document.templates.data.rota.ShiftStatus;
import com.demcha.compose.document.templates.data.rota.StructuredRotaData;
import com.demcha.compose.document.templates.data.rota.StructuredRotaDocumentSpec;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/**
 * Smoke test for {@link CobaltRota} — proves the preset renders a
 * {@link StructuredRotaDocumentSpec} end-to-end with its packaged icons on one
 * landscape page, renders an empty rota through its guards, follows the
 * document's own day count rather than assuming a week, keeps a short or long
 * legend square with the sheet, and reports an unknown icon token by name.
 *
 * <p>The sheet is composed table cells almost end to end, which emit no placed
 * node of their own — the layout snapshot sees two nodes for eighty-four cells —
 * so what a cell contains is asserted here, on the text layer.</p>
 */
class CobaltRotaSmokeTest {

    private static byte[] render(StructuredRotaDocumentSpec spec) throws Exception {
        // The preset owns its page geometry, so the session starts unconfigured.
        try (DocumentSession session = GraphCompose.document().create()) {
            CobaltRota.create().compose(session, spec);
            assertThat(session.roots()).isNotEmpty();
            byte[] pdfBytes = session.toPdfBytes();
            assertThat(pdfBytes).isNotEmpty();
            return pdfBytes;
        }
    }

    private static String textOf(byte[] pdfBytes) throws Exception {
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            return new PDFTextStripper().getText(document);
        }
    }

    @Test
    void exposesStableIdentity() {
        DocumentTemplate<StructuredRotaDocumentSpec> template = CobaltRota.create();
        assertThat(template.id()).isEqualTo(CobaltRota.ID);
        assertThat(template.displayName()).isEqualTo(CobaltRota.DISPLAY_NAME);
    }

    @Test
    void rendersTheCanonicalRotaOnOneLandscapePage() throws Exception {
        try (DocumentSession session = GraphCompose.document().create()) {
            CobaltRota.create().compose(session, CobaltRotaFixtures.canonicalRota());
            assertThat(session.layoutSnapshot().totalPages()).isEqualTo(1);
        }
    }

    @Test
    void rendersEmptyRota() throws Exception {
        // Exercises the guards: no lockup, no days, no legend, no bands, no
        // staff, and a footer with nothing to sign off with.
        render(StructuredRotaDocumentSpec.from(StructuredRotaData.builder().build()));
    }

    @Test
    void canonicalRenderCarriesEveryRegionOnTheTextLayer() throws Exception {
        // Eighty-four composed cells emit no placed node, so this is where their
        // content is assertable at all.
        String text = textOf(render(CobaltRotaFixtures.canonicalRota()));
        assertThat(text)
                .contains("QUAY").contains("QUAYSIDE BAR")
                .contains("MONDAY").contains("SUNDAY")
                .contains("STATUS LEGEND").contains("Standby")
                .contains("COVERS").contains("104")
                .contains("MANAGEMENT").contains("BARBACKS")
                .contains("VESNA")
                .contains("09:00-18:00").contains("HOL")
                .contains("Quayside Bar");
    }

    @Test
    void everyStatusIsDrawnInItsOwnColourAndCoversTheAreaItShould() throws Exception {
        // What neither other gate can see. The snapshot holds two nodes for the
        // whole sheet, and a pixel budget large enough to survive a font's
        // antialiasing is large enough for a couple of dozen chips to lose their
        // fill inside it. This counts the ink of each status directly: a chip
        // drawn in the wrong colour, or drawn plain when it should be filled,
        // moves two of these counts at once.
        byte[] pdfBytes = render(CobaltRotaFixtures.canonicalRota());
        BufferedImage page;
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            page = new PDFRenderer(document).renderImage(0, 1.0f);
        }
        assertArea(page, "REQUEST", 176, 176, 192, 1_278);
        assertArea(page, "OFF", 208, 16, 16, 30_528);
        assertArea(page, "HOLIDAY", 255, 192, 32, 22_712);
        assertArea(page, "STOCK", 16, 160, 80, 13_455);
        assertArea(page, "STANDBY", 192, 144, 224, 1_321);
        assertArea(page, "TRAINING", 240, 80, 16, 7_475);
        assertArea(page, "SUPPORT", 208, 216, 240, 1_290);
        // The bands, the covers label and the rules; and the stripe under every
        // other person, which is what a reader follows across the columns.
        assertArea(page, "navy", 16, 32, 80, 55_857);
        assertArea(page, "zebra", 228, 235, 248, 54_983);
    }

    /**
     * How much of the page one exact colour covers, against what it covered when
     * the sheet was known good.
     *
     * <p>A tenth either way: a solid fill has no antialiasing in its middle, so
     * these counts move with the shapes and not with the renderer. Losing one
     * chip of a colour is about a sixth of the smallest count here.</p>
     */
    private static void assertArea(BufferedImage page, String what,
                                   int red, int green, int blue, int expected) {
        int rgb = (red << 16) | (green << 8) | blue;
        int found = 0;
        for (int y = 0; y < page.getHeight(); y++) {
            for (int x = 0; x < page.getWidth(); x++) {
                if ((page.getRGB(x, y) & 0xFFFFFF) == rgb) {
                    found++;
                }
            }
        }
        assertThat(found)
                .describedAs("area drawn in %s", what)
                .isBetween((int) (expected * 0.9), (int) (expected * 1.1));
    }

    @Test
    void aSplitDayPrintsBothOfItsHalves() throws Exception {
        // The shape the model exists for: a day holding two entries draws two
        // chips, not one that wins.
        StructuredRotaDocumentSpec spec = StructuredRotaDocumentSpec.from(
                StructuredRotaData.builder()
                        .days(List.of(new RotaDay("MONDAY", "31", "ST")))
                        .groups(List.of(new RotaGroup("BAR", List.of(
                                new RotaStaff("PRIYA", List.of(List.of(
                                        RotaShift.strong("09:00-16:00", ShiftStatus.STOCK),
                                        RotaShift.soft("16:00-22:00", ShiftStatus.STOCK))))))))
                        .build());
        assertThat(textOf(render(spec))).contains("09:00-16:00").contains("16:00-22:00");
    }

    @Test
    void theSheetHasAsManyDayColumnsAsTheDocumentHasDays() throws Exception {
        // Nothing in the preset says a week: a five-day rota is as ordinary as a
        // seven-day one, and a person's entries are read by position against the
        // document's own day list.
        StructuredRotaDocumentSpec spec = StructuredRotaDocumentSpec.from(
                StructuredRotaData.builder()
                        .days(List.of(
                                new RotaDay("MONDAY", "31", "ST"),
                                new RotaDay("TUESDAY", "1", "ST"),
                                new RotaDay("WEDNESDAY", "2", "ND"),
                                new RotaDay("THURSDAY", "3", "RD"),
                                new RotaDay("FRIDAY", "4", "TH")))
                        .groups(List.of(new RotaGroup("BAR", List.of(
                                new RotaStaff("PRIYA", List.of(
                                        List.of(RotaShift.hours("09:00-17:00")),
                                        List.of(),
                                        List.of(RotaShift.strong("OFF", ShiftStatus.OFF))))))))
                        .build());
        String text = textOf(render(spec));
        assertThat(text).contains("FRIDAY").contains("09:00-17:00").contains("OFF");
        assertThat(text).doesNotContain("SATURDAY");
    }

    @Test
    void aLegendLongerThanTheWeekRunsOnRatherThanLosingItsTail() throws Exception {
        // A status a reader cannot look up is worse than a second strip. Twelve
        // statuses across seven columns are two strips, and every one of the
        // twelve is on the sheet — asserting only the first would pass with the
        // tail silently dropped.
        List<RotaLegend.Entry> entries = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            entries.add(new RotaLegend.Entry("S" + i, ShiftStatus.OFF));
        }
        StructuredRotaData base = CobaltRotaFixtures.canonicalRota().rota();
        StructuredRotaDocumentSpec spec = StructuredRotaDocumentSpec.from(
                StructuredRotaData.builder()
                        .venue(base.venue()).days(base.days())
                        .legend(new RotaLegend("STATUS", "COVERS", "L", "D", entries))
                        .build());
        String text = textOf(render(spec));
        for (int i = 0; i < 12; i++) {
            assertThat(text).describedAs("legend entry S%d", i).contains("S" + i);
        }
    }

    @Test
    void aLegendShorterThanTheWeekStillFillsTheSheetsWidth() throws Exception {
        // The other side of the same seam: a short strip is closed off with
        // empty cells so the table stays square. Two statuses under a seven-day
        // week is one strip, and the sheet is as wide as it was.
        StructuredRotaData base = CobaltRotaFixtures.canonicalRota().rota();
        StructuredRotaDocumentSpec spec = StructuredRotaDocumentSpec.from(
                StructuredRotaData.builder()
                        .venue(base.venue()).days(base.days())
                        .legend(new RotaLegend("STATUS", "COVERS", "L", "D", List.of(
                                new RotaLegend.Entry("S0", ShiftStatus.OFF),
                                new RotaLegend.Entry("S1", ShiftStatus.STOCK))))
                        .build());
        try (DocumentSession session = GraphCompose.document().create()) {
            CobaltRota.create().compose(session, spec);
            assertThat(tableWidth(session)).isEqualTo(tableWidth(canonical()), within(0.01));
            assertThat(textOf(session.toPdfBytes())).contains("S0").contains("S1");
        }
    }

    private static DocumentSession canonical() {
        DocumentSession session = GraphCompose.document().create();
        CobaltRota.create().compose(session, CobaltRotaFixtures.canonicalRota());
        return session;
    }

    /** The table's own placed width, which a ragged row would change. */
    private static double tableWidth(DocumentSession session) {
        return session.layoutSnapshot().nodes().stream()
                .filter(node -> "Schedule".equals(node.entityName()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no Schedule node"))
                .placementWidth();
    }

    @Test
    void aRotaThatStatesNoLegendOrCoversSpendsNoRowsOnThem() throws Exception {
        // An empty navy bar over seven empty boxes is two rows of a sheet that
        // is short of rows already. A rota that says nothing about either simply
        // does not carry them.
        StructuredRotaData base = CobaltRotaFixtures.canonicalRota().rota();
        StructuredRotaDocumentSpec spec = StructuredRotaDocumentSpec.from(
                StructuredRotaData.builder()
                        .venue(base.venue()).week(base.week()).days(base.days())
                        .groups(base.groups()).footer(base.footer())
                        .build());
        try (DocumentSession session = GraphCompose.document().create()) {
            CobaltRota.create().compose(session, spec);
            assertThat(textOf(session.toPdfBytes()))
                    .doesNotContain("STATUS LEGEND").doesNotContain("COVERS");
            // Two rows lighter than the canonical sheet, which is the whole point.
            assertThat(tableHeight(session)).isLessThan(tableHeight(canonical()));
        }
    }

    private static double tableHeight(DocumentSession session) {
        return session.layoutSnapshot().nodes().stream()
                .filter(node -> "Schedule".equals(node.entityName()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no Schedule node"))
                .placementHeight();
    }

    @Test
    void aBandThatNamesNoMarkIsStillTheBand() throws Exception {
        // The strip and its label are the design's; the mark before them is the
        // document's, and a band naming none simply opens with its label.
        StructuredRotaDocumentSpec spec = StructuredRotaDocumentSpec.from(
                StructuredRotaData.builder()
                        .days(List.of(new RotaDay("MONDAY", "31", "ST")))
                        .groups(List.of(new RotaGroup("KITCHEN", List.of(
                                new RotaStaff("PRIYA", List.of(List.of()))))))
                        .build());
        assertThat(textOf(render(spec))).contains("KITCHEN").contains("PRIYA");
    }

    @Test
    void rejectsUnknownIconTokenByName() {
        StructuredRotaDocumentSpec spec = StructuredRotaDocumentSpec.from(
                StructuredRotaData.builder()
                        .days(List.of(new RotaDay("MONDAY", "31", "ST")))
                        .groups(List.of(new RotaGroup("KITCHEN", "no-such-icon", List.of())))
                        .build());
        try (DocumentSession session = GraphCompose.document().create()) {
            assertThatThrownBy(() -> CobaltRota.create().compose(session, spec))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("no-such-icon")
                    .hasMessageContaining("bartenders");
        }
    }

    @Test
    void aRotaTooLongForOnePageTakesASecondRatherThanLosingAnyone() throws Exception {
        // The sheet holds twelve on a page. A thirteenth is not a fault and is
        // certainly not dropped: the header repeats and the rota continues.
        StructuredRotaData base = CobaltRotaFixtures.canonicalRota().rota();
        List<RotaStaff> crowd = new ArrayList<>();
        for (int i = 0; i < 40; i++) {
            crowd.add(new RotaStaff("PERSON " + i, List.of()));
        }
        StructuredRotaDocumentSpec spec = StructuredRotaDocumentSpec.from(
                StructuredRotaData.builder()
                        .venue(base.venue()).week(base.week()).days(base.days())
                        .legend(base.legend()).footer(base.footer())
                        .groups(List.of(new RotaGroup("BAR", "bartenders", crowd)))
                        .build());
        try (DocumentSession session = GraphCompose.document().create()) {
            CobaltRota.create().compose(session, spec);
            assertThat(session.layoutSnapshot().totalPages()).isGreaterThan(1);
            String text = textOf(session.toPdfBytes());
            assertThat(text).contains("PERSON 0").contains("PERSON 39");
            // The header repeats, so the second page is not seven unlabelled
            // columns: repeatHeader carries all four of its rows.
            assertThat(text.split("MONDAY", -1).length - 1).isGreaterThan(1);
        }
    }
}
