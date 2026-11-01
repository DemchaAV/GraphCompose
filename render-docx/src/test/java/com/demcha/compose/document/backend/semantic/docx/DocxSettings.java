package com.demcha.compose.document.backend.semantic.docx;

import org.apache.poi.openxml4j.opc.PackagePart;
import org.apache.poi.openxml4j.opc.PackagingURIHelper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Reads the document-wide settings of an exported file.
 *
 * <p>POI's {@code getEvenAndOddHeadings()} returns {@code isSetEvenAndOddHeaders()}, so it answers
 * whether the element is there and not whether it says yes, and calls a document that states
 * {@code w:val="false"} one with different even and odd pages. The tests read the part instead,
 * which {@code DocxPageZoneKindsTest} and {@code DocxPageReferenceTest} each held their own copy
 * of.</p>
 *
 * @author Artem Demchyshyn
 */
final class DocxSettings {

    /**
     * The element in either form it may be written in. A pattern for the self-closed form alone
     * reads {@code <w:evenAndOddHeaders w:val="1"></w:evenAndOddHeaders>} as a document that does
     * not state even and odd pages, which is the answer that lets a check of the even pages' own
     * header pass without running.
     */
    private static final Pattern EVEN_AND_ODD = Pattern.compile("<w:evenAndOddHeaders([^>]*?)/?>");

    private DocxSettings() {
    }

    /**
     * Whether the document states different even and odd pages: the element written, and not
     * written off. {@code ST_OnOff} spells off as {@code false}, {@code 0} or {@code off}, and the
     * element with no value at all means on, so anything else is read as on — a reading that errs
     * towards running the check of the even pages' own header rather than skipping it.
     *
     * @param document an exported document
     * @return whether it states different even and odd pages
     */
    static boolean evenAndOddPages(XWPFDocument document) throws Exception {
        Matcher element = EVEN_AND_ODD.matcher(xmlOf(document));
        if (!element.find()) {
            return false;
        }
        String attributes = element.group(1);
        return !attributes.contains("\"false\"") && !attributes.contains("\"0\"")
               && !attributes.contains("\"off\"");
    }

    /**
     * @param document an exported document, which every export of this backend writes the part in
     * @return the text of its settings part
     */
    static String xmlOf(XWPFDocument document) throws Exception {
        PackagePart settings = document.getPackage()
                .getPart(PackagingURIHelper.createPartName("/word/settings.xml"));
        assertThat(settings).as("a settings part, which every export writes").isNotNull();
        try (InputStream input = settings.getInputStream()) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
