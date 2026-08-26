package com.demcha.compose.font;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the rule the layout snapshot reports as the resolved font.
 *
 * <p>The declared name and the family the text is actually set in are not always the
 * same, and the two rewrites that separate them are both silent: {@code DEFAULT} becomes
 * {@code HELVETICA}, and a standard-14 face becomes its family. Neither fails, neither
 * measures differently, and neither shows up in the render — so a diagnostic that wants
 * to say "you asked for one thing and got another" has to compute it, which means the
 * rule has to be reachable and has to be exactly the one the library uses internally.</p>
 *
 * <p>{@link FontFaceAliasWarningTest} covers the warning; this covers the value.</p>
 */
class FontFamilyResolutionTest {

    private ListAppender<ILoggingEvent> appender;
    private ch.qos.logback.classic.Logger logger;

    @BeforeEach
    void captureWarnings() {
        FontLibrary.WARNED_FACE_ALIASES.clear();
        logger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(FontLibrary.class);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        logger.setLevel(Level.WARN);
    }

    @AfterEach
    void releaseAppender() {
        logger.detachAppender(appender);
        FontLibrary.WARNED_FACE_ALIASES.clear();
    }

    @Test
    void aFamilyResolvesToItself() {
        assertThat(FontLibrary.resolveFamily(FontName.HELVETICA)).isEqualTo(FontName.HELVETICA);
        assertThat(FontLibrary.resolveFamily(FontName.LATO)).isEqualTo(FontName.LATO);
        assertThat(FontLibrary.resolveFamily(FontName.TIMES_ROMAN)).isEqualTo(FontName.TIMES_ROMAN);
    }

    @Test
    void aStandardFourteenFaceResolvesToItsFamily() {
        // The expensive case: the style names the bold face, the face carries no weight
        // of its own, and the document renders regular while the Java reads correctly.
        assertThat(FontLibrary.resolveFamily(FontName.HELVETICA_BOLD)).isEqualTo(FontName.HELVETICA);
        assertThat(FontLibrary.resolveFamily(FontName.TIMES_BOLD_ITALIC)).isEqualTo(FontName.TIMES_ROMAN);
        assertThat(FontLibrary.resolveFamily(FontName.COURIER_OBLIQUE)).isEqualTo(FontName.COURIER);
    }

    @Test
    void theAbsenceOfAChoiceResolvesToHelvetica() {
        assertThat(FontLibrary.resolveFamily(FontName.DEFAULT)).isEqualTo(FontName.HELVETICA);
        assertThat(FontLibrary.resolveFamily(null)).isEqualTo(FontName.HELVETICA);
    }

    @Test
    void resolvingIsSilent() {
        // A snapshot is taken to look at a document, not to change what it logs. If
        // reading the resolved name warned, every diagnostic pass would fill the output
        // with warnings the render itself had already reported once.
        FontLibrary.resolveFamily(FontName.HELVETICA_BOLD);
        FontLibrary.resolveFamily(FontName.TIMES_BOLD);
        FontLibrary.resolveFamily(FontName.DEFAULT);

        assertThat(appender.list).isEmpty();
    }

    @Test
    void resolvingDoesNotConsumeTheWarningTheRenderStillOwes() {
        // The warning is once-per-name and process-wide. If the pure lookup marked a
        // name as warned, taking a snapshot before rendering would silence the one
        // signal that makes the face-alias trap visible.
        FontLibrary.resolveFamily(FontName.HELVETICA_BOLD);

        new FontLibrary().getFont(FontName.HELVETICA_BOLD, Object.class);

        assertThat(appender.list)
                .describedAs("the render must still warn even though a snapshot looked first")
                .hasSize(1);
    }

    @Test
    void aCustomRegisteredFamilyResolvesToItself() {
        // Only the standard-14 faces are aliased. A font the author registered
        // themselves is whatever they called it, and reporting otherwise would
        // invent a substitution that never happened.
        assertThat(FontLibrary.resolveFamily(FontName.of("Inter"))).isEqualTo(FontName.of("Inter"));
    }

}
