package com.demcha.compose.document.backend.fixed.pptx;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.font.FontFamilyDefinition;
import com.demcha.compose.font.FontName;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * A registered font family that cannot be carried costs the deck nothing but the font.
 *
 * <p>Embedding is best-effort by contract: a family whose licence forbids it, or whose
 * bytes cannot be read, degrades to a plain family-name reference rather than failing a
 * render the PDF backend would complete. A source may refuse <em>unchecked</em> — the
 * classpath one reports a missing resource with an {@link IllegalArgumentException} — and
 * only the checked refusal was caught, so a family registered and never drawn with was
 * enough to lose a deck that had already laid out.</p>
 *
 * <p>Degrading is not the same as failing fast. A family the deck actually draws in still
 * fails the render, because the page cannot measure a face it cannot read; the second test
 * pins that side, so widening the catch cannot turn a loud failure into a silent one.</p>
 *
 * <p>And degrading is for the family, not the face: the third test holds a family whose
 * bold face cannot be read to leaving nothing of itself in the deck, since one boolean
 * answers for the whole family.</p>
 */
class PptxUnreadableFontSourceTest {

    /** A resource no module on the test classpath ships. */
    private static final String ABSENT = "fonts/absent/Absent-Regular.ttf";

    private ListAppender<ILoggingEvent> appender;
    private Logger logger;

    @BeforeEach
    void attachAppender() {
        logger = (Logger) LoggerFactory.getLogger("com.demcha.compose.engine.render");
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void detachAppender() {
        logger.detachAppender(appender);
        appender.stop();
    }

    @Test
    void aRegisteredFamilyWhoseClasspathResourceIsMissingIsNamedAndTheDeckIsWritten() throws Exception {
        // Anchored on the class the production source reads through, not on this test's.
        assertThat(FontFamilyDefinition.class.getResource("/" + ABSENT))
                .as("the premise: the resource is not on the classpath")
                .isNull();

        byte[] deck = render(page -> page.addParagraph(paragraph -> paragraph
                .text("Drawn in the default face")));

        assertThat(deck).as("the deck is written").isNotEmpty();
        assertThat(slideText(deck))
                .as("the premise: the page laid out, and the slide carries its text")
                .contains("Drawn in the default face");
        assertThat(fontParts(deck))
                .as("the face that could not be read, not carried")
                .isEmpty();
        assertThat(part(deck, "ppt/presentation.xml"))
                .as("and the presentation declares no embedded font it has not got")
                .doesNotContain("embeddedFontLst")
                .doesNotContain("Absent");

        assertThat(warnings("render.pptx.font.embed.skipped"))
                .as("the face it could not carry is named, with why")
                .singleElement(InstanceOfAssertFactories.STRING)
                .contains("family=Absent")
                .contains(ABSENT)
                .as("the premise: the source refused unchecked, the path this test guards")
                .contains("IllegalArgumentException");
        assertThat(warnings("render.pptx.font.substitution"))
                .as("and the reader is told the deck does not carry it")
                .anySatisfy(warning -> assertThat(warning).contains("Absent"));
    }

    @Test
    void aDeckDrawnInAFamilyWhoseClasspathResourceIsMissingStillFailsTheRender() {
        // The other side of the contract. Embedding is best-effort, measuring is not: a face
        // the page has to set text in cannot be substituted silently, so the render fails and
        // names the resource. Widening the embed catch must not reach this.
        assertThatThrownBy(() -> render(page -> page.addParagraph(paragraph -> paragraph
                .text("Set in a face that stays home")
                .textStyle(DocumentTextStyle.builder()
                        .fontName(FontName.of("Absent")).size(10).build()))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(ABSENT);

        assertThat(warnings("render.pptx.font.embed.skipped"))
                .as("the premise: measurement refused before a slide was built, so embedding "
                        + "never ran and the widened catch cannot be what let this through")
                .isEmpty();
    }

    @Test
    void aFamilyOneOfWhoseFacesCannotBeReadIsCarriedWholeOrNotAtAll() throws Exception {
        // Embedding answers for the family, not the face: one boolean decides whether the
        // viewer metrics are kept and whether the reader is told to install the font. So a
        // family whose bold face cannot be read must leave nothing of itself in the deck —
        // otherwise the warning says the deck references the family by name only while the
        // deck is in fact carrying its regular face.
        String present = "fonts/google/lato/Lato-Regular.ttf";
        FontFamilyDefinition half = FontFamilyDefinition.classpath("Half", present)
                .boldResource(ABSENT)
                .wordFamily("Half")
                .build();

        // The premise, asserted rather than assumed: the regular face reads and the bold one
        // does not, so the failure arrives with a program already in hand. Were the builder
        // to fill every slot from one source, this test would quietly become a second copy of
        // the first and stop guarding the order at all.
        assertThat(FontFamilyDefinition.class.getResource("/" + present)).isNotNull();
        assertThat(half.fontSourceSet().orElseThrow())
                .satisfies(sources -> {
                    assertThat(sources.regular().description()).isEqualTo(present);
                    assertThat(sources.bold().description()).isEqualTo(ABSENT);
                });

        byte[] deck = renderWith(half,
                page -> page.addParagraph(paragraph -> paragraph.text("Drawn in the default face")));

        assertThat(deck).as("the deck is written").isNotEmpty();
        assertThat(fontParts(deck))
                .as("the readable face of a family that could not be carried whole")
                .isEmpty();
        assertThat(part(deck, "ppt/presentation.xml"))
                .as("and the presentation declares no embedded font it has not got")
                .doesNotContain("embeddedFontLst")
                .doesNotContain("Half");

        assertThat(warnings("render.pptx.font.embed.skipped"))
                .as("the face it could not read is named")
                .singleElement(InstanceOfAssertFactories.STRING)
                .contains(ABSENT);
        assertThat(warnings("render.pptx.font.substitution"))
                .as("and the reader is told the deck does not carry the family")
                .anySatisfy(warning -> assertThat(warning).contains("Half"));
    }

    /** Renders a one-page deck that registers the absent family, drawn with or not. */
    private static byte[] render(Consumer<PageFlowBuilder> content) {
        return renderWith(FontFamilyDefinition.classpath("Absent", ABSENT).build(), content);
    }

    /** Renders a one-page deck that registers the given family, drawn with or not. */
    private static byte[] renderWith(FontFamilyDefinition family, Consumer<PageFlowBuilder> content) {
        try (DocumentSession document = GraphCompose.document()
                .pageSize(400, 160)
                .margin(DocumentInsets.of(20))
                .registerFontFamily(family)
                .create()) {

            document.pageFlow(content::accept);
            return document.toPptxBytes();
        }
    }

    /** One part of the written package, as text. */
    private static String part(byte[] pptx, String name) throws Exception {
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(pptx))) {
            for (ZipEntry entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
                if (entry.getName().equals(name)) {
                    return new String(zip.readAllBytes(), StandardCharsets.UTF_8);
                }
            }
        }
        throw new AssertionError("the package has no " + name);
    }

    private List<String> warnings(String key) {
        return appender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .filter(message -> message.startsWith(key))
                .toList();
    }

    /** The presentation's embedded font parts, by name. */
    private static List<String> fontParts(byte[] pptx) throws Exception {
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

    /** Every run of text on the deck's slides. */
    private static List<String> slideText(byte[] pptx) throws Exception {
        List<String> text = new ArrayList<>();
        try (XMLSlideShow show = new XMLSlideShow(new ByteArrayInputStream(pptx))) {
            show.getSlides().forEach(slide -> slide.getShapes().forEach(shape -> {
                if (shape instanceof XSLFTextShape textShape) {
                    text.add(textShape.getText());
                }
            }));
        }
        return text;
    }
}
