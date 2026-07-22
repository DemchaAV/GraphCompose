package com.demcha.compose.document.backend.fixed.pptx;

import org.apache.poi.ooxml.POIXMLProperties;
import org.apache.poi.xslf.usermodel.XMLSlideShow;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Makes a rendered .pptx byte-identical across runs. POI seeds two clocks into
 * every written deck: the OPC core properties ({@code dcterms:created} /
 * {@code dcterms:modified}) and the modification time of every zip entry.
 * {@link #pinCoreProperties} replaces the former with a fixed instant before
 * the show is written; {@link #normalizeZipEntries} rewrites the finished
 * archive with every entry's timestamp pinned. Applied by
 * {@link PptxFixedLayoutBackend} only when deterministic output is enabled.
 *
 * @author Artem Demchyshyn
 */
final class PptxDeterminismWriter {

    private PptxDeterminismWriter() {
        // Utility class, no instantiation.
    }

    /**
     * Pins the presentation's OPC created / modified core properties to
     * {@code timestamp}, replacing the wall-clock values POI stamps at
     * creation time.
     */
    static void pinCoreProperties(XMLSlideShow show, Instant timestamp) {
        POIXMLProperties.CoreProperties core = show.getProperties().getCoreProperties();
        Optional<Date> pinned = Optional.of(Date.from(timestamp));
        core.setCreated(pinned);
        core.setModified(pinned);
    }

    /**
     * Rewrites the archive with every entry's modification time pinned to
     * {@code timestamp}. The zip DOS time fields are written from a
     * zone-independent {@link LocalDateTime} (derived at UTC), so the output is
     * byte-identical across machines regardless of their default time zone;
     * entry order and content are preserved, extra fields (which can carry
     * high-resolution timestamps) are dropped.
     */
    static byte[] normalizeZipEntries(byte[] pptx, Instant timestamp) throws IOException {
        LocalDateTime pinned = LocalDateTime.ofInstant(timestamp, ZoneOffset.UTC);
        ByteArrayOutputStream output = new ByteArrayOutputStream(pptx.length);
        try (ZipInputStream input = new ZipInputStream(new ByteArrayInputStream(pptx));
             ZipOutputStream zip = new ZipOutputStream(output)) {
            for (ZipEntry entry; (entry = input.getNextEntry()) != null; ) {
                ZipEntry copy = new ZipEntry(entry.getName());
                copy.setTimeLocal(pinned);
                zip.putNextEntry(copy);
                input.transferTo(zip);
                zip.closeEntry();
            }
        }
        return output.toByteArray();
    }
}
