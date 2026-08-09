package com.demcha.examples.templates.receipt;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.core.identity.SvgGlyph;
import com.demcha.compose.document.templates.core.theme.BrandTheme;
import com.demcha.compose.document.templates.data.receipt.ReceiptDocumentSpec;
import com.demcha.compose.document.templates.receipt.presets.ModernReceipt;
import com.demcha.examples.support.ExampleDataFactory;
import com.demcha.examples.support.ExampleOutputPaths;

import java.nio.file.Path;

/**
 * Renders the layered {@code receipt} Modern Receipt preset — a settled
 * direct-debit collection from a fictional bank.
 *
 * <p>Output:
 * {@code examples/target/generated-pdfs/templates/receipt/receipt-modern.pdf}.</p>
 *
 * <p>The branding is the part worth reading: the theme
 * ({@link BrandTheme#receiptModern()}) carries no brand colour at all, and
 * the issuer's identity arrives through {@code ModernReceipt.Options} as a
 * recolourable SVG mark plus one accent. Point those two at a different
 * institution and the same preset renders that institution's
 * confirmation.</p>
 */
public final class ModernReceiptExample {

    /** Classpath location of the fictional issuer's mark. */
    private static final String MARK = "/brand/northwind-pay-mark.svg";

    /** The fictional issuer's brand accent. */
    private static final DocumentColor ACCENT = DocumentColor.rgb(23, 92, 211);

    private ModernReceiptExample() {
    }

    /**
     * @return absolute path of the rendered PDF
     * @throws Exception if rendering fails
     */
    public static Path generate() throws Exception {
        Path outputFile = ExampleOutputPaths.prepare("templates/receipt", "receipt-modern.pdf");
        ReceiptDocumentSpec spec = ExampleDataFactory.sampleReceipt();
        BrandTheme theme = BrandTheme.receiptModern();

        // A symbol mark, not a wordmark: it renders inline beside the issuer's
        // name, so it is sized to the name's cap height rather than to a
        // wordmark's full width.
        ModernReceipt.Options options = ModernReceipt.Options
                .branded(SvgGlyph.fromResource(MARK), ACCENT)
                .withLogoWidth(14)
                .withLogoColor(ACCENT);
        DocumentTemplate<ReceiptDocumentSpec> template = ModernReceipt.create(theme, options);

        float m = (float) ModernReceipt.RECOMMENDED_MARGIN;
        try (DocumentSession document = GraphCompose.document(outputFile)
                .pageSize(DocumentPageSize.A4)
                .pageBackground(theme.palette().mainFill())
                .margin(m, m, m, m)
                .create()) {
            template.compose(document, spec);
            document.buildPdf();
        }
        return outputFile;
    }

    /**
     * @param args ignored
     * @throws Exception if rendering fails
     */
    public static void main(String[] args) throws Exception {
        System.out.println("Generated: " + generate());
    }
}
