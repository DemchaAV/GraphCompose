package com.demcha.compose.document.backend.semantic.docx;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.xmlbeans.XmlObject;
import org.junit.jupiter.api.Test;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTHyperlink;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTP;

import javax.xml.namespace.QName;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A path of child steps walked over an element's children selects what the same path run as an
 * XPath selects, in the same order: the passes over a written document ask their questions so.
 *
 * @author Artem Demchyshyn
 */
class DocxXmlChildrenTest {

    private static final String W = "http://schemas.openxmlformats.org/wordprocessingml/2006/main";
    private static final String NAMESPACE = "declare namespace w='" + W + "' ";
    private static final QName R = new QName(W, "r");
    private static final QName T = new QName(W, "t");
    private static final QName HYPERLINK = new QName(W, "hyperlink");
    private static final QName RPR = new QName(W, "rPr");
    private static final QName SHD = new QName(W, "shd");

    @Test
    void aPathSelectsWhatItsXPathSelectsInTheSameOrder() throws Exception {
        try (XWPFDocument document = new XWPFDocument()) {
            CTP paragraph = paragraphOfRunsAndLinks(document);

            assertSameAsXPath(paragraph, "./w:r", R);
            assertSameAsXPath(paragraph, "./w:hyperlink/w:r", HYPERLINK, R);
            assertSameAsXPath(paragraph, "./w:hyperlink/w:r/w:t", HYPERLINK, R, T);
            assertThat(texts(DocxXmlChildren.path(paragraph, HYPERLINK, R, T)))
                    .containsExactly("link one a", "link one b", "link two a");
        }
    }

    @Test
    void aStepPassesOverChildrenOfOtherNames() throws Exception {
        try (XWPFDocument document = new XWPFDocument()) {
            CTP paragraph = paragraphOfRunsAndLinks(document);

            assertThat(texts(DocxXmlChildren.path(paragraph, R, T))).containsExactly("first", "between", "last");
        }
    }

    @Test
    void theFirstIsTheFirstInDocumentOrderAndNoneWhereThePathEndsInNothing() throws Exception {
        try (XWPFDocument document = new XWPFDocument()) {
            CTP paragraph = paragraphOfRunsAndLinks(document);

            assertThat(DocxXmlChildren.first(paragraph, HYPERLINK, R, T))
                    .isSameAs(paragraph.selectPath(NAMESPACE + "./w:hyperlink/w:r/w:t")[0]);
            assertThat(DocxXmlChildren.first(paragraph, R, RPR, SHD)).isNull();
            assertThat(DocxXmlChildren.path(paragraph, R, RPR, SHD)).isEmpty();
        }
    }

    @Test
    void untypedXmlIsWalkedAsTypedIs() throws Exception {
        // A text box's paragraphs come back as XML of no Word type: the walk reads them by name.
        XmlObject untyped = XmlObject.Factory.parse(
                "<x xmlns:w='" + W + "'><w:p><w:r><w:rPr><w:b/><w:shd w:fill='FF0000'/></w:rPr><w:t>a</w:t></w:r>"
                + "<w:hyperlink><w:r><w:t>b</w:t></w:r></w:hyperlink><w:r><w:t>c</w:t></w:r></w:p></x>");
        XmlObject paragraph = untyped.selectPath(NAMESPACE + ".//w:p")[0];

        assertSameAsXPath(paragraph, "./w:r", R);
        assertSameAsXPath(paragraph, "./w:r/w:rPr/w:shd", R, RPR, SHD);
        assertThat(texts(DocxXmlChildren.path(paragraph, R, T))).containsExactly("a", "c");
    }

    /** Runs and links interleaved: run, link of two runs, run, link of one run, run. */
    private static CTP paragraphOfRunsAndLinks(XWPFDocument document) {
        XWPFParagraph para = document.createParagraph();
        CTP paragraph = para.getCTP();
        paragraph.addNewR().addNewT().setStringValue("first");
        CTHyperlink one = paragraph.addNewHyperlink();
        one.addNewR().addNewT().setStringValue("link one a");
        one.addNewR().addNewT().setStringValue("link one b");
        paragraph.addNewR().addNewT().setStringValue("between");
        paragraph.addNewHyperlink().addNewR().addNewT().setStringValue("link two a");
        paragraph.addNewR().addNewT().setStringValue("last");
        return paragraph;
    }

    private static void assertSameAsXPath(XmlObject from, String xpath, QName... steps) {
        XmlObject[] expected = from.selectPath(NAMESPACE + xpath);
        List<XmlObject> walked = DocxXmlChildren.path(from, steps);
        assertThat(expected).isNotEmpty();
        assertThat(walked).hasSameSizeAs(expected);
        for (int index = 0; index < expected.length; index++) {
            assertThat(walked.get(index)).as(xpath + " #" + index).isSameAs(expected[index]);
        }
    }

    private static List<String> texts(List<XmlObject> texts) {
        return texts.stream().map(text -> {
            try (org.apache.xmlbeans.XmlCursor cursor = text.newCursor()) {
                return cursor.getTextValue();
            }
        }).toList();
    }
}
