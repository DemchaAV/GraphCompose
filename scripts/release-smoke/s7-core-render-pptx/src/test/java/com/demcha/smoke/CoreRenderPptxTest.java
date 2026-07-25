package com.demcha.smoke;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.backend.fixed.BackendProviders;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Scenario 7 — {@code graph-compose-core} + {@code graph-compose-render-pptx}
 * resolved from Maven Central produces a real, editable PowerPoint deck.
 *
 * <p>The deck is inspected with {@link ZipInputStream} rather than Apache POI:
 * the point is to prove the <em>published</em> artifacts work for a consumer who
 * installed nothing else, so the scenario must not pull a parsing library of its
 * own to make its assertions pass.</p>
 *
 * <p>An empty file with a valid ZIP header would satisfy a naive smoke test, so
 * the assertions go further: the slide part must exist, carry real text runs, and
 * consist of native shapes rather than one full-slide picture — the failure mode
 * that would mean the backend silently fell back to rasterising everything.</p>
 */
class CoreRenderPptxTest {

    /** EMU per PostScript point, the unit OOXML stores slide dimensions in. */
    private static final int EMU_PER_POINT = 12700;

    @Test
    void coreWithRenderPptxProducesAnEditableDeck() throws Exception {
        byte[] deck;
        try (DocumentSession document = GraphCompose.document()
                .pageSize(DocumentPageSize.SLIDE_16_9)
                .margin(48f, 48f, 48f, 48f)
                .create()) {
            document.add(document.dsl().paragraph()
                    .text("graph-compose-core + graph-compose-render-pptx renders via the SPI.")
                    .build());
            deck = document.toPptxBytes();
        }

        assertThat(deck).isNotEmpty();
        assertThat(new String(deck, 0, 2, StandardCharsets.US_ASCII))
                .describedAs("a .pptx is an OPC package, so it must start with the ZIP signature")
                .isEqualTo("PK");

        Map<String, byte[]> parts = unzip(deck);
        assertThat(parts).containsKey("[Content_Types].xml");
        assertThat(parts)
                .describedAs("one resolved page must become one slide part")
                .containsKey("ppt/slides/slide1.xml");
        assertThat(parts.keySet().stream().filter(name -> name.startsWith("ppt/slides/slide")))
                .hasSize(1);

        String presentation = text(parts.get("ppt/presentation.xml"));
        assertThat(presentation)
                .describedAs("SLIDE_16_9 is 960x540 pt, which OOXML stores in EMU")
                .contains("cx=\"" + (960 * EMU_PER_POINT) + "\"")
                .contains("cy=\"" + (540 * EMU_PER_POINT) + "\"");

        String slide = text(parts.get("ppt/slides/slide1.xml"));
        assertThat(slide)
                .describedAs("the paragraph must land as an editable text run, not as pixels")
                .contains("<a:t>");
        assertThat(countOccurrences(slide, "<p:sp>"))
                .describedAs("the slide must be built from native shapes")
                .isPositive();
        // POI writes both elements without attributes, verified against a generated
        // deck that does contain a picture — so neither assertion is vacuous, and
        // "<p:sp>" cannot accidentally match "<p:spTree" or "<p:spPr>".
        assertThat(countOccurrences(slide, "<p:pic>"))
                .describedAs("a text-only document must produce no pictures at all in vector mode")
                .isZero();
    }

    @Test
    void thePptxProviderIsDiscoveredByFormat() {
        assertThat(BackendProviders.fixedLayout("pptx").format()).isEqualTo("pptx");
    }

    private static Map<String, byte[]> unzip(byte[] archive) throws Exception {
        Map<String, byte[]> parts = new HashMap<>();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(archive))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                zip.transferTo(bytes);
                parts.put(entry.getName(), bytes.toByteArray());
            }
        }
        return parts;
    }

    private static String text(byte[] part) {
        assertThat(part).isNotNull();
        return new String(part, StandardCharsets.UTF_8);
    }

    private static int countOccurrences(String haystack, String needle) {
        int count = 0;
        for (int at = haystack.indexOf(needle); at >= 0; at = haystack.indexOf(needle, at + needle.length())) {
            count++;
        }
        return count;
    }
}
