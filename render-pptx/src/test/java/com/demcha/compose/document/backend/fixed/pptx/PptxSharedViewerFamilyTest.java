package com.demcha.compose.document.backend.fixed.pptx;

import static org.assertj.core.api.Assertions.assertThat;

import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.font.DefaultFonts;
import com.demcha.compose.font.FontFamilyDefinition;
import com.demcha.compose.font.FontName;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * A style slot belongs to the declared viewer family, not to the registration.
 *
 * <p>{@code XSLFFontInfo} looks a font-list entry up by typeface, ignoring case, so two
 * definitions whose {@link FontFamilyDefinition#wordFamily()} matches that way write into
 * one entry — one declaration and one program per style for both of them. The guard
 * {@link PptxFontStyleSlotTest} covers answers for one definition's faces only, so each of
 * two such definitions passed its own four slots and the later one still wrote over the
 * earlier one's part.</p>
 *
 * <p>Measured before the fix, on both public paths: two definitions under one
 * {@code wordFamily}, each with a single regular face, left one entry declaring
 * {@code <p:regular>} and one part holding the <em>second</em> face's bytes, with nothing
 * in the log. PowerPoint 16.0 reported the family embedded and drew both families' text
 * from that one program — in an exported PDF there was a single subset of 1,060 glyphs,
 * the second face's glyph count, where a deck declaring the two under different names
 * exported two subsets of 3,023 and 1,060. The reachable form of it needs only one
 * registration: a caller declaring their own family under a <em>bundled</em> family's
 * viewer name collides with the bundled program, because the bundled pass skips only a
 * family whose {@code FontName} the caller registered, not one that merely shares a
 * declared name.</p>
 *
 * <p>The guard's boundary is what {@code FontName} counts as one family, which is
 * case-insensitive: two registrations of one name are one family here, and the later
 * program travels rather than being refused. That is not a softer form of the defect but
 * the opposite of it — the font library keeps the <em>last</em> factory registered under a
 * name, so the later program is the one the page measured with, and refusing it would
 * leave the deck carrying a face no text was laid out in.</p>
 *
 * <p>The family whose face arrives into a slot another family already holds is refused
 * whole, as a family with an unreadable face is — a face of it bound for a free slot does
 * not travel either; the family holding the slot keeps everything. That order is a decision
 * rather than a property of the format, and in the reachable form it leaves the explicit
 * registration carrying its own font, the bundled pass running after the last slide. Its
 * limit is that a registered family is carried before anything is drawn, so one declared
 * under a bundled family's name and never drawn in still takes the slot and the bundled
 * family's own text is drawn from it — pinned here rather than argued.</p>
 *
 * <p>What refusing cannot fix, and what the log therefore says: the refused family's text
 * carries the shared declared name too, so a viewer draws it from the program that did
 * travel. A run names a family and no more, so no deck holds two programs for one name and
 * one style. What the refusal buys is the <em>other</em> family's text, which was being
 * drawn from the wrong program before.</p>
 *
 * <p>Two definitions can also share a name without colliding — one holding the regular
 * slot and the other the bold one — and that keeps working: two parts, both declared under
 * the one entry. It gets a warning rather than a refusal, because sharing the name is a
 * hazard the slots cannot show: the page measured each family with its own program while
 * the deck has one name for both, so every style a family does not itself hold under that
 * name is drawn from whatever the other put there.</p>
 */
class PptxSharedViewerFamilyTest {

    private static final String LATO = "fonts/google/lato/Lato-Regular.ttf";
    private static final String LATO_BOLD = "fonts/google/lato/Lato-Bold.ttf";
    private static final String LATO_ITALIC = "fonts/google/lato/Lato-Italic.ttf";
    /** A second family's regular cut: a real face that collides with Lato's. */
    private static final String POPPINS = "fonts/google/poppins/Poppins-Regular.ttf";

    @TempDir
    Path directory;

    private RenderLogCapture log;

    @BeforeEach
    void captureRenderLog() {
        log = RenderLogCapture.attach();
    }

    @AfterEach
    void releaseRenderLog() {
        log.close();
    }

    @Test
    void theFaceArrivingIntoATakenSlotIsRefusedAndTheHolderKeepsItsProgram() throws Exception {
        Path latoFile = Files.write(directory.resolve("Lato-Regular.ttf"),
                PptxDeck.face(LATO));
        Path poppinsFile = Files.write(directory.resolve("Poppins-Regular.ttf"),
                PptxDeck.face(POPPINS));

        // The premise, read through the backend's own reader rather than assumed: two real
        // regular cuts, each of its own family, agree on the element. Without it the test
        // would pass on a pair that never shared a slot and would be guarding nothing.
        assertThat(PptxDeck.slotOf(PptxDeck.face(LATO))).isEqualTo("regular");
        assertThat(PptxDeck.slotOf(PptxDeck.face(POPPINS))).isEqualTo("regular");

        byte[] deck = deckOf(
                FontFamilyDefinition.files("First", latoFile).wordFamily("Brand").build(),
                FontFamilyDefinition.files("Second", poppinsFile).wordFamily("Brand").build());

        assertThat(deck).as("the deck is written").isNotEmpty();
        assertThat(PptxDeck.part(deck, "ppt/slides/slide1.xml"))
                .as("the premise: the page laid out in both families, and the slide carries "
                        + "the text of each")
                .contains("Alpha in the first family", "Beta in the second family");
        assertThat(PptxDeck.fontParts(deck))
                .as("one part, the holder's, where it used to be one part holding the "
                        + "arriving face's bytes")
                .hasSize(1);
        assertThat(carriedBy(deck, "ppt/fonts/font1.fntdata"))
                .as("and the bytes in it are the holder's, not the arriving family's")
                .isEqualTo(LATO);
        assertThat(PptxDeck.part(deck, "ppt/presentation.xml"))
                .as("declared once, under the one shared name")
                .contains("<p:font typeface=\"Brand\"/><p:regular")
                .doesNotContain("<p:bold");

        assertThat(log.messages("render.pptx.font.embed.skipped"))
                .as("both faces are named, each with its OWN weight and the family it was "
                        + "registered under, since the holder is in another registration and "
                        + "a caller has to be told which of the two to go and look at")
                .singleElement(InstanceOfAssertFactories.STRING)
                .contains("family=Second")
                .contains("declares \"Brand\"")
                .contains("the one font-list entry \"Brand\"")
                .contains("the family \"First\" already carries a face there")
                .contains("\"" + latoFile + "\" (weight 400) holds regular")
                .contains("\"" + poppinsFile + "\" (weight 400) reads back as that same one")
                .contains("italic false")
                .contains("This family is the one refused");
        assertThat(log.messages("render.pptx.font.substitution"))
                .as("and the refused family's own line says the text is drawn from the "
                        + "program that travelled — not, as every other refusal says, that "
                        + "the font has to be installed")
                .singleElement(InstanceOfAssertFactories.STRING)
                .contains("family=Second")
                .contains("viewer family \"Brand\"")
                .contains("draws it from the program that did travel")
                .doesNotContain("needs that font installed");
    }

    @Test
    void theRefusedFamilyLosesItsFreeSlotFaceToo() throws Exception {
        // The unit of refusal is the family, not the colliding face — as for a family with
        // an unreadable face. The arriving family here has a second face that would have
        // landed in a slot nobody holds, and it does not travel either: the refusal is read
        // on the first face and the family is given up whole. Without this the prose's
        // "refused as an unreadable one is" would be untested, and the natural misreading —
        // that only the colliding face is dropped — would look true.
        Path latoFile = Files.write(directory.resolve("Lato-Regular.ttf"),
                PptxDeck.face(LATO));
        Path poppinsFile = Files.write(directory.resolve("Poppins-Regular.ttf"),
                PptxDeck.face(POPPINS));
        Path boldFile = Files.write(directory.resolve("Lato-Bold.ttf"),
                PptxDeck.face(LATO_BOLD));

        assertThat(PptxDeck.slotOf(PptxDeck.face(LATO_BOLD)))
                .as("the premise: the arriving family's second face wants a free slot")
                .isEqualTo("bold");

        byte[] deck = deckOf(
                FontFamilyDefinition.files("First", latoFile).wordFamily("Brand").build(),
                FontFamilyDefinition.files("Second", poppinsFile)
                        .boldPath(boldFile).wordFamily("Brand").build());

        assertThat(PptxDeck.fontParts(deck))
                .as("one part: the holder's, and neither face of the refused family")
                .hasSize(1);
        assertThat(carriedBy(deck, "ppt/fonts/font1.fntdata")).isEqualTo(LATO);
        assertThat(PptxDeck.part(deck, "ppt/presentation.xml"))
                .as("so the bold element the second family would have filled is not there")
                .doesNotContain("<p:bold");
        assertThat(log.messages("render.pptx.font.embed.skipped"))
                .singleElement(InstanceOfAssertFactories.STRING)
                .contains("family=Second");
    }

    @Test
    void aRegisteredFamilyNeverDrawnInStillTakesTheSlotFromTheBundledOne() throws Exception {
        // The limit of the chosen order, pinned rather than argued. A registered family is
        // carried whether or not a run asks for it, and that happens before anything is
        // drawn — so a family declared under a bundled family's viewer name and never drawn
        // in still holds the slot, and the bundled family, whose text IS on the slide, is
        // the one refused. Before the guard the bundled face wrote over that part and its
        // text was right. Both arrangements are wrong for one of the two; this one is the
        // decision, and the warning says which program the text comes from.
        Path poppinsFile = Files.write(directory.resolve("Poppins-Regular.ttf"),
                PptxDeck.face(POPPINS));
        FontFamilyDefinition unusedButDeclared =
                FontFamilyDefinition.files("Corporate", poppinsFile).wordFamily("Lato").build();

        byte[] deck = PptxDeck.registering(List.of(unusedButDeclared), page ->
                page.addParagraph(paragraph -> paragraph
                        .text("Only the bundled family is drawn in")
                        .textStyle(DocumentTextStyle.builder()
                                .fontName(FontName.LATO).size(10).build())));

        assertThat(PptxDeck.fontParts(deck)).hasSize(1);
        assertThat(carriedBy(deck, "ppt/fonts/font1.fntdata"))
                .as("the deck carries the family nothing was drawn in")
                .isEqualTo(POPPINS);
        assertThat(log.messages("render.pptx.font.embed.skipped"))
                .as("and the bundled family is the refused one, named with the holder")
                .singleElement(InstanceOfAssertFactories.STRING)
                .contains("family=Lato")
                .contains("the family \"Corporate\" already carries a face there");
        assertThat(log.messages("render.pptx.font.substitution"))
                .filteredOn(warning -> warning.contains("family=Lato"))
                .singleElement(InstanceOfAssertFactories.STRING)
                .contains("draws it from the program that did travel");
    }

    @Test
    void noTwoBundledFamiliesDeclareOneViewerFamily() {
        // What makes the refusal's own wording true — "another REGISTERED family already
        // carries a face under the viewer family this one declares". The holder can only be
        // a registration as long as no two bundled families share a declared name, so a
        // font bump that added a second family under one name would quietly make that line
        // lie. Measured across the catalogue, as the per-definition guard's own catalogue
        // case is.
        List<FontFamilyDefinition> bundled = DefaultFonts.bundledFamilies();
        assertThat(bundled)
                .as("the premise: this walked the catalogue rather than an empty list")
                .hasSizeGreaterThan(30);
        List<String> declared = bundled.stream().map(FontFamilyDefinition::wordFamily).toList();
        // Compared with equalsIgnoreCase and not by folding the names to a key, because that
        // is the operator the entry lookup uses — the same reason the ledger walks a list
        // instead of keying a map, and a folded key here would be the substitution the
        // ledger's own javadoc refuses.
        for (int first = 0; first < declared.size(); first++) {
            for (int second = first + 1; second < declared.size(); second++) {
                assertThat(declared.get(first).equalsIgnoreCase(declared.get(second)))
                        .as("two bundled families declare one viewer family: \""
                                + declared.get(first) + "\" and \"" + declared.get(second)
                                + "\"")
                        .isFalse();
            }
        }
    }

    @Test
    void theClasspathPathRefusesTheArrivingFaceToo() throws Exception {
        // The other public path to the same guard, and not a copy of the first: the sources
        // differ in how they open and in what they call themselves, and the bundled families
        // travel this road, so a guard that only held for files on disk would leave it open.
        byte[] deck = deckOf(
                FontFamilyDefinition.classpath("First", LATO).wordFamily("Brand").build(),
                FontFamilyDefinition.classpath("Second", POPPINS).wordFamily("Brand").build());

        assertThat(PptxDeck.fontParts(deck)).as("one part, the holder's").hasSize(1);
        assertThat(carriedBy(deck, "ppt/fonts/font1.fntdata")).isEqualTo(LATO);
        assertThat(log.messages("render.pptx.font.embed.skipped"))
                .as("both resources are named by the path a caller registered them under")
                .singleElement(InstanceOfAssertFactories.STRING)
                .contains("family=Second")
                .contains(LATO)
                .contains(POPPINS);
    }

    @Test
    void namesDifferingOnlyInCaseAreOneDeclaration() throws Exception {
        // The lookup the slot's capacity follows is case-insensitive, so the guard's has to
        // be. Without this the two would pass as separate declarations here while the deck
        // put them in one entry, which is the whole defect with an extra step.
        Path latoFile = Files.write(directory.resolve("Lato-Regular.ttf"),
                PptxDeck.face(LATO));
        Path poppinsFile = Files.write(directory.resolve("Poppins-Regular.ttf"),
                PptxDeck.face(POPPINS));

        byte[] deck = deckOf(
                FontFamilyDefinition.files("First", latoFile).wordFamily("Brand").build(),
                FontFamilyDefinition.files("Second", poppinsFile).wordFamily("brand").build());

        assertThat(PptxDeck.fontParts(deck)).as("one part, the holder's").hasSize(1);
        assertThat(carriedBy(deck, "ppt/fonts/font1.fntdata")).isEqualTo(LATO);
        assertThat(PptxDeck.part(deck, "ppt/presentation.xml"))
                .as("one entry, spelled as the family that declared it first spelled it")
                .contains("typeface=\"Brand\"")
                .doesNotContain("typeface=\"brand\"");
        assertThat(log.messages("render.pptx.font.embed.skipped"))
                .as("the refusal names the caller's own spelling and the one the deck's "
                        + "entry carries, which is the first family's — a reader looking for "
                        + "\"brand\" in the package would find nothing")
                .singleElement(InstanceOfAssertFactories.STRING)
                .contains("family=Second")
                .contains("declares \"brand\"")
                .contains("the one font-list entry \"Brand\"")
                .contains("the family \"First\" already carries a face there");
    }

    @Test
    void twoRegistrationsOfOneFamilyNameAreOneFamilyAndTheLaterProgramTravels()
            throws Exception {
        // Deliberately NOT refused, and the boundary of the guard. `FontName` is
        // case-insensitive, so these two registrations are one family to everything above
        // this backend — one `FontLibrary` key, and that library keeps the LAST factory
        // registered under a name, so Poppins is what the page measured BOTH paragraphs
        // with. Refusing the later program would leave the deck carrying Lato, a face no
        // text was laid out in: the very defect this guard exists to stop, in a milder
        // dress. So the later program travels, as it did before the guard, and the slot
        // ledger has to agree with `FontName` about what one family is for that to hold.
        Path latoFile = Files.write(directory.resolve("Lato-Regular.ttf"),
                PptxDeck.face(LATO));
        Path poppinsFile = Files.write(directory.resolve("Poppins-Regular.ttf"),
                PptxDeck.face(POPPINS));

        byte[] deck = deckOf(
                FontFamilyDefinition.files("Brand", latoFile).build(),
                FontFamilyDefinition.files("brand", poppinsFile).build());

        assertThat(PptxDeck.fontParts(deck))
                .as("one part: one declared name, written twice over")
                .hasSize(1);
        assertThat(carriedBy(deck, "ppt/fonts/font1.fntdata"))
                .as("and it holds the LATER program, which is the one the page measured "
                        + "with — the font library keeps the last factory under a name")
                .isEqualTo(POPPINS);
        assertThat(log.messages("render.pptx.font.embed.skipped"))
                .as("nothing refused: the two are one family, not two sharing a name")
                .isEmpty();
        assertThat(log.messages("render.pptx.font.shared.family"))
                .as("and nothing is being shared, so no warning either")
                .isEmpty();
    }

    @Test
    void aHolderThatNeverReachedTheDeckDoesNotCostTheNextFamilyItsProgram() throws Exception {
        // The claim stands for a part that is about to exist, so it is made at the
        // hand-over's doorstep and not at the read. Between the two, a registered family's
        // viewer metrics are read, and a face fontbox accepts while `Font.createFont`
        // refuses — one truncated by a byte — gives the family up there, with nothing
        // written for it: no entry, no part. A claim made at the read would have let that
        // family hold the regular slot it never filled, refuse the next family declaring
        // the name, and tell it its text is drawn from a program that travelled when none
        // did.
        byte[] lato = PptxDeck.face(LATO);
        Path truncated = Files.write(directory.resolve("Truncated-Regular.ttf"),
                java.util.Arrays.copyOf(lato, lato.length - 1));
        Path poppinsFile = Files.write(directory.resolve("Poppins-Regular.ttf"),
                PptxDeck.face(POPPINS));

        byte[] deck = deckOf(
                FontFamilyDefinition.files("Truncated", truncated).wordFamily("Brand").build(),
                FontFamilyDefinition.files("Second", poppinsFile).wordFamily("Brand").build());

        assertThat(log.messages("render.pptx.font.embed.skipped"))
                .as("the premise: the holder gave up on its viewer metrics, which is the "
                        + "step between the read and the hand-over")
                .singleElement(InstanceOfAssertFactories.STRING)
                .contains("family=Truncated")
                .contains("reason=viewer metrics");
        assertThat(PptxDeck.fontParts(deck))
                .as("and the next family's program travels: nothing held the slot")
                .hasSize(1);
        assertThat(carriedBy(deck, "ppt/fonts/font1.fntdata")).isEqualTo(POPPINS);
        assertThat(log.messages("render.pptx.font.substitution"))
                .as("the refused family is named once, for the metrics read — not for a "
                        + "shared name, and the second family is not named at all")
                .singleElement(InstanceOfAssertFactories.STRING)
                .contains("family=Truncated")
                .doesNotContain("draws it from the program that did travel");
        assertThat(log.messages("render.pptx.font.shared.family"))
                .as("and nothing was shared, the holder's slots never having been claimed")
                .isEmpty();
    }

    @Test
    void aBundledFamilyJoiningADeclaredNameInAFreeSlotIsWarnedAboutToo() throws Exception {
        // The only path on which the shared-name warning crosses the two passes, and the
        // reason its wording says "font family" and not "registered family": the caller
        // declares a bundled family's viewer name with one regular face, the deck draws the
        // bundled family in BOLD only, so the bundled pass reads only its bold face and
        // lands in a slot still free. Both travel, and the pair being warned about is one
        // registration and one bundled family.
        Path poppinsFile = Files.write(directory.resolve("Poppins-Regular.ttf"),
                PptxDeck.face(POPPINS));
        FontFamilyDefinition mine =
                FontFamilyDefinition.files("Corporate", poppinsFile).wordFamily("Lato").build();

        byte[] deck = PptxDeck.registering(List.of(mine), page -> {
            page.addParagraph(paragraph -> paragraph
                    .text("Alpha in my own family")
                    .textStyle(DocumentTextStyle.builder()
                            .fontName(FontName.of("Corporate")).size(10).build()));
            page.addParagraph(paragraph -> paragraph
                    .text("Beta in the bundled Lato, bold")
                    .textStyle(DocumentTextStyle.builder()
                            .fontName(FontName.LATO).size(10)
                            .decoration(DocumentTextDecoration.BOLD).build()));
        });

        assertThat(PptxDeck.fontParts(deck))
                .as("both travel: the bundled bold face took a slot the caller left free")
                .hasSize(2);
        assertThat(carriedBy(deck, "ppt/fonts/font1.fntdata")).isEqualTo(POPPINS);
        assertThat(carriedBy(deck, "ppt/fonts/font2.fntdata")).isEqualTo(LATO_BOLD);
        assertThat(log.messages("render.pptx.font.embed.skipped")).isEmpty();
        assertThat(log.messages("render.pptx.font.shared.family"))
                .as("warned once, and not of two registered families — one of this pair is "
                        + "bundled")
                .singleElement(InstanceOfAssertFactories.STRING)
                .contains("typeface=\"Lato\"")
                .contains("more than one font family declares this viewer family");
    }

    @Test
    void aCallersFamilyUnderABundledOnesNameKeepsItsOwnProgram() throws Exception {
        // The reachable form, and the one the decision's order is for. The bundled pass
        // skips a family whose FontName the caller registered; this one's FontName differs
        // and only the declared name collides, so the bundled Lato reaches the embedder —
        // after the last slide, by which time the caller's family holds the regular slot.
        Path poppinsFile = Files.write(directory.resolve("Poppins-Regular.ttf"),
                PptxDeck.face(POPPINS));
        FontFamilyDefinition mine =
                FontFamilyDefinition.files("Corporate", poppinsFile).wordFamily("Lato").build();

        byte[] deck = PptxDeck.registering(List.of(mine), page -> {
            page.addParagraph(paragraph -> paragraph
                    .text("Alpha in my own family")
                    .textStyle(DocumentTextStyle.builder()
                            .fontName(FontName.of("Corporate")).size(10).build()));
            page.addParagraph(paragraph -> paragraph
                    .text("Beta in the bundled Lato")
                    .textStyle(DocumentTextStyle.builder()
                            .fontName(FontName.LATO).size(10).build()));
        });

        assertThat(PptxDeck.fontParts(deck))
                .as("one part, and it is the caller's — the bundled program used to write "
                        + "over it")
                .hasSize(1);
        assertThat(carriedBy(deck, "ppt/fonts/font1.fntdata"))
                .as("the caller's own font travels; an explicit registration is the holder "
                        + "because the bundled pass runs after the slides")
                .isEqualTo(POPPINS);
        assertThat(log.messages("render.pptx.font.embed.skipped"))
                .as("the bundled family is the refused one, named with the family that holds "
                        + "the slot")
                .singleElement(InstanceOfAssertFactories.STRING)
                .contains("family=Lato")
                .contains("the family \"Corporate\" already carries a face there");
        assertThat(log.messages("render.pptx.font.substitution"))
                .filteredOn(warning -> warning.contains("family=Lato"))
                .as("and the bundled family's line is the shared-name one, not the bundled "
                        + "\"install the font\" one")
                .singleElement(InstanceOfAssertFactories.STRING)
                .contains("draws it from the program that did travel");
    }

    @Test
    void twoFamiliesSharingANameWithoutSharingASlotBothTravel() throws Exception {
        // The control, and the load-bearing half: the guard is on the slot, not on the
        // declaration, so two definitions that split the styles of one viewer family keep
        // working. A guard that refused on the shared name alone would eat this, and it
        // would do so silently from a caller's side — both programs are in the deck today.
        Path latoFile = Files.write(directory.resolve("Lato-Regular.ttf"),
                PptxDeck.face(LATO));
        Path boldFile = Files.write(directory.resolve("Lato-Bold.ttf"),
                PptxDeck.face(LATO_BOLD));

        assertThat(PptxDeck.slotOf(PptxDeck.face(LATO))).isEqualTo("regular");
        assertThat(PptxDeck.slotOf(PptxDeck.face(LATO_BOLD)))
                .as("the premise: the second family's one face holds a different slot")
                .isEqualTo("bold");

        byte[] deck = deckOf(
                FontFamilyDefinition.files("First", latoFile).wordFamily("Brand").build(),
                FontFamilyDefinition.files("Second", boldFile).wordFamily("Brand").build());

        assertThat(PptxDeck.fontParts(deck))
                .as("both programs travel: nothing collided")
                .hasSize(2);
        assertThat(carriedBy(deck, "ppt/fonts/font1.fntdata")).isEqualTo(LATO);
        assertThat(carriedBy(deck, "ppt/fonts/font2.fntdata")).isEqualTo(LATO_BOLD);
        assertThat(PptxDeck.part(deck, "ppt/presentation.xml"))
                .as("one entry, declaring both styles")
                .contains("<p:font typeface=\"Brand\"/><p:regular")
                .contains("<p:bold");
        assertThat(log.messages("render.pptx.font.embed.skipped"))
                .as("and neither family is refused")
                .isEmpty();
        assertThat(log.messages("render.pptx.font.shared.family"))
                .as("warned once for the declared name, because the page measured two "
                        + "programs where the deck has one name for both")
                .singleElement(InstanceOfAssertFactories.STRING)
                .contains("typeface=\"Brand\"")
                .contains("one program per style for all of them");
    }

    @Test
    void theSharedNameWarningIsOnePerDeclaredNameHoweverManyFamiliesJoinIt() throws Exception {
        // One line per declared name, not per family or per face. THREE families under the
        // first name, so the claim is load-bearing: a run of two would give one line whether
        // the line is per name or per joining family, and the dedupe would go untested. Each
        // of the three holds a different style, because a family refused over a slot never
        // reaches the warning at all. A second shared name alongside it pins the other half —
        // two names shared in one render are two different facts about the deck, so a single
        // line for the whole render would leave one of them unsaid.
        Path latoFile = Files.write(directory.resolve("Lato-Regular.ttf"),
                PptxDeck.face(LATO));
        Path boldFile = Files.write(directory.resolve("Lato-Bold.ttf"),
                PptxDeck.face(LATO_BOLD));
        Path italicFile = Files.write(directory.resolve("Lato-Italic.ttf"),
                PptxDeck.face(LATO_ITALIC));
        Path poppinsFile = Files.write(directory.resolve("Poppins-Regular.ttf"),
                PptxDeck.face(POPPINS));

        byte[] deck = PptxDeck.registering(List.of(
                FontFamilyDefinition.files("First", latoFile).wordFamily("Brand").build(),
                FontFamilyDefinition.files("Second", boldFile).wordFamily("brand").build(),
                FontFamilyDefinition.files("Third", italicFile).wordFamily("BRAND").build(),
                FontFamilyDefinition.files("Fourth", poppinsFile).wordFamily("Other").build(),
                FontFamilyDefinition.files("Fifth", boldFile).wordFamily("Other").build()),
                page -> page.addParagraph(paragraph -> paragraph.text("Drawn in the default")));

        assertThat(deck).as("the deck is written").isNotEmpty();
        assertThat(PptxDeck.fontParts(deck))
                .as("the premise: all five families claimed a free style and travel, so none "
                        + "of them was refused before the warning could be reached")
                .hasSize(5);
        assertThat(log.messages("render.pptx.font.embed.skipped")).isEmpty();
        assertThat(log.messages("render.pptx.font.shared.family"))
                .as("two shared names, one line each — not one per family joining a name, "
                        + "which would be three for the first name alone")
                .hasSize(2);
        assertThat(log.messages("render.pptx.font.shared.family").get(0))
                .contains("typeface=\"Brand\"");
        assertThat(log.messages("render.pptx.font.shared.family").get(1))
                .contains("typeface=\"Other\"");
    }

    @Test
    void familiesUnderDifferentNamesAreTwoDeclarationsAndSayNothing() throws Exception {
        // The control for both halves: the same two faces that collide above travel whole
        // under names of their own, each in its own entry, with no refusal and no warning.
        // Without it every assertion here would hold just as well on a guard that refused
        // or warned about any two registered families at all.
        Path latoFile = Files.write(directory.resolve("Lato-Regular.ttf"),
                PptxDeck.face(LATO));
        Path poppinsFile = Files.write(directory.resolve("Poppins-Regular.ttf"),
                PptxDeck.face(POPPINS));

        byte[] deck = deckOf(
                FontFamilyDefinition.files("First", latoFile).wordFamily("Alpha").build(),
                FontFamilyDefinition.files("Second", poppinsFile).wordFamily("Beta").build());

        assertThat(PptxDeck.fontParts(deck)).as("a part each").hasSize(2);
        assertThat(carriedBy(deck, "ppt/fonts/font1.fntdata")).isEqualTo(LATO);
        assertThat(carriedBy(deck, "ppt/fonts/font2.fntdata")).isEqualTo(POPPINS);
        assertThat(PptxDeck.part(deck, "ppt/presentation.xml"))
                .as("two entries, one per declared name")
                .contains("typeface=\"Alpha\"")
                .contains("typeface=\"Beta\"");
        assertThat(log.messages("render.pptx.font.embed.skipped")).isEmpty();
        assertThat(log.messages("render.pptx.font.shared.family")).isEmpty();
    }

    @Test
    void oneFamilyRegisteredTwiceIsNotRefused() throws Exception {
        // The guard keys on the family a face was registered under, so an identical
        // definition registered twice stays harmless: the same program, the same slot, no
        // refusal and no warning. Pinned because the obvious reading of "a slot holds one
        // face" would refuse the second and cost a caller their font over a duplicate call.
        Path latoFile = Files.write(directory.resolve("Lato-Regular.ttf"),
                PptxDeck.face(LATO));
        FontFamilyDefinition family =
                FontFamilyDefinition.files("Brand", latoFile).wordFamily("Brand").build();

        byte[] deck = deckOf(family, family);

        assertThat(PptxDeck.fontParts(deck)).as("the one program, written once").hasSize(1);
        assertThat(carriedBy(deck, "ppt/fonts/font1.fntdata")).isEqualTo(LATO);
        assertThat(log.messages("render.pptx.font.embed.skipped")).isEmpty();
        assertThat(log.messages("render.pptx.font.shared.family")).isEmpty();
    }

    /** A deck that registers the families and draws a paragraph in each of the first two. */
    private static byte[] deckOf(FontFamilyDefinition first, FontFamilyDefinition second) {
        return PptxDeck.registering(List.of(first, second), page -> {
            page.addParagraph(paragraph -> paragraph
                    .text("Alpha in the first family")
                    .textStyle(DocumentTextStyle.builder()
                            .fontName(first.name()).size(10).build()));
            page.addParagraph(paragraph -> paragraph
                    .text("Beta in the second family")
                    .textStyle(DocumentTextStyle.builder()
                            .fontName(second.name()).size(10).build()));
        });
    }

    /**
     * Which bundled face's bytes the deck's part carries, by its resource path.
     *
     * <p>The container ends with the font program verbatim, so the part is matched against
     * the candidate faces themselves rather than against a length or a name the EOT header
     * also carries: a header names the family the <em>face</em> declares, which is what both
     * sides of a collision read the same way.</p>
     */
    private static String carriedBy(byte[] deck, String part) throws Exception {
        byte[] container = PptxDeck.partBytes(deck, part);
        List<String> carried = new ArrayList<>();
        for (String resource : List.of(LATO, LATO_BOLD, LATO_ITALIC, POPPINS)) {
            byte[] face = PptxDeck.face(resource);
            if (container.length >= face.length
                    && java.util.Arrays.equals(container, container.length - face.length,
                    container.length, face, 0, face.length)) {
                carried.add(resource);
            }
        }
        assertThat(carried)
                .as("the part carries exactly one of the faces this test knows")
                .hasSize(1);
        return carried.get(0);
    }

}
