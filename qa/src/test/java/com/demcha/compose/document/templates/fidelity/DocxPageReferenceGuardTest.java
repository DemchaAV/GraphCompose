package com.demcha.compose.document.templates.fidelity;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.backend.semantic.docx.DocxExportReport;
import com.demcha.compose.document.backend.semantic.docx.DocxSemanticBackend;
import com.demcha.compose.document.layout.LayoutAnchorNode;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.PageReferenceNode;
import com.demcha.compose.document.templates.fidelity.PageReferenceDocxDocuments.Reference;
import com.demcha.compose.document.templates.fidelity.PageReferenceDocxDocuments.ReferringDocument;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicReference;
import java.util.jar.JarFile;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

/**
 * Every {@code PAGEREF} field the DOCX export writes names exactly one bookmark the file holds, so
 * Word shows a page number once it updates its fields rather than "Error! Bookmark not defined."
 *
 * <p>Held over the template corpus, which refers to no page today, and over documents that refer
 * to an anchor on every kind of node that declares one, from a table of contents, the body, a
 * table's composed cell, a layer stack's column, a panel and a row's column
 * ({@link PageReferenceDocxDocuments}). Where the export writes an anchor no bookmark — a block that
 * draws nothing, one whose first drawing fills a page nothing is written on, one composed in a
 * table's cell, a drawn shape, an anchor the document does not have — the reference is the number
 * it reads, and the report says so: never a field on a bookmark the file lacks.</p>
 *
 * <p>The fields, bookmarks and paragraphs are read from every XML part of the file
 * ({@link DocxPageReferenceFields}), wherever they stand.</p>
 */
class DocxPageReferenceGuardTest {

    private static final String NO_BOOKMARK = "its anchor has no bookmark in the Word file";

    @Test
    void everyPageReferenceInTheCorpusNamesABookmarkTheFileHolds() throws Exception {
        List<String> problems = new ArrayList<>();
        int read = 0;
        for (DocxCorpusDocument document : DocxFidelityCorpusTest.corpus()) {
            AtomicReference<DocxExportReport> report = new AtomicReference<>();
            byte[] docx;
            int references;
            try (DocumentSession session = DocxFidelityCorpusTest.composed(document)) {
                references = PageReferenceDocxDocuments.pageReferencesIn(session.roots());
                docx = session.export(DocxSemanticBackend.builder().deterministic(true).reportSink(report::set).build());
            }
            assertLaidOut(document.stem(), report.get());
            DocxPageReferenceFields fields = DocxPageReferenceFields.read(docx);
            assertThat(fields.parts()).as("%s: its body read", document.stem()).contains("word/document.xml");
            assertThat(fields.paragraphs()).as("%s: its paragraphs read", document.stem()).isNotEmpty();
            fields.problems().forEach(problem -> problems.add(document.stem() + ": " + problem));
            assertThat(fields.pageReferences().size() + notedAsText(report.get()))
                    .as("%s: each page reference a field or its number, named", document.stem())
                    .isEqualTo(references);
            read++;
        }
        assertThat(read).as("the corpus read, as the fidelity gate holds it").isGreaterThanOrEqualTo(60);
        assertThat(problems).isEmpty();
    }

    @Test
    void eachReferenceIsAFieldOnItsAnchorsBookmarkOrItsNumberNamed() throws Exception {
        for (ReferringDocument document : PageReferenceDocxDocuments.documents()) {
            AtomicReference<DocxExportReport> report = new AtomicReference<>();
            byte[] docx;
            Map<String, Integer> pages = new HashMap<>();
            try (DocumentSession session = GraphCompose.document().create()) {
                document.compose().accept(session);
                assertThat(PageReferenceDocxDocuments.pageReferencesIn(session.roots()))
                        .as("%s: every reference it holds listed", document.name())
                        .isEqualTo(document.references().size());
                for (Reference reference : document.references()) {
                    session.pageIndex().pageNumberOf(reference.anchor())
                            .ifPresent(page -> pages.put(reference.anchor(), page));
                }
                docx = session.export(DocxSemanticBackend.builder().deterministic(true).reportSink(report::set).build());
            }
            assertLaidOut(document.name(), report.get());
            for (String premise : document.premises()) {
                assertThat(report.get().notes()).as("%s holds the shape its report names: %s", document.name(), premise)
                        .anyMatch(note -> note.detail().contains(premise));
            }
            DocxPageReferenceFields fields = DocxPageReferenceFields.read(docx);

            assertThat(fields.problems()).as(document.name()).isEmpty();
            // Each field on its anchor's bookmark, reading the page the layout resolved for it.
            List<Tuple> expected = document.fields().stream()
                    .map(reference -> tuple(reference.bookmark(), reads(reference, pages)))
                    .toList();
            assertThat(fields.pageReferences())
                    .extracting(DocxPageReferenceFields.Field::bookmark, DocxPageReferenceFields.Field::result)
                    .as("%s: a field for each reference whose anchor has a bookmark, on it", document.name())
                    .containsExactlyInAnyOrderElementsOf(expected);
            assertThat(notedAsText(report.get()))
                    .as("%s: each reference whose anchor has none written as its number, and named", document.name())
                    .isEqualTo(document.asText());
            // A reference standing between paragraphs of its own: what its paragraph reads, and that it
            // is there at all — one reading nothing is otherwise one not written.
            for (Reference reference : document.references()) {
                if (reference.label() == null) {
                    continue;
                }
                List<DocxPageReferenceFields.Paragraph> following = fields.following(reference.label(), 2);
                assertThat(following.get(1).text()).as("%s: the paragraph after the reference to %s",
                        document.name(), reference.anchor()).isEqualTo(reference.next());
                assertThat(following.get(0).text()).as("%s: the reference to %s reads", document.name(),
                        reference.anchor()).isEqualTo(reads(reference, pages));
                assertThat(following.get(0).holdsAField()).as("%s: the reference to %s is a field", document.name(),
                        reference.anchor()).isEqualTo(reference.bookmark() != null);
            }
        }
    }

    @Test
    void theDocumentsReferToAnAnchorOfEveryKindOfNodeThatDeclaresOne() throws Exception {
        Set<String> referred = new TreeSet<>();
        for (ReferringDocument document : PageReferenceDocxDocuments.documents()) {
            try (DocumentSession session = GraphCompose.document().create()) {
                document.compose().accept(session);
                List<String> anchors = document.references().stream().map(Reference::anchor).toList();
                PageReferenceDocxDocuments.anchored(session.roots()).forEach((anchor, kind) -> {
                    if (anchors.contains(anchor)) {
                        referred.add(kind);
                    }
                });
            }
        }

        assertThat(nodeKindsDeclaringAnAnchor()).as("the premise: the kinds GraphCompose ships with an anchor")
                .hasSizeGreaterThanOrEqualTo(9);
        assertThat(referred).containsExactlyInAnyOrderElementsOf(nodeKindsDeclaringAnAnchor());
    }

    // The reader, on files written by hand: it must find what it is to check wherever it stands.

    @Test
    void aFieldOnABookmarkTheFileHoldsHasNoProblem() throws Exception {
        DocxPageReferenceFields fields = DocxPageReferenceFields.read(docx(Map.of("word/document.xml",
                body(paragraph(bookmark("1", "intro") + field(" PAGEREF intro \\h ", "2"))))));

        assertThat(fields.pageReferences()).singleElement().satisfies(field -> {
            assertThat(field.bookmark()).isEqualTo("intro");
            assertThat(field.result()).isEqualTo("2");
        });
        assertThat(fields.bookmarks()).singleElement().satisfies(bookmark -> assertThat(bookmark.ended()).isTrue());
        assertThat(fields.paragraphs()).singleElement().satisfies(paragraph -> {
            assertThat(paragraph.text()).isEqualTo("2");
            assertThat(paragraph.holdsAField()).isTrue();
        });
        assertThat(fields.problems()).isEmpty();
    }

    @Test
    void aFieldOnABookmarkTheBodyLacksIsAProblem() throws Exception {
        // The export writes its bookmarks in the body: one of the name in a header is not the anchor's.
        DocxPageReferenceFields fields = DocxPageReferenceFields.read(docx(Map.of(
                "word/document.xml", body(paragraph(bookmark("1", "intro") + field(" PAGEREF terms \\h ", "3")
                                                    + field(" PAGEREF head \\h ", "3"))),
                "word/header1.xml", "<w:hdr " + NAMESPACES + ">" + paragraph(bookmark("2", "head")) + "</w:hdr>")));

        assertThat(fields.problems()).satisfiesExactly(
                problem -> assertThat(problem).contains("PAGEREF \"terms\"").contains("names a bookmark the file does not hold"),
                problem -> assertThat(problem).contains("PAGEREF \"head\"").contains("names a bookmark the file does not hold"));
    }

    @Test
    void aFieldIsFoundInAHeaderANestedTableAndATextBox() throws Exception {
        String nested = "<w:tbl><w:tr><w:tc><w:tbl><w:tr><w:tc>" + paragraph(field(" PAGEREF inTable \\h ", "1"))
                        + "</w:tc></w:tr></w:tbl></w:tc></w:tr></w:tbl>";
        String textBox = paragraph("<w:r><w:drawing><wp:anchor><a:graphic><a:graphicData><wps:wsp><wps:txbx>"
                                   + "<w:txbxContent>" + paragraph(field(" PAGEREF inBox \\h ", "1"))
                                   + "</w:txbxContent></wps:txbx></wps:wsp></a:graphicData></a:graphic></wp:anchor>"
                                   + "</w:drawing></w:r>");
        DocxPageReferenceFields fields = DocxPageReferenceFields.read(docx(Map.of(
                "word/document.xml", body(nested + textBox),
                "word/header1.xml", "<w:hdr " + NAMESPACES + ">" + paragraph(field(" PAGEREF inHeader \\h ", "1"))
                                    + "</w:hdr>")));

        assertThat(fields.pageReferences()).extracting(DocxPageReferenceFields.Field::bookmark)
                .containsExactlyInAnyOrder("inTable", "inBox", "inHeader");
        assertThat(fields.problems()).hasSize(3).allSatisfy(problem -> assertThat(problem).contains("does not hold"));
    }

    @Test
    void anInstructionSpelledOverRunsInAnyCaseNestedOrSimpleIsRead() throws Exception {
        String split = "<w:r><w:fldChar w:fldCharType=\"begin\"/></w:r>"
                       + "<w:r><w:instrText xml:space=\"preserve\"> PAGE</w:instrText></w:r>"
                       + "<w:r><w:instrText xml:space=\"preserve\">REF split \\h </w:instrText></w:r>"
                       + "<w:r><w:fldChar w:fldCharType=\"separate\"/></w:r><w:r><w:t>4</w:t></w:r>"
                       + "<w:r><w:fldChar w:fldCharType=\"end\"/></w:r>";
        String simple = "<w:fldSimple w:instr=\" PAGEREF simple \\h \"><w:r><w:t>5</w:t></w:r></w:fldSimple>";
        // A field in another's instruction, as { IF { PAGEREF nested } = 6 "six" "other" }.
        String nested = "<w:r><w:fldChar w:fldCharType=\"begin\"/></w:r>"
                        + "<w:r><w:instrText xml:space=\"preserve\"> IF </w:instrText></w:r>"
                        + field(" PAGEREF nested ", "6")
                        + "<w:r><w:instrText xml:space=\"preserve\"> = 6 \"six\" \"other\" </w:instrText></w:r>"
                        + "<w:r><w:fldChar w:fldCharType=\"separate\"/></w:r><w:r><w:t>six</w:t></w:r>"
                        + "<w:r><w:fldChar w:fldCharType=\"end\"/></w:r>";
        DocxPageReferenceFields fields = DocxPageReferenceFields.read(docx(Map.of("word/document.xml",
                body(paragraph(split) + paragraph(simple) + paragraph(field(" pageref lower \\h ", "7"))
                     + paragraph(nested)))));

        assertThat(fields.pageReferences()).extracting(DocxPageReferenceFields.Field::bookmark,
                DocxPageReferenceFields.Field::result).containsExactly(
                tuple("split", "4"), tuple("simple", "5"), tuple("lower", "7"), tuple("nested", "6"));
    }

    @Test
    void aBookmarkHeldTwiceNeverClosedOrInAnotherCaseIsAProblem() throws Exception {
        String twice = bookmark("1", "twice") + bookmark("2", "TWICE");
        String open = "<w:bookmarkStart w:id=\"3\" w:name=\"open\"/>";
        String cased = bookmark("4", "Cased");
        DocxPageReferenceFields fields = DocxPageReferenceFields.read(docx(Map.of("word/document.xml",
                body(paragraph(twice + open + cased + field(" PAGEREF twice \\h ", "1") + field(" PAGEREF open \\h ", "1")
                               + field(" PAGEREF cased \\h ", "1"))))));

        assertThat(fields.problems()).satisfiesExactly(
                problem -> assertThat(problem).contains("\"twice\"").contains("names 2 bookmarks"),
                problem -> assertThat(problem).contains("\"open\"").contains("never closes"),
                problem -> assertThat(problem).contains("\"cased\"").contains("in another case"));
    }

    @Test
    void aNameWordDoesNotTakeIsAProblem() throws Exception {
        // Past 40 characters, and no name at all: the switch is read as the name.
        String longName = "Terms_and_conditions_of_the_agreement_between";
        DocxPageReferenceFields fields = DocxPageReferenceFields.read(docx(Map.of("word/document.xml",
                body(paragraph(bookmark("1", longName) + field(" PAGEREF " + longName + " \\h ", "1")
                               + field(" PAGEREF \\h ", "1"))))));

        assertThat(fields.problems()).satisfiesExactly(
                problem -> assertThat(problem).contains(longName).contains("names no bookmark Word can hold"),
                problem -> assertThat(problem).contains("PAGEREF \"\\h\"").contains("names no bookmark Word can hold"));
    }

    @Test
    void aFieldLeftOpenOrMarkedOutOfOrderIsAProblem() throws Exception {
        String stray = "<w:r><w:fldChar w:fldCharType=\"end\"/></w:r>";
        String untyped = "<w:r><w:fldChar w:fldCharType=\"middle\"/></w:r>";
        String unended = "<w:r><w:fldChar w:fldCharType=\"begin\"/></w:r>"
                         + "<w:r><w:instrText> PAGEREF a \\h </w:instrText></w:r>";
        DocxPageReferenceFields fields = DocxPageReferenceFields.read(docx(Map.of(
                "word/document.xml", body(paragraph(bookmark("1", "a") + stray) + paragraph(untyped)
                                          + paragraph(unended)))));

        assertThat(fields.problems()).satisfiesExactly(
                problem -> assertThat(problem).contains("end mark with no field begun"),
                problem -> assertThat(problem).contains("no type Word reads (\"middle\")"),
                problem -> assertThat(problem).contains("never ended"));
    }

    @Test
    void aStrayInstructionASimpleFieldClosedOverAnotherOrAnIdOpenedTwiceIsAProblem() throws Exception {
        // Each in a part of its own, so the order they are named in is the parts'.
        String stray = "<w:r><w:instrText> PAGEREF a \\h </w:instrText></w:r>";
        String over = "<w:fldSimple w:instr=\" PAGE \"><w:r><w:fldChar w:fldCharType=\"begin\"/></w:r></w:fldSimple>";
        String twice = "<w:bookmarkStart w:id=\"5\" w:name=\"one\"/><w:bookmarkStart w:id=\"5\" w:name=\"two\"/>"
                       + "<w:bookmarkEnd w:id=\"5\"/>";
        DocxPageReferenceFields fields = DocxPageReferenceFields.read(docx(Map.of(
                "word/document.xml", body(paragraph(stray)),
                "word/header1.xml", "<w:hdr " + NAMESPACES + ">" + paragraph(over) + "</w:hdr>",
                "word/header2.xml", "<w:hdr " + NAMESPACES + ">" + paragraph(twice) + "</w:hdr>")));

        assertThat(fields.problems()).satisfiesExactly(
                problem -> assertThat(problem).isEqualTo("an instruction where no field's code is open, in word/document.xml"),
                problem -> assertThat(problem).isEqualTo("a simple field closed over an open one, in word/header1.xml"),
                problem -> assertThat(problem).isEqualTo("2 field(s) never ended, in word/header1.xml"),
                problem -> assertThat(problem).isEqualTo("bookmark id 5 opened twice, in word/header2.xml"));
    }

    /** What a reference reads in the file: as the document states, or the page of its anchor, or nothing. */
    private static String reads(Reference reference, Map<String, Integer> pages) {
        if (reference.reads() != null) {
            return reference.reads();
        }
        Integer page = pages.get(reference.anchor());
        return page == null ? "" : String.valueOf(page);
    }

    /**
     * The export was made from the document's layout: where the layout fails, the export writes the
     * document without it, saying so under this subject, and a page reference reads no page.
     */
    private static void assertLaidOut(String document, DocxExportReport report) {
        assertThat(report.bySubject()).as("%s: exported from its layout", document).doesNotContainKey("measured geometry");
    }

    private static long notedAsText(DocxExportReport report) {
        return report.notes().stream()
                .filter(note -> note.subject().equals("PageReferenceNode") && note.detail().contains(NO_BOOKMARK))
                .count();
    }

    /**
     * The node kinds GraphCompose ships whose record has an anchor of its own, read off the classes
     * of the node packages: a page reference names an anchor and declares none.
     */
    private static Set<String> nodeKindsDeclaringAnAnchor() throws Exception {
        Path source = Path.of(DocumentNode.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        List<String> classFiles = new ArrayList<>();
        for (String pkg : List.of(DocumentNode.class.getPackageName(), LayoutAnchorNode.class.getPackageName())) {
            String folder = pkg.replace('.', '/') + "/";
            if (Files.isDirectory(source)) {
                try (Stream<Path> files = Files.list(source.resolve(folder))) {
                    files.map(file -> folder + file.getFileName()).forEach(classFiles::add);
                }
            } else {
                try (JarFile jar = new JarFile(source.toFile())) {
                    jar.stream().map(ZipEntry::getName)
                            .filter(name -> name.startsWith(folder) && name.indexOf('/', folder.length()) < 0)
                            .forEach(classFiles::add);
                }
            }
        }
        Set<String> kinds = new TreeSet<>();
        for (String file : classFiles) {
            if (!file.endsWith(".class") || file.endsWith("package-info.class")) {
                continue;
            }
            Class<?> type = Class.forName(file.substring(0, file.length() - ".class".length()).replace('/', '.'),
                    false, DocumentNode.class.getClassLoader());
            if (DocumentNode.class.isAssignableFrom(type) && type.isRecord() && type != PageReferenceNode.class
                && !Modifier.isAbstract(type.getModifiers())
                && Arrays.stream(type.getRecordComponents())
                        .anyMatch(component -> component.getName().equals("anchor") && component.getType() == String.class)) {
                kinds.add(type.getSimpleName());
            }
        }
        return kinds;
    }

    private static final String NAMESPACES =
            "xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\" "
            + "xmlns:wp=\"http://schemas.openxmlformats.org/drawingml/2006/wordprocessingDrawing\" "
            + "xmlns:a=\"http://schemas.openxmlformats.org/drawingml/2006/main\" "
            + "xmlns:wps=\"http://schemas.microsoft.com/office/word/2010/wordprocessingShape\"";

    private static String body(String content) {
        return "<w:document " + NAMESPACES + "><w:body>" + content + "</w:body></w:document>";
    }

    private static String paragraph(String content) {
        return "<w:p>" + content + "</w:p>";
    }

    private static String bookmark(String id, String name) {
        return "<w:bookmarkStart w:id=\"" + id + "\" w:name=\"" + name + "\"/><w:bookmarkEnd w:id=\"" + id + "\"/>";
    }

    private static String field(String instruction, String result) {
        return "<w:r><w:fldChar w:fldCharType=\"begin\"/></w:r>"
               + "<w:r><w:instrText xml:space=\"preserve\">" + instruction + "</w:instrText></w:r>"
               + "<w:r><w:fldChar w:fldCharType=\"separate\"/></w:r>"
               + "<w:r><w:t>" + result + "</w:t></w:r>"
               + "<w:r><w:fldChar w:fldCharType=\"end\"/></w:r>";
    }

    /** A package of the given parts, enough for the reader, which reads XML parts alone. */
    private static byte[] docx(Map<String, String> parts) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
            for (Map.Entry<String, String> part : new TreeMap<>(parts).entrySet()) {
                zip.putNextEntry(new ZipEntry(part.getKey()));
                zip.write(part.getValue().getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
        }
        return bytes.toByteArray();
    }
}
