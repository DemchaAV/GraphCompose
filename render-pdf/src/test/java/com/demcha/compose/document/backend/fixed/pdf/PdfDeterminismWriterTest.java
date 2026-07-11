package com.demcha.compose.document.backend.fixed.pdf;

import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit contract for the deterministic {@code /ID} derivation. The full
 * render-twice byte-identity guarantee lives in {@link PdfDeterministicOutputTest};
 * these pin the id-from-metadata logic in isolation.
 */
class PdfDeterminismWriterTest {

    private static final Instant T = Instant.parse("2020-05-15T00:00:00Z");

    private static PDDocumentInformation info(String title) {
        PDDocumentInformation info = new PDDocumentInformation();
        info.setTitle(title);
        return info;
    }

    @Test
    void idIsStableForTheSameMetadataAndTimestamp() {
        assertThat(PdfDeterminismWriter.documentId(info("Report"), T))
                .isEqualTo(PdfDeterminismWriter.documentId(info("Report"), T));
    }

    @Test
    void idDiffersWhenMetadataDiffers() {
        assertThat(PdfDeterminismWriter.documentId(info("Report"), T))
                .isNotEqualTo(PdfDeterminismWriter.documentId(info("Invoice"), T));
    }

    @Test
    void idDiffersWhenTimestampDiffers() {
        assertThat(PdfDeterminismWriter.documentId(info("Report"), T))
                .isNotEqualTo(PdfDeterminismWriter.documentId(info("Report"), Instant.EPOCH));
    }

    @Test
    void idIsA16ByteHashEvenWithNoMetadata() {
        assertThat(PdfDeterminismWriter.documentId(new PDDocumentInformation(), T)).hasSize(16);
    }
}
