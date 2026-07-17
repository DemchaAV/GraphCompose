package com.demcha.compose.document.backend.fixed.pptx;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.demcha.compose.document.backend.fixed.pdf.PdfMeasurementResources;
import com.demcha.compose.font.FontFamilyDefinition;
import com.demcha.compose.font.FontName;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A font that cannot travel into the deck identically must say so: standard-14
 * families warn about their metric-compatible replacement, unregistered names
 * warn that the deck carries a name-only reference, and each family warns once
 * per render pass. An embedded family stays silent.
 */
class PptxFontSubstitutionWarningTest {

    private static final FontName EMBEDDED = FontName.of("WarnLato");
    private static final FontFamilyDefinition EMBEDDED_FAMILY = FontFamilyDefinition.classpath(
                    EMBEDDED, "fonts/google/lato/Lato-Regular.ttf")
            .wordFamily("Lato")
            .build();

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
    void substitutedFamiliesWarnOncePerRenderPassAndEmbeddedFamiliesStaySilent() throws Exception {
        try (XMLSlideShow show = new XMLSlideShow();
             PdfMeasurementResources measurement = PdfMeasurementResources.open(List.of(EMBEDDED_FAMILY))) {
            PptxRenderSession session = new PptxRenderSession(show, 300, 200, 1);
            PptxRenderEnvironment environment = new PptxRenderEnvironment(
                    show, session, 0, 200, measurement.fontLibrary(), List.of(EMBEDDED_FAMILY));

            assertThat(environment.fontFamily(FontName.HELVETICA)).isEqualTo("Arial");
            assertThat(environment.fontFamily(FontName.HELVETICA)).isEqualTo("Arial");
            assertThat(environment.fontFamily(FontName.of("Roboto-Regular"))).isEqualTo("Roboto");
            assertThat(environment.fontFamily(FontName.of("Roboto-Bold")))
                    .as("facet of an already-warned family")
                    .isEqualTo("Roboto");
            assertThat(environment.fontFamily(EMBEDDED)).isEqualTo("Lato");

            List<String> warnings = appender.list.stream()
                    .map(ILoggingEvent::getFormattedMessage)
                    .filter(message -> message.startsWith("render.pptx.font.substitution"))
                    .toList();
            assertThat(warnings)
                    .as("one warning per substituted family, none for the embedded one")
                    .hasSize(2);
            assertThat(warnings.get(0))
                    .contains("Helvetica")
                    .contains("Arial")
                    .contains("metric-compatible");
            assertThat(warnings.get(1))
                    .contains("Roboto-Regular")
                    .contains("by name");
            assertThat(warnings).noneMatch(message -> message.contains("WarnLato"));
        }
    }
}
