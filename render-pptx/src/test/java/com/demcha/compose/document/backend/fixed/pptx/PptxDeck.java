package com.demcha.compose.document.backend.fixed.pptx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.font.FontFamilyDefinition;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Writes a one-page deck through the public session and reads its package back.
 *
 * <p>The font tests ask the same two questions — what did this deck end up carrying, and
 * does any part of it name the family. The walk over the zip entries was written out
 * twice, and so was this render: the two other builds of the same page drive a
 * configured backend rather than {@code toPptxBytes}, so they stay where they are. The
 * tests added beside this class would have made a third copy.</p>
 */
final class PptxDeck {

    private PptxDeck() {
    }

    /** A bundled face's bytes, by its resource path, for a test that needs a real font. */
    static byte[] face(String resource) throws IOException {
        try (java.io.InputStream stream =
                     FontFamilyDefinition.class.getResourceAsStream("/" + resource)) {
            if (stream == null) {
                throw new AssertionError("not on the classpath: " + resource);
            }
            return stream.readAllBytes();
        }
    }

    /** A one-page deck of the given content. */
    static byte[] of(Consumer<PageFlowBuilder> content) {
        return write(List.of(), content);
    }

    /** A one-page deck that registers the given family, drawn with or not. */
    static byte[] registering(FontFamilyDefinition family, Consumer<PageFlowBuilder> content) {
        return write(List.of(family), content);
    }

    private static byte[] write(List<FontFamilyDefinition> families,
                                Consumer<PageFlowBuilder> content) {
        GraphCompose.DocumentBuilder builder = GraphCompose.document()
                .pageSize(400, 160)
                .margin(DocumentInsets.of(20));
        families.forEach(builder::registerFontFamily);
        try (DocumentSession document = builder.create()) {
            document.pageFlow(content::accept);
            return document.toPptxBytes();
        }
    }

    /** The deck's embedded font parts, by name. */
    static List<String> fontParts(byte[] pptx) throws IOException {
        List<String> parts = new ArrayList<>();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(pptx))) {
            for (ZipEntry entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
                if (entry.getName().startsWith("ppt/fonts/")) {
                    parts.add(entry.getName());
                }
            }
        }
        return parts;
    }

    /** One part of the written package, as text. */
    static String part(byte[] pptx, String name) throws IOException {
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(pptx))) {
            for (ZipEntry entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
                if (entry.getName().equals(name)) {
                    return new String(zip.readAllBytes(), StandardCharsets.UTF_8);
                }
            }
        }
        throw new AssertionError("the package has no " + name);
    }

    /**
     * Every part whose bytes carry the family name — a slide that drew text in it, the
     * presentation declaring it embedded, or an embedded program naming itself.
     *
     * <p>Every entry is searched, binary parts included, rather than the places a reference
     * is expected. Decoding as UTF-8 can only lose a match, never invent one — a font
     * program that names itself in UTF-16 is missed — and not inventing one is what the
     * negative assertion this serves, that a deck names a family nowhere, needs of it.</p>
     */
    static List<String> partsNaming(byte[] pptx, String family) throws IOException {
        List<String> naming = new ArrayList<>();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(pptx))) {
            for (ZipEntry entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
                if (new String(zip.readAllBytes(), StandardCharsets.UTF_8).contains(family)) {
                    naming.add(entry.getName());
                }
            }
        }
        return naming;
    }
}
