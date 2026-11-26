package com.demcha.compose.document.backend.fixed.pptx;

import com.demcha.compose.font.FontFamilyDefinition;
import com.demcha.compose.font.FontName;
import org.junit.jupiter.api.Test;

import java.awt.Font;
import java.awt.font.FontRenderContext;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * How many times {@link PptxViewerMetrics#load} reads each font program, and that reading a
 * program once still answers each slot from its own program.
 */
class PptxViewerMetricsTest {

    private static final String COUSINE = "fonts/google/cousine/Cousine-Regular.ttf";
    private static final String ALEGREYA = "fonts/google/alegreyasans/AlegreyaSans-Regular.ttf";
    private static final String ARSENAL = "fonts/google/arsenal/Arsenal-Regular.ttf";
    private static final String GOTHIC = "fonts/google/gothica1/GothicA1-Regular.ttf";

    private static final FontFamilyDefinition FAMILY =
            FontFamilyDefinition.classpath(FontName.of("Viewer Metrics Probe"), COUSINE).build();

    @Test
    void aProgramFillingAllFourSlotsIsReadOnce() throws Exception {
        CountingSource only = new CountingSource(COUSINE);

        PptxViewerMetrics.ViewerFontMetrics metrics = PptxViewerMetrics.load(
                FAMILY, new FontFamilyDefinition.FontSourceSet(only, only, only, only));

        assertThat(only.opens.get())
                .as("a family registered from one file repeats it in every slot")
                .isEqualTo(1);
        double expected = referenceAscent(COUSINE);
        assertThat(List.of(metrics.regularAscent(), metrics.boldAscent(),
                metrics.italicAscent(), metrics.boldItalicAscent()))
                .containsOnly(expected);
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
        List<Double> expected = List.of(referenceAscent(COUSINE), referenceAscent(ALEGREYA),
                referenceAscent(ARSENAL), referenceAscent(GOTHIC));
        assertThat(expected)
                .as("the premise: four programs with four different ascents, so a slot answered "
                        + "from another slot's program shows")
                .doesNotHaveDuplicates();

        PptxViewerMetrics.ViewerFontMetrics metrics = PptxViewerMetrics.load(FAMILY,
                new FontFamilyDefinition.FontSourceSet(regular, bold, italic, boldItalic));

        assertThat(List.of(metrics.regularAscent(), metrics.boldAscent(),
                metrics.italicAscent(), metrics.boldItalicAscent()))
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

    private static double referenceAscent(String resource) throws Exception {
        try (InputStream stream = open(resource)) {
            Font font = Font.createFont(Font.TRUETYPE_FONT, stream).deriveFont(1000.0f);
            return font.getLineMetrics("Hg", new FontRenderContext(null, true, true)).getAscent()
                    / 1000.0f;
        }
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
