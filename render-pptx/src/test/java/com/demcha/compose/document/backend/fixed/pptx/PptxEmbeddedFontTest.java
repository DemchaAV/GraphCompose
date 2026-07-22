package com.demcha.compose.document.backend.fixed.pptx;

import org.apache.poi.common.usermodel.fonts.FontHeader;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.assertj.core.api.Assertions.assertThat;

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
}
