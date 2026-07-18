package com.demcha.compose.document.backend.fixed.pptx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Deterministic output: with a pinned timestamp the same document renders to
 * byte-identical .pptx across runs — the OPC created/modified clocks and every
 * zip entry time carry the pinned instant instead of POI's wall clock.
 */
class PptxDeterminismTest {

    private static final Instant PINNED = Instant.parse("2024-05-05T12:34:56Z");

    private static DocumentSession composeDocument() {
        DocumentSession session = GraphCompose.document()
                .pageSize(400, 300)
                .margin(DocumentInsets.of(24))
                .create();
        session.add(new ParagraphBuilder().name("Title")
                .text("Deterministic deck")
                .build());
        session.add(session.dsl().shape().name("Card").size(140, 40)
                .fillColor(DocumentColor.ROYAL_BLUE)
                .build());
        return session;
    }

    @Test
    void deterministicRendersAreByteIdentical() throws Exception {
        try (DocumentSession session = composeDocument()) {
            PptxFixedLayoutBackend backend = PptxFixedLayoutBackend.builder()
                    .deterministic(PINNED)
                    .build();
            byte[] first = session.render(backend);
            byte[] second = session.render(backend);
            assertThat(second).isEqualTo(first);
        }
    }

    @Test
    void thePinnedInstantLandsInCorePropertiesAndZipEntries() throws Exception {
        try (DocumentSession session = composeDocument()) {
            byte[] pptx = session.render(PptxFixedLayoutBackend.builder()
                    .deterministic(PINNED)
                    .build());
            try (XMLSlideShow show = new XMLSlideShow(new ByteArrayInputStream(pptx))) {
                var core = show.getProperties().getCoreProperties();
                assertThat(core.getCreated().toInstant()).isEqualTo(PINNED);
                assertThat(core.getModified().toInstant()).isEqualTo(PINNED);
            }
            LocalDateTime expected = LocalDateTime.ofInstant(PINNED, ZoneOffset.UTC);
            try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(pptx))) {
                int entries = 0;
                for (ZipEntry entry; (entry = zip.getNextEntry()) != null; entries++) {
                    assertThat(entry.getTimeLocal())
                            .as("entry %s carries the pinned time", entry.getName())
                            .isEqualTo(expected);
                }
                assertThat(entries).isGreaterThan(3);
            }
        }
    }
}
