package com.demcha.compose.document.backend.fixed.pdf;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.api.MultiSectionDocument;
import com.demcha.compose.document.node.ShapeNode;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.style.DocumentTextStyle;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.time.Instant;
import java.util.Calendar;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Reproducible-output contract for {@link PdfFixedLayoutBackend}. With
 * {@code deterministic(...)} the same document renders to byte-identical PDF
 * output across runs — the CreationDate / ModDate are pinned and the {@code /ID}
 * is derived from the metadata instead of PDFBox's time-seeded default. The
 * default backend leaves the live timestamp untouched.
 */
class PdfDeterministicOutputTest {

    private static byte[] render(PdfFixedLayoutBackend backend) throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(200, 200)
                .margin(DocumentInsets.of(10))
                .create()) {
            session.add(new ShapeNode("Box", 80, 80, Color.LIGHT_GRAY,
                    DocumentStroke.of(DocumentColor.BLACK, 1), DocumentInsets.zero(), DocumentInsets.zero()));
            return session.render(backend);
        }
    }

    private static byte[] renderText(PdfFixedLayoutBackend backend) throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(240, 180)
                .margin(DocumentInsets.of(12))
                .create()) {
            session.pageFlow()
                    .name("Root")
                    .addParagraph("Reproducible text embeds a font subset.", DocumentTextStyle.DEFAULT)
                    .build();
            return session.render(backend);
        }
    }

    /** Two sections with different page geometry, combined via the public path. */
    private static byte[] renderMultiSection(PdfFixedLayoutBackend backend) throws Exception {
        DocumentSession cover = GraphCompose.document()
                .pageSize(200, 200)
                .margin(DocumentInsets.of(10))
                .create();
        cover.add(new ShapeNode("Cover", 80, 80, Color.LIGHT_GRAY,
                DocumentStroke.of(DocumentColor.BLACK, 1), DocumentInsets.zero(), DocumentInsets.zero()));
        DocumentSession body = GraphCompose.document()
                .pageSize(240, 180)
                .margin(DocumentInsets.of(12))
                .create();
        body.pageFlow()
                .name("Body")
                .addParagraph("Reproducible multi-section body.", DocumentTextStyle.DEFAULT)
                .build();
        try (MultiSectionDocument document = GraphCompose.documents()
                .section(cover)
                .section(body)
                .create()) {
            return document.toPdfBytes(backend);
        }
    }

    @Test
    void deterministicOutputIsByteIdenticalAcrossRuns() throws Exception {
        byte[] first = render(PdfFixedLayoutBackend.builder().deterministic(true).build());
        byte[] second = render(PdfFixedLayoutBackend.builder().deterministic(true).build());

        assertThat(second).isEqualTo(first);
    }

    @Test
    void deterministicOutputIsByteIdenticalForEmbeddedFontText() throws Exception {
        // Text embeds a font subset — the part of the output most likely to vary
        // run-to-run if subset naming were non-deterministic.
        byte[] first = renderText(PdfFixedLayoutBackend.builder().deterministic(true).build());
        byte[] second = renderText(PdfFixedLayoutBackend.builder().deterministic(true).build());

        assertThat(second).isEqualTo(first);
    }

    @Test
    void deterministicOutputIsByteIdenticalForMultiSectionDocuments() throws Exception {
        // The public GraphCompose.documents() path: the configured backend is
        // passed to toPdfBytes(backend) instead of the built-in default.
        byte[] first = renderMultiSection(PdfFixedLayoutBackend.builder().deterministic(true).build());
        byte[] second = renderMultiSection(PdfFixedLayoutBackend.builder().deterministic(true).build());

        assertThat(second).isEqualTo(first);
    }

    @Test
    void deterministicOutputPinsTheCreationDate() throws Exception {
        Instant pinned = Instant.parse("2020-05-15T00:00:00Z");

        byte[] bytes = render(PdfFixedLayoutBackend.builder().deterministic(pinned).build());

        try (PDDocument document = Loader.loadPDF(bytes)) {
            Calendar creation = document.getDocumentInformation().getCreationDate();
            assertThat(creation).isNotNull();
            assertThat(creation.toInstant()).isEqualTo(pinned);
        }
    }

    @Test
    void subSecondTimestampsTruncateToWholeSeconds() throws Exception {
        // PDF dates carry second precision; the builder truncates up front so the
        // serialized dates and the /ID agree — a sub-second instant renders the
        // same bytes as its whole-second truncation.
        byte[] withNanos = render(PdfFixedLayoutBackend.builder()
                .deterministic(Instant.parse("2020-05-15T00:00:00.123456789Z")).build());
        byte[] wholeSecond = render(PdfFixedLayoutBackend.builder()
                .deterministic(Instant.parse("2020-05-15T00:00:00Z")).build());

        assertThat(withNanos).isEqualTo(wholeSecond);
    }

    @Test
    void defaultOutputLeavesTheCreationDateUnset() throws Exception {
        byte[] bytes = render(PdfFixedLayoutBackend.builder().build());

        try (PDDocument document = Loader.loadPDF(bytes)) {
            assertThat(document.getDocumentInformation().getCreationDate()).isNull();
        }
    }
}
