package com.demcha.compose.document.backend.fixed.pptx;

import com.demcha.compose.font.FontFamilyDefinition;
import com.demcha.compose.font.FontName;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Which face holds each style slot of each declared family name, for this render.
 *
 * <p>A style slot's capacity belongs to the <em>declared</em> family name rather than to
 * a definition: {@code XSLFFontInfo} looks an entry up by typeface, ignoring case, so two
 * definitions whose {@link FontFamilyDefinition#wordFamily()} matches that way write into
 * one entry and one set of four slots. Keyed the way that lookup is, and shared across
 * the whole render rather than built per definition, because the two halves of it are far
 * apart: a registered family is carried while the environment is being constructed, and a
 * bundled one after the last slide is drawn.</p>
 *
 * <p>Nothing here is claimed until the caller is about to hand a family's faces over, so
 * a family whose read or whose viewer metrics refuse claims nothing: a claim is what
 * stops a later family writing over a part, and it has to stand for a part that is about
 * to exist. It is made before the hand-over rather than after, because a hand-over that
 * fails part way through has still written what it took. What that leaves is a family the
 * presentation refuses the <em>first</em> of its faces, which holds claimed slots with
 * only a declaration written — and the one refusal still possible there is POI's part
 * store, which no font can provoke (see {@code PptxFontEmbedder.handOverPrograms}).</p>
 */
final class PptxSlotLedger {

    /**
     * A face already seated in a style slot, as a refusal over that slot has to name it.
     *
     * <p>Its own weight, not the colliding face's: the two differ — a bold slot holding a
     * 300-weight face collides with a 400-weight regular — and the weight is what a caller
     * can go and look for in their font, so naming one of the two for both would send them
     * after the wrong face. The italic flag is not here because the two always agree on it:
     * the slot is read from the weight's side of 400 and that flag, so faces that share a
     * slot share the flag.</p>
     *
     * <p>Not a {@link PptxFontFace}, which is the slot a family <em>registers</em> a face
     * under. The slot a refusal is about is the one the container reads back as, and the whole
     * of what is guarded is that the two need not agree.</p>
     *
     * @param family      the family the face was registered under, for a refusal that
     *                    crosses two of them. A {@link FontName}, so that two registrations
     *                    the rest of the engine treats as one family — the name being
     *                    case-insensitive — are one family here too
     * @param description the source the face was read from, as it names itself
     * @param weight      that face's own weight, as the presentation reads it
     */
    record Seated(FontName family, String description, int weight) {
    }

    /**
     * One declared family name, as the first family to declare it spelled it, the face
     * holding each of its four style slots, and whether the sharing of it has been
     * reported.
     *
     * <p>Held in a list walked with {@link String#equalsIgnoreCase}, not in a map on a
     * case-folded key: that operator is the one {@code XSLFFontInfo} matches an entry
     * with, and a key derived here instead could disagree with it for a name where
     * folding and {@code equalsIgnoreCase} part company — which would be a collision the
     * deck makes and this does not see. A render declares a handful of families, so the
     * walk is the cheaper half of that trade as well.</p>
     */
    private static final class Declared {
        private final String typeface;
        private final Map<String, Seated> bySlot = new LinkedHashMap<>();
        private boolean sharingReported;

        private Declared(String typeface) {
            this.typeface = typeface;
        }
    }

    private final List<Declared> declared = new ArrayList<>();

    /** The face holding that slot of that declared name, or {@code null}. */
    Seated holderOf(String typeface, String slot) {
        return entry(typeface).map(found -> found.bySlot.get(slot)).orElse(null);
    }

    /** Records that these faces hold these slots of that declared name. */
    void claim(String typeface, Map<String, Seated> bySlot) {
        entry(typeface).orElseGet(() -> {
            Declared fresh = new Declared(typeface);
            declared.add(fresh);
            return fresh;
        }).bySlot.putAll(bySlot);
    }

    /**
     * That name as the deck declares it: the spelling of the family that declared it
     * first, which is the one {@code XSLFFontInfo} writes into the entry and keeps.
     *
     * <p>What a line about the <em>entry</em> has to name, since a later family's own
     * spelling appears nowhere in the package and would send a reader looking for a
     * declaration the deck does not contain.</p>
     */
    String asDeclared(String typeface) {
        return entry(typeface).map(found -> found.typeface).orElse(typeface);
    }

    /** Whether that declared name already carries a face of another family. */
    boolean alreadyDeclaredByAnother(String typeface, FontName family) {
        return entry(typeface).stream()
                .flatMap(found -> found.bySlot.values().stream())
                .anyMatch(seated -> !seated.family().equals(family));
    }

    /**
     * Whether that declared name carries a face of this family: a family that holds a slot of
     * its own travels, however much of the name another family also holds.
     */
    boolean carriesAFaceOf(String typeface, FontName family) {
        return entry(typeface).stream()
                .flatMap(found -> found.bySlot.values().stream())
                .anyMatch(seated -> seated.family().equals(family));
    }

    /**
     * Whether the sharing of that declared name is still to be reported, marking it
     * reported if so — once for the name, however many families join it.
     *
     * <p>Only ever asked of a name an entry already exists for, the caller having found
     * another family in it first, so the flag can live on the entry.</p>
     */
    boolean reportSharingOnce(String typeface) {
        Optional<Declared> owed = entry(typeface).filter(found -> !found.sharingReported);
        owed.ifPresent(found -> found.sharingReported = true);
        return owed.isPresent();
    }

    private Optional<Declared> entry(String typeface) {
        return declared.stream()
                .filter(found -> typeface.equalsIgnoreCase(found.typeface))
                .findFirst();
    }
}
