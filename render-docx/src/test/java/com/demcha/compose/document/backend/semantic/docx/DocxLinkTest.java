package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.node.DocumentLinkTarget;
import org.apache.poi.openxml4j.opc.PackageRelationship;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFHyperlinkRun;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A link in the document is a link in the file.
 *
 * <p>Every one was dropped: a reader opened an exported document, found the text of a link
 * with nothing behind it, and a reference to another section that went nowhere. For a
 * document handed over to be edited rather than read, that is most of the point of handing
 * it over.</p>
 *
 * <p>Word owns both kinds — {@code w:hyperlink} with a relationship for an address, or with
 * {@code w:anchor} for a bookmark in the same document — so what these pin is a mapping,
 * not an approximation.</p>
 *
 * @author Artem Demchyshyn
 */
class DocxLinkTest {

    @Test
    void aParagraphLinkBecomesAHyperlinkWithItsAddress() throws Exception {
        try (XWPFDocument document = exported(page -> page.addParagraph(p -> p
                .text("The licence")
                .linkTarget(DocumentLinkTarget.external("https://example.org/licence"))))) {

            XWPFHyperlinkRun run = onlyHyperlink(document);
            assertThat(run.getText(0)).isEqualTo("The licence");
            assertThat(addressOf(document, run))
                    .as("the address the document asked for, through a real relationship")
                    .isEqualTo("https://example.org/licence");
        }
    }

    @Test
    void oneLinkedPhraseLeavesTheRestOfTheSentenceAlone() throws Exception {
        try (XWPFDocument document = exported(page -> page.addParagraph(p -> p
                .inlineText("See ")
                .inlineLink("the licence",
                        new com.demcha.compose.document.node.DocumentLinkOptions("https://example.org/licence"))
                .inlineText(" for the terms.")))) {

            XWPFParagraph para = document.getParagraphs().get(0);
            assertThat(para.getText()).isEqualTo("See the licence for the terms.");
            List<XWPFHyperlinkRun> links = para.getRuns().stream()
                    .filter(XWPFHyperlinkRun.class::isInstance)
                    .map(XWPFHyperlinkRun.class::cast)
                    .toList();
            assertThat(links).hasSize(1);
            assertThat(links.get(0).getText(0)).isEqualTo("the licence");
        }
    }

    @Test
    void anInternalLinkPointsAtTheBookmarkItsAnchorBecame() throws Exception {
        try (XWPFDocument document = exported(page -> page
                .addParagraph(p -> p.text("Back to terms")
                        .linkTarget(DocumentLinkTarget.anchor("Terms & conditions")))
                .addParagraph(p -> p.text("Terms and conditions")
                        .anchor("Terms & conditions")))) {

            String anchor = onlyHyperlink(document).getAnchor();
            assertThat(anchor)
                    .as("cleaned to what Word accepts: letters, digits and underscores")
                    .isEqualTo("Terms_conditions");
            assertThat(bookmarkNames(document))
                    .as("and the bookmark carries the same name, or the link goes nowhere")
                    .containsExactly(anchor);
        }
    }

    @Test
    void aForwardReferenceResolvesJustAsWell() throws Exception {
        // The link is written before the anchor it points at exists in the file. Nothing
        // resolves anything at write time — both sides go through the same naming — so the
        // order the document happens to be in does not matter.
        try (XWPFDocument document = exported(page -> page
                .addParagraph(p -> p.text("Jump ahead").linkTarget(DocumentLinkTarget.anchor("appendix")))
                .addParagraph(p -> p.text("Appendix").anchor("appendix")))) {

            assertThat(onlyHyperlink(document).getAnchor()).isEqualTo("appendix");
            assertThat(bookmarkNames(document)).containsExactly("appendix");
        }
    }

    @Test
    void theBookmarkWrapsTheTextRatherThanSittingBeforeIt() throws Exception {
        try (XWPFDocument document = exported(page -> page
                .addParagraph(p -> p.text("Appendix").anchor("appendix")))) {

            String xml = document.getParagraphs().get(0).getCTP().xmlText();
            assertThat(xml.indexOf("bookmarkStart"))
                    .as("opened before the text")
                    .isLessThan(xml.indexOf("<w:r>"));
            assertThat(xml.indexOf("bookmarkEnd"))
                    .as("and closed after it, so the reader lands on the paragraph")
                    .isGreaterThan(xml.indexOf("<w:r>"));
        }
    }

    @Test
    void twoAnchorsThatCleanToTheSameTextStayApart() throws Exception {
        try (XWPFDocument document = exported(page -> page
                .addParagraph(p -> p.text("One").anchor("Section 1"))
                .addParagraph(p -> p.text("Two").anchor("Section-1")))) {

            assertThat(bookmarkNames(document))
                    .as("two anchors are two bookmarks, whatever they clean to")
                    .doesNotHaveDuplicates()
                    .hasSize(2);
        }
    }

    @Test
    void anAnchorStartingWithADigitIsStillAName() throws Exception {
        // Word will not take a bookmark whose name starts with anything but a letter, and
        // refuses the file rather than the bookmark.
        try (XWPFDocument document = exported(page -> page
                .addParagraph(p -> p.text("Clause").anchor("2024-terms")))) {

            assertThat(bookmarkNames(document)).hasSize(1);
            assertThat(bookmarkNames(document).get(0)).matches("^[A-Za-z][A-Za-z0-9_]*$");
        }
    }

    @Test
    void aParagraphWithNoLinkCarriesNoHyperlink() throws Exception {
        try (XWPFDocument document = exported(page -> page.addParagraph(p -> p.text("Plain")))) {
            assertThat(document.getParagraphs().get(0).getCTP().sizeOfHyperlinkArray()).isZero();
            assertThat(bookmarkNames(document)).isEmpty();
        }
    }

    @Test
    void aLinkInsideAListItemIsStillALink() throws Exception {
        // A list item made of runs is written by its own path, which learned styles and
        // chips but not links: the same phrase was a link in a sentence and dead text in a
        // bullet. hangingIndent, because an item made of runs is laid out only with it.
        try (XWPFDocument document = exported(page -> page
                .addList(list -> list
                        .bullet()
                        .hangingIndent(true)
                        .addItem(rich -> rich.plain("Read the ")
                                .link("guide", "https://graphcompose.dev/guide")
                                .plain(" first"))))) {

            XWPFHyperlinkRun link = onlyHyperlink(document);
            assertThat(link.text()).isEqualTo("guide");
            assertThat(addressOf(document, link)).isEqualTo("https://graphcompose.dev/guide");
            assertThat(document.getParagraphs().get(0).getText())
                    .as("the rest of the item is still there, around the link")
                    .contains("Read the guide first");
        }
    }

    @Test
    void anInternalLinkInsideAListItemPointsAtItsBookmark() throws Exception {
        try (XWPFDocument document = exported(page -> page
                .addList(list -> list
                        .bullet()
                        .hangingIndent(true)
                        .addItem(rich -> rich.plain("See ").linkTo("terms", "terms")))
                .addParagraph(p -> p.text("Terms").anchor("terms")))) {

            List<String> anchors = document.getParagraphs().stream()
                    .flatMap(p -> p.getCTP().getHyperlinkList().stream())
                    .map(hyperlink -> hyperlink.getAnchor())
                    .toList();
            assertThat(anchors).containsExactly("terms");
            assertThat(bookmarkNames(document)).contains("terms");
        }
    }

    private static XWPFHyperlinkRun onlyHyperlink(XWPFDocument document) {
        List<XWPFHyperlinkRun> links = document.getParagraphs().stream()
                .flatMap(p -> p.getRuns().stream())
                .filter(XWPFHyperlinkRun.class::isInstance)
                .map(XWPFHyperlinkRun.class::cast)
                .toList();
        assertThat(links).hasSize(1);
        return links.get(0);
    }

    /** The address behind a hyperlink run, read through the part's own relationship. */
    private static String addressOf(XWPFDocument document, XWPFHyperlinkRun run) {
        PackageRelationship relationship = document.getPackagePart()
                .getRelationship(run.getHyperlinkId());
        assertThat(relationship).as("the run points at a relationship that exists").isNotNull();
        return relationship.getTargetURI().toString();
    }

    private static List<String> bookmarkNames(XWPFDocument document) {
        return document.getParagraphs().stream()
                .flatMap(p -> p.getCTP().getBookmarkStartList().stream())
                .map(bookmark -> bookmark.getName())
                .toList();
    }

    private static XWPFDocument exported(Consumer<PageFlowBuilder> content) throws Exception {
        return DocxExports.withLayout(400, 600, 20, content);
    }
}
