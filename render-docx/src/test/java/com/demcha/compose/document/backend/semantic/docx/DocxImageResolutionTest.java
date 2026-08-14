package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.image.DocumentImageData;
import com.demcha.compose.document.style.DocumentInsets;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Holds the DOCX export to resolving an image once.
 *
 * <p>Resolving is not free. {@code ImageSourceCache.fromBytes} copies the byte array
 * whole and takes a SHA-256 over it, and a path-sourced image announces its arrival on
 * the way in. {@code writeImage} needs the data twice over — once for the bytes it
 * writes and the intrinsic size it measures against, once for the box the frame gets —
 * and asked for it twice, so a large image paid both costs twice on every export.</p>
 *
 * <p>Counted through the log rather than through a spy: {@link DocumentImageData} is
 * final, so there is no seam to instrument, and the announcement is the one observable
 * the resolution leaves behind. It counts what the copy and the hash cannot be caught
 * doing directly, and it counts it from the outside — nothing is added to production
 * code to make this measurable.</p>
 */
class DocxImageResolutionTest {

    @Test
    void anImageIsResolvedOncePerExport(@TempDir Path dir) throws Exception {
        Path png = dir.resolve("probe.png");
        ImageIO.write(new BufferedImage(40, 20, BufferedImage.TYPE_INT_RGB), "png", png.toFile());

        Logger logger = (Logger) LoggerFactory.getLogger(
                "com.demcha.compose.engine.components.content.ImageData");
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            try (DocumentSession document = GraphCompose.document()
                    .pageSize(400, 200)
                    .margin(DocumentInsets.of(20))
                    .create()) {
                document.pageFlow(page -> page.addImage(image -> image
                        .source(DocumentImageData.fromPath(png))
                        .width(100)));
                document.export(new DocxSemanticBackend());
            }

            List<String> arrivals = appender.list.stream()
                    .map(ILoggingEvent::getFormattedMessage)
                    .filter(message -> message.contains("Create an image from path")
                            && message.contains(png.getFileName().toString()))
                    .toList();

            assertThat(arrivals)
                    .describedAs("one image, one resolution — the sizing pass takes the data "
                            + "the writer already holds instead of resolving it again; "
                            + "messages were %s", arrivals)
                    .hasSize(1);
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
    }
}
