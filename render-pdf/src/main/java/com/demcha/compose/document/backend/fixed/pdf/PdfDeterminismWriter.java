package com.demcha.compose.document.backend.fixed.pdf;

import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSString;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.GregorianCalendar;
import java.util.List;

/**
 * Makes a {@link PDDocument} render to byte-identical output across runs: pins
 * the CreationDate / ModDate to a fixed instant and replaces PDFBox's
 * time-seeded {@code /ID} trailer with one derived from the document's info
 * dictionary. Applied by {@link PdfFixedLayoutBackend} only when deterministic
 * output is enabled.
 *
 * <p>The {@code /ID} is derived from the info-dictionary entries (not the page
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
     * stable {@code /ID} derived from the info dictionary (the pinned dates are
     * set first, so they participate in the id).
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
     * Derives a stable 16-byte {@code /ID} from the pinned timestamp and the
     * complete info dictionary — every entry participates (title, author,
     * keywords, custom fields, …), canonicalized as sorted, length-prefixed
     * key/value frames so distinct dictionaries cannot collide structurally.
     * Deterministic across runs; SHA-256 truncated to the 16 bytes a PDF
     * {@code /ID} carries.
     */
    static byte[] documentId(PDDocumentInformation info, Instant timestamp) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            putFrame(digest, String.valueOf(timestamp));
            List<COSName> keys = new ArrayList<>(info.getCOSObject().keySet());
            keys.sort(Comparator.comparing(COSName::getName));
            for (COSName key : keys) {
                putFrame(digest, key.getName());
                putFrame(digest, stringValue(info.getCOSObject().getDictionaryObject(key)));
            }
            return Arrays.copyOf(digest.digest(), 16);
        } catch (NoSuchAlgorithmException ex) {
            // SHA-256 is guaranteed on every JRE; unreachable in practice.
            throw new IllegalStateException("SHA-256 unavailable for deterministic PDF /ID", ex);
        }
    }

    /** Length-prefixes each value so adjacent frames cannot blur into each other. */
    private static void putFrame(MessageDigest digest, String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        digest.update(new byte[] {
                (byte) (bytes.length >>> 24), (byte) (bytes.length >>> 16),
                (byte) (bytes.length >>> 8), (byte) bytes.length});
        digest.update(bytes);
    }

    private static String stringValue(COSBase value) {
        if (value instanceof COSString string) {
            return string.getString();
        }
        if (value instanceof COSName name) {
            return name.getName();
        }
        return String.valueOf(value);
    }
}
