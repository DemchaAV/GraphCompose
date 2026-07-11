package com.demcha.compose.document.backend.fixed.pdf;

import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSString;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Calendar;
import java.util.GregorianCalendar;

/**
 * Makes a {@link PDDocument} render to byte-identical output across runs: pins
 * the CreationDate / ModDate to a fixed instant and replaces PDFBox's
 * time-seeded {@code /ID} trailer with one derived from the document's info
 * dictionary. Applied by {@link PdfFixedLayoutBackend} only when deterministic
 * output is enabled.
 *
 * <p>The {@code /ID} is derived from the info-dictionary values (not the page
 * content), so documents with identical metadata but different content share an
 * {@code /ID}. Package-private — engine surface, not public API.</p>
 *
 * @author Artem Demchyshyn
 */
final class PdfDeterminismWriter {

    private PdfDeterminismWriter() {
        // Utility class, no instantiation.
    }

    /**
     * Pins the document's CreationDate / ModDate to {@code timestamp} and sets a
     * stable {@code /ID} derived from the info dictionary.
     *
     * @param document  the assembled document, mutated in place
     * @param timestamp the instant to pin CreationDate / ModDate to
     */
    static void apply(PDDocument document, Instant timestamp) {
        Calendar pinned = GregorianCalendar.from(timestamp.atZone(ZoneOffset.UTC));
        PDDocumentInformation info = document.getDocumentInformation();
        info.setCreationDate(pinned);
        info.setModificationDate(pinned);

        COSString idString = new COSString(documentId(info, timestamp));
        COSArray idArray = new COSArray();
        idArray.add(idString);
        idArray.add(idString);
        document.getDocument().getTrailer().setItem(COSName.ID, idArray);
    }

    /**
     * Derives a stable 16-byte {@code /ID} from the info dictionary and the pinned
     * timestamp — deterministic across runs, and distinct for documents whose
     * metadata differs.
     */
    static byte[] documentId(PDDocumentInformation info, Instant timestamp) {
        String seed = String.join(" ",
                String.valueOf(timestamp),
                nullToEmpty(info.getTitle()),
                nullToEmpty(info.getAuthor()),
                nullToEmpty(info.getSubject()),
                nullToEmpty(info.getCreator()),
                nullToEmpty(info.getProducer()));
        try {
            return MessageDigest.getInstance("MD5").digest(seed.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException ex) {
            // MD5 is guaranteed on every JRE; unreachable in practice.
            throw new IllegalStateException("MD5 unavailable for deterministic PDF /ID", ex);
        }
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
