package com.demcha.compose.document.backend.semantic.docx;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.style.DocumentInsets;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Covers the signal for inline runs the DOCX export cannot carry.
 *
 * <p>{@code ParagraphNode.inlineTextRuns()} keeps text and highlight chips but
 * drops image / shape / SVG runs by contract, so a paragraph's icons vanish
 * from the DOCX while its text survives. Block-level drops already warn once
 * per node kind; without the inline mirror the only observable difference
 * between "rendered" and "lost" was opening the file. The warning is the
 * whole of what makes the inline drop visible, which makes it worth a test.</p>
 *
 * <p>Pictures and SVG icons are written now (see {@code DocxInlinePictureTest}); an inline
 * shape — a dot, an arrow — is what is still dropped.</p>
 */
class DocxInlineRunDropWarningTest {

    private ListAppender<ILoggingEvent> appender;
    private ch.qos.logback.classic.Logger logger;

    @BeforeEach
    void captureWarnings() {
        logger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(DocxSemanticBackend.class);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void releaseAppender() {
        logger.detachAppender(appender);
    }

    private List<String> inlineDropWarnings() {
        return appender.list.stream()
                .filter(event -> event.getLevel() == Level.WARN)
                .map(ILoggingEvent::getFormattedMessage)
                .filter(message -> message.contains("dropping inline"))
                .toList();
    }

    @Test
    void droppedInlineShapeRunsWarnOncePerExportAndKeepTheText() throws Exception {
        com.demcha.compose.document.style.DocumentColor ink = com.demcha.compose.document.style.DocumentColor.rgb(255, 0, 0);
        byte[] docx;
        try (DocumentSession document = GraphCompose.document()
                .pageSize(400, 200)
                .margin(DocumentInsets.of(20))
                .create()) {
            document.pageFlow(page -> {
                page.addParagraph(p -> p
                        .inlineText("before ")
                        .dot(6, ink)
                        .inlineText(" after"));
                page.addParagraph(p -> p
                        .inlineText("second ")
                        .dot(6, ink));
            });
            docx = document.export(new DocxSemanticBackend());
        }

        List<String> warned = inlineDropWarnings();
        assertThat(warned).hasSize(1);
        assertThat(warned.get(0)).contains("InlineShapeRun");

        try (XWPFDocument opened = new XWPFDocument(new ByteArrayInputStream(docx))) {
            String text = opened.getParagraphs().stream()
                    .map(paragraph -> paragraph.getText())
                    .reduce("", (a, b) -> a + "\n" + b);
            assertThat(text).contains("before").contains("after").contains("second");
        }
    }

    @Test
    void textOnlyParagraphsStayQuiet() throws Exception {
        try (DocumentSession document = GraphCompose.document()
                .pageSize(400, 200)
                .margin(DocumentInsets.of(20))
                .create()) {
            document.pageFlow(page -> page.addParagraph("plain text only"));
            document.export(new DocxSemanticBackend());
        }

        assertThat(inlineDropWarnings()).isEmpty();
    }
}
