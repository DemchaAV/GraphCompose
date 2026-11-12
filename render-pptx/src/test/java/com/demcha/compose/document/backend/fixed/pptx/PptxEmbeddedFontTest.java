package com.demcha.compose.document.backend.fixed.pptx;

import org.apache.poi.common.usermodel.fonts.FontHeader;
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
    void aNameThePresentationCannotReadBackIsRefusedHere() throws Exception {
        // The container's names are measured where they are written, because the reader
        // they have to clear refuses unchecked and from inside the hand-over, where a
        // refusal no longer costs only the font. Both sides of the limit are asserted
        // against that reader itself, so the number here cannot drift from the one POI
        // actually applies.
        byte[] face = PptxDeck.face("fonts/google/lato/Lato-Regular.ttf");

        // Just inside: a 490-character family makes a 980-byte name record and, with
        // "Regular", a 996-byte full name. Reading it back through POI's own reader is the
        // premise for the number — a limit tighter than POI's would fail here, and the
        // embedder would be refusing faces the presentation can carry.
        byte[] insideTheLimit = PptxEmbeddedFont.wrap(new ByteArrayInputStream(
                FontNameTable.with(face, 1, "N".repeat(490))));
        FontHeader header = new FontHeader();
        try (InputStream parsed = header.bufferInit(
                new ByteArrayInputStream(insideTheLimit))) {
            assertThat(parsed).isNotNull();
            assertThat(header.getFamilyName())
                    .as("the presentation's reader takes the longest name the embedder "
                            + "will write, and reads it back whole")
                    .hasSize(490);
        }

        // Just outside, and outside on the *derived* name: the family's own record is 994
        // bytes, inside the limit, while the full name the container joins from the family
        // and the style is 1,010 — which is the shape of the face that used to cost a deck.
        assertThatThrownBy(() -> PptxEmbeddedFont.wrap(new ByteArrayInputStream(
                FontNameTable.with(face, 1, "N".repeat(497)))))
                .as("a name past it is refused here, checked, with both numbers named")
                .isInstanceOf(IOException.class)
                .hasMessageContaining("1010")
                .hasMessageContaining("1000");
    }
}
