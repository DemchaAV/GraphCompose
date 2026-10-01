package com.demcha.compose.document.templates.receipt.presets;

import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.templates.core.theme.BrandTheme;
import com.demcha.compose.document.templates.fidelity.DocxCorpusDocument;

import java.util.List;

/** The receipt preset of the DOCX fidelity corpus. */
public final class ReceiptDocxCorpus {

    private ReceiptDocxCorpus() {
    }

    public static List<DocxCorpusDocument> documents() {
        return List.of(new DocxCorpusDocument("receipt", "modern", ModernReceipt.RECOMMENDED_MARGIN,
                s -> ModernReceipt.create(BrandTheme.receiptModern(),
                                ModernReceipt.Options.branded(null, DocumentColor.rgb(23, 92, 211)))
                        .compose(s, ReceiptFixtures.canonicalReceipt())));
    }
}
