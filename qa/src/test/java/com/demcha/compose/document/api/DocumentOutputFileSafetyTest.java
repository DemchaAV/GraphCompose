package com.demcha.compose.document.api;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.exceptions.AtomicNodeTooLargeException;
import com.demcha.compose.document.image.DocumentImageData;
import com.demcha.compose.document.node.ImageNode;
import com.demcha.compose.document.style.DocumentInsets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import javax.imageio.ImageIO;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The session-level half of the non-destructive output contract: a render that
 * fails must leave the document already at that path exactly as it was.
 *
 * <p>The failure is triggered the way a real one arrives — an atomic node taller
 * than the page, which fails while the layout graph is being compiled, i.e.
 * after the destination would have been opened by the old implementation and
 * before a single output byte exists. Anything published to a server and then
 * re-rendered with bad input hits this exact window.</p>
 *
 * <p>Lives in qa rather than core because reaching the render path at all needs
 * a font-metrics provider, which the lean core has no backend to supply.</p>
 */
class DocumentOutputFileSafetyTest {

    private static final String SENTINEL = "previously published document";

    @Test
    void aFailedRenderLeavesTheExistingDocumentUntouched(@TempDir Path tempDir) throws Exception {
        Path published = tempDir.resolve("report.pdf");
        Files.writeString(published, SENTINEL);

        try (DocumentSession session = oversizedDocument()) {
            assertThatThrownBy(() -> session.buildPdf(published))
                    .isInstanceOf(AtomicNodeTooLargeException.class);
        }

        assertThat(Files.readString(published))
                .describedAs("a failed render must not truncate the document it was replacing")
                .isEqualTo(SENTINEL);
    }

    @Test
    void aFailedRenderLeavesNoScratchFileBehind(@TempDir Path tempDir) throws Exception {
        Path published = tempDir.resolve("report.pdf");
        Files.writeString(published, SENTINEL);

        try (DocumentSession session = oversizedDocument()) {
            assertThatThrownBy(() -> session.buildPdf(published))
                    .isInstanceOf(AtomicNodeTooLargeException.class);
        }

        assertThat(listFiles(tempDir))
                .describedAs("the scratch file must be removed when the render fails")
                .containsExactly(published);
    }

    @Test
    void aSuccessfulRenderReplacesTheDocumentAndLeavesNoScratchFile(@TempDir Path tempDir) throws Exception {
        Path published = tempDir.resolve("report.pdf");
        Files.writeString(published, SENTINEL);

        try (DocumentSession session = GraphCompose.document()
                .pageSize(200, 200)
                .margin(DocumentInsets.of(12))
                .create()) {
            session.pageFlow(page -> page.module("m", module -> module.paragraph("hello")));
            session.buildPdf(published);
        }

        assertThat(Files.readAllBytes(published)).startsWith('%', 'P', 'D', 'F');
        assertThat(listFiles(tempDir)).containsExactly(published);
    }

    /** A document whose only node is taller than the page can ever be. */
    private static DocumentSession oversizedDocument() throws Exception {
        DocumentSession session = GraphCompose.document()
                .pageSize(180, 180)
                .margin(DocumentInsets.of(12))
                .create();
        session.add(new ImageNode(
                "TooTallImage",
                DocumentImageData.fromBytes(onePixelPng()),
                96.0,
                240.0,
                DocumentInsets.zero(),
                DocumentInsets.zero()));
        return session;
    }

    private static byte[] onePixelPng() throws IOException {
        BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ImageIO.write(image, "png", bytes);
        return bytes.toByteArray();
    }

    private static List<Path> listFiles(Path directory) throws IOException {
        try (var entries = Files.list(directory)) {
            return entries.sorted().toList();
        }
    }
}
