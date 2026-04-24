package com.demcha.compose.document.diagnostics;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.templates.builtins.CvTemplateV1;
import com.demcha.mock.CvDocumentSpecMock;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LifecycleLoggingTest {

    @Test
    void shouldEmitLifecycleLogsForTemplateLayoutSnapshotAndPdfBuild() throws Exception {
        try (CapturedLogger documentLog = CapturedLogger.debug("com.demcha.compose.document.lifecycle");
             CapturedLogger templateLog = CapturedLogger.debug("com.demcha.compose.templates.lifecycle");
             CapturedLogger layoutLog = CapturedLogger.debug("com.demcha.compose.engine.layout");
             CapturedLogger paginationLog = CapturedLogger.debug("com.demcha.compose.engine.pagination");
             CapturedLogger renderLog = CapturedLogger.debug("com.demcha.compose.engine.render")) {
            Path outputFile = Files.createTempFile("graphcompose-lifecycle", ".pdf");
            DocumentSession document = GraphCompose.document(outputFile).create();

            try {
                new CvTemplateV1().compose(document, new CvDocumentSpecMock().getCv());
                document.layoutSnapshot();
                byte[] bytes = document.toPdfBytes();
                document.buildPdf();
                document.close();

                assertThat(bytes).isNotEmpty();
                assertThat(outputFile).isNotEmptyFile();
                assertThat(documentLog.messages()).anyMatch(message -> message.contains("document.session.created"));
                assertThat(documentLog.messages()).anyMatch(message -> message.contains("document.layoutSnapshot.start"));
                assertThat(documentLog.messages()).anyMatch(message -> message.contains("document.pdf.bytes.end"));
                assertThat(documentLog.messages()).anyMatch(message -> message.contains("document.pdf.build.start"));
                assertThat(documentLog.messages()).anyMatch(message -> message.contains("document.pdf.build.end"));
                assertThat(documentLog.messages()).anyMatch(message -> message.contains("document.session.close.end"));
                assertThat(templateLog.messages()).anyMatch(message -> message.contains("template.compose.start"));
                assertThat(templateLog.messages()).anyMatch(message -> message.contains("template.compose.end"));
                assertThat(templateLog.messages()).anyMatch(message -> message.contains("template.module.compose.start"));
                assertThat(templateLog.messages()).anyMatch(message -> message.contains("template.module.compose.end"));
                assertThat(layoutLog.messages()).anyMatch(message -> message.contains("layout.compile.start"));
                assertThat(layoutLog.messages()).anyMatch(message -> message.contains("layout.compile.end"));
                assertThat(paginationLog.messages()).anyMatch(message -> message.contains("pagination.compile.start"));
                assertThat(paginationLog.messages()).anyMatch(message -> message.contains("pagination.compile.end"));
                assertThat(renderLog.messages()).anyMatch(message -> message.contains("render.pdf.fixed.start"));
                assertThat(renderLog.messages()).anyMatch(message -> message.contains("render.pdf.fixed.end"));
            } finally {
                try {
                    document.close();
                } catch (Exception ignored) {
                    // Best-effort cleanup after assertion failures.
                }
                Files.deleteIfExists(outputFile);
            }
        }
    }

    private static final class CapturedLogger implements AutoCloseable {
        private final Logger logger;
        private final Level previousLevel;
        private final boolean previousAdditive;
        private final ListAppender<ILoggingEvent> appender;

        private CapturedLogger(String name) {
            this.logger = (Logger) LoggerFactory.getLogger(name);
            this.previousLevel = logger.getLevel();
            this.previousAdditive = logger.isAdditive();
            this.appender = new ListAppender<>();
            this.appender.start();
            this.logger.setLevel(Level.DEBUG);
            this.logger.setAdditive(false);
            this.logger.addAppender(appender);
        }

        private static CapturedLogger debug(String name) {
            return new CapturedLogger(name);
        }

        private List<String> messages() {
            return appender.list.stream()
                    .map(ILoggingEvent::getFormattedMessage)
                    .toList();
        }

        @Override
        public void close() {
            logger.detachAppender(appender);
            appender.stop();
            logger.setLevel(previousLevel);
            logger.setAdditive(previousAdditive);
        }
    }
}
