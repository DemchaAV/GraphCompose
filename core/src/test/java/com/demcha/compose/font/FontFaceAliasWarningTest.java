package com.demcha.compose.font;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Covers the signal that makes the face-alias trap visible.
 *
 * <p>A standard-14 face constant is an alias of its family, so a style naming one and
 * setting no decoration renders regular without failing, logging or measuring
 * differently. The warning is the whole of what makes that observable, which makes it
 * worth a test rather than a hope.</p>
 *
 * <p><strong>Known limit, asserted here rather than left implicit:</strong> the warning
 * is keyed on the alias alone, not on the pairing of alias and decoration. The library
 * cannot see the decoration at the point it rewrites the name. So the first use of
 * {@code HELVETICA_BOLD} consumes the warning even when that use is correct, and a later
 * broken one is silent. The signal catches a codebase that uses the form, not the
 * individual style that gets it wrong; {@code DocsBoldFaceGuardTest} and
 * {@code FontFaceResolutionTest} cover the specific sites.</p>
 */
class FontFaceAliasWarningTest {

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

    private List<String> warnings() {
        return appender.list.stream()
                .filter(event -> event.getLevel() == Level.WARN)
                .map(ILoggingEvent::getFormattedMessage)
                .toList();
    }

    @Test
    void namingAFaceWarnsOnceAndNamesBothTheConstantAndItsFamily() {
        FontLibrary library = new FontLibrary();

        library.getFont(FontName.HELVETICA_BOLD, Object.class);

        assertThat(warnings())
                .describedAs("naming a face must say so — it is the only signal that the "
                        + "constant carries no weight of its own")
                .hasSize(1);
        assertThat(warnings().get(0))
                .contains(FontName.HELVETICA_BOLD.name())
                .contains(FontName.HELVETICA.name())
                .contains("decoration");
    }

    @Test
    void repeatingTheSameFaceDoesNotRepeatTheWarning() {
        FontLibrary library = new FontLibrary();

        for (int i = 0; i < 50; i++) {
            library.getFont(FontName.HELVETICA_BOLD, Object.class);
        }

        assertThat(warnings())
                .describedAs("the warning is per name, not per lookup: a document that uses the "
                        + "form on every span would otherwise bury its own output")
                .hasSize(1);
    }

    @Test
    void eachFaceGetsItsOwnWarning() {
        FontLibrary library = new FontLibrary();

        library.getFont(FontName.HELVETICA_BOLD, Object.class);
        library.getFont(FontName.TIMES_BOLD, Object.class);
        library.getFont(FontName.COURIER_OBLIQUE, Object.class);

        assertThat(warnings()).hasSize(3);
        assertThat(warnings()).anyMatch(message -> message.contains(FontName.TIMES_ROMAN.name()));
    }

    @Test
    void namingAFamilyIsSilent() {
        FontLibrary library = new FontLibrary();

        library.getFont(FontName.HELVETICA, Object.class);
        library.getFont(FontName.TIMES_ROMAN, Object.class);
        library.getFont(FontName.COURIER, Object.class);

        assertThat(warnings())
                .describedAs("the correct form must not be noisy, or the signal is worthless")
                .isEmpty();
    }
}
