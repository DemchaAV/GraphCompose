package com.demcha.compose.document.backend.fixed.pptx;

import com.demcha.compose.document.backend.fixed.pptx.PptxSlotLedger.Seated;
import com.demcha.compose.font.FontFamilyDefinition;
import com.demcha.compose.font.FontName;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFFontInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Carries font families into a presentation: reads every face a family is to travel with,
 * refuses it whole where a face cannot travel (holding back only the face where its licence
 * is preview-and-print), claims its style slots in the render's
 * {@link PptxSlotLedger} and hands the programs to the presentation.
 *
 * <p>Stateless: what a render remembers between families lives in the ledger and in
 * {@link PptxDeckFonts}, which decides which families are offered here and what the
 * reader is told about a family that did not travel.</p>
 */
final class PptxFontEmbedder {

    private static final Logger LOG = LoggerFactory.getLogger("com.demcha.compose.engine.render");

    private PptxFontEmbedder() {
    }

    /**
     * How much of a family the presentation ended up holding.
     *
     * <p>Three answers rather than whether the family travelled, because the two ways of not
     * travelling leave different decks and the reader is told different things about them.
     * One refusal a font can provoke costs a face and not the family: a face whose licence allows
     * embedding for preview and print only is held back, and the family travels without it, which
     * is {@link #PART}. Every other one arrives while the presentation is still untouched — a face
     * that cannot be read, one whose licence bits forbid embedding, a header the presentation
     * could not read back, two of the family's faces reading back as one style slot, a face
     * arriving into a slot another family holds, the viewer metrics — and leaves nothing of the
     * family: {@link #NONE}. The presentation refusing a face it has been handed is the
     * other {@link #PART}: it keeps what it took before the refusal, and what is left of that
     * refusal is POI's own part store failing (see {@link #handOverPrograms}). The
     * presentation's first read of a face, {@code FontHeader.bufferInit}, reads only the header,
     * which {@link PptxEmbeddedFont} has already measured against it.</p>
     */
    enum Carried {
        /** Every face the family was offered with is in the deck. */
        WHOLE,
        /** Nothing of the family is: it was refused before the presentation was handed a face. */
        NONE,
        /**
         * Some of the family is in the deck and not all of it. Either a face was held back for a
         * preview-and-print licence and every other face travelled, or the presentation refused
         * a face it was handed and keeps what it took before that: the declaration at least, and
         * any face handed over first.
         */
        PART
    }

    /**
     * What carrying a registered family came to.
     *
     * @param carried how much of the family the deck holds
     * @param metrics the viewer metrics its text is seated by; {@code null} unless the family
     *                travelled whole, or travelled short only of a face held back for its
     *                licence
     */
    record Embedding(Carried carried, PptxViewerMetrics.ViewerFontMetrics metrics) {
        private static final Embedding NONE = new Embedding(Carried.NONE, null);
        private static final Embedding PART = new Embedding(Carried.PART, null);
    }

    /**
     * Carries one registered font family into the presentation, with the viewer
     * metrics its text is seated by. Embedding is best-effort: a font whose
     * license bits forbid embedding, or whose bytes cannot be read at all — a
     * missing file, a missing classpath resource, a program either reader cannot
     * parse — costs the deck the font program rather than the render: page
     * geometry is unaffected, and text drawn in the family needs that font on the
     * machine the deck is opened on. None of those fails the export here, however
     * the refusal arrives; this answers how much of the family the deck holds
     * instead, and names the face in the log.
     *
     * <p>A face whose licence allows embedding for preview and print only costs the deck that
     * face and nothing else: the family travels without it, with its metrics, as
     * {@link Carried#PART}. See {@link #readPrograms}.</p>
     *
     * <p>One answer covers the program and the metrics: a program the metrics
     * reader rejects is not handed to the presentation either (see the comment
     * where the metrics are read). A family the page
     * had to <em>measure</em> is a different matter and fails the render before a
     * slide is built. The presentation refusing a face it is handed is a third,
     * and what is left of it after the name check in {@link PptxEmbeddedFont} is
     * POI's part store — see {@link #handOverPrograms}.</p>
     *
     * <p>A fourth refuses the family too: a face arriving into a style slot another family
     * already holds under the same declared name. The caller does not tell those that answer here
     * apart by cause: an unreadable face, a restricted or bitmap-only one, the viewer metrics and
     * the slot are each {@link Carried#NONE}, the presentation being untouched when they arrive,
     * while a face held back for a preview-and-print licence and the presentation refusing a face
     * are each {@link Carried#PART}. Which line the caller writes for a
     * family not carried whole is decided by what the deck
     * carries under the family's declared name once every registration has been read — see
     * {@code PptxDeckFonts.warnRegisteredFamilyNotCarried} — so a family sharing a name in a
     * <em>free</em> slot and then given up by the metrics read is told the same as one refused
     * for the slot, and in either order of registration.</p>
     *
     * @return how much of the family the deck holds, with its viewer metrics where it travelled
     *         whole or short only of a face held back for its licence
     */
    static Embedding embedFontFamilyWithMetrics(
            XMLSlideShow show,
            FontFamilyDefinition family,
            FontFamilyDefinition.FontSourceSet sources,
            PptxSlotLedger ledger,
            Map<FontName, Set<String>> carriedSlots) {
        FamilyPrograms read =
                readPrograms(family, sources, EnumSet.allOf(PptxFontFace.class), ledger);
        if (read == null) {
            return Embedding.NONE;
        }
        // The other read of the same programs, and the last one before the presentation is
        // touched: a font whose tail is missing passes fontbox's header parse, which skips
        // a table that runs past the end of the file, and is rejected outright by the
        // reader below.
        //
        // It runs before the presentation is handed anything because one answer has to
        // gate both halves. The seat is no longer what speaks against carrying a family
        // whose metrics would not read: one without metrics of its own is seated from the
        // program the page measured it with (PptxViewerMetrics.seatPoints), the same OS/2
        // fields PowerPoint reads. What does is the program: one reader rejecting it says
        // it is damaged, and which of the two readers is right about such a file was not
        // measured, so it is not handed to a viewer. Not carrying it is the degradation
        // the backend already has a warning for.
        PptxViewerMetrics.ViewerFontMetrics viewerMetrics;
        try {
            viewerMetrics = PptxViewerMetrics.load(family, sources);
        } catch (PptxViewerMetrics.UnreadableFaceException exception) {
            // Same key and fields as the program read's refusal, so one log filter catches
            // every face this deck could not carry — and the reason says which of the two
            // reads turned it down, since a face the embedder took and the metrics reader
            // refused is a different thing for the reader to act on than one neither could
            // read.
            LOG.warn("render.pptx.font.embed.skipped family={} source={} reason=viewer "
                            + "metrics: {}",
                    family.name().name(), exception.source(),
                    String.valueOf(exception.getCause()));
            return Embedding.NONE;
        }
        claimSlotsAndReportSharing(ledger, family, read.slots());
        if (!handOverPrograms(show, family, read.programs())) {
            return Embedding.PART;
        }
        // Recorded here and not at the read, for the reason the slot claim is: a family that did
        // not make it into the presentation carries nothing, and a run of it has nothing but the
        // family's name to go by. An absent entry says exactly that, so nothing has to be undone
        // on the way out of a refusal.
        carriedSlots.put(family.name(), Set.copyOf(read.slots().keySet()));
        // A family short of a face held back for its licence keeps its metrics: every face it
        // carries is in the deck as handed over, so for those faces they are the metrics its
        // text is drawn with. The held-back style is still seated from that face's own metrics;
        // how far that is from the face a viewer draws instead was not measured in general
        // (Zilla Slab's italic and bold-italic faces state the same Windows and typographic
        // metrics, so they seat alike).
        return new Embedding(read.whole() ? Carried.WHOLE : Carried.PART, viewerMetrics);
    }

    /**
     * Embeds the named faces of a family, without viewer metrics.
     *
     * <p>Faces rather than the whole family, because embedding carries a font program
     * whole: a deck drawing one weight of a CJK family was shipping the other at over two
     * megabytes for glyphs no slide contains. A family whose source set repeats a source
     * across faces — a regular-and-bold family asked for italic — embeds it once, since
     * the sources dedupe by description.</p>
     *
     * <p>Embedding is best-effort here as it is for a registered family: a face whose
     * licence bits forbid embedding, or whose bytes cannot be read, costs the deck the
     * font program rather than the render, and this answers {@link Carried#NONE} with the face
     * named in the log. A face held back for a preview-and-print licence costs only that face,
     * and this answers {@link Carried#PART} for a family that carries the rest.</p>
     *
     * <p>The bundled families take this road, and deliberately get no metrics: see
     * {@link PptxDeckFonts#embedBundledFontsUsed()}. A family a caller registered goes through
     * {@link #embedFontFamilyWithMetrics} instead, which reads its metrics in the same
     * pre-flight.</p>
     *
     * @return how much of the family the presentation holds
     */
    static Carried embedFontFamily(XMLSlideShow show,
                                   FontFamilyDefinition family,
                                   FontFamilyDefinition.FontSourceSet sources,
                                   Set<PptxFontFace> faces,
                                   PptxSlotLedger ledger) {
        FamilyPrograms read = readPrograms(family, sources, faces, ledger);
        if (read == null) {
            return Carried.NONE;
        }
        claimSlotsAndReportSharing(ledger, family, read.slots());
        return handOverPrograms(show, family, read.programs()) && read.whole()
                ? Carried.WHOLE : Carried.PART;
    }

    /**
     * Reads and wraps the named faces' font programs, keyed by source description.
     *
     * <p>Every face is read before any of them is handed to the presentation, because one
     * answer covers the whole family: it decides whether the family is carried at all and
     * whether the reader is told to install the font. The presentation is mutated at both
     * of the hand-over's steps — constructing the font info declares the family, and
     * adding a facet writes its part and sets the embedded-font flags — so handing faces
     * over as they were read left a family whose second face could not be read with its
     * first face in the deck, while the caller was told the deck carries nothing of it. A
     * family whose first face could not be read left the bare declaration.</p>
     *
     * <p>That holds for everything that fails before the hand-over: a face that cannot be
     * read, and a face read whole whose licence bits forbid embedding, which
     * {@link PptxEmbeddedFont#wrap} refuses. So a family of one embeddable face and one
     * restricted face carries neither, where it used to carry the first. It does not hold
     * for the presentation refusing a face it is handed: see {@link #handOverPrograms}.</p>
     *
     * <p>Nor for a face whose licence allows embedding for preview and print only
     * ({@code fsType} bit {@code 0x0004} without {@code 0x0008}), which is held back on its own
     * and named in the log, while the family's other faces are read and carried. That is the
     * DOCX backend's reading of the same bits, and it refuses per face there too. Nothing is
     * wrong with such a face except that a deck is a file people edit, so it is not a reason to
     * keep its family's other faces out. Of the 130 bundled faces, {@code ZillaSlab-BoldItalic}
     * is the only one this reaches. A family all of whose offered faces are held back answers
     * {@code null}, as a refusal does, so the presentation never declares a family it carries
     * no face of.</p>
     *
     * <p>One refusal is the family's rather than a face's, and belongs here for that
     * reason: a presentation seats a face in one of four style slots, chosen off the
     * container, and holds one face in each. Two faces of a family that read back as the
     * same slot cannot both travel, and the slot a face lands in is not the slot it was
     * registered under — a face in the bold slot whose own weight is 400 or less reads
     * back as the regular one. That used to end with the deck carrying the later face's
     * program under the earlier face's declaration, and nothing said: the presentation
     * reports the family as embedded, and every run of it, the regular ones included, is
     * drawn from the one program that survived. Refused rather than resolved, because the
     * slot is the presentation's answer and not a choice this backend has: there is no
     * fifth slot to move a face to, and the two that collide agree on the fields the slot
     * is read from. Refused for the family rather than the face for the reason the other
     * refusals here have on the registered path — keeping the first would leave the family
     * its viewer metrics while a face of it did not travel — and, on the bundled path,
     * which has no metrics to keep, because a refusal here answers nothing for a family
     * either way: it leaves nothing of it, which is what the caller's substitution warning
     * then says. A deck carrying some of a family is left to a face held back for its licence,
     * which is not a fault of the family's, and to the one refusal that can still arrive
     * after the presentation is handed it.</p>
     *
     * <p>The slot's capacity belongs to the <em>declared</em> family name rather than to a
     * definition, so the same refusal answers for two definitions that share one. Two
     * definitions whose {@link FontFamilyDefinition#wordFamily()} matches, ignoring case,
     * write into one font-list entry — that is how {@code XSLFFontInfo} looks an entry up —
     * and so into one set of four slots. The family whose face arrives into a slot another
     * family already holds is refused whole, as a family with an unreadable face is — not
     * only the colliding face, so a face of it that would have landed in a free slot does
     * not travel either; the family holding the slot keeps its program, and its viewer
     * metrics where it has any — the holder is always a registration, the registered pass
     * running before a slide is drawn and no two bundled families declaring one name, so the
     * bundled path's deliberate lack of metrics never comes up here. Measured: two
     * definitions under one
     * {@code wordFamily}, each with a single regular face, left one part holding the
     * second's bytes with nothing said, and PowerPoint 16.0 drew both families' text from
     * it.</p>
     *
     * <p>Which way round the refusal falls is a decision rather than a property of the
     * format, and it leaves an explicitly registered family carrying its own font where a
     * bundled one of the same name would otherwise have written over it. Its limit is that
     * the registered pass runs before a run is drawn, so it cannot prefer the family the
     * deck actually sets text in: a family registered under a bundled one's name and never
     * drawn in still takes the slot, and the bundled family's own text is then drawn from
     * that program — where before this guard the bundled face wrote over it and that text
     * was right. Both arrangements are wrong for one of the two families, this one says
     * which, and the alternative — holding a registered family's hand-over back until the
     * faces the deck drew with are known — would decide it by what is on the slides rather
     * than by registration order.</p>
     *
     * <p>What refusing does <em>not</em> fix, and what the message therefore says: the
     * refused family's text carries the shared declared name too, so a viewer draws it from
     * the program that did travel. A run names a family and nothing else, so no deck can
     * hold two programs for one name and one style. What the refusal buys is the other
     * family's text, which was being drawn from the wrong program before.</p>
     *
     * <p>What this does <em>not</em> cover: the refusal keys on the family a face was
     * registered under, and on it as a {@link FontName}, which is case-insensitive. So two
     * registrations the rest of the engine treats as one family are one family here, and the
     * second is not refused: its faces take the slots as the first's would, and where the two
     * name different files the later program still writes over the earlier one. Deliberate on
     * both counts. An identical definition registered twice has to stay harmless; and for two
     * registrations of one {@code FontName} the later program is the one the page
     * <em>measured</em> with, because the font library keeps the last factory registered under
     * a name — so refusing it would leave the deck carrying a face no text was laid out in,
     * which is the defect this guard exists to stop rather than a milder form of it.</p>
     *
     * <p>Nothing of the presentation is touched here, so a refusal costs the deck the family
     * and nothing else. Nothing is written to the ledger either: the slots are answered to the
     * caller, which claims them when it is about to hand the faces over. Answers
     * {@code null} where a face refused, having named it in the log; the caller reports the family. The cost of buffering is that one
     * family's programs are held twice while its facets are added, since the parts keep
     * their bytes as well.</p>
     *
     * @return the wrapped programs and the slots they would take, or {@code null} where a
     *         face refused or every face was held back
     */
    private static FamilyPrograms readPrograms(FontFamilyDefinition family,
                                               FontFamilyDefinition.FontSourceSet sources,
                                               Set<PptxFontFace> faces,
                                               PptxSlotLedger ledger) {
        String typeface = family.wordFamily();
        FontName familyName = family.name();
        Map<String, byte[]> programs = new LinkedHashMap<>();
        Map<String, Seated> bySlot = new LinkedHashMap<>();
        // The faces held back for their licence, by source description, so a source the
        // family repeats across faces is named once, as one it carries is embedded once.
        Set<String> heldBack = new LinkedHashSet<>();
        for (FontFamilyDefinition.FontBinarySource source
                : faces.stream().map(face -> face.in(sources)).toList()) {
            // Named before it is read, so a source that cannot even say what it is still
            // warns rather than failing the render inside the warning. Only the library's
            // own sources arrive here: FontBinarySource is a public interface, but
            // FontFamilyDefinition builds every source itself, from a classpath resource or
            // a Path, and offers no public way to hand it another. Neither of those
            // description() methods can throw in an ordinary run, so the sentinel is
            // defensive and no test reaches it.
            String description = "<unnamed>";
            try {
                description = source.description();
                if (programs.containsKey(description) || heldBack.contains(description)) {
                    continue;
                }
                byte[] program;
                try (InputStream stream = source.openStream()) {
                    program = stream.readAllBytes();
                }
                byte[] container = PptxEmbeddedFont.wrap(program);
                // A face whose program states a family of its own, said where the names are known.
                // A presentation can match an embedded face to the declared typeface by that
                // record rather than by the names the container carries, and what that costs was
                // measured in PowerPoint 16 and differs by case. It cost everything once: the
                // italic face of a family whose two faces state DIFFERENT names was replaced by a
                // substitute, its program used by nothing. Twice it cost nothing: a face in the
                // bold element stating another family, and an italic face of a family whose faces
                // all state the same other name, were each drawn from the carried program after
                // all, 0.16pt and 0.27pt off the line the page had laid out, both under a declared
                // name of no font on the measuring machine. So this says what the two names are
                // and names no outcome.
                //
                // What it must not be read as is an account of a deck drawn from a family the
                // viewing machine has under the declared name — there the carried face may not be
                // drawn whatever it states, which no record in the deck can change and this line
                // would wrongly take credit for. That limit is the capability details' to carry.
                //
                // Nothing is refused for it, and nothing here says the family travels: a later
                // face of it can still collide, be unreadable, or lose its viewer metrics, and
                // each of those says so on its own line. What this line is about is the naming.
                //
                // The names are compared ignoring case, which folds the same way wherever the
                // export runs. Whether a viewer matches case-insensitively was not measured, so
                // a pair differing only in case is left silent rather than warned about on a
                // guess.
                String stated = PptxEmbeddedFont.statedFamilyOf(program);
                if (stated != null && !stated.equalsIgnoreCase(typeface)) {
                    LOG.warn("render.pptx.font.name.mismatch family={} typeface={} face={} "
                             + "states={} — a presentation can match an embedded face on the "
                             + "family its program states for itself rather than on the one this "
                             + "deck declares. What that costs was measured in PowerPoint 16 and "
                             + "differs: the italic face of a family whose faces state different "
                             + "names was replaced by a substitute, while a face in the bold "
                             + "element stating another family, and an italic face of a family "
                             + "whose faces all state the same other name, were each drawn from "
                             + "the carried program after all. So this names the two families and "
                             + "no outcome",
                            familyName.name(), typeface, description, stated);
                }
                // Which slot the face is seated in is not the slot it was registered under:
                // a presentation reads it off the container, so a face put in one slot of
                // the family travels in whichever one its own weight and italic flag name.
                // Two faces naming the same slot cannot both be carried — there are four and
                // the second overwrites the first's part — so this answers for the family
                // the way every other refusal here does, rather than letting the deck keep
                // the last one under the first one's declaration.
                PptxEmbeddedFont.Style style = PptxEmbeddedFont.styleOf(container);
                // containsKey, so the branch reads as "this slot is taken" rather than as
                // something about the face in it. putIfAbsent would do the same here, since
                // what is stored is a record this method builds and so is never null; the
                // two part company only for a map whose value could be, which this one's was
                // before the seated face's own weight had to be kept.
                if (bySlot.containsKey(style.slot())) {
                    Seated seated = bySlot.get(style.slot());
                    throw new IOException("a presentation seats one face per style and reads "
                            + "the style off the face: \"" + seated.description()
                            + "\" (weight " + seated.weight() + ") and \"" + description
                            + "\" (weight " + style.weight() + ") both read back as "
                            + style.slot() + ", italic " + style.italic()
                            + ", so a deck carrying both would hold only the second, under "
                            + "the declaration the first made");
                }
                // The same slot, one step wider: this family's own faces are above, and a
                // family that shares its declared name is here. Checked while the
                // presentation is still untouched, as every refusal on this path is, and
                // against the ledger rather than the entry's own XML, which records a dataId
                // per element and nothing of the family that registered it or the source it
                // came from, and which a reader would have to pair with the written part's
                // own header to recover a weight from at all.
                Seated holder = ledger.holderOf(typeface, style.slot());
                if (holder != null && !holder.family().equals(familyName)) {
                    throw new IOException("a presentation seats one face per style, and the "
                            + "style belongs to the declared family rather than to a "
                            + "registration: this family declares \"" + typeface
                            + "\", which the deck holds as the one font-list entry \""
                            + ledger.asDeclared(typeface) + "\" — the lookup ignores case — "
                            + "and the family \"" + holder.family().name() + "\" already "
                            + "carries a face there: \"" + holder.description() + "\" (weight "
                            + holder.weight() + ") holds " + style.slot() + ", and \""
                            + description + "\" (weight " + style.weight() + ") reads back as "
                            + "that same one, italic " + style.italic() + ", so a deck "
                            + "carrying both would hold only this one, under the declaration "
                            + "the other made. This family is the one refused, the other "
                            + "having travelled already. What that does not fix is this "
                            + "family's own text: it carries the same declared name, so a "
                            + "viewer draws it from the program that travelled — a run names "
                            + "a family and no more, and one name holds one program per style");
                }
                bySlot.put(style.slot(), new Seated(familyName, description, style.weight()));
                programs.put(description, container);
            } catch (PptxEmbeddedFont.PreviewAndPrintOnlyException previewOnly) {
                // The one refusal that costs the face and not the family, as the DOCX backend
                // reads the same bits: nothing about the face is wrong for the deck except its
                // licence, so the family's other faces travel and only this one is named. It
                // is caught before it reaches a slot, so it claims none, and the faces read
                // after it are checked against the slots the carried faces take.
                LOG.warn("render.pptx.font.embed.skipped family={} source={} reason={}",
                        family.name().name(), description, previewOnly.getMessage());
                heldBack.add(description);
            } catch (IOException | RuntimeException exception) {
                // A source may fail unchecked: the classpath one reports a missing resource
                // with an IllegalArgumentException. Named here and the family degrades, as
                // for a missing file on disk — the render is not the caller's to lose.
                // toString rather than getMessage because an unchecked failure may carry none.
                LOG.warn("render.pptx.font.embed.skipped family={} source={} reason={}",
                        family.name().name(), description, exception.toString());
                return null;
            }
        }
        if (programs.isEmpty()) {
            // Every face offered was held back. Handing over nothing would still make the
            // presentation declare the family, through the XSLFFontInfo constructor, so this
            // answers as a refusal does and the deck is left untouched.
            return null;
        }
        return new FamilyPrograms(programs, bySlot, heldBack.isEmpty());
    }

    /**
     * Claims a family's style slots, about to hand its faces to the presentation.
     *
     * <p>Called from the hand-over's doorstep rather than from the read, which is why it is a
     * method of its own rather than the tail of one: a claim is what stops a later family writing
     * over a part, so it must stand for a part that is about to exist. Between the read and
     * here, the registered path also reads the family's viewer metrics and gives the family
     * up if that read refuses — and such a family has had nothing written for it, no entry
     * and no part, so claiming at the read would have let it hold slots it never filled.
     * The next family declaring that name would then be refused in favour of a face that is
     * not in the deck, lose its own program, and be told its text is drawn from one that
     * travelled when none did.</p>
     *
     * <p>Before the hand-over and not after, because a hand-over that fails part way through
     * has still written what it took.</p>
     */
    private static void claimSlotsAndReportSharing(PptxSlotLedger ledger,
                                                   FontFamilyDefinition family,
                                                   Map<String, Seated> bySlot) {
        String typeface = family.wordFamily();
        if (ledger.alreadyDeclaredByAnother(typeface, family.name())
                && ledger.reportSharingOnce(typeface)) {
            // Said even though nothing collided, and said once for the declared name rather
            // than per face or per family: the sharing itself is the hazard the slots cannot
            // show. The page measured each family with its own program, while the deck has
            // one name for both, and a run names a family and no more — so every style this
            // family does not itself hold under that name is drawn from whatever the other
            // one put there. Two names shared in one render are two different facts about
            // the deck, so the line is per name; a third family joining one of them adds
            // nothing a reader can act on. "Font family" rather than "registered family":
            // the other half of such a pair can be a bundled family, which joins a name a
            // caller declared when the face it draws with lands in a slot still free.
            LOG.warn("render.pptx.font.shared.family typeface=\"{}\" — more than one font "
                            + "family declares this viewer family, so the deck has one "
                            + "font-list entry and one program per style for all of them; "
                            + "text measured in one of those families is drawn from whichever "
                            + "one carries the style it uses, which for a face it does not "
                            + "carry itself is not its own font",
                    ledger.asDeclared(typeface));
        }
        ledger.claim(typeface, bySlot);
    }

    /**
     * A family's wrapped font programs and the style slots they would be seated in.
     *
     * <p>Neither map is copied on the way in, and neither needs to be: both are built by
     * {@link #readPrograms} and go out of its scope when it returns, so the only holder
     * afterwards is the caller — which reads {@code programs} once to hand the faces over
     * and copies {@code slots} into the ledger. Copying {@code slots} here would also drop
     * the order it was read in, which the ledger does not care about but which
     * {@code programs} is kept in deliberately.</p>
     *
     * @param programs the containers by source description, in the order they were read
     * @param slots    the face taking each style slot, for the ledger the caller claims in
     * @param whole    whether every face offered is among {@code programs}, which is false
     *                 where a face was held back for a preview-and-print licence
     */
    private record FamilyPrograms(Map<String, byte[]> programs, Map<String, Seated> slots,
                                  boolean whole) {
    }

    /**
     * Hands read programs to the presentation, returning whether it took them all.
     *
     * <p>From here the handling is the presentation's own, and exactly what it was before
     * the reads were pulled forward: a refusal while taking a face degrades the family,
     * and this does not undo what it took. What it took depends on where it refused.
     * {@code addFacet} reads the EOT header before it writes anything, so a refusal over
     * the container leaves only the declaration the {@code XSLFFontInfo} constructor makes
     * on every call — one entry, not a part, a dataId and the embedded-font flags. Those
     * come after, and a refusal among them is POI's own part store failing, which no font
     * can provoke.</p>
     *
     * <p>The refusal over the container is the one that mattered — it arrived
     * <em>unchecked</em>, straight past this catch, and cost the whole deck after the page
     * had laid out — and it is now spent before this: {@link PptxEmbeddedFont} measures the
     * header against what a presentation reads, where a refusal is the checked one
     * {@link #readPrograms} degrades a family on, on this path and on the bundled one,
     * which has no metrics pre-flight at all. What the limit is and what it does not
     * promise is written down once, beside the constant there.</p>
     *
     * <p>Widening this catch instead would have swallowed the part store's failures with
     * it and left a deck in exactly the half-written state above, which is why the guard
     * sits where the container is built rather than here.</p>
     *
     * <p>One thing the writer does not refuse at all: a second face seated in a style slot
     * a face already holds. It takes the {@code dataId} the first one wrote and writes over
     * that part, so nothing arrives here to catch and the deck comes out carrying the later
     * face under the earlier one's declaration. That is spent before the hand-over, in
     * {@link #readPrograms} — for the faces of one definition, which are read together
     * there, and for two definitions sharing a declared name, which the slot ledger carries
     * between them. What can still arrive here is two registrations of one family naming
     * different files: those are one family to the guard, deliberately (see
     * {@link #readPrograms}), so the later {@code addFacet} takes the first's {@code dataId}
     * and writes over that part — which is what the page measured with, the font library
     * keeping the last factory registered under a name.</p>
     *
     * @return whether the presentation took every program it was handed
     */
    private static boolean handOverPrograms(XMLSlideShow show,
                                            FontFamilyDefinition family,
                                            Map<String, byte[]> programs) {
        XSLFFontInfo embedded = new XSLFFontInfo(show, family.wordFamily());
        for (Map.Entry<String, byte[]> program : programs.entrySet()) {
            try {
                embedded.addFacet(new ByteArrayInputStream(program.getValue()));
            } catch (IOException exception) {
                LOG.warn("render.pptx.font.embed.skipped family={} source={} reason={}",
                        family.name().name(), program.getKey(), exception.toString());
                return false;
            }
        }
        return true;
    }
}
