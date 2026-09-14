package com.demcha.compose.document.backend.fixed.pdf;

import com.demcha.compose.document.backend.fixed.pdf.options.PdfProtectionOptions;
import org.apache.pdfbox.pdmodel.PDDocument;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Saves a document whose dictionaries can only be finished once PDFBox has built its font subsets.
 *
 * <p>PDFBox subsets embedded fonts inside {@code save()}, and offers no hook between subsetting
 * and writing. Two things this backend writes depend on what the subsetter produces: the glyph
 * maps of Arabic text drawn in shaped forms, which {@link PdfShapedGlyphUnicode} corrects, and
 * the letter-spaced font resources of {@link PdfTrackedFontResources}, which share a base font's
 * subset and cannot name it before it exists.</p>
 *
 * <p>A document that needs neither is saved exactly once, as it always was. One that needs
 * either is saved twice: once into a null sink, which builds the subsets and clears PDFBox's
 * subsetting queue, and once for real after the dictionaries are finished. Both saves stream;
 * nothing is buffered, so memory stays flat for a document of any size.</p>
 *
 * <p>{@code deferredProtection} is how both survive encryption. Encrypting is part of saving and
 * writes ciphertext back into the streams it encrypts, so a protected first save would leave glyph
 * maps the correction cannot read, and would encrypt the document a second time on the real save.
 * The caller therefore builds a document that saves twice without its protection and hands the
 * policy here, to be applied once, before the final save. A deferred policy is applied on the
 * single-save path as well, so a caller that defers protection cannot lose it.</p>
 */
final class PdfSubsetAwareSave {

    private PdfSubsetAwareSave() {
    }

    /**
     * Saves {@code document}, finishing what depends on its font subsets.
     *
     * @param document           the rendered document
     * @param mayCarryShapedText whether the render drew any reordered text
     * @param letterSpacedFonts  the letter-spaced font resources the render created
     * @param deferredProtection protection to apply before the final save, or {@code null} when
     *                           the document is unprotected or was already protected by the build
     * @param output             where to write
     * @throws IOException if saving fails
     */
    static void save(PDDocument document,
                     boolean mayCarryShapedText,
                     PdfTrackedFontResources letterSpacedFonts,
                     PdfProtectionOptions deferredProtection,
                     OutputStream output) throws IOException {
        boolean finishesFonts = letterSpacedFonts.hasResources();
        if (!mayCarryShapedText && !finishesFonts) {
            if (deferredProtection != null) {
                PdfDocumentPostProcessor.applyProtection(document, deferredProtection);
            }
            document.save(output);
            return;
        }

        // The first save builds the font subsets and, with them, the glyph maps and names the
        // corrections below need, and clears the subsetting queue so the second save writes the
        // finished dictionaries instead of rebuilding them. Its bytes are not kept.
        document.save(OutputStream.nullOutputStream());
        if (mayCarryShapedText) {
            PdfShapedGlyphUnicode.restoreBaseLetters(document);
        }
        // After the glyph maps: a letter-spaced resource shares its base font's ToUnicode by
        // reference, so it has to pick up the corrected stream rather than the one it replaced.
        if (finishesFonts) {
            letterSpacedFonts.completeAfterSubsetting();
        }
        if (deferredProtection != null) {
            PdfDocumentPostProcessor.applyProtection(document, deferredProtection);
        }
        document.save(output);
    }
}
