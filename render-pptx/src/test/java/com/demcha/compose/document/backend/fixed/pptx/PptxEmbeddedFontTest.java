package com.demcha.compose.document.backend.fixed.pptx;

import org.apache.poi.common.usermodel.fonts.FontHeader;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFFontInfo;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PptxEmbeddedFontTest {

    @Test
    void wrapsRawTrueTypeDataInAnUncompressedEotContainer() throws Exception {
        byte[] ttf;
        try (InputStream input = getClass().getResourceAsStream(
                "/fonts/google/lato/Lato-BoldItalic.ttf")) {
            assertThat(input).isNotNull();
            ttf = input.readAllBytes();
        }

        byte[] eot = PptxEmbeddedFont.wrap(new ByteArrayInputStream(ttf));
        FontHeader header = new FontHeader();
        try (InputStream parsed = header.bufferInit(new ByteArrayInputStream(eot))) {
            assertThat(header.getFamilyName()).isEqualTo("Lato");
            assertThat(header.getStyleName()).isEqualTo("Bold Italic");
            assertThat(header.isBold()).isTrue();
            assertThat(header.isItalic()).isTrue();
            assertThat(ByteBuffer.wrap(eot).order(ByteOrder.LITTLE_ENDIAN).getInt(8))
                    .isEqualTo(0x00020001);
            assertThat(eot).endsWith(ttf);
            assertThat(parsed.readNBytes(4)).containsExactly(
                    (byte) eot[0], (byte) eot[1], (byte) eot[2], (byte) eot[3]);
        }
    }

    @Test
    void aHeaderThePresentationCannotReadBackIsRefusedHere() throws Exception {
        // The header is measured where it is written, against the stricter of the two
        // thousand-byte rules the presentation reads it under: taking the face reads each
        // name on its own, and reading the written part back parses the whole header out of
        // the part's first thousand bytes. Both sides of that limit are asserted against
        // POI's own reader — the largest header a face can express is read back, and four
        // bytes more overruns — so the number here can be neither tighter than what the
        // presentation accepts nor looser than what it reads.
        //
        // A header moves in fours — the family is written twice, alone and inside the full
        // name — so 998 is the largest one a face can express and 1,002 the smallest that
        // is too big. These are that pair, at the family: 218 characters make a 436-byte
        // record and, with "Regular", a 452-byte full name, so 96 + 436 + 14 + 452 = 998;
        // 219 make 438 and 454, so 1,002.
        byte[] face = PptxDeck.face("fonts/google/lato/Lato-Regular.ttf");

        byte[] largestReadable = PptxEmbeddedFont.wrap(new ByteArrayInputStream(
                FontNameTable.with(face, 1, "N".repeat(218))));
        FontHeader header = new FontHeader();
        try (InputStream parsed = header.bufferInit(
                new ByteArrayInputStream(largestReadable))) {
            assertThat(header.getFamilyName())
                    .as("the hand-over's reader takes it")
                    .hasSize(218);
            assertThat(parsed.readNBytes(4))
                    .as("and the container it was read from is the one written")
                    .containsExactly(largestReadable[0], largestReadable[1],
                            largestReadable[2], largestReadable[3]);
        }
        try (XMLSlideShow show = new XMLSlideShow()) {
            XSLFFontInfo embedded = new XSLFFontInfo(show, "Largest");
            embedded.addFacet(new ByteArrayInputStream(largestReadable));
            assertThat(embedded.getFacets())
                    .as("and the part it writes reads back, which the per-name limit alone "
                            + "did not give: a header of 1,002 is inside that limit and "
                            + "still overruns the thousand bytes this read takes")
                    .singleElement()
                    .satisfies(facet -> assertThat(facet.getWeight()).isEqualTo(400));
        }

        // The other side, against POI rather than against this code's own arithmetic: four
        // bytes more than the container above and the part POI writes cannot be read back.
        // It has to be built by hand, because the whole point is that the embedder will not
        // write one — growing the accepted container's family record is the smallest
        // mutation that makes the header 1,002 while leaving every field POI reads before
        // it where it was.
        try (XMLSlideShow show = new XMLSlideShow()) {
            XSLFFontInfo embedded = new XSLFFontInfo(show, "Overrun");
            embedded.addFacet(new ByteArrayInputStream(grownBy(largestReadable, 4)));
            assertThatThrownBy(() -> embedded.getFacets().get(0).getWeight())
                    .as("the reader that takes the face accepts it — its rule is per name "
                            + "— and the read of the written part is the one that breaks")
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Buffer overrun")
                    .hasMessageContaining("1000");
        }

        assertThatThrownBy(() -> PptxEmbeddedFont.wrap(new ByteArrayInputStream(
                FontNameTable.with(face, 1, "N".repeat(219)))))
                .as("so the embedder refuses it, checked, with the header and the limit "
                        + "named")
                .isInstanceOf(IOException.class)
                .hasMessageContaining("1002")
                .hasMessageContaining("1000")
                .as("and with each name's own length, since the one that overflows is the "
                        + "joined full name, which no record of the font carries")
                .hasMessageContaining("full name 454");
    }

    /**
     * The same container with its family record longer by the given bytes.
     *
     * <p>The family's length field sits where the fixed fields end — 80 bytes in, after a
     * 2-byte padding — so this grows that field, inserts the bytes it now claims, and
     * carries the container's own size along. Every field the presentation reads before the
     * names is untouched, so the only thing that differs is how far the header reaches.</p>
     */
    private static byte[] grownBy(byte[] container, int bytes) {
        ByteBuffer header = ByteBuffer.wrap(container).order(ByteOrder.LITTLE_ENDIAN);
        int familyLengthAt = 82;
        int familyLength = Short.toUnsignedInt(header.getShort(familyLengthAt));
        int familyEnd = familyLengthAt + 2 + familyLength;
        byte[] grown = new byte[container.length + bytes];
        System.arraycopy(container, 0, grown, 0, familyEnd);
        System.arraycopy(container, familyEnd, grown, familyEnd + bytes,
                container.length - familyEnd);
        ByteBuffer patched = ByteBuffer.wrap(grown).order(ByteOrder.LITTLE_ENDIAN);
        patched.putShort(familyLengthAt, (short) (familyLength + bytes));
        patched.putInt(0, grown.length);
        return grown;
    }
}
