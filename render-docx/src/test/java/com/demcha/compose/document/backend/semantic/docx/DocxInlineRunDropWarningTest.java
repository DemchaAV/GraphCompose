package com.demcha.compose.document.backend.semantic.docx;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.svg.SvgIcon;
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
 */
class DocxInlineRunDropWarningTest {

    private static final String ICON = """
            <svg viewBox="0 0 10 10"><rect width="10" height="10" fill="#ff0000"/></svg>""";

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
    void droppedInlineSvgRunsWarnOncePerExportAndKeepTheText() throws Exception {
        SvgIcon icon = SvgIcon.parse(ICON);
        byte[] docx;
        try (DocumentSession document = GraphCompose.document()
                .pageSize(400, 200)
                .margin(DocumentInsets.of(20))
                .create()) {
            document.pageFlow(page -> {
                page.addParagraph(p -> p
                        .inlineText("before ")
                        .inlineSvgIcon(icon, 10)
                        .inlineText(" after"));
                page.addParagraph(p -> p
                        .inlineText("second ")
                        .inlineSvgIcon(icon, 10));
            });
            docx = document.export(new DocxSemanticBackend());
        }

        List<String> warned = inlineDropWarnings();
        assertThat(warned).hasSize(1);
        assertThat(warned.get(0)).contains("InlineSvgRun");

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
