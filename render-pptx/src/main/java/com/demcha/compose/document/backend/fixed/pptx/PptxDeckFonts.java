package com.demcha.compose.document.backend.fixed.pptx;

import com.demcha.compose.document.backend.fixed.pptx.handlers.PptxFontMapping;
import com.demcha.compose.font.DefaultFonts;
import com.demcha.compose.font.FontFamilyDefinition;
import com.demcha.compose.font.FontName;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * The fonts of one PPTX render: which registered families the deck carries, the viewer
 * family each run is written with, the faces the runs drew, and the bundled families
 * carried once the last run is drawn.
 *
 * <p>Writes the {@code render.pptx.font.substitution} lines, so that each family the deck
 * does not carry is reported once and by what the deck ends up carrying under its declared
 * name. The programs themselves are read and handed over by {@link PptxFontEmbedder}, which
 * writes the lines that say why a face did not travel ({@code render.pptx.font.embed.skipped})
 * and what it noticed on the way ({@code render.pptx.font.shared.family},
 * {@code render.pptx.font.name.mismatch}).</p>
 *
 * <p>Mutable and confined to one render pass, as {@link PptxRenderEnvironment} is.</p>
 */
final class PptxDeckFonts {

    private static final Logger LOG = LoggerFactory.getLogger("com.demcha.compose.engine.render");

    private final XMLSlideShow show;
    private final Map<FontName, String> customFontFamilies;
    /**
     * Which style elements a registered family's own programs fill, by the name it was registered
     * under — the answer to "can this family name a face itself?", which is what decides whether
     * its <em>name</em> may contribute a run flag (see {@link #tokensTheNameMaySpeak}).
     *
     * <p>Written where the faces are handed to the presentation, so an entry stands for a
     * registered family the presentation took whole, or took short only of a face held back for
     * its licence, whose entry then holds the elements it does fill. Everything else is absent, and absent means
     * "the name is all a run has": a family registered without sources, one refused or given up,
     * one whose hand-over failed part way — which leaves what it took, so such a family does carry
     * a face the name then speaks over — and every bundled family, which travels by another road
     * and never carries a style token in its name to begin with. The slot a program lands in is
     * read off the program
     * itself ({@code PptxEmbeddedFont.styleOf}) and not off the slot a caller put it in, because
     * those need not agree — the file a caller passes as the family's regular source may be a bold
     * program, which is exactly the registration this permission exists to read.</p>
     */
    private final Map<FontName, Set<String>> carriedSlots;
    private final Map<FontName, PptxViewerMetrics.ViewerFontMetrics> viewerFontMetrics;
    private final Set<String> substitutionWarned = new LinkedHashSet<>();
    /**
     * Every face a run asked for: family to the bold/italic combinations drawn with it.
     *
     * <p>Kept per face rather than per family because embedding is whole-font. A deck
     * showing six Korean characters in one weight was carrying Gothic A1 twice — 4.4 MB
     * for a facet that is never on a slide.</p>
     */
    private final Map<FontName, Set<PptxFontFace>> usedFaces = new LinkedHashMap<>();
    /** Bundled families already noted as carried, so the note is one line per family. */
    private final Set<String> bundledCarryNoted = new LinkedHashSet<>();
    /** Which face holds each style slot of each declared family name, for this render. */
    private final PptxSlotLedger slotLedger = new PptxSlotLedger();
    /** Bundled families that carry a binary, i.e. the ones the embedder can take. */
    private static final Map<FontName, FontFamilyDefinition> EMBEDDABLE_BUNDLED =
            DefaultFonts.bundledFamilies().stream()
                    .filter(family -> family.fontSourceSet().isPresent())
                    .collect(java.util.stream.Collectors.toMap(
                            FontFamilyDefinition::name, family -> family, (a, b) -> a,
                            LinkedHashMap::new));
    /** Whether this render will offer the families it used to the embedder. */
    private boolean embedsBundledFonts = true;

    /**
     * Carries the registered families into the presentation, before a slide is drawn.
     *
     * @param show               the presentation the families are carried into
     * @param customFontFamilies the families the document registered, in order
     */
    PptxDeckFonts(XMLSlideShow show, Collection<FontFamilyDefinition> customFontFamilies) {
        this.show = show;
        Map<FontName, String> familyNames = new LinkedHashMap<>();
        Map<FontName, PptxViewerMetrics.ViewerFontMetrics> metrics = new LinkedHashMap<>();
        Map<FontName, Set<String>> slotsCarried = new LinkedHashMap<>();
        // The families the deck will not carry, keyed by name so that each is reported once
        // however many times it was registered, and by the last registration of that name —
        // the one whose declared family the runs are written with (familyNames keeps the last)
        // and whose program the page was measured in (the font library keeps the last factory).
        Map<FontName, FontFamilyDefinition> notCarried = new LinkedHashMap<>();
        // The names some registration was carried, or handed to the presentation, under: a later
        // registration of the same name that is not carried must not be told none of it travels.
        Set<FontName> anyCarried = new LinkedHashSet<>();
        for (FontFamilyDefinition family : customFontFamilies) {
            familyNames.put(family.name(), family.wordFamily());
            PptxFontEmbedder.Embedding embedding = family.fontSourceSet()
                    .map(sources -> PptxFontEmbedder.embedFontFamilyWithMetrics(
                            show, family, sources, slotLedger, slotsCarried))
                    .orElse(null);
            PptxFontEmbedder.Carried carried =
                    embedding == null ? PptxFontEmbedder.Carried.NONE : embedding.carried();
            if (carried != PptxFontEmbedder.Carried.NONE) {
                anyCarried.add(family.name());
            }
            if (embedding != null && embedding.metrics() != null) {
                // A family carried whole, or short only of a face held back for its licence:
                // either way every face it carries is in the deck as it was handed over. The
                // held-back style keeps that face's own ascent (PptxFontEmbedder says why).
                metrics.put(family.name(), embedding.metrics());
            }
            if (carried == PptxFontEmbedder.Carried.WHOLE) {
                notCarried.remove(family.name());
            } else {
                notCarried.put(family.name(), family);
            }
        }
        // Reported after the last registration and not as each one is read, because which line
        // is true depends on what the deck ends up carrying under the family's declared name,
        // and a family registered later can be the one that carries it.
        notCarried.values().forEach(family -> warnRegisteredFamilyNotCarried(
                family, anyCarried.contains(family.name()), slotLedger));
        if (!anyCarried.isEmpty()) {
            // Cleared, never set: POI's blank template already carries this flag set, and
            // addFacet sets it again for every face it takes, so a standing instruction to
            // subset on save is what a deck says by default. Nothing here subsets a face —
            // a program travels with every glyph it has — so a deck that carries one says
            // the opposite. The Word counterpart of that instruction was measured cutting
            // an embedded face down on a save (docs/recipes/docx-export.md), which is why
            // a document never writes it; PowerPoint's own handling of the pair has not
            // been measured. A deck that carries nothing leaves the template's value alone,
            // where embedding is off and it decides nothing; one carried short of a face, held
            // back or refused by the presentation, may have had the flag set on the way, and is
            // cleared too.
            show.getCTPresentation().setSaveSubsetFonts(false);
        }
        this.customFontFamilies = Map.copyOf(familyNames);
        this.carriedSlots = Map.copyOf(slotsCarried);
        this.viewerFontMetrics = Map.copyOf(metrics);
    }

    /**
     * The viewer family a run in that font is written with, recording the face it drew.
     * See {@link PptxRenderEnvironment#fontFamily(FontName, boolean, boolean)}.
     */
    String fontFamily(FontName fontName, boolean bold, boolean italic) {
        if (fontName != null) {
            usedFaces.computeIfAbsent(fontName, name -> new LinkedHashSet<>())
                    .add(PptxFontFace.of(bold, italic));
            String customFamily = customFontFamilies.get(fontName);
            if (customFamily != null) {
                return customFamily;
            }
        }
        // Dedupe on the resolved viewer family: a standard-14 alias and its base name share one
        // line, while a name no registration covers is counted as written. Facet-suffixed
        // spellings of such a name used to share a line too, when this read took the suffix off
        // them — a grouping no render could reach, since such a name fails the layout first.
        // The message still names the source font the document asked for.
        String viewerFamily = PptxFontMapping.familyFor(fontName);
        String sourceName = (fontName == null ? FontName.HELVETICA : fontName).name();
        String replacement = PptxFontMapping.standardReplacementFor(fontName);
        if (replacement != null) {
            if (substitutionWarned.add(viewerFamily)) {
                LOG.warn("render.pptx.font.substitution family={} — a PDF built-in with no "
                                + "PPTX counterpart, rendered as metric-compatible \"{}\" "
                                + "(identical widths, slightly different letterforms); register "
                                + "a binary font family for identical glyphs in both formats",
                        sourceName, replacement);
            }
        } else if (embedsBundledFonts && EMBEDDABLE_BUNDLED.containsKey(fontName)) {
            // A bundled family this render carries itself. Warning here would tell the
            // reader the opposite of what the deck ends up containing; if the embed
            // fails, embedBundledFontsUsed says so at the point it knows. Noted once per
            // family, as the warning below is — an Arabic article draws sixteen hundred
            // runs in one family, and sixteen hundred identical lines say nothing more
            // than the first. Its own set, so a later decline still warns.
            if (bundledCarryNoted.add(viewerFamily)) {
                LOG.debug("render.pptx.font.bundled family={} — carried by this deck",
                        sourceName);
            }
        } else if (substitutionWarned.add(viewerFamily)) {
            if (slotLedger.alreadyDeclaredByAnother(viewerFamily, fontName)) {
                // Nothing carries this family, but a registered one carries a face under the
                // very name its runs are written with — a bundled family this render declined,
                // drawn beside a registration that declares its name — so its text is drawn
                // from that program and "by name only" would be untrue. The ledger already
                // answers for every carrier while runs are drawn: registered families are
                // carried before the first slide, and the only ones carried later are bundled
                // — none where the render declined them, and otherwise each under its own
                // name, which a run reaching this branch cannot have, since a run in a bundled
                // family the render carries takes the branch above.
                warnDrawnFromAnotherFamilysProgram(fontName, viewerFamily);
            } else {
                // Nothing will carry it: either it was never registered, or this render was
                // asked not to carry bundled families. Either way the deck can only name it,
                // so a viewer without it installed substitutes.
                LOG.warn("render.pptx.font.substitution family={} — not registered with binary "
                                + "sources for this render, the deck references \"{}\" by name "
                                + "only; register the family (registerFontFamily) to embed it "
                                + "and guarantee identical glyphs on every machine",
                        sourceName, viewerFamily);
            }
        }
        return viewerFamily;
    }

    /**
     * Whether a family's registered name may say anything about a run's face, given the faces the
     * family itself carries. One rule: <b>the name speaks only where it names exactly the one face
     * the family carries, or where the family carries none.</b>
     *
     * <ul>
     *   <li><b>two or more elements carried</b> — nothing. Such a family distinguishes its faces by
     *       the element a run asks for, and the span's decoration is what says which. Measured in
     *       PowerPoint 16 on a 45-character line at 20pt: a single definition under
     *       {@code Acme-Sans-Bold} holding a regular file and a bold one used to flag every run of
     *       itself, and its plain paragraph came out of the bold program 12.90pt wider than the
     *       frame the engine had sized for it. With the flag off that run, the same deck drew
     *       531.94pt against the 531.90pt the page laid out, from the regular program, and the bold
     *       paragraph did not move.</li>
     *   <li><b>one element carried</b> — every token, but only if the name names <em>that</em>
     *       element. This is the registration a caller makes per face, where nothing but the name
     *       can say which face its runs want. A name ending {@code -Bold} over a single bold
     *       program speaks; the same name over a single <em>regular</em> program does not, which is
     *       the other half of the same gap (that deck's line came out 9.17pt short of its frame);
     *       and it does not speak over a single bold-<em>italic</em> program either, because a run
     *       flagged bold alone would ask for an element that deck does not hold.</li>
     *   <li><b>nothing carried</b> — every token. A family registered without sources has no faces
     *       to distinguish, so its name is all a run has. This is also the answer for a bundled or
     *       standard-14 name, which no registration covers, and for a family the presentation did
     *       not take: an absent entry and an empty one say the same thing here.</li>
     * </ul>
     *
     * @param fontName the family a run names, may be {@code null}
     * @return the style tokens {@link PptxFontMapping} may read out of the name
     */
    Set<String> tokensTheNameMaySpeak(FontName fontName) {
        Set<String> carried = fontName == null ? null : carriedSlots.get(fontName);
        if (carried == null || carried.isEmpty()) {
            return PptxFontMapping.ANY_TOKEN;
        }
        if (carried.size() > 1) {
            return Set.of();
        }
        return PptxFontMapping.namesExactly(fontName, carried.iterator().next())
                ? PptxFontMapping.ANY_TOKEN
                : Set.of();
    }

    /** The viewer metrics a registered family that travelled was read with, or {@code null}. */
    PptxViewerMetrics.ViewerFontMetrics viewerMetrics(FontName fontName) {
        return viewerFontMetrics.get(fontName);
    }

    /**
     * Reports a registered family the deck does not carry, once, with the line that is true of
     * what the deck carries under the family's declared name.
     *
     * <p>Which line is true is decided by the deck rather than by why this family missed: where
     * another family carries a face under the same declared name, the text is drawn from that
     * program, whether this family was registered without sources, could not be read, or was
     * refused for the slot — and in whichever order the two were registered.</p>
     *
     * <p>Three arrangements are not seen, and keep the line the cause alone would give. "Another
     * family" is another {@link FontName}, because the ledger does not count slots the same
     * name holds: a name registered first with sources that travelled, and then again without
     * any, is told no program for it travels while the earlier registration's program is in the
     * deck — text drawn from a program the page was not measured in, which no line here
     * describes. Registered again with sources that are not carried, it is told the family is
     * not carried whole, which is true of that deck and still does not describe it. The same
     * holds for a name registered again with sources one face of which is held back for its
     * licence: the earlier registration's program for that style stays in the deck under the
     * name, where the later one's would have written over it, so that style's text is drawn from
     * a program the page was not measured in while the line says the font is needed. And
     * "another family" is another <em>registered</em> family, because this runs while the
     * environment is built: a bundled family carried under the same declared name is handed over after the last slide, so a
     * registration declaring a bundled family's name that is not carried is told it needs its
     * font installed while the bundled program travels under that name.</p>
     *
     * <p>Where the family's own sources could not be carried, the line says how much of it is in
     * the deck: none of it, which is what every refusal that costs a family leaves, or not all of
     * it, where a face was held back for a preview-and-print licence, the presentation refused a
     * face it had been handed, or an earlier registration of the name did travel.</p>
     *
     * <p>A family that holds a slot of its own under a name another family also holds is told it
     * is not carried whole, not that no program of it travels. That is a family held short of a
     * face for its licence, whose free slot another family declaring the same name then filled:
     * its other faces are in the deck, and the text in the style it lacks is drawn from the
     * other family's program, which {@code render.pptx.font.shared.family} has already said of
     * the name.</p>
     *
     * @param family     the last registration of a family name the deck does not carry
     * @param anyCarried whether some registration of that name travelled, or was handed to the
     *                   presentation before it refused a face
     * @param ledger     the render's slot ledger, complete for the registered families
     */
    private static void warnRegisteredFamilyNotCarried(FontFamilyDefinition family,
                                                       boolean anyCarried,
                                                       PptxSlotLedger ledger) {
        // A family that holds a slot of its own under the name is told it is not carried whole
        // below, not that nothing of it travels, however much of the name another family holds:
        // a family held short of a face for its licence leaves that slot free for another one.
        if (ledger.alreadyDeclaredByAnother(family.wordFamily(), family.name())
                && !ledger.carriesAFaceOf(family.wordFamily(), family.name())) {
            warnDrawnFromAnotherFamilysProgram(family.name(), family.wordFamily());
        } else if (family.fontSourceSet().isEmpty()) {
            // Said of the text, not of the deck: this runs before a single run is drawn,
            // so whether anything will use the family is not known here, and a family
            // registered this way and never drawn in is named nowhere in the package.
            // "No program" rather than "nothing": where text is drawn, the family's name
            // does travel, on the slide that drew it. The substitution is part of the same
            // clause and not a second sentence, so that it too says nothing about a deck
            // that draws no text in the family. "No program for" the declared name holds
            // because the branch above takes every family whose name another one carries,
            // short of the first arrangement the javadoc names. The second never reaches here:
            // a family without sources declares only its own name, and registering a bundled
            // family's name keeps the bundled pass from carrying that family.
            LOG.warn("render.pptx.font.substitution family={} — registered without binary "
                            + "sources, so no program for \"{}\" travels with the deck; any "
                            + "text drawn with it is referenced by name only and "
                            + "substituted by viewers that do not have the font installed",
                    family.name().name(), family.wordFamily());
        } else if (!anyCarried) {
            // Same key as every other substitution so one log filter catches them all; the
            // embed-skip warning carries the cause. Every refusal that costs a family arrives
            // while the presentation is still untouched (PptxFontEmbedder.Carried), so nothing
            // of the family is in the deck, and this says so rather than hedging. It says what
            // the text needs rather than what a viewer will do with it, since with no program
            // of the family carried, what a viewer draws its text from is the viewer's to
            // decide. "Any text drawn with it", because this runs before a run is drawn.
            LOG.warn("render.pptx.font.substitution family={} — could not be embedded, so no "
                            + "face of it travels with the deck; any text drawn with it is "
                            + "referenced as \"{}\" by name, and needs that font installed where "
                            + "the deck is opened for identical glyphs",
                    family.name().name(), family.wordFamily());
        } else {
            // Something of the name is in the deck: a face was held back for a preview-and-print
            // licence and the family's other faces travelled, the presentation refused a face it
            // had been handed and keeps what it took before that — the declaration at least — or
            // an earlier registration of the name travelled and this last one did not. "Not carried
            // whole" because what the reader has to act on is that some face did not travel, and
            // this line says what the text drawn in that face needs rather than what a viewer will do with it: where a face
            // of the family is in the deck, what a viewer draws a run of a missing face from — a
            // synthetic face, or another family — is the viewer's to decide. It was watched once:
            // PowerPoint 16 drew a bold-italic run whose face was held back from the carried
            // italic face, emboldened, which is one viewer and one family.
            LOG.warn("render.pptx.font.substitution family={} — not every face of it could "
                            + "be embedded, so \"{}\" is not carried whole; text drawn in a face "
                            + "that did not travel is referenced by name, and needs "
                            + "that font installed where the deck is opened for "
                            + "identical glyphs",
                    family.name().name(), family.wordFamily());
        }
    }

    /**
     * Reports a family the deck does not carry, under whose declared name another family
     * carries a face.
     *
     * <p>Its own line rather than the substitution warning the other misses share, because
     * that one tells the reader to install the font and here that would be the opposite of
     * what a viewer does: a program <em>is</em> in the deck under the very name this
     * family's text carries, and the text is drawn from it. The only thing the reader can
     * act on is the declaration — two families asking for one viewer name is a choice they
     * made and can unmake, and nothing short of unmaking it lets both programs travel.</p>
     *
     * <p>Said for a registered family, a bundled one and a name only a run carries alike, and
     * worded for all three: where the family came from changes nothing about what its text is
     * drawn from, and the family is named either way. The carrier is always a registration —
     * the registered pass runs before a slide is drawn and no two bundled families declare one
     * name — which is what the line says of it.</p>
     *
     * @param family   the family the document asked for, as it was named
     * @param declared the viewer family its text is written with
     */
    private static void warnDrawnFromAnotherFamilysProgram(FontName family, String declared) {
        LOG.warn("render.pptx.font.substitution family={} — another registered family already "
                        + "carries a face under the viewer family \"{}\" this one declares, so "
                        + "no program of this family travels under that name; its text is "
                        + "referenced by that shared name and a viewer draws it from the "
                        + "program that did travel, not from this family's own font. Declare "
                        + "the two under different viewer families for each to carry its own",
                family.name(), declared);
    }

    /**
     * Tells this render whether the families it uses will be carried.
     *
     * <p>Set before anything is drawn, because the answer changes what a run is warned
     * about. A family the deck is about to carry must not be reported as one the reader
     * has to install. A render explicitly asked not to carry them gets the name-only
     * warning for each bundled family it draws in that ships a binary and is not
     * registered, and that warning does tell it to register the family: registering is
     * how one family travels while the rest stay declined, since a family registered
     * through {@code registerFontFamily} with binary sources goes to the embedder
     * whatever this flag says.</p>
     *
     * @param enabled whether {@link #embedBundledFontsUsed()} will run for this render
     */
    void embedsBundledFonts(boolean enabled) {
        this.embedsBundledFonts = enabled;
    }

    /**
     * Embeds the bundled families this deck drew with, on the terms a registered one gets.
     *
     * <p>A family a caller registers themselves is embedded, and warned about when it
     * cannot be. A family this library <em>ships</em> — Amiri for Arabic, David Libre for
     * Hebrew, the Noto faces for Georgian and Armenian, Gothic A1 for Hangul — was neither:
     * the deck named it and embedded nothing, so a viewer without it installed substituted,
     * and for a script the substitute does not cover the slide showed boxes. The asymmetry
     * was the sharp edge, since the shipped families are the ones a deck reaches for
     * precisely when the reader is least likely to have the font.</p>
     *
     * <p>Only what was drawn is embedded, down to the face — the bundled set is dozens of
     * families, and embedding carries a font program whole with no subsetting, so a deck
     * offering all of them would be tens of megabytes and one offering a face it never
     * drew would pay megabytes for glyphs no slide contains.</p>
     *
     * <p>Metrics are deliberately untouched. A registered family also contributes viewer
     * metrics, which participate in layout; taking those from a bundled family now would
     * move text in every deck that already renders correctly. This changes what the file
     * carries, not where anything sits.</p>
     *
     * <p>Call after the last run is drawn — the recorded face set is only complete then.</p>
     */
    void embedBundledFontsUsed() {
        boolean embeddedAny = false;
        for (FontFamilyDefinition family : EMBEDDABLE_BUNDLED.values()) {
            // EMBEDDABLE_BUNDLED already excludes the standard-14 definitions: they carry
            // no binary to embed, and PPTX maps them to viewer-facing metric-compatible
            // replacements (Helvetica to Arial, and so on) rather than assuming the
            // viewer has the PDF built-in.
            Set<PptxFontFace> faces = usedFaces.get(family.name());
            if (faces == null || customFontFamilies.containsKey(family.name())) {
                // Not drawn with, or the caller registered their own under this name — the
                // deck then references that family, so the bundled binary would be unused.
                continue;
            }
            Optional<FontFamilyDefinition.FontSourceSet> sources = family.fontSourceSet();
            PptxFontEmbedder.Carried carried = PptxFontEmbedder.embedFontFamily(
                    show, family, sources.orElseThrow(), faces, slotLedger);
            if (carried != PptxFontEmbedder.Carried.NONE) {
                // A family carried short of a face, held back or refused by the presentation,
                // may have had the subsetting flag set on the way, as a whole one has, so it
                // counts here too.
                embeddedAny = true;
            }
            if (carried == PptxFontEmbedder.Carried.WHOLE) {
                continue;
            }
            if (slotLedger.alreadyDeclaredByAnother(family.wordFamily(), family.name())
                    && !slotLedger.carriesAFaceOf(family.wordFamily(), family.name())) {
                // The reachable half of this: a caller registers their own family under a
                // bundled one's viewer name. The bundled pass runs after the last slide, so
                // the caller's family holds the slot by now and this one is the refused one
                // — which is the way round the decision wants it, an explicit registration
                // keeping its own font. Asked of the ledger rather than of the refusal, as
                // for a registered family, so the line follows what the deck carries under
                // the name and not which of the reads turned the family down.
                warnDrawnFromAnotherFamilysProgram(family.name(), family.wordFamily());
            } else if (carried == PptxFontEmbedder.Carried.NONE) {
                // Reached on the same answers as the registered-family lines above, and worded
                // the same way. The family was certainly drawn in — the loop skips one that was
                // not — so unlike the registered line this one does know the deck sets text in
                // the family: "its text" rather than "any text drawn with it".
                LOG.warn("render.pptx.font.substitution family={} — a bundled family that "
                                + "could not be embedded, so no face of it travels with the "
                                + "deck; its text is referenced as \"{}\" by name, and needs "
                                + "that font installed where the deck is opened for identical "
                                + "glyphs",
                        family.name().name(), family.wordFamily());
            } else {
                // A face drawn with was held back for a preview-and-print licence, or the
                // presentation refused a face it had been handed and keeps what it took before
                // that, so the line says what is not carried and what the text drawn in the face
                // that did not travel needs.
                LOG.warn("render.pptx.font.substitution family={} — a bundled family not every "
                                + "face of which could be embedded, so \"{}\" is not carried whole; text "
                                + "drawn in a face that did not travel is referenced by name, "
                                + "and needs that font installed where the deck is opened for "
                                + "identical glyphs",
                        family.name().name(), family.wordFamily());
            }
        }
        if (embeddedAny) {
            // The bundled half of the same clear the constructor makes for a registered
            // family — on its own for a deck that registered none, and needed again after
            // one that did, because addFacet set the flag back while taking these faces.
            show.getCTPresentation().setSaveSubsetFonts(false);
        }
    }
}
