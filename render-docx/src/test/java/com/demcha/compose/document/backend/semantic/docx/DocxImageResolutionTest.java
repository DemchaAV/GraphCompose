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

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFPictureData;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
    void theWriterResolvesAnImageOnce(@TempDir Path dir) throws Exception {
        Path png = dir.resolve("probe.png");
        ImageIO.write(new BufferedImage(40, 20, BufferedImage.TYPE_INT_RGB), "png", png.toFile());

        assertThat(resolutionsOf(png, false))
                .describedAs("one image, one resolution — the sizing pass takes the data the "
                        + "writer already holds instead of resolving it again")
                .hasSize(1);
    }

    @Test
    void askingForTheLayoutResolvesItAgain(@TempDir Path dir) throws Exception {
        // Worth stating rather than discovering: the export asks for the compiled layout,
        // and compiling one measures every image for itself. The writer's own share is
        // still the single resolution above — this is what the request costs on top, and
        // it is the same work a PDF render of the document does.
        Path png = dir.resolve("probe.png");
        ImageIO.write(new BufferedImage(40, 20, BufferedImage.TYPE_INT_RGB), "png", png.toFile());

        assertThat(resolutionsOf(png, true))
                .describedAs("the writer's one, plus the layout's own")
                .hasSizeGreaterThan(1);
    }

    /**
     * How many times exporting that image reads it.
     *
     * @param png         the image the document carries
     * @param withSession true to export through a session, which compiles a layout first
     */
    private static List<String> resolutionsOf(Path png, boolean withSession) throws Exception {
        Logger logger = (Logger) LoggerFactory.getLogger(
                "com.demcha.compose.engine.components.content.ImageData");
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            java.util.function.Consumer<com.demcha.compose.document.dsl.PageFlowBuilder> content =
                    page -> page.addImage(image -> image
                            .source(DocumentImageData.fromPath(png))
                            .width(100));
            if (withSession) {
                DocxExports.withLayout(400, 200, 20, content).close();
            } else {
                DocxExports.withoutLayout(400, 200, 20, content).close();
            }
            return appender.list.stream()
                    .map(ILoggingEvent::getFormattedMessage)
                    .filter(message -> message.contains("Create an image from path")
                            && message.contains(png.getFileName().toString()))
                    .toList();
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
    }

    @Test
    void theEmbeddedBytesAndTheSizeComeFromOneResolution(@TempDir Path dir) throws Exception {
        // The source cache keys on the path alone, with no regard for what the file has
        // since become. Reading the file separately therefore did not merely read it
        // twice: after a first render warmed the cache, the bytes came fresh off disk
        // while the metadata sizing the frame came from the version before it, and the
        // document carried a picture drawn at the wrong image's dimensions.
        Path png = dir.resolve("swap.png");
        ImageIO.write(new BufferedImage(40, 20, BufferedImage.TYPE_INT_RGB), "png", png.toFile());
        export(png);

        // Same path, different image. Anything reading the file now disagrees with
        // anything reading the cache.
        ImageIO.write(new BufferedImage(90, 30, BufferedImage.TYPE_INT_RGB), "png", png.toFile());
        byte[] docx = export(png);

        BufferedImage embedded = ImageIO.read(new ByteArrayInputStream(onlyPicture(docx)));
        assertThat(embedded.getWidth())
                .describedAs("the embedded picture must be the one the frame was sized "
                        + "from — both come from a single resolution, so the export is "
                        + "consistent with itself even when the file has moved on")
                .isEqualTo(40);
        assertThat(embedded.getHeight()).isEqualTo(20);
    }

    @Test
    void anImageThatCannotBeReadFailsTheExportRatherThanVanishing(@TempDir Path dir) {
        // Not a behaviour this backend chose: the old byte read swallowed its exception
        // and returned nothing, which the length check then read as "nothing to draw".
        // A document that silently comes back one picture short is the worst of the
        // available answers, and it was reachable for a path that simply was not there.
        Path absent = dir.resolve("never-written.png");

        // Named rather than left as any Exception: what this pins is that the failure is
        // the source read, so a later change that starts failing here for some other
        // reason cannot pass by throwing something else. Matched on the phrase, not the
        // whole message — the rest of it is a temp-directory path.
        assertThatThrownBy(() -> export(absent))
                .describedAs("an unreadable source stops the export instead of quietly "
                        + "removing the image from the document")
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Failed to read image bytes");
    }

    /** The bytes of the document's only embedded picture. */
    private static byte[] onlyPicture(byte[] docx) throws Exception {
        try (XWPFDocument word = new XWPFDocument(new ByteArrayInputStream(docx))) {
            List<XWPFPictureData> pictures = word.getAllPictures();
            assertThat(pictures).describedAs("one image in, one picture out").hasSize(1);
            return pictures.get(0).getData();
        }
    }

    private static byte[] export(Path png) throws Exception {
        try (DocumentSession document = GraphCompose.document()
                .pageSize(400, 200)
                .margin(DocumentInsets.of(20))
                .create()) {
            document.pageFlow(page -> page.addImage(image -> image
                    .source(DocumentImageData.fromPath(png))
                    .width(100)));
            return document.export(new DocxSemanticBackend());
        }
    }
}
