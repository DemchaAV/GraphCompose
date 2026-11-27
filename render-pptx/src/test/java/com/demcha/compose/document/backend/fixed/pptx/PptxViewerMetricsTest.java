package com.demcha.compose.document.backend.fixed.pptx;

import com.demcha.compose.document.backend.fixed.pdf.PdfMeasurementResources;
import com.demcha.compose.engine.components.content.text.TextStyle;
import com.demcha.compose.engine.render.pdf.PdfFont;
import com.demcha.compose.font.FontFamilyDefinition;
import com.demcha.compose.font.FontName;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Where PowerPoint seats the first baseline, by the rule {@link PptxViewerMetrics} reads it
 * by, and how many times {@link PptxViewerMetrics#load} reads each font program.
 *
 * <p>The expected seats are worked out by hand from each face's OS/2 fields, not by the
 * method under test: share = 25/32, line = 1.2 em,
 * {@code seat = 1.2 · A' / (A' + D')} with {@code A' = max(a, share·(a+d))} and
 * {@code D' = max(d, (1−share)·(a+d))}.</p>
 */
class PptxViewerMetricsTest {

    private static final String COUSINE = "fonts/google/cousine/Cousine-Regular.ttf";
    private static final String ALEGREYA = "fonts/google/alegreyasans/AlegreyaSans-Regular.ttf";
    private static final String ARSENAL = "fonts/google/arsenal/Arsenal-Regular.ttf";
    private static final String GOTHIC = "fonts/google/gothica1/GothicA1-Regular.ttf";

    /** No USE_TYPO_METRICS: Windows ascent 1705, descent 615 — below the share, 870/971 em. */
    private static final double COUSINE_SEAT = 870.0 / 971.0;
    /** USE_TYPO_METRICS: typo ascender 800 + line gap 200, descender 200 — 96/101 em. */
    private static final double ALEGREYA_SEAT = 96.0 / 101.0;
    /** USE_TYPO_METRICS: typo ascender 1000, descender 254 — 19200/20389 em. */
    private static final double ARSENAL_SEAT = 19200.0 / 20389.0;
    /** USE_TYPO_METRICS: typo ascender 817 + line gap 256, descender 207 — 2146/2255 em. */
    private static final double GOTHIC_SEAT = 2146.0 / 2255.0;

    private static final FontFamilyDefinition FAMILY =
            FontFamilyDefinition.classpath(FontName.of("Viewer Metrics Probe"), COUSINE).build();

    @Test
    void aFaceSeatsByHowItsMetricsDivideAndNotByHowTallTheyAre() {
        assertThat(PptxViewerMetrics.seatRatio(25, 7))
                .as("a face at exactly the share stands its baseline 1.2 x 25/32 em down")
                .isEqualTo(1.2 * 25 / 32);
        assertThat(PptxViewerMetrics.seatRatio(800, 200))
                .as("four fifths above: the share below is raised to 7/32 of the height")
                .isCloseTo(PptxViewerMetrics.seatRatio(1000, 250), Offset.offset(1e-15))
                .isCloseTo(1.2 * 800 / (800 + 1000 * 7.0 / 32), Offset.offset(1e-15));
        assertThat(PptxViewerMetrics.seatRatio(600, 400))
                .as("three fifths above: the share above is raised to 25/32 of the height")
                .isCloseTo(1.2 * (1000 * 25.0 / 32) / (1000 * 25.0 / 32 + 400), Offset.offset(1e-15));
        assertThat(PptxViewerMetrics.seatRatio(1000, -250))
                .as("a descender written as a negative typo value, as OS/2 stores it")
                .isEqualTo(PptxViewerMetrics.seatRatio(1000, 250));
        assertThat(PptxViewerMetrics.seatRatio(0, 0))
                .as("no height at all: the seat of a face exactly at the share")
                .isEqualTo(1.2 * 25 / 32);
    }

    /**
     * A program with no OS/2 table, and one whose OS/2 fields give the face no height, fall back
     * to the program's own ascent and descent as Java reads them. Alegreya Sans shows which: its
     * {@code hhea} values, 900 over 300, seat it at 1.2 × 937.5 / 1237.5 em, where its OS/2
     * fields would seat it at 96/101.
     */
    @Test
    void aFaceWithoutUsableOs2MetricsIsSeatedFromItsOwnAscentAndDescent() throws Exception {
        double fromHhea = 1.2 * 937.5 / 1237.5;
        byte[] alegreya;
        try (InputStream stream = open(ALEGREYA)) {
            alegreya = stream.readAllBytes();
        }

        byte[] withoutOs2 = alegreya.clone();
        int os2 = tableEntry(withoutOs2, "OS/2");
        withoutOs2[os2 + 3] = '3';
        byte[] zeroed = alegreya.clone();
        int offset = readInt(zeroed, tableEntry(zeroed, "OS/2") + 8);
        // sTypoAscender, sTypoDescender, sTypoLineGap, usWinAscent, usWinDescent, and
        // USE_TYPO_METRICS (fsSelection bit 7) cleared, so Java's own read takes hhea rather
        // than the zeroed typographic values. An early 68-byte table ends before these fields,
        // which fontbox then reads as zeros, and it predates the USE_TYPO_METRICS bit.
        java.util.Arrays.fill(zeroed, offset + 68, offset + 78, (byte) 0);
        zeroed[offset + 63] &= (byte) ~0x80;

        assertThat(PptxViewerMetrics.load(FAMILY, onlySource(withoutOs2, "no-os2.ttf")).regularSeat())
                .as("no OS/2 table")
                .isCloseTo(fromHhea, Offset.offset(1e-6));
        assertThat(PptxViewerMetrics.load(FAMILY, onlySource(zeroed, "zeroed-os2.ttf")).regularSeat())
                .as("an OS/2 table whose metrics are all zero")
                .isCloseTo(fromHhea, Offset.offset(1e-6));
    }

    private static int tableEntry(byte[] font, String tag) {
        int tables = ((font[4] & 0xff) << 8) | (font[5] & 0xff);
        for (int i = 0; i < tables; i++) {
            int entry = 12 + 16 * i;
            if (new String(font, entry, 4, java.nio.charset.StandardCharsets.ISO_8859_1).equals(tag)) {
                return entry;
            }
        }
        throw new AssertionError("no " + tag + " table");
    }

    private static int readInt(byte[] font, int at) {
        return ((font[at] & 0xff) << 24) | ((font[at + 1] & 0xff) << 16)
                | ((font[at + 2] & 0xff) << 8) | (font[at + 3] & 0xff);
    }

    private static FontFamilyDefinition.FontSourceSet onlySource(byte[] program, String name) {
        FontFamilyDefinition.FontBinarySource source = new FontFamilyDefinition.FontBinarySource() {
            @Override
            public InputStream openStream() {
                return new ByteArrayInputStream(program);
            }

            @Override
            public String description() {
                return name;
            }
        };
        return new FontFamilyDefinition.FontSourceSet(source, source, source, source);
    }

    @Test
    void eachRouteReadsTheFieldsPowerPointReads() throws Exception {
        // Cousine sets no USE_TYPO_METRICS and is read from its Windows metrics; the other three
        // set it, and Alegreya Sans and Gothic A1 carry a typographic line gap that counts above
        // the baseline. Read through hhea instead, every one of the four seats differently.
        PptxViewerMetrics.ViewerFontMetrics metrics = PptxViewerMetrics.load(FAMILY,
                new FontFamilyDefinition.FontSourceSet(new CountingSource(COUSINE),
                        new CountingSource(ALEGREYA), new CountingSource(ARSENAL),
                        new CountingSource(GOTHIC)));

        assertThat(metrics.regularSeat()).isCloseTo(COUSINE_SEAT, Offset.offset(1e-12));
        assertThat(metrics.boldSeat()).isCloseTo(ALEGREYA_SEAT, Offset.offset(1e-12));
        assertThat(metrics.italicSeat()).isCloseTo(ARSENAL_SEAT, Offset.offset(1e-12));
        assertThat(metrics.boldItalicSeat()).isCloseTo(GOTHIC_SEAT, Offset.offset(1e-12));
    }

    /**
     * Arial's Windows metrics are 1854 over 434 units of 2048, above the share: the part below
     * is raised to 7/32 of 2288 = 500.5, so a 22pt Helvetica run, drawn in Arial, stands
     * 22 × 1.2 × 1854 / 2354.5 = 20.788107878530475pt below its frame's top.
     */
    @Test
    void aStandardFourteenRunIsSeatedFromTheFacePowerPointDrawsItIn() throws Exception {
        try (PdfMeasurementResources measurement = PdfMeasurementResources.open(List.of())) {
            PdfFont font = measurement.fontLibrary()
                    .getFont(FontName.HELVETICA, PdfFont.class).orElseThrow();
            TextStyle style = TextStyle.builder().fontName(FontName.HELVETICA).size(22).build();

            assertThat(PptxViewerMetrics.seatPoints(style, font, null, false, false))
                    .isCloseTo(20.788107878530475, Offset.offset(1e-9));
        }
    }

    /**
     * A bundled family has no metrics of its own read for it — the deck carries it after the
     * slides are drawn — so it is seated from the program the page measured it with. Alegreya
     * Sans is where that differs from its descriptor: the PDF descriptor says 900 over 300
     * (hhea), which would seat it at 1.2 × 937.5 / 1237.5 = 0.9091 em, 0.91pt high at 22pt.
     */
    @Test
    void aBundledFamilyIsSeatedFromTheProgramThePageMeasuredItWith() throws Exception {
        try (PdfMeasurementResources measurement = PdfMeasurementResources.open(List.of())) {
            PdfFont font = measurement.fontLibrary()
                    .getFont(FontName.ALEGREYA_SANS, PdfFont.class).orElseThrow();
            TextStyle style = TextStyle.builder().fontName(FontName.ALEGREYA_SANS).size(22).build();
            PdfFont.VerticalMetrics descriptor = font.verticalMetrics(style);
            assertThat(PptxViewerMetrics.seatRatio(descriptor.ascent(), descriptor.descent()))
                    .as("the premise: the descriptor would seat this family 0.0414 em higher")
                    .isCloseTo(1.2 * 937.5 / 1237.5, Offset.offset(1e-6));

            assertThat(PptxViewerMetrics.seatPoints(style, font, null, false, false))
                    .isCloseTo(22 * ALEGREYA_SEAT, Offset.offset(1e-9));
        }
    }

    @Test
    void aProgramFillingAllFourSlotsIsReadOnce() throws Exception {
        CountingSource only = new CountingSource(COUSINE);

        PptxViewerMetrics.ViewerFontMetrics metrics = PptxViewerMetrics.load(
                FAMILY, new FontFamilyDefinition.FontSourceSet(only, only, only, only));

        assertThat(only.opens.get())
                .as("a family registered from one file repeats it in every slot")
                .isEqualTo(1);
        assertThat(List.of(metrics.regularSeat(), metrics.boldSeat(),
                metrics.italicSeat(), metrics.boldItalicSeat()))
                .containsOnly(metrics.regularSeat());
        assertThat(metrics.regularSeat()).isCloseTo(COUSINE_SEAT, Offset.offset(1e-12));
    }

    @Test
    void twoSourcesDescribingTheSameProgramAreReadOnce() throws Exception {
        // Keyed by description, as the program reading beside this one keys its programs, not
        // by identity: the two reads have to agree on which slots share a program.
        CountingSource first = new CountingSource(COUSINE);
        CountingSource second = new CountingSource(COUSINE);

        PptxViewerMetrics.load(
                FAMILY, new FontFamilyDefinition.FontSourceSet(first, second, first, second));

        assertThat(first.opens.get() + second.opens.get()).isEqualTo(1);
    }

    @Test
    void eachSlotIsAnsweredFromItsOwnProgram() throws Exception {
        CountingSource regular = new CountingSource(COUSINE);
        CountingSource bold = new CountingSource(ALEGREYA);
        CountingSource italic = new CountingSource(ARSENAL);
        CountingSource boldItalic = new CountingSource(GOTHIC);
        List<Double> expected = List.of(COUSINE_SEAT, ALEGREYA_SEAT, ARSENAL_SEAT, GOTHIC_SEAT);
        assertThat(expected)
                .as("the premise: four programs with four different seats, so a slot answered "
                        + "from another slot's program shows")
                .doesNotHaveDuplicates();

        PptxViewerMetrics.ViewerFontMetrics metrics = PptxViewerMetrics.load(FAMILY,
                new FontFamilyDefinition.FontSourceSet(regular, bold, italic, boldItalic));

        assertThat(List.of(metrics.regularSeat(), metrics.boldSeat(),
                metrics.italicSeat(), metrics.boldItalicSeat()))
                .usingElementComparator((x, y) -> Math.abs(x - y) < 1e-12 ? 0 : Double.compare(x, y))
                .containsExactlyElementsOf(expected);
        assertThat(List.of(regular.opens.get(), bold.opens.get(), italic.opens.get(),
                boldItalic.opens.get()))
                .containsOnly(1);
    }

    @Test
    void anUnreadableFaceAfterAReadOneIsStillNamed() {
        CountingSource regular = new CountingSource(COUSINE);
        FontFamilyDefinition.FontBinarySource damaged = new FontFamilyDefinition.FontBinarySource() {
            @Override
            public InputStream openStream() {
                return new ByteArrayInputStream(new byte[] {0, 1, 0, 0, 0, 0});
            }

            @Override
            public String description() {
                return "damaged-bold.ttf";
            }
        };

        assertThatThrownBy(() -> PptxViewerMetrics.load(FAMILY,
                new FontFamilyDefinition.FontSourceSet(regular, damaged, regular, damaged)))
                .isInstanceOfSatisfying(PptxViewerMetrics.UnreadableFaceException.class,
                        refusal -> assertThat(refusal.source()).isEqualTo("damaged-bold.ttf"));
    }

    private static InputStream open(String resource) throws IOException {
        InputStream stream = PptxViewerMetricsTest.class.getResourceAsStream("/" + resource);
        if (stream == null) {
            throw new IOException("missing test font " + resource);
        }
        return stream;
    }

    private static final class CountingSource implements FontFamilyDefinition.FontBinarySource {

        private final String resource;
        private final AtomicInteger opens = new AtomicInteger();

        private CountingSource(String resource) {
            this.resource = resource;
        }

        @Override
        public InputStream openStream() throws IOException {
            opens.incrementAndGet();
            return open(resource);
        }

        @Override
        public String description() {
            return resource;
        }
    }
}
