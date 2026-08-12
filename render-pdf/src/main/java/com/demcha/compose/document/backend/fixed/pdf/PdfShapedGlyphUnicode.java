package com.demcha.compose.document.backend.fixed.pdf;

import com.demcha.compose.document.backend.fixed.pdf.options.PdfProtectionOptions;
import com.demcha.compose.engine.text.bidi.ArabicShaper;

import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSStream;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Makes a document say the letters an author wrote, not the shapes it drew them as.
 *
 * <p>Arabic letters change form with their neighbours, and a PDF font is addressed by
 * character rather than by glyph — {@code showText} looks each character up in the font's
 * character map and never runs the substitutions that would join anything. So the engine
 * shapes the text first, into the Unicode block that names the joined forms directly: the
 * alef an author typed as U+0627 is drawn as U+FE8E, its final form.</p>
 *
 * <p>That is a decision about drawing, and it should stop at the page. It does not. The
 * font's {@code ToUnicode} map — what each glyph in the file <em>means</em> — is built by
 * the subsetter from the characters shown, so it inherits the shapes: the file states that
 * this glyph means U+FE8E. A reader that applies a compatibility normalisation recovers the
 * letter; a reader that hands the code point straight to a search box does not, and a search
 * for the ordinary spelling of the word finds nothing with no sign of why.</p>
 *
 * <p>This rewrites those maps after the subsetter has built them, so the file names the
 * letters. Hebrew and every other script are untouched: only the Arabic presentation forms
 * are shapes standing in for something else.</p>
 *
 * <p>The rewrite has to happen after the map exists, and the map is built during the save.
 * Hence the shape of {@link #save}: the first save is what builds the subsets, and a second
 * one is spent only when there is something to correct. A document that never reordered a
 * line skips all of it and is saved exactly as before.</p>
 */
final class PdfShapedGlyphUnicode {

    /** Arabic Presentation Forms-B, the block the shaper draws from. */
    private static final int FORMS_FIRST = 0xFE70;
    private static final int FORMS_LAST = 0xFEFC;

    /** {@code <src> <dst>} in a bfchar block, {@code <lo> <hi> <dst>} in a bfrange one. */
    private static final Pattern MAPPING_BLOCK =
            Pattern.compile("(\\d+)[ \\t\\r\\n]+begin(bfchar|bfrange)(.*?)end\\2", Pattern.DOTALL);

    /** The most entries one mapping block may carry, per the CMap specification. */
    private static final int BLOCK_LIMIT = 100;
    private static final Pattern MAPPING_ENTRY =
            Pattern.compile("^[ \\t]*<([0-9A-Fa-f]{4})>[ \\t]+(?:<([0-9A-Fa-f]{4})>[ \\t]+)?"
                    + "<([0-9A-Fa-f]{4,})>[ \\t]*$", Pattern.MULTILINE);

    private PdfShapedGlyphUnicode() {
    }

    /**
     * Saves {@code document}, correcting what its glyphs claim to mean.
     *
     * <p>{@code mayCarryShapedText} is the caller's answer to whether the render reordered
     * anything, which is the only way text reaches the page in a shaped form. When it did
     * not, this is {@link PDDocument#save(OutputStream)} and nothing else — no second
     * pass, no behaviour to regress.</p>
     *
     * <p>When it did, the map this needs to read is built <em>during</em> a save, so the
     * document is saved twice: once into a null sink, which builds the font subsets and
     * their glyph maps and clears the subsetting queue, and once for real after the maps
     * are corrected. Both saves stream; nothing is buffered.</p>
     *
     * <p>{@code deferredProtection} is how the correction survives encryption. Encrypting
     * is part of saving, and it writes the ciphertext back into the streams it encrypted —
     * so a map built by a protected first save would be unreadable, and the correction
     * would silently find nothing. The caller therefore builds a protected, reordered
     * document <em>without</em> its protection and hands it here; the policy is applied
     * between the two saves, so the first save writes readable maps and the second
     * encrypts the corrected document exactly once.</p>
     *
     * @param document           the rendered document
     * @param mayCarryShapedText whether the render drew any reordered text
     * @param deferredProtection protection to apply between the saves, or {@code null}
     *                           when the document is unprotected or was already protected
     *                           by the build (which the caller does whenever no text was
     *                           reordered)
     * @param output             where to write
     * @throws IOException if saving fails
     */
    static void save(PDDocument document,
                     boolean mayCarryShapedText,
                     PdfProtectionOptions deferredProtection,
                     OutputStream output) throws IOException {

        if (!mayCarryShapedText) {
            document.save(output);
            return;
        }

        // The first save is what builds the font subsets and, with them, the glyph maps
        // this needs to read. It also clears the document's subsetting queue, so the
        // second save writes the corrected maps rather than rebuilding them. Its bytes
        // are not kept: the second save produces the same document, corrected, and
        // streaming it directly to the caller is what keeps memory flat for a document
        // of any size.
        document.save(OutputStream.nullOutputStream());
        restoreBaseLetters(document);
        if (deferredProtection != null) {
            PdfDocumentPostProcessor.applyProtection(document, deferredProtection);
        }
        document.save(output);
    }

    /**
     * Rewrites every glyph map in the document that names a shaped form.
     *
     * @param document a document whose fonts have been subset
     * @return whether any map was rewritten
     * @throws IOException if a map cannot be read or replaced
     */
    static boolean restoreBaseLetters(PDDocument document) throws IOException {
        // One font is usually on several pages, and its map must not be rewritten twice —
        // the second pass would find base letters, conclude the map needs nothing, and
        // quietly undo the bookkeeping that says something changed.
        Set<COSStream> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        boolean changed = false;
        for (PDPage page : document.getPages()) {
            PDResources resources = page.getResources();
            if (resources == null) {
                continue;
            }
            // Walked as dictionaries rather than through resources.getFont(name): building
            // a PDFont parses the embedded font program, and the only thing needed here
            // is the ToUnicode entry on the font's own dictionary.
            COSDictionary fonts = resources.getCOSObject().getCOSDictionary(COSName.FONT);
            if (fonts == null) {
                continue;
            }
            for (COSName name : fonts.keySet()) {
                COSBase entry = fonts.getDictionaryObject(name);
                if (!(entry instanceof COSDictionary font)) {
                    continue;
                }
                COSBase raw = font.getDictionaryObject(COSName.TO_UNICODE);
                if (!(raw instanceof COSStream map) || !visited.add(map)) {
                    continue;
                }
                changed |= rewrite(document, font, map);
            }
        }
        return changed;
    }

    /** Replaces one font's map, if it names any shaped form. */
    private static boolean rewrite(PDDocument document, COSDictionary font, COSStream map)
            throws IOException {

        String cmap;
        try (InputStream in = map.createInputStream()) {
            cmap = new String(in.readAllBytes(), StandardCharsets.ISO_8859_1);
        }
        String corrected = withBaseLetters(cmap);
        if (corrected == null) {
            return false;
        }

        COSStream replacement = document.getDocument().createCOSStream();
        try (OutputStream out = replacement.createOutputStream(COSName.FLATE_DECODE)) {
            out.write(corrected.getBytes(StandardCharsets.ISO_8859_1));
        }
        font.setItem(COSName.TO_UNICODE, replacement);
        return true;
    }

    /**
     * The same CMap with shaped forms replaced by the letters they stand for.
     *
     * <p>Only the mapping blocks are touched. {@code codespacerange} states which byte
     * sequences are valid glyph codes rather than what any of them means, and the header
     * and trailer are the CMap's own scaffolding — rewriting entries in place keeps all of
     * it exactly as the subsetter wrote it.</p>
     *
     * @param cmap the map as written
     * @return the corrected map, or {@code null} when nothing named a shaped form
     */
    private static String withBaseLetters(String cmap) {
        StringBuilder out = new StringBuilder(cmap.length());
        Matcher blocks = MAPPING_BLOCK.matcher(cmap);
        boolean changed = false;
        int copiedTo = 0;
        while (blocks.find()) {
            boolean ranges = "bfrange".equals(blocks.group(2));
            List<String> corrected = correctedBlockEntries(blocks.group(3), ranges);
            if (corrected == null) {
                continue;
            }
            changed = true;
            // The whole block is rewritten, not just its body: expanding a multi-code
            // range changes how many entries the block holds, and the count before
            // "begin" has to say so. A block can also outgrow the specification's
            // hundred-entry limit that way, in which case it is split into as many
            // blocks as its entries need.
            out.append(cmap, copiedTo, blocks.start(1));
            String kind = blocks.group(2);
            for (int from = 0; from < corrected.size(); from += BLOCK_LIMIT) {
                List<String> chunk = corrected.subList(from,
                        Math.min(corrected.size(), from + BLOCK_LIMIT));
                if (from > 0) {
                    out.append('\n');
                }
                out.append(chunk.size()).append(" begin").append(kind).append('\n');
                for (String entry : chunk) {
                    out.append(entry).append('\n');
                }
                out.append("end").append(kind);
            }
            copiedTo = blocks.end();
        }
        if (!changed) {
            return null;
        }
        return out.append(cmap, copiedTo, cmap.length()).toString();
    }

    /**
     * One block's entries after correction, or {@code null} when there is nothing to say.
     *
     * <p>Every entry of the block comes back — corrected where it named a shaped form,
     * copied as written where it did not — so the caller can rebuild the block with a
     * count that matches. A block holding any line the entry pattern cannot read is left
     * untouched entirely, rather than risk dropping what was not understood: an
     * unfamiliar map keeps whatever it said. A range may also spell its destinations as
     * an array, one per code; nothing produces that here, and such a line fails the
     * pattern and keeps its block as written.</p>
     */
    private static List<String> correctedBlockEntries(String body, boolean ranges) {
        int lines = 0;
        for (String line : body.split("\\R")) {
            if (!line.isBlank()) {
                lines++;
            }
        }

        List<String> entries = new ArrayList<>();
        Matcher matcher = MAPPING_ENTRY.matcher(body);
        boolean changed = false;
        int recognised = 0;
        while (matcher.find()) {
            recognised++;
            int first = Integer.parseInt(matcher.group(1), 16);
            int last = ranges && matcher.group(2) != null
                    ? Integer.parseInt(matcher.group(2), 16) : first;
            String destination = matcher.group(3);

            if (last < first) {
                // A range running backwards is a malformed map. Correcting its healthy
                // neighbours while carrying the malformed line forward would be a
                // half-rewrite of something already broken — keep the block as written.
                return null;
            }
            List<String> corrected = correctedEntries(first, last, destination, ranges);
            if (corrected == null) {
                entries.add(matcher.group().strip());
            } else {
                changed = true;
                entries.addAll(corrected);
            }
        }
        if (!changed || recognised != lines) {
            return null;
        }
        return entries;
    }

    /**
     * The lines that replace one entry, or {@code null} when it names no shaped form.
     *
     * <p>A range states that consecutive glyph codes mean consecutive characters, which
     * stops being true the moment any of them is rewritten — the forms of one letter are
     * consecutive, the letters themselves are not. Such a range is spelled out one code at
     * a time, which a range block still accepts: a single code is a range of one.</p>
     */
    private static List<String> correctedEntries(int first, int last, String destination,
                                                 boolean ranges) {
        int start = Integer.parseInt(destination.substring(0, 4), 16);
        boolean multiCharacter = destination.length() > 4;
        if (multiCharacter || start > FORMS_LAST || start + (last - first) < FORMS_FIRST) {
            // Already spelled as several characters, or the whole run lies outside the
            // block — either way there is nothing here that stands for something else.
            return null;
        }

        List<String> lines = new ArrayList<>(last - first + 1);
        boolean changed = false;
        for (int code = first; code <= last; code++) {
            int meaning = start + (code - first);
            String base = meaning >= FORMS_FIRST && meaning <= FORMS_LAST
                    ? ArabicShaper.baseLettersOf(meaning) : null;
            if (base != null) {
                changed = true;
            }
            String target = base == null ? String.format("%04X", meaning) : hex(base);
            lines.add(ranges
                    ? String.format("<%04X> <%04X> <%s>", code, code, target)
                    : String.format("<%04X> <%s>", code, target));
        }
        return changed ? lines : null;
    }

    /** A destination as the CMap spells one: the UTF-16 code units, big-endian. */
    private static String hex(String characters) {
        StringBuilder out = new StringBuilder(characters.length() * 4);
        for (int index = 0; index < characters.length(); index++) {
            out.append(String.format("%04X", (int) characters.charAt(index)));
        }
        return out.toString();
    }
}
