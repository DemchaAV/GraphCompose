package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.document.backend.semantic.docx.DocxFontTableFixtures.FontElement;
import com.demcha.compose.document.backend.semantic.docx.DocxFontTableFixtures.Shipped;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.font.FontFamilyDefinition;
import com.demcha.compose.font.FontName;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static com.demcha.compose.document.backend.semantic.docx.DocxFontTableFixtures.LATO_REGULAR;
import static com.demcha.compose.document.backend.semantic.docx.DocxFontTableFixtures.MONO_BOLD;
import static com.demcha.compose.document.backend.semantic.docx.DocxFontTableFixtures.MONO_REGULAR;
import static com.demcha.compose.document.backend.semantic.docx.DocxFontTableFixtures.PAIR_BOLD;
import static com.demcha.compose.document.backend.semantic.docx.DocxFontTableFixtures.PAIR_REGULAR;
import static com.demcha.compose.document.backend.semantic.docx.DocxFontTableFixtures.SHIPPED_LATO;
import static com.demcha.compose.document.backend.semantic.docx.DocxFontTableFixtures.SHIPPED_MONO;
import static com.demcha.compose.document.backend.semantic.docx.DocxFontTableFixtures.SHIPPED_PAIR;
import static com.demcha.compose.document.backend.semantic.docx.DocxFontTableFixtures.assertCarries;
import static com.demcha.compose.document.backend.semantic.docx.DocxFontTableFixtures.bold;
import static com.demcha.compose.document.backend.semantic.docx.DocxFontTableFixtures.carries;
import static com.demcha.compose.document.backend.semantic.docx.DocxFontTableFixtures.exportSetIn;
import static com.demcha.compose.document.backend.semantic.docx.DocxFontTableFixtures.face;
import static com.demcha.compose.document.backend.semantic.docx.DocxFontTableFixtures.families;
import static com.demcha.compose.document.backend.semantic.docx.DocxFontTableFixtures.fontElements;
import static com.demcha.compose.document.backend.semantic.docx.DocxFontTableFixtures.fontParts;
import static com.demcha.compose.document.backend.semantic.docx.DocxFontTableFixtures.fontTable;
import static com.demcha.compose.document.backend.semantic.docx.DocxFontTableFixtures.latoRegular;
import static com.demcha.compose.document.backend.semantic.docx.DocxFontTableFixtures.resource;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * One face set per declared name: where two families ask for one name, the family set in text
 * first keeps it and ships its faces, and the other is given up whole and named in the report.
 *
 * <p>Measured in Word 16 and LibreOffice 24.2, both editors set every run naming the name in
 * the face of the element declared first, so a second face set under it travels for nothing.
 * These pin what the font table writes then, and the line each family's paragraphs are
 * written at.</p>
 *
 * @author Artem Demchyshyn
 */
class DocxSharedFontNameTest {

    @Test
    void twoFamiliesUnderOneNameShipOneFaceSet() throws Exception {
        // Measured in Word 16 and LibreOffice 24.2: a document declaring two face sets of
        // different typefaces under one name had both paragraphs set in the face of the element
        // declared first, the other program ignored. So the later family travels with nothing
        // rather than with a face no editor reads. The two here are Lato and IBM Plex Mono.
        try (Shipped shipped = exportSetIn(List.of(latoAs("ExactShared"), monoAs("ExactShared")),
                List.of(face(SHIPPED_LATO), face(SHIPPED_MONO)))) {

            assertThat(families(shipped.document())).containsExactly("ExactShared");
            assertShipsOneFace(shipped.document(), latoRegular());
        }
    }

    @Test
    void aNameDifferingOnlyInCaseIsTheSameName() throws Exception {
        // Measured in both editors: a run naming the second spelling was set in the face declared
        // under the first, so the two spellings are one name and not two.
        try (Shipped shipped = exportSetIn(List.of(latoAs("CaseShared"), monoAs("CASESHARED")),
                List.of(face(SHIPPED_LATO), face(SHIPPED_MONO)))) {

            assertThat(families(shipped.document()))
                    .as("declared once, spelled as the family that keeps the name spells it")
                    .containsExactly("CaseShared");
            assertShipsOneFace(shipped.document(), latoRegular());
        }
    }

    @Test
    void aFamilyReachedByBothItsFamilyNameAndAFaceNameOfItWritesOneElement() throws Exception {
        // A style can name a face of a family and the family itself, and both ask for that one
        // family: the standard-14 face names resolve to theirs, so a family registered under one
        // of those names is reached twice. Read per name it declared itself twice and so met
        // itself over its own name; read by family it writes one element holding both slots.
        // Two faces of one typeface, told apart by their files: a family whose slots held two
        // typefaces would also be named for that, which is not what this case is about.
        FontFamilyDefinition ownHelvetica = FontFamilyDefinition.classpath(FontName.HELVETICA, PAIR_REGULAR)
                .boldResource(PAIR_BOLD).build();
        try (Shipped shipped = exportSetIn(List.of(ownHelvetica),
                List.of(face(FontName.HELVETICA), bold(FontName.HELVETICA_BOLD)))) {

            assertThat(families(shipped.document()))
                    .as("one element, not one per name a style used")
                    .containsExactly("Helvetica");
            assertThat(fontTable(shipped.document()))
                    .as("holding every slot asked of the family, wherever it was asked for")
                    .contains("<w:embedRegular")
                    .contains("<w:embedBold");
            assertThat(fontParts(shipped.document())).hasSize(2);
            assertThat(shipped.report().notes())
                    .as("a family does not give its own name up to itself, and its two faces are "
                        + "of one typeface, so nothing is said about either")
                    .noneMatch(note -> note.subject().equals("embedded font"));
        }
    }

    @Test
    void theDocumentAndNotTheRegistrationDecidesWhichFamilyKeepsTheName() throws Exception {
        // Registered first and set in text second: the faces are read in the order the tree names
        // them, so the family the first paragraph is set in keeps the name. The PPTX backend
        // decides it the other way, by the registration, because it carries a registered family
        // before anything is drawn.
        try (Shipped shipped = exportSetIn(List.of(latoAs("ReverseShared"), monoAs("ReverseShared")),
                List.of(face(SHIPPED_MONO), face(SHIPPED_LATO)))) {

            assertThat(families(shipped.document())).containsExactly("ReverseShared");
            assertShipsOneFace(shipped.document(), monoRegular());
        }
    }

    @Test
    void aGivenUpFamilysFreeSlotDoesNotTravelEither() throws Exception {
        // Measured in both editors: a family declaring only embedBold, beside one declaring only
        // embedRegular under the same name, had its bold program ignored — Word synthesised bold
        // from the other family's regular face and LibreOffice drew it unemboldened. So a second
        // entry under the name buys nothing where the two programs are faces of different
        // typefaces, which is what this fixture's Lato and IBM Plex Mono are; a pair of one
        // typeface is read, and the refusal gives one up (see the element's own case below).
        FontFamilyDefinition mono = FontFamilyDefinition.classpath(SHIPPED_MONO, MONO_REGULAR)
                .boldResource(MONO_BOLD).wordFamily("SlotsShared").build();
        try (Shipped shipped = exportSetIn(List.of(latoAs("SlotsShared"), mono),
                List.of(face(SHIPPED_LATO), bold(SHIPPED_MONO)))) {

            assertThat(families(shipped.document())).containsExactly("SlotsShared");
            assertThat(fontTable(shipped.document()))
                    .contains("<w:embedRegular")
                    .as("the bold face of the family given up does not travel either")
                    .doesNotContain("<w:embedBold");
            assertShipsOneFace(shipped.document(), latoRegular());
            // The route into the line map where the name matches and the slot does not, which
            // only this arrangement reaches: the two families declare one name, so the given-up
            // family's runs would read the key the holder filled — but their slot is one the
            // holder left empty, so there is no such key and they read no line. The paragraph is
            // then written at its own face's line.
            DocxNaturalLine monoBold = DocxNaturalLine.of(resource(MONO_BOLD));
            assertThat(Math.round((monoBold.ascent() + monoBold.descent()) * 10 * 20))
                    .as("the premise the written height rests on, as the sibling test pins its own")
                    .isEqualTo(260);
            assertThat(lineOf(shipped.document(), 1)).isEqualTo("260 exact");
        }
    }

    @Test
    void anElementDeclaresOnlyTheFacesOfTheFamilyItIsNamedFor() throws Exception {
        // A w:font holds one child per face, so a family given up for a shared name could have
        // had its free slot written as another child of the holder's element rather than as an
        // element of its own. Measured in Word 16.0.20527 and LibreOffice 24.2, both shapes give
        // the same answer twice, and the shape is not what either editor goes by: a face of a
        // different typeface under the name was used by neither paragraph as a second element and
        // as a second child alike, in either order of the two children, while two faces of one
        // typeface were both read in either shape. So what the export writes is pinned here
        // rather than argued from the editors: each element declares the faces of its own family
        // and no others.
        //
        // With a second element beside the contended one, so a face bound into the wrong element
        // fails as well as an extra child in the right one; and the own-name family is built from
        // a third typeface, so that each child's program says which family it came from — two
        // children holding one file would make the comparison pass on either binding. The
        // given-up family is set in text in both of its slots, the one the holder filled and the
        // one it left empty, so that neither of its files could reach a part by any route and
        // both halves of the closing check can fail.
        FontFamilyDefinition holder = latoAs("Contended");
        FontFamilyDefinition givenUp = FontFamilyDefinition.classpath(SHIPPED_MONO, MONO_REGULAR)
                .boldResource(MONO_BOLD).wordFamily("Contended").build();
        FontFamilyDefinition ownName = FontFamilyDefinition.classpath(SHIPPED_PAIR, PAIR_REGULAR)
                .boldResource(PAIR_BOLD).wordFamily("OwnName").build();
        try (Shipped shipped = exportSetIn(List.of(holder, givenUp, ownName),
                List.of(face(SHIPPED_LATO), bold(SHIPPED_MONO), face(SHIPPED_MONO),
                        face(SHIPPED_PAIR), bold(SHIPPED_PAIR)))) {

            // The fixture reached the collision: without this the rest passes on a document where
            // the two families never contended for the name at all.
            assertThat(shipped.report().notes())
                    .filteredOn(note -> note.subject().equals("embedded font"))
                    .singleElement()
                    .satisfies(note -> assertThat(note.detail())
                            .as("the family that came to the taken name was given up")
                            .contains("'shipped-mono' travels under the Word name 'Contended'"));

            List<FontElement> elements = fontElements(shipped.document());
            assertThat(elements).extracting(FontElement::name)
                    .as("one element per family that keeps a name")
                    .containsExactly("Contended", "OwnName");
            assertThat(elements.get(0).slots())
                    .as("the holder declares the slot it filled and no other")
                    .containsExactly("embedRegular");
            assertCarries(elements.get(0).programs().get(0), resource(LATO_REGULAR));
            assertThat(elements.get(1).slots())
                    .as("the family under its own name declares both slots it filled")
                    .containsExactly("embedRegular", "embedBold");
            assertCarries(elements.get(1).programs().get(0), resource(PAIR_REGULAR));
            assertCarries(elements.get(1).programs().get(1), resource(PAIR_BOLD));

            // Neither file of the family given up travels in any font part — not under an element
            // of its own, and not under the element that keeps the name. The count is pinned too,
            // so a part no element points at would be seen.
            assertThat(fontParts(shipped.document()))
                    .as("one part per child the table declares, and no more").hasSize(3);
            for (byte[] part : fontParts(shipped.document())) {
                assertThat(carries(part, resource(MONO_REGULAR)))
                        .as("no part holds the given-up family's regular face").isFalse();
                assertThat(carries(part, resource(MONO_BOLD)))
                        .as("no part holds the given-up family's bold face").isFalse();
            }
        }
    }

    @Test
    void aFamilyGivenUpForASharedNameIsNamedInTheReport() throws Exception {
        try (Shipped shipped = exportSetIn(List.of(latoAs("ExactShared"), monoAs("ExactShared")),
                List.of(face(SHIPPED_LATO), face(SHIPPED_MONO)))) {

            assertShipsOneFace(shipped.document(), latoRegular());
            assertThat(shipped.report().notes())
                    .filteredOn(note -> note.subject().equals("embedded font"))
                    .singleElement()
                    .satisfies(note -> {
                        assertThat(note.severity()).isEqualTo(DocxExportReport.Severity.DROPPED);
                        assertThat(note.detail())
                                .as("both families by the names they were registered under, the name "
                                    + "each declares, and what a reader sees")
                                .contains("'shipped-mono' travels under the Word name 'ExactShared'")
                                .contains("'shipped-lato' already ships as 'ExactShared'")
                                .contains("travels with nothing")
                                .contains("its runs, which carry its own name")
                                .contains("the face 'shipped-lato' ships under it");
                    });
        }
    }

    @Test
    void theFamilyThatKeepsTheNameKeepsTheLineItsParagraphsMayGrowTo() throws Exception {
        // The line each shipped face is set in is kept under the name the runs spell. With two
        // face sets under one name, the later family's line stood under the same key and the
        // holder's paragraph was written at an exact height instead of a least one: measured, the
        // text then stood 0.36pt higher in Word 16 and 0.40pt higher in LibreOffice 24.2.
        // At the size those two were measured at, so the line read here is the line measured.
        DocumentTextStyle lato = DocumentTextStyle.builder().fontName(SHIPPED_LATO).size(14).build();
        DocumentTextStyle mono = DocumentTextStyle.builder().fontName(SHIPPED_MONO).size(14).build();
        DocxNaturalLine latoLine = DocxNaturalLine.of(latoRegular());
        DocxNaturalLine monoLine = DocxNaturalLine.of(monoRegular());
        assertThat(monoLine.ascent() + monoLine.descent())
                .as("the premise the holder's line rests on: the other face's line is the taller of "
                    + "the two, so standing in for this one is what wrote an exact height")
                .isGreaterThan(latoLine.ascent() + latoLine.descent());
        // And the premise the given-up family's own line rests on, which is a different one: the
        // two faces seat their text far enough apart that a least line would stand it off the
        // page's baseline, and the writer holds such a line to an exact height instead
        // (DocxSemanticBackend's least-grown-line shift, a quarter point). Read off the faces'
        // descents, which is the part of that shift this test can reach.
        assertThat(Math.abs(monoLine.descent() - latoLine.descent()) * 14).isGreaterThan(0.25);

        try (Shipped alone = exportSetIn(List.of(latoAs("SoloLato")), List.of(lato));
             Shipped shared = exportSetIn(List.of(latoAs("ExactShared"), monoAs("ExactShared")),
                     List.of(lato, mono))) {

            assertThat(lineOf(alone.document(), 0))
                    .as("the premise: a face that travels alone sets a line its paragraph may grow to")
                    .isEqualTo("336 atLeast");
            assertThat(lineOf(shared.document(), 0))
                    .as("the family that keeps the name is written as it is when it travels alone")
                    .isEqualTo(lineOf(alone.document(), 0));
            // The given-up family's runs spell the same name, so they read the key the holder
            // filled rather than a line of their own. Where that stands their text more than a
            // quarter point off the page's baseline — the premise above — the written line stays
            // exact. The sibling test below reaches the same key by a spelling in another case.
            assertThat(lineOf(shared.document(), 1)).isEqualTo("364 exact");
        }
    }

    @Test
    void aGivenUpFamilySpelledInAnotherCaseReadsTheHoldersLine() throws Exception {
        // The route into the line map where the spelling differs in case: the runs of the family
        // given up spell the name their own way, and an editor sets them in the face declared
        // under it all the same, so they read the line the holder shipped, as runs spelling it
        // the holder's way do. With these two faces that line stands the text more than a
        // quarter point off the page's baseline, so the paragraph is held exact, at the height
        // the page measured — which is where the free-slot route lands too, by no line at all.
        // Each route is pinned because reaching that answer one way says nothing about reaching
        // it another; the test of a holder's line a paragraph may grow to is the one that tells
        // this route from the free slot's.
        DocumentTextStyle lato = DocumentTextStyle.builder().fontName(SHIPPED_LATO).size(14).build();
        DocumentTextStyle mono = DocumentTextStyle.builder().fontName(SHIPPED_MONO).size(14).build();
        try (Shipped shared = exportSetIn(List.of(latoAs("CaseShared"), monoAs("CASESHARED")),
                List.of(lato, mono))) {

            // The route, read off the document: the table declares the holder's spelling alone,
            // and the given-up family's runs carry theirs.
            assertThat(families(shared.document())).containsExactly("CaseShared");
            assertThat(asciiOf(shared.document(), 1)).isEqualTo("CASESHARED");

            assertThat(lineOf(shared.document(), 0))
                    .as("the family that keeps the name is unaffected")
                    .isEqualTo("336 atLeast");
            // Where the written height comes from, so the figure below is not a bare one: the
            // page's own measurement of the face those runs are set in, which is what an exact
            // line holds the paragraph to.
            DocxNaturalLine monoLine = DocxNaturalLine.of(monoRegular());
            assertThat(Math.round((monoLine.ascent() + monoLine.descent()) * 14 * 20)).isEqualTo(364);
            assertThat(lineOf(shared.document(), 1)).isEqualTo("364 exact");
        }
    }

    @Test
    void aGivenUpFamilyReadsTheHoldersLineWhicheverCaseItSpellsTheNameIn() throws Exception {
        // Word draws a run from the face declared under its name in another case — measured on a
        // document of this shape, its one element in the holder's spelling, where upper- and
        // lower-case runs were drawn from the holder's program at its width and on its line — so a
        // given-up family's runs are set in the holder's face whether they spell the name as the
        // holder did or not, and read the holder's line either way.
        // Two registrations of one face, so the line the holder ships is a line those runs may grow
        // to, and the written line says which of the two routes found it: a least line where the
        // holder's line was read, an exact one where none was. The
        // holder's own spelling writes no font on the run, the document's default face being that
        // name, so that row is read through the default; the other two through the run's spelling.
        FontName again = FontName.of("shipped-lato-again");
        DocumentTextStyle holder = DocumentTextStyle.builder().fontName(SHIPPED_LATO).size(14).build();
        DocumentTextStyle givenUp = DocumentTextStyle.builder().fontName(again).size(14).build();

        List<String> lines = new java.util.ArrayList<>();
        for (String spelling : List.of("FoldShared", "FOLDSHARED", "foldshared")) {
            try (Shipped shared = exportSetIn(List.of(latoAs("FoldShared"),
                            FontFamilyDefinition.classpath(again, LATO_REGULAR).wordFamily(spelling).build()),
                    List.of(holder, givenUp))) {
                assertThat(families(shared.document()))
                        .as("the premise: one family keeps the name, in the holder's spelling")
                        .containsExactly("FoldShared");
                if (!spelling.equals("FoldShared")) {
                    // A run spelling the name as the document's default face does writes no
                    // font of its own, so only the other spellings can be read off the run.
                    assertThat(asciiOf(shared.document(), 1))
                            .as("the premise: the given-up family's runs spell it their own way")
                            .isEqualTo(spelling);
                }
                lines.add(lineOf(shared.document(), 1));
            }
        }
        assertThat(lines)
                .as("the holder's line, read by every spelling of the name")
                .containsExactly("336 atLeast", "336 atLeast", "336 atLeast");
    }

    @Test
    void aFamilyThatShipsNoFaceKeepsNoName(@TempDir Path folder) throws Exception {
        // The name is taken where a face is shipped under it, not where one is asked for: a
        // family that writes no w:font leaves the name free for the family behind it. The export
        // before this rule existed wrote the same document, because a family with no face to ship
        // never reached the list either — what this holds is where the claim sits now that there
        // is one, against an implementation that claimed the name at the face's read instead.
        byte[] font = latoRegular();
        int os2 = DocxFontEmbedding.tableOffset(font, "OS/2", 10);
        font[os2 + 8] = 0x00;
        font[os2 + 9] = 0x04;
        assertThat(DocxFontEmbedding.permissionOf(font))
                .as("the premise: the first family's only face is one that stays home")
                .isEqualTo(DocxFontEmbedding.Permission.PREVIEW_ONLY);
        FontName staysHome = FontName.of("preview-only");
        try (Shipped shipped = exportSetIn(
                List.of(FontFamilyDefinition
                                .files(staysHome, Files.write(folder.resolve("PreviewOnly.ttf"), font))
                                .wordFamily("SharedByBoth").build(),
                        monoAs("SharedByBoth")),
                List.of(face(staysHome), face(SHIPPED_MONO)))) {

            assertThat(families(shipped.document())).containsExactly("SharedByBoth");
            assertShipsOneFace(shipped.document(), monoRegular());
            assertThat(shipped.report().notes())
                    .filteredOn(note -> note.subject().equals("embedded font"))
                    .singleElement()
                    .satisfies(note -> assertThat(note.detail())
                            .as("the one note is about that face's terms, not about a shared name")
                            .contains("reading and printing only"));
        }
    }

    @Test
    void aRegisteredFamilyClaimingABundledFamilysNameGivesWayToIt() throws Exception {
        // The form this is reachable in with one registration: a caller's own family declared
        // under a bundled family's Word name. Families are kept by the name they were registered
        // under, so both reach the font table, and the bundled one, set in text first, keeps the
        // name.
        FontFamilyDefinition mine = monoAs("Lato");
        try (Shipped shipped = exportSetIn(List.of(mine), List.of(face(FontName.LATO), face(SHIPPED_MONO)))) {

            assertThat(families(shipped.document())).containsExactly("Lato");
            assertShipsOneFace(shipped.document(), latoRegular());
            assertThat(shipped.report().notes())
                    .filteredOn(note -> note.subject().equals("embedded font"))
                    .singleElement()
                    .satisfies(note -> assertThat(note.detail())
                            .contains("'shipped-mono' travels under the Word name 'Lato'"));
        }
    }

    /** The one face the document ships, as the font it was read from. */
    private static void assertShipsOneFace(XWPFDocument document, byte[] source) throws Exception {
        List<byte[]> parts = fontParts(document);
        assertThat(parts).as("one name declared, one face shipped under it").hasSize(1);
        assertCarries(parts.get(0), source);
    }

    /** The family a paragraph's one run names, as the document carries it. */
    private static String asciiOf(XWPFDocument document, int paragraph) {
        return document.getParagraphs().get(paragraph).getRuns().get(0).getFontFamily();
    }

    /** The line a paragraph is written with: its height in twips and the rule it is held to. */
    private static String lineOf(XWPFDocument document, int paragraph) {
        var properties = document.getParagraphs().get(paragraph).getCTP().getPPr();
        if (properties == null || !properties.isSetSpacing()) {
            return "(no spacing)";
        }
        return properties.getSpacing().getLine() + " " + properties.getSpacing().getLineRule();
    }

    private static FontFamilyDefinition latoAs(String wordFamily) {
        return FontFamilyDefinition.classpath(SHIPPED_LATO, LATO_REGULAR).wordFamily(wordFamily).build();
    }

    private static FontFamilyDefinition monoAs(String wordFamily) {
        return FontFamilyDefinition.classpath(SHIPPED_MONO, MONO_REGULAR).wordFamily(wordFamily).build();
    }

    private static byte[] monoRegular() throws Exception {
        return resource(MONO_REGULAR);
    }
}
