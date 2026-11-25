package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.font.FontFamilyDefinition;
import com.demcha.compose.font.FontName;
import org.apache.poi.openxml4j.opc.PackagePart;
import org.apache.poi.xwpf.usermodel.XWPFDocument;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * What the tests of the font table share: the faces they register, a document exported with
 * them, and the font table and font parts read back out of it.
 *
 * @author Artem Demchyshyn
 */
final class DocxFontTableFixtures {

    static final String LATO_REGULAR = "fonts/google/lato/Lato-Regular.ttf";
    static final String MONO_REGULAR = "fonts/google/ibmplexmono/IBMPlexMono-Regular.ttf";
    static final String MONO_BOLD = "fonts/google/ibmplexmono/IBMPlexMono-Bold.ttf";
    static final String MONO_ITALIC = "fonts/google/ibmplexmono/IBMPlexMono-Italic.ttf";
    static final String PAIR_REGULAR = "fonts/google/poppins/Poppins-Regular.ttf";
    static final String PAIR_BOLD = "fonts/google/poppins/Poppins-Bold.ttf";
    static final String ASAP_REGULAR = "fonts/google/asapcondensed/AsapCondensed-Regular.ttf";
    static final String ASAP_ITALIC = "fonts/google/asapcondensed/AsapCondensed-Italic.ttf";
    /**
     * The names two families are registered under in the tests of one declared name. Two faces of
     * plainly different widths, so which of them a part holds is read off the file and not off a
     * length alone: Lato is proportional, IBM Plex Mono is not.
     */
    static final FontName SHIPPED_LATO = FontName.of("shipped-lato");
    static final FontName SHIPPED_MONO = FontName.of("shipped-mono");
    /**
     * A family of two faces under a name of its own, where a document needs a second element. A
     * third typeface, so that a child's program says which of the three families shipped it.
     */
    static final FontName SHIPPED_PAIR = FontName.of("shipped-pair");
    /** The bundled family whose italic face states a family record its siblings do not. */
    static final FontName SHIPPED_ASAP = FontName.of("shipped-asap");

    private DocxFontTableFixtures() {
    }

    /** A document and what the export said about it. */
    record Shipped(XWPFDocument document, DocxExportReport report) implements AutoCloseable {
        @Override
        public void close() throws java.io.IOException {
            document.close();
        }
    }

    /**
     * Exports a document that registers these families and holds one paragraph per style, in the
     * order given: the order the styles are in is the order the font table reads the faces in.
     */
    static Shipped exportSetIn(List<FontFamilyDefinition> registered,
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

    static byte[] resource(String path) throws Exception {
        try (InputStream in = DocxFontTableFixtures.class.getClassLoader().getResourceAsStream(path)) {
            assertThat(in).as("the bundled font artifact is on the test classpath").isNotNull();
            return in.readAllBytes();
        }
    }

    static DocumentTextStyle bold(FontName name) {
        return DocumentTextStyle.builder().fontName(name).size(10)
                .decoration(DocumentTextDecoration.BOLD).build();
    }

    static byte[] latoRegular() throws Exception {
        return resource(LATO_REGULAR);
    }

    static String fontTable(XWPFDocument document) throws Exception {
        PackagePart part = document.getPackage()
                .getPart(org.apache.poi.openxml4j.opc.PackagingURIHelper
                        .createPartName("/word/fontTable.xml"));
        assertThat(part).as("the document carries a font table").isNotNull();
        try (InputStream in = part.getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    /**
     * One element of the font table: the name it declares, the slots it holds in the order they
     * are written, and the part each of those children points at.
     */
    record FontElement(String name, List<String> slots, List<byte[]> programs) {
    }

    /**
     * The font table's elements, each child tied to the part its own relationship names, so the
     * program a slot carries is read rather than inferred from the order the parts were written.
     */
    static List<FontElement> fontElements(XWPFDocument document) throws Exception {
        String xml = fontTable(document);
        PackagePart table = document.getPackage()
                .getPart(org.apache.poi.openxml4j.opc.PackagingURIHelper
                        .createPartName("/word/fontTable.xml"));
        List<FontElement> elements = new java.util.ArrayList<>();
        java.util.regex.Matcher element = java.util.regex.Pattern
                .compile("<w:font w:name=\"([^\"]+)\">(.*?)</w:font>", java.util.regex.Pattern.DOTALL)
                .matcher(xml);
        while (element.find()) {
            List<String> slots = new java.util.ArrayList<>();
            List<byte[]> programs = new java.util.ArrayList<>();
            java.util.regex.Matcher child = java.util.regex.Pattern
                    .compile("<w:(embed[A-Za-z]+) r:id=\"([^\"]+)\"")
                    .matcher(element.group(2));
            while (child.find()) {
                slots.add(child.group(1));
                PackagePart part = table.getRelatedPart(table.getRelationship(child.group(2)));
                try (InputStream in = part.getInputStream()) {
                    programs.add(in.readAllBytes());
                }
            }
            elements.add(new FontElement(element.group(1), slots, programs));
        }
        // The pattern reads an element with children; one written without any — which `resolve`
        // does not produce, since it adds no `Embedded` without a face — would be skipped rather
        // than reported, and `families` is built on this.
        assertThat(elements).as("every element the part declares was read")
                .hasSize(xml.split("<w:font ", -1).length - 1);
        return elements;
    }

    /** Whether a part holds this font, past the scrambled header the obfuscation writes. */
    static boolean carries(byte[] part, byte[] source) {
        return part.length == source.length
               && java.util.Arrays.equals(part, 32, part.length, source, 32, source.length);
    }

    static void assertCarries(byte[] part, byte[] source) {
        assertThat(part).as("the part is as long as the font it should hold").hasSameSizeAs(source);
        assertThat(carries(part, source))
                .as("the part holds this font, past the scrambled header").isTrue();
    }

    /** The families the font table ships faces for, one entry per element it declares. */
    static List<String> families(XWPFDocument document) throws Exception {
        return fontElements(document).stream().map(FontElement::name).toList();
    }

    static DocumentTextStyle face(FontName name) {
        return DocumentTextStyle.builder().fontName(name).size(10).build();
    }

    static List<byte[]> fontParts(XWPFDocument document) throws Exception {
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
}
