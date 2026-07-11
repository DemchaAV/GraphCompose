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

    @Test
    void idDoesNotCollideWhenFieldBoundariesShift() {
        // "A B" + "" vs "A" + "B" concatenate identically without length-prefixed
        // frames; the canonical form must keep them apart.
        PDDocumentInformation first = new PDDocumentInformation();
        first.setTitle("A B");
        first.setAuthor("");
        PDDocumentInformation second = new PDDocumentInformation();
        second.setTitle("A");
        second.setAuthor("B");

        assertThat(PdfDeterminismWriter.documentId(first, T))
                .isNotEqualTo(PdfDeterminismWriter.documentId(second, T));
    }

    @Test
    void keywordsAndCustomFieldsParticipateInTheId() {
        byte[] base = PdfDeterminismWriter.documentId(info("Report"), T);

        PDDocumentInformation withKeywords = info("Report");
        withKeywords.setKeywords("alpha beta");
        PDDocumentInformation withCustom = info("Report");
        withCustom.setCustomMetadataValue("Team", "Docs");

        assertThat(PdfDeterminismWriter.documentId(withKeywords, T)).isNotEqualTo(base);
        assertThat(PdfDeterminismWriter.documentId(withCustom, T)).isNotEqualTo(base);
    }
}
