package com.demcha.examples;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.templates.builtins.InvoiceTemplateV2;
import com.demcha.compose.document.templates.data.invoice.InvoiceDocumentSpec;
import com.demcha.compose.document.theme.BusinessTheme;
import com.demcha.examples.support.ExampleDataFactory;
import com.demcha.examples.support.ExampleOutputPaths;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Phase E.4 — runnable showcase for the canonical
 * {@code DocumentSession.writePdf(OutputStream)} streaming path.
 *
 * <p>The example deliberately keeps Spring out of the
 * {@code graphcompose-examples} module so the artifact stays
 * dependency-light. Instead, it isolates the exact code that any
 * server-side endpoint would call into a single
 * {@link #streamInvoiceTo(InvoiceDocumentSpec, OutputStream)} method,
 * then exercises that method end-to-end against
 * {@link ByteArrayOutputStream} (in {@link #generate()}) so the
 * example actually produces a PDF you can open.</p>
 *
 * <p>To wire the same code into a Spring Boot controller, copy the
 * snippet below into your project — the body delegates to
 * {@link #streamInvoiceTo(InvoiceDocumentSpec, OutputStream)} and the
 * Servlet response stream is the {@code OutputStream} sink:</p>
 *
 * <pre>{@code
 * @RestController
 * @RequestMapping("/api/invoices")
 * public class InvoiceController {
 *
 *     @GetMapping(value = "/{id}.pdf", produces = MediaType.APPLICATION_PDF_VALUE)
 *     public void downloadInvoice(@PathVariable String id,
 *                                 HttpServletResponse response) throws Exception {
 *         InvoiceDocumentSpec invoice = invoiceService.findById(id);
 *
 *         response.setHeader("Content-Disposition",
 *                 "attachment; filename=invoice-" + id + ".pdf");
 *
 *         // GraphCompose writes the PDF bytes to the stream but does
 *         // NOT close it — the Servlet container still owns the stream
 *         // lifecycle.
 *         HttpStreamingExample.streamInvoiceTo(invoice, response.getOutputStream());
 *     }
 * }
 * }</pre>
 *
 * <p>The same pattern works for any sink that exposes an
 * {@link OutputStream}: AWS S3 multipart uploads, GCS resumable
 * uploads, Azure block blobs, Kafka producers wrapped in a custom
 * stream, etc. Because GraphCompose never holds the rendered PDF in
 * memory as a single byte array, the streaming path scales to large
 * documents without the {@code -Xmx} hit of {@code toPdfBytes()}.</p>
 *
 * <p>See {@code docs/recipes/streaming.md} for the full discussion of
 * file vs. stream vs. byte-array output and the trade-offs around
 * caching, retries, and error reporting.</p>
 *
 * @author Artem Demchyshyn
 */
public final class HttpStreamingExample {

    private HttpStreamingExample() {
    }

    /**
     * Renders the supplied invoice into the supplied output stream
     * via {@code InvoiceTemplateV2} on {@link BusinessTheme#modern()}.
     *
     * <p>The stream is intentionally <strong>not closed</strong> by
     * this method — that lets the same code be used from a Servlet,
     * an S3 multipart uploader, or a {@link Files#newOutputStream}
     * call without forcing the caller into a particular stream
     * lifecycle.</p>
     *
     * @param invoice invoice spec to render
     * @param sink    destination stream owned by the caller
     * @throws Exception if PDF rendering fails
     */
    public static void streamInvoiceTo(InvoiceDocumentSpec invoice, OutputStream sink) throws Exception {
        BusinessTheme theme = BusinessTheme.modern();
        InvoiceTemplateV2 template = new InvoiceTemplateV2(theme);

        try (DocumentSession document = GraphCompose.document()
                .pageSize(DocumentPageSize.A4)
                .pageBackground(theme.pageBackground())
                .margin(DocumentInsets.of(28))
                .create()) {
            template.compose(document, invoice);
            document.writePdf(sink);
        }
        // Note: we deliberately do not call sink.close().
        // In a Servlet or storage upload the caller closes it.
    }

    /**
     * Runs the streaming pipeline end-to-end against
     * {@link ByteArrayOutputStream}, then writes the captured bytes
     * to disk so reviewers have a real PDF to open. The byte-buffer
     * acts as a stand-in for {@code response.getOutputStream()} and
     * proves the streaming code path produces a complete, well-formed
     * document.
     *
     * @return path to the generated PDF
     * @throws Exception if PDF rendering or file IO fails
     */
    public static Path generate() throws Exception {
        Path outputFile = ExampleOutputPaths.prepare("invoice-http-stream.pdf");
        try (ByteArrayOutputStream buffer = new ByteArrayOutputStream(64 * 1024)) {
            streamInvoiceTo(ExampleDataFactory.sampleInvoice(), buffer);
            Files.write(outputFile, buffer.toByteArray());
        }
        return outputFile;
    }

    public static void main(String[] args) throws Exception {
        System.out.println("Generated: " + generate());
    }
}
