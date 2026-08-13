package com.demcha.compose.document.backend.fixed.pptx;

import static org.assertj.core.api.Assertions.assertThat;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.node.TextDirection;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.backend.fixed.FixedLayoutRenderContext;
import com.demcha.compose.document.backend.fixed.SectionUnit;
import com.demcha.compose.document.layout.LayoutGraph;
import com.demcha.compose.font.FontName;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

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

    @Test
    void aFamilyTheDeckCarriesIsNotReportedAsOneTheReaderMustInstall() throws Exception {
        // The warning fires while a run is drawn; the embedding happens after the last
        // one. Left alone, a deck that carries Amiri told its author "not registered with
        // binary sources ... register the family to embed it" — the opposite of what the
        // file ends up containing, and nothing in the suite could see it because no test
        // read the log.
        List<String> warnings = substitutionWarningsFrom(true);

        assertThat(warnings)
                .describedAs("a carried family is not a substituted one")
                .noneMatch(message -> message.contains("Amiri"));
    }

    @Test
    void decliningToCarryThemStillReportsWhatTheReaderMustInstall() throws Exception {
        // The other half: a render told not to carry the fonts genuinely does reference
        // them by name, so the reader does need to install them — and saying so is right.
        assertThat(substitutionWarningsFrom(false))
                .describedAs("declined, so the family really is name-only")
                .anyMatch(message -> message.contains("Amiri"));
    }

    /** The substitution warnings a render emits for a bundled Arabic family. */
    private static List<String> substitutionWarningsFrom(boolean carry) throws Exception {
        Logger logger = (Logger) LoggerFactory.getLogger("com.demcha.compose.engine.render");
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            renderWith(PptxFixedLayoutBackend.builder().embedBundledFonts(carry).build(),
                    FontName.AMIRI, "مرحبا");
            return appender.list.stream()
                    .map(ILoggingEvent::getFormattedMessage)
                    .filter(message -> message.startsWith("render.pptx.font.substitution"))
                    .toList();
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
    }

    /** Renders the Georgian deck through a configured backend. */
    private static byte[] renderWith(PptxFixedLayoutBackend backend) throws Exception {
        return renderWith(backend, FontName.NOTO_SANS_GEORGIAN, "გამარჯობა");
    }

    private static byte[] renderWith(PptxFixedLayoutBackend backend, FontName font, String text)
            throws Exception {
        try (DocumentSession document = GraphCompose.document()
                .pageSize(400, 160)
                .margin(DocumentInsets.of(20))
                .create()) {

            document.pageFlow(page -> page.addParagraph(p -> p
                    .text(text)
                    .textStyle(DocumentTextStyle.builder().fontName(font).size(18).build())));

            LayoutGraph graph = document.render(new GraphCapturingBackend());
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            backend.write(graph, new FixedLayoutRenderContext(graph.canvas(), List.of(), null, output));
            return output.toByteArray();
        }
    }

    @Test
    void aSectionedDeckCarriesThemToo() throws Exception {
        // renderSections is its own assembly with its own environment and its own write,
        // and it had no embed call at all — a sectioned deck showed the reported boxes.
        // Driven through the @Beta backend surface directly, because the convenience
        // MultiSectionDocument path hands every section a PDF chrome and a PPTX render
        // rejects that before it gets here.
        PptxFixedLayoutBackend backend = PptxFixedLayoutBackend.builder().build();
        try (DocumentSession document = GraphCompose.document()
                .pageSize(400, 160).margin(DocumentInsets.of(20)).create()) {

            document.pageFlow(page -> page.addParagraph(p -> p
                    .text("გამარჯობა")
                    .textStyle(DocumentTextStyle.builder()
                            .fontName(FontName.NOTO_SANS_GEORGIAN).size(18).build())));

            LayoutGraph graph = document.render(new GraphCapturingBackend());
            byte[] combined = backend.renderSections(List.of(
                    new SectionUnit(graph, graph.canvas(), List.of(), backend)));

            assertThat(fontPartsOf(combined))
                    .describedAs("a sectioned deck carries what a single-document one carries")
                    .isNotEmpty();
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
