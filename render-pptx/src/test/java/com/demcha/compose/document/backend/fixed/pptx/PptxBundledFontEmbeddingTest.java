package com.demcha.compose.document.backend.fixed.pptx;

import static org.assertj.core.api.Assertions.assertThat;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.node.TextDirection;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.backend.fixed.FixedLayoutRenderContext;
import com.demcha.compose.document.layout.LayoutGraph;
import com.demcha.compose.font.FontName;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Holds a deck to carrying the bundled fonts it drew with.
 *
 * <p>A family a caller registers is embedded, and warned about when it cannot be. A family
 * this library <em>ships</em> was neither: the deck named it and embedded nothing, so a
 * viewer without it installed substituted — and for Georgian, Armenian or Hangul, where a
 * substitute rarely covers the script, the slide showed boxes. The asymmetry was the sharp
 * edge, because the shipped families are exactly what a deck reaches for when the reader is
 * least likely to have the font.</p>
 *
 * <p>Only what was drawn is embedded. The bundled set is dozens of families and embedding
 * here is whole-font, so a deck that carried all of them would be tens of megabytes — which
 * is what the last case is about.</p>
 */
class PptxBundledFontEmbeddingTest {

    @Test
    void aDeckDrawnWithABundledFamilyCarriesIt() throws Exception {
        List<String> parts = fontPartsOf(render(page -> page.addParagraph(p -> p
                .text("გამარჯობა")
                .textStyle(DocumentTextStyle.builder()
                        .fontName(FontName.NOTO_SANS_GEORGIAN).size(18).build()))));

        assertThat(parts)
                .describedAs("without this the deck names Noto Sans Georgian and ships "
                        + "nothing, so a viewer that lacks it draws boxes")
                .isNotEmpty();
    }

    @Test
    void aRightToLeftDeckCarriesTheFamilyThatMakesItReadable() throws Exception {
        assertThat(fontPartsOf(render(page -> page.addParagraph(p -> p
                .text("مرحبا بالعالم")
                .direction(TextDirection.RTL)
                .textStyle(DocumentTextStyle.builder()
                        .fontName(FontName.AMIRI).size(18).build())))))
                .isNotEmpty();
    }

    @Test
    void aDeckCarriesOnlyTheFamiliesItDrewWith() throws Exception {
        // The bundled set runs to dozens of families and embedding is whole-font, so a deck
        // that offered all of them rather than the ones it used would be tens of megabytes.
        byte[] one = render(page -> page.addParagraph(p -> p
                .text("გამარჯობა")
                .textStyle(DocumentTextStyle.builder()
                        .fontName(FontName.NOTO_SANS_GEORGIAN).size(18).build())));
        byte[] two = render(page -> page
                .addParagraph(p -> p.text("გამარჯობა")
                        .textStyle(DocumentTextStyle.builder()
                                .fontName(FontName.NOTO_SANS_GEORGIAN).size(18).build()))
                .addParagraph(p -> p.text("Բարև աշխարհ")
                        .textStyle(DocumentTextStyle.builder()
                                .fontName(FontName.NOTO_SANS_ARMENIAN).size(18).build())));

        assertThat(fontPartsOf(two))
                .describedAs("the second family is carried because it was drawn with")
                .hasSizeGreaterThan(fontPartsOf(one).size());
    }

    @Test
    void aStandardFamilyIsNamedRatherThanCarried() throws Exception {
        // Helvetica and its siblings are the viewer's by definition; carrying one would be
        // weight for nothing, and there is no binary to carry in the first place.
        assertThat(fontPartsOf(render(page -> page.addParagraph(p -> p
                .text("Plain Latin text")
                .textStyle(DocumentTextStyle.builder()
                        .fontName(FontName.HELVETICA).size(18).build())))))
                .describedAs("a deck of standard-14 text embeds nothing")
                .isEmpty();
    }

    @Test
    void aDeckCanBeAskedToCarryNothing() throws Exception {
        // The escape hatch, and the reason it exists: embedding is whole-font, so the
        // shipped five-script catalogue goes from 27 KB to 3 MB. A deck whose readers are
        // known to have the fonts can decline that.
        assertThat(fontPartsOf(renderWith(PptxFixedLayoutBackend.builder()
                .embedBundledFonts(false)
                .build())))
                .describedAs("declined, so the deck names the family and carries none")
                .isEmpty();
    }

    /** Renders the Georgian deck through a configured backend. */
    private static byte[] renderWith(PptxFixedLayoutBackend backend) throws Exception {
        try (DocumentSession document = GraphCompose.document()
                .pageSize(400, 160)
                .margin(DocumentInsets.of(20))
                .create()) {

            document.pageFlow(page -> page.addParagraph(p -> p
                    .text("გამარჯობა")
                    .textStyle(DocumentTextStyle.builder()
                            .fontName(FontName.NOTO_SANS_GEORGIAN).size(18).build())));

            LayoutGraph graph = document.render(new GraphCapturingBackend());
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            backend.write(graph, new FixedLayoutRenderContext(graph.canvas(), List.of(), null, output));
            return output.toByteArray();
        }
    }

    /** The presentation's embedded font parts, by name. */
    private static List<String> fontPartsOf(byte[] pptx) throws Exception {
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

    private static byte[] render(Consumer<com.demcha.compose.document.dsl.PageFlowBuilder> content) {
        try (DocumentSession document = GraphCompose.document()
                .pageSize(400, 160)
                .margin(DocumentInsets.of(20))
                .create()) {

            document.pageFlow(content::accept);
            return document.toPptxBytes();
        }
    }
}
