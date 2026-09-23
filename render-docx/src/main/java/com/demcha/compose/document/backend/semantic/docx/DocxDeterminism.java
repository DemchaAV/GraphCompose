package com.demcha.compose.document.backend.semantic.docx;

import org.apache.poi.ooxml.POIXMLProperties;
import org.apache.poi.xwpf.usermodel.XWPFDocument;

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
 * Makes an exported .docx byte-identical across runs.
 *
 * <p>POI seeds two clocks into every package it writes: the OPC core properties
 * ({@code dcterms:created} / {@code dcterms:modified}) and the modification time of every zip
 * entry. {@link #pinCoreProperties} replaces the former with a fixed instant before the
 * document is written; {@link #normalizeZipEntries} rewrites the finished archive with every
 * entry's time pinned. Applied only when deterministic output is enabled.</p>
 *
 * <p>The same two steps the PPTX backend takes — {@code PptxDeterminismWriter} — kept as a
 * twin rather than shared, because the two modules do not depend on each other and the plan
 * for this export rules out a shared OOXML module. A third source of difference is DOCX's
 * alone, embedded fonts' obfuscation keys, and {@link DocxFontEmbedding} derives those from
 * the font so they need no pinning at all.</p>
 *
 * @author Artem Demchyshyn
 */
final class DocxDeterminism {

    private DocxDeterminism() {
    }

    /**
     * Pins the document's OPC created / modified core properties to {@code timestamp},
     * replacing the wall-clock values POI stamps at creation time.
     */
    static void pinCoreProperties(XWPFDocument document, Instant timestamp) {
        POIXMLProperties.CoreProperties core = document.getProperties().getCoreProperties();
        Optional<Date> pinned = Optional.of(Date.from(timestamp));
        core.setCreated(pinned);
        core.setModified(pinned);
    }

    /**
     * Rewrites the archive with every entry's modification time pinned to {@code timestamp}.
     * The zip DOS time fields are written from a zone-independent {@link LocalDateTime}
     * derived at UTC, so the output is byte-identical across machines whatever their default
     * time zone; entry order and content are preserved, and extra fields — which can carry
     * high-resolution timestamps — are dropped.
     */
    static byte[] normalizeZipEntries(byte[] docx, Instant timestamp) throws IOException {
        LocalDateTime pinned = LocalDateTime.ofInstant(timestamp, ZoneOffset.UTC);
        ByteArrayOutputStream output = new ByteArrayOutputStream(docx.length);
        try (ZipInputStream input = new ZipInputStream(new ByteArrayInputStream(docx));
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
