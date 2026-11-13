package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.dsl.TableBuilder;
import com.demcha.compose.document.node.InlineTextRun;
import com.demcha.compose.document.node.ListMarker;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.output.DocumentHeaderFooter;
import com.demcha.compose.document.output.DocumentHeaderFooterZone;
import com.demcha.compose.document.output.DocumentPageZone;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.table.DocumentTableCell;
import com.demcha.compose.font.FontFamilyDefinition;
import com.demcha.compose.font.FontName;
import org.apache.poi.openxml4j.opc.PackagePart;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The document ships the faces it is set in.
 *
 * <p>A face was named and never shipped, so on a machine without it installed the reader
 * saw a substituted one — measured here, where Lato is not installed, Word swapped it. A
 * substituted face has different glyph widths, so every line breaks somewhere else and the
 * geometry above it stops meaning anything.</p>
 *
 * <p>What is shipped is narrow on purpose: the faces the document uses, and only those. One
 * family's four faces are about two and a half megabytes, which is not a price a document
 * setting a single line in it should pay.</p>
 *
 * @author Artem Demchyshyn
 */
class DocxFontTableTest {

    private static final String LATO_REGULAR = "fonts/google/lato/Lato-Regular.ttf";
    private static final String MONO_REGULAR = "fonts/google/ibmplexmono/IBMPlexMono-Regular.ttf";
    private static final String MONO_BOLD = "fonts/google/ibmplexmono/IBMPlexMono-Bold.ttf";
    /**
     * The names two families are registered under in the tests of one declared name. Two faces of
     * plainly different widths, so which of them a part holds is read off the file and not off a
     * length alone: Lato is proportional, IBM Plex Mono is not.
     */
    private static final FontName SHIPPED_LATO = FontName.of("shipped-lato");
    private static final FontName SHIPPED_MONO = FontName.of("shipped-mono");

    @Test
    void aBundledFamilyTravelsWithTheDocument() throws Exception {
        try (XWPFDocument document = export(page -> page.addParagraph(p -> p
                .text("Set in Lato")
                .textStyle(DocumentTextStyle.builder().fontName(FontName.LATO).build())))) {

            String table = fontTable(document);
            assertThat(table).contains("w:name=\"Lato\"").contains("<w:embedRegular");
            assertThat(fontParts(document))
                    .as("one face used, one face shipped")
                    .hasSize(1);
        }
    }

    @Test
    void onlyTheFacesTheDocumentUsesAreShipped() throws Exception {
        try (XWPFDocument document = export(page -> page
                .addParagraph(p -> p.text("Regular")
                        .textStyle(DocumentTextStyle.builder().fontName(FontName.LATO).build()))
                .addParagraph(p -> p.text("Bold")
                        .textStyle(DocumentTextStyle.builder().fontName(FontName.LATO)
                                .decoration(DocumentTextDecoration.BOLD).build())))) {

            String table = fontTable(document);
            assertThat(table).contains("<w:embedRegular").contains("<w:embedBold");
            assertThat(table)
                    .as("nothing asked for italic, so italic does not travel")
                    .doesNotContain("<w:embedItalic");
            assertThat(fontParts(document)).hasSize(2);
        }
    }

    @Test
    void aStandardFaceShipsNothingBecauseThereIsNothingToShip() throws Exception {
        // Helvetica is a name, not a file: nothing bundles it, and a reader opening the
        // document gets Word's substitution — the same one a PDF viewer applies.
        try (XWPFDocument document = export(page -> page.addParagraph(p -> p
                .text("Set in Helvetica")
                .textStyle(DocumentTextStyle.builder().fontName(FontName.HELVETICA).build())))) {

            assertThat(fontParts(document)).isEmpty();
            assertThat(partNames(document))
                    .as("and no font table either, rather than an empty one")
                    .noneMatch(name -> name.contains("fontTable"));
        }
    }

    @Test
    void aDocumentThatShipsAFaceAsksTheEditorToSaveItBackWhole() throws Exception {
        // LibreOffice writes the faces back on save only when the settings ask for embedded fonts;
        // measured, its save of a document without the request reopened in substitutes. Subset
        // fonts are never asked for: measured, Word then saved only the characters typed so far,
        // and characters typed after a reopen fell to another face.
        try (XWPFDocument document = export(page -> page.addParagraph("Set in Lato", face(FontName.LATO)))) {
            String settings = DocxSettings.xmlOf(document);

            assertThat(settings).contains("<w:embedTrueTypeFonts/>");
            assertThat(settings).doesNotContain("saveSubsetFonts");
        }
    }

    @Test
    void aDocumentThatShipsNoFaceAsksForNoneOnSave() throws Exception {
        // The request is for the faces the document carries. With it on, an editor's save embeds
        // faces of its own as well (Word 16 added Aptos to a document that shipped faces), which a
        // document that shipped none would then carry.
        try (XWPFDocument document = export(page -> page.addParagraph("Set in Helvetica", face(FontName.HELVETICA)))) {
            assertThat(fontParts(document)).isEmpty();
            assertThat(DocxSettings.xmlOf(document)).doesNotContain("embedTrueTypeFonts");
        }
    }

    @Test
    void aFaceThatMayNotTravelLeavesNoRequestToSaveFacesEither(@TempDir Path folder) throws Exception {
        // A face that may only be previewed and printed is named and refused, so the document
        // ships nothing, and the settings say nothing about keeping it.
        byte[] font = latoRegular();
        int os2 = DocxFontEmbedding.tableOffset(font, "OS/2", 10);
        font[os2 + 8] = 0x00;
        font[os2 + 9] = 0x04;
        assertThat(DocxFontEmbedding.permissionOf(font))
                .as("the fixture's face is the preview-only one the export refuses")
                .isEqualTo(DocxFontEmbedding.Permission.PREVIEW_ONLY);
        Path previewOnly = Files.write(folder.resolve("PreviewOnly.ttf"), font);

        byte[] docx;
        AtomicReference<DocxExportReport> report = new AtomicReference<>();
        try (DocumentSession session = GraphCompose.document().pageSize(400, 600)
                .margin(DocumentInsets.of(20))
                .registerFontFamily("Preview Only", previewOnly).create()) {
            session.pageFlow(page -> page.addParagraph("Set in a face that stays home",
                    DocumentTextStyle.builder().fontName(FontName.of("Preview Only")).size(10).build()));
            docx = session.export(new DocxSemanticBackend(report::set));
        }
        // Refused, not missed: a family that never resolved would ship nothing and ask for
        // nothing as well, without passing the face's terms at all.
        assertThat(report.get().notes())
                .filteredOn(note -> note.subject().equals("embedded font"))
                .singleElement()
                .satisfies(note -> {
                    assertThat(note.severity()).isEqualTo(DocxExportReport.Severity.DROPPED);
                    assertThat(note.detail()).contains("'Preview Only'").contains("reading and printing only");
                });
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(docx))) {
            assertThat(fontParts(document)).as("the face was refused").isEmpty();
            assertThat(DocxSettings.xmlOf(document)).doesNotContain("embedTrueTypeFonts");
        }
    }

    @Test
    void aFaceThatMayNotBeEmbeddedIsNamedAndNotShipped(@TempDir Path folder) throws Exception {
        // OS/2 fsType 0x0002: restricted. The page cannot measure such a face either, so the
        // document is written without a layout; the face is named, not shipped.
        byte[] font = latoRegular();
        int os2 = DocxFontEmbedding.tableOffset(font, "OS/2", 10);
        font[os2 + 8] = 0x00;
        font[os2 + 9] = 0x02;
        assertThat(DocxFontEmbedding.permissionOf(font))
                .as("the fixture's face is the restricted one the export refuses")
                .isEqualTo(DocxFontEmbedding.Permission.RESTRICTED);
        Path restricted = Files.write(folder.resolve("Restricted.ttf"), font);

        FontExport exported = exportSetIn(GraphCompose.document().registerFontFamily("Restricted", restricted),
                DocumentTextStyle.builder().fontName(FontName.of("Restricted")).size(10).build());

        assertThat(exported.layoutFailure()).as("the premise: the page could not lay the face out")
                .contains("does not permit embedding");
        assertThat(exported.fontNote()).satisfies(note -> {
            assertThat(note.severity()).isEqualTo(DocxExportReport.Severity.DROPPED);
            assertThat(note.detail()).isEqualTo("'Restricted' does not permit embedding (OS/2 fsType), so a "
                                                + "reader without it installed sees a substituted face");
        });
        assertThat(fontParts(exported.document())).as("the face was refused").isEmpty();
    }

    @Test
    void aFaceWhoseFileCannotBeReadIsNamedAndNotShipped(@TempDir Path folder) throws Exception {
        // The bold face's file is missing: the page cannot lay it out, and the export, written
        // without a layout, names the face it could not read.
        Path regular = Files.write(folder.resolve("Unread-Regular.ttf"), latoRegular());
        Path missing = folder.resolve("Unread-Bold.ttf");

        FontExport exported = exportSetIn(GraphCompose.document().registerFontFamily(
                        com.demcha.compose.font.FontFamilyDefinition.files("Unread", regular).boldPath(missing).build()),
                DocumentTextStyle.builder().fontName(FontName.of("Unread")).size(10)
                        .decoration(DocumentTextDecoration.BOLD).build());

        assertThat(exported.layoutFailure()).as("the premise: the page could not lay the face out").isNotEmpty();
        assertThat(exported.fontNote().detail()).as("the premise: the bold face's file is the one missing")
                .contains("Unread-Bold.ttf");
        assertThat(exported.fontNote()).satisfies(note -> {
            assertThat(note.severity()).isEqualTo(DocxExportReport.Severity.DROPPED);
            assertThat(note.detail()).startsWith("the embedBold face of 'Unread' could not be read (")
                    .contains(missing.toString())
                    .endsWith("), so a reader without it installed sees a substituted face");
        });
        assertThat(fontParts(exported.document())).as("the one face the text is set in, not shipped").isEmpty();
    }

    @Test
    void aFaceWhoseClasspathResourceIsMissingIsNamedAndNotShipped() throws Exception {
        // A classpath source reports a missing resource by throwing an unchecked exception, not
        // an IOException. The export names the face it could not read, as for a missing file,
        // and still writes the document.
        String resource = "fonts/absent/Absent-Regular.ttf";
        assertThat(DocxFontTableTest.class.getResource("/" + resource))
                .as("the premise: the resource is not on the test classpath")
                .isNull();

        FontExport exported = exportSetIn(GraphCompose.document().registerFontFamily(
                        com.demcha.compose.font.FontFamilyDefinition.classpath("Absent", resource).build()),
                DocumentTextStyle.builder().fontName(FontName.of("Absent")).size(10).build());

        assertThat(exported.layoutFailure()).as("the premise: the page could not lay out the missing face")
                .contains(resource);
        assertThat(exported.fontNote()).satisfies(note -> {
            assertThat(note.severity()).isEqualTo(DocxExportReport.Severity.DROPPED);
            assertThat(note.detail()).startsWith("the embedRegular face of 'Absent' could not be read (")
                    .as("the premise: the source failed unchecked, the path this test guards")
                    .contains("IllegalArgumentException")
                    .contains(resource)
                    .endsWith("), so a reader without it installed sees a substituted face");
        });
        assertThat(fontParts(exported.document())).as("the face that could not be read, not shipped").isEmpty();
        assertThat(exported.document().getParagraphs())
                .as("the text is still written")
                .anySatisfy(paragraph -> assertThat(paragraph.getText()).contains("Set in a face that stays home"));
    }

    @Test
    void twoFamiliesUnderOneNameShipOneFaceSet() throws Exception {
        // Measured in Word 16 and LibreOffice 24.2: a document declaring two face sets under one
        // name had both paragraphs set in the face declared first, the later program ignored. So
        // the later family travels with nothing rather than with a face no editor reads.
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
        FontFamilyDefinition ownHelvetica = FontFamilyDefinition.classpath(FontName.HELVETICA, LATO_REGULAR)
                .boldResource(MONO_REGULAR).build();
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
                    .as("a family does not give its own name up to itself")
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
        // entry under the name buys nothing even where the two families' slots do not meet.
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
            // The route into the line map where the spelling matches and the slot does not, which
            // only this arrangement reaches: the two families spell the name the same way, so the
            // given-up family's runs would read the key the holder filled — but their slot is one
            // the holder left empty, so there is no
            // such key and they read no line, which is the answer a spelling the map does not hold
            // gives as well. The paragraph is then written at its own face's line.
            DocxNaturalLine monoBold = DocxNaturalLine.of(resource(MONO_BOLD));
            assertThat(Math.round((monoBold.ascent() + monoBold.descent()) * 10 * 20))
                    .as("the premise the written height rests on, as the sibling test pins its own")
                    .isEqualTo(260);
            assertThat(lineOf(shipped.document(), 1)).isEqualTo("260 exact");
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
            // exact. The sibling test below reaches this map the other way, by a spelling it does
            // not hold at all, and lands on the same line for a reason of its own.
            assertThat(lineOf(shared.document(), 1)).isEqualTo("364 exact");
        }
    }

    @Test
    void aGivenUpFamilySpelledAnotherWayClaimsNoLineOfItsOwn() throws Exception {
        // The route into the line map where the spelling itself does not match: the runs of the
        // family given up spell the name their own way, and that spelling is not a key in it at
        // all. The paragraph lands
        // at the height the page measured with no least-height claim, which is where the free-slot
        // route lands too — each route is pinned because reaching that answer one way says nothing
        // about reaching it another.
        DocumentTextStyle lato = DocumentTextStyle.builder().fontName(SHIPPED_LATO).size(14).build();
        DocumentTextStyle mono = DocumentTextStyle.builder().fontName(SHIPPED_MONO).size(14).build();
        try (Shipped shared = exportSetIn(List.of(latoAs("CaseShared"), monoAs("CASESHARED")),
                List.of(lato, mono))) {

            // The route, read off the document: the table declares the holder's spelling alone,
            // and the given-up family's runs carry theirs, so the line kept under the declared
            // spelling is not a line those runs can read.
            assertThat(families(shared.document())).containsExactly("CaseShared");
            assertThat(asciiOf(shared.document(), 1)).isEqualTo("CASESHARED");

            assertThat(lineOf(shared.document(), 0))
                    .as("the family that keeps the name is unaffected")
                    .isEqualTo("336 atLeast");
            // Where the written height comes from, so the figure below is not a bare one: the
            // page's own measurement of the face those runs are set in, which is the whole of
            // what the paragraph can say once no line is read for it.
            DocxNaturalLine monoLine = DocxNaturalLine.of(monoRegular());
            assertThat(Math.round((monoLine.ascent() + monoLine.descent()) * 14 * 20)).isEqualTo(364);
            assertThat(lineOf(shared.document(), 1)).isEqualTo("364 exact");
        }
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

    /** A document and what the export said about it. */
    private record Shipped(XWPFDocument document, DocxExportReport report) implements AutoCloseable {
        @Override
        public void close() throws java.io.IOException {
            document.close();
        }
    }

    /**
     * Exports a document that registers these families and holds one paragraph per style, in the
     * order given: the order the styles are in is the order the font table reads the faces in.
     */
    private static Shipped exportSetIn(List<FontFamilyDefinition> registered,
                                       List<DocumentTextStyle> styles) throws Exception {
        AtomicReference<DocxExportReport> report = new AtomicReference<>();
        byte[] docx;
        GraphCompose.DocumentBuilder builder = GraphCompose.document().pageSize(400, 600)
                .margin(DocumentInsets.of(20));
        for (FontFamilyDefinition family : registered) {
            builder = builder.registerFontFamily(family);
        }
        try (DocumentSession session = builder.create()) {
            session.pageFlow(page -> styles.forEach(style -> page.addParagraph("Set in a shared name", style)));
            docx = session.export(new DocxSemanticBackend(report::set));
        }
        return new Shipped(new XWPFDocument(new ByteArrayInputStream(docx)), report.get());
    }

    /** The one face the document ships, as the font it was read from. */
    private static void assertShipsOneFace(XWPFDocument document, byte[] source) throws Exception {
        List<byte[]> parts = fontParts(document);
        assertThat(parts).as("one name declared, one face shipped under it").hasSize(1);
        byte[] part = parts.get(0);
        assertThat(part).hasSameSizeAs(source);
        assertThat(java.util.Arrays.copyOfRange(part, 32, part.length))
                .as("the face shipped is this font, past the scrambled header")
                .isEqualTo(java.util.Arrays.copyOfRange(source, 32, source.length));
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

    private static byte[] resource(String path) throws Exception {
        try (InputStream in = DocxFontTableTest.class.getClassLoader().getResourceAsStream(path)) {
            assertThat(in).as("the bundled font artifact is on the test classpath").isNotNull();
            return in.readAllBytes();
        }
    }

    private static DocumentTextStyle bold(FontName name) {
        return DocumentTextStyle.builder().fontName(name).size(10)
                .decoration(DocumentTextDecoration.BOLD).build();
    }

    /**
     * A paragraph set in one face, exported: the document, the report's one note on the embedded
     * font, and the layout's failure where the page could not lay the face out, or empty.
     */
    private record FontExport(XWPFDocument document, DocxExportReport.Note fontNote, String layoutFailure) {
    }

    private static FontExport exportSetIn(GraphCompose.DocumentBuilder builder, DocumentTextStyle style) throws Exception {
        AtomicReference<DocxExportReport> report = new AtomicReference<>();
        byte[] docx;
        try (DocumentSession session = builder.pageSize(400, 600).margin(DocumentInsets.of(20)).create()) {
            session.pageFlow(page -> page.addParagraph("Set in a face that stays home", style));
            docx = session.export(new DocxSemanticBackend(report::set));
        }
        DocxExportReport.Note fontNote = report.get().notes().stream()
                .filter(note -> note.subject().equals("embedded font"))
                .reduce((one, two) -> {
                    throw new AssertionError("more than one note on the embedded font");
                })
                .orElseThrow(() -> new AssertionError("no note on the embedded font"));
        String layoutFailure = report.get().notes().stream()
                .filter(note -> note.subject().equals("measured geometry"))
                .map(DocxExportReport.Note::detail)
                .filter(detail -> detail.contains("the layout failed: "))
                .findFirst().orElse("");
        return new FontExport(new XWPFDocument(new ByteArrayInputStream(docx)), fontNote, layoutFailure);
    }

    @Test
    void aFaceSetOnlyInATablesCellBuiltFromANodeTravelsToo() throws Exception {
        // A cell built from a node keeps it in the cell, not among its table's children: read by
        // the children alone, the faces of the cells — a nested table's among them — were named on
        // their runs and never shipped.
        try (XWPFDocument document = export(page -> page
                .addParagraph("Invoice", face(FontName.LATO))
                .addTable(t -> t.autoColumns(1).rowCells(DocumentTableCell.node(new ParagraphBuilder()
                        .text("Consulting").textStyle(face(FontName.POPPINS)).build())))
                .addTable(t -> t.autoColumns(1).rowCells(DocumentTableCell.node(new TableBuilder().autoColumns(1)
                        .rowCells(DocumentTableCell.node(new ParagraphBuilder()
                                .text("Nested").textStyle(face(FontName.PT_SERIF)).build()))
                        .build()))))) {

            assertThat(families(document)).containsExactlyInAnyOrder("Lato", "Poppins", "PT Serif");
        }
    }

    @Test
    void theFacesThePageZonesAndBandsAreSetInTravelToo() throws Exception {
        // The header and footer parts are written from the zones and the bands of text, not from
        // the document's tree.
        byte[] docx;
        try (DocumentSession session = GraphCompose.document().pageSize(400, 600)
                .margin(DocumentInsets.of(20)).create()) {
            session.chrome().zone(DocumentPageZone.footer(24, page -> new ParagraphBuilder().text("Acme")
                    .textStyle(face(FontName.BARLOW)).build()));
            session.chrome().zone(DocumentPageZone.header(24, page -> page.pageNumber(face(FontName.UBUNTU))));
            session.chrome().footer(DocumentHeaderFooter.builder().zone(DocumentHeaderFooterZone.FOOTER)
                    .fontName(FontName.ARSENAL).centerText("Page {page}").build());
            session.pageFlow(page -> page.addParagraph("Invoice", face(FontName.LATO)));
            docx = session.export(new DocxSemanticBackend());
        }
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(docx))) {
            assertThat(families(document)).containsExactlyInAnyOrder("Lato", "Barlow", "Ubuntu", "Arsenal");
        }
    }

    @Test
    void theFacesAZoneIsSetInOnItsFirstPageAndOnTheRestBothTravel() throws Exception {
        // Built otherwise for the first page, the zone is written into the first page's footer and
        // the others': each is set in a face of its own.
        byte[] docx;
        try (DocumentSession session = GraphCompose.document().pageSize(300, 200)
                .margin(DocumentInsets.of(30)).create()) {
            session.chrome().zone(DocumentPageZone.footer(20, page -> new ParagraphBuilder()
                    .text(page.isFirst() ? "Cover" : "Running")
                    .textStyle(face(page.isFirst() ? FontName.BARLOW : FontName.UBUNTU)).build()));
            session.pageFlow(page -> {
                for (int line = 0; line < 12; line++) {
                    page.addParagraph("Line " + line, face(FontName.LATO));
                }
            });
            docx = session.export(new DocxSemanticBackend());
        }
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(docx))) {
            assertThat(families(document)).containsExactlyInAnyOrder("Lato", "Barlow", "Ubuntu");
        }
    }

    @Test
    void aPageReferenceAndTheRunsOfAListsItemsAndMarkersShipTheirFaces() throws Exception {
        try (XWPFDocument document = export(page -> page
                .addParagraph(p -> p.text("Terms").anchor("terms").textStyle(face(FontName.LATO)))
                .addPageReference("terms", face(FontName.CARLITO), TextAlign.LEFT)
                // A list of plain items keeps its marker of runs as its own; one of items made of
                // runs gives each item its marker.
                .addList(list -> list.textStyle(face(FontName.LATO)).hangingIndent(true)
                        .marker(marker -> marker.style("*", face(FontName.FIRA_SANS)))
                        .items("plain"))
                .addList(list -> list.textStyle(face(FontName.LATO)).hangingIndent(true)
                        .markerFor(1, ListMarker.ofRuns(List.of(new InlineTextRun("-", face(FontName.UBUNTU)))))
                        .addItem(item -> item.plain("plain ").style("rich", face(FontName.ARSENAL)),
                                body -> body.addItem(item -> item.style("deeper", face(FontName.CRIMSON_TEXT))))))) {

            assertThat(families(document)).containsExactlyInAnyOrder(
                    "Lato", "Carlito", "Fira Sans", "Arsenal", "Ubuntu", "Crimson Text");
        }
    }

    @Test
    void theShippedFaceIsTheRealFontBehindItsScrambledHeader() throws Exception {
        try (XWPFDocument document = export(page -> page.addParagraph(p -> p
                .text("Set in Lato")
                .textStyle(DocumentTextStyle.builder().fontName(FontName.LATO).build())))) {

            byte[] part = fontParts(document).get(0);
            byte[] source = latoRegular();

            assertThat(part).hasSameSizeAs(source);
            assertThat(java.util.Arrays.copyOfRange(part, 0, 32))
                    .as("the header is not the font's own")
                    .isNotEqualTo(java.util.Arrays.copyOfRange(source, 0, 32));
            assertThat(java.util.Arrays.copyOfRange(part, 32, part.length))
                    .as("and everything past it is, byte for byte")
                    .isEqualTo(java.util.Arrays.copyOfRange(source, 32, source.length));

            // Unscrambling with the key the table states gives the font back, which is the
            // only thing that makes the part usable.
            java.util.UUID key = java.util.UUID.fromString(
                    fontTable(document).replaceAll("(?s).*w:fontKey=\"\\{([^}]+)\\}\".*", "$1"));
            assertThat(DocxFontEmbedding.obfuscate(part, key).bytes()).isEqualTo(source);
        }
    }

    private static byte[] latoRegular() throws Exception {
        return resource(LATO_REGULAR);
    }

    private static String fontTable(XWPFDocument document) throws Exception {
        PackagePart part = document.getPackage()
                .getPart(org.apache.poi.openxml4j.opc.PackagingURIHelper
                        .createPartName("/word/fontTable.xml"));
        assertThat(part).as("the document carries a font table").isNotNull();
        try (InputStream in = part.getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    /** The families the font table ships faces for. */
    private static List<String> families(XWPFDocument document) throws Exception {
        java.util.regex.Matcher name = java.util.regex.Pattern.compile("<w:font w:name=\"([^\"]+)\"")
                .matcher(fontTable(document));
        List<String> families = new java.util.ArrayList<>();
        while (name.find()) {
            families.add(name.group(1));
        }
        return families;
    }

    private static DocumentTextStyle face(FontName name) {
        return DocumentTextStyle.builder().fontName(name).size(10).build();
    }

    private static List<byte[]> fontParts(XWPFDocument document) throws Exception {
        return document.getPackage().getParts().stream()
                .filter(part -> part.getPartName().getName().startsWith("/word/fonts/"))
                .map(part -> {
                    try (InputStream in = part.getInputStream()) {
                        return in.readAllBytes();
                    } catch (Exception failure) {
                        throw new IllegalStateException(failure);
                    }
                })
                .toList();
    }

    private static List<String> partNames(XWPFDocument document) throws Exception {
        return document.getPackage().getParts().stream()
                .map(part -> part.getPartName().getName())
                .toList();
    }

    private static XWPFDocument export(Consumer<PageFlowBuilder> content) throws Exception {
        return DocxExports.withLayout(400, 600, 20, content);
    }
}
