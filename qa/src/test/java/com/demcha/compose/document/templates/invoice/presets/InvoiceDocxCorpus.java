package com.demcha.compose.document.templates.invoice.presets;

import com.demcha.compose.document.templates.data.invoice.InvoiceServiceLines;
import com.demcha.compose.document.templates.data.invoice.StructuredInvoiceData;
import com.demcha.compose.document.templates.fidelity.DocxCorpusDocument;

import java.util.ArrayList;
import java.util.List;

/** The invoice presets of the DOCX fidelity corpus, with the long fixtures that run to more pages. */
public final class InvoiceDocxCorpus {

    private InvoiceDocxCorpus() {
    }

    public static List<DocxCorpusDocument> documents() {
        double classic = ClassicInvoice.RECOMMENDED_MARGIN;
        double modern = ModernInvoice.RECOMMENDED_MARGIN;
        return List.of(
                invoice("classic", classic, s -> ClassicInvoice.create().compose(s, InvoicePresetFixtures.canonicalInvoice())),
                invoice("classic_long", classic, s -> ClassicInvoice.create().compose(s, InvoicePresetFixtures.stressInvoice())),
                invoice("modern", modern, s -> ModernInvoice.create().compose(s, InvoicePresetFixtures.canonicalInvoice())),
                invoice("modern_long", modern, s -> ModernInvoice.create().compose(s, InvoicePresetFixtures.stressInvoice())),
                invoice("consulting", -1,
                        s -> ConsultingInvoice.create().compose(s, ConsultingInvoiceFixtures.canonicalInvoice())),
                invoice("consulting_long", -1,
                        s -> ConsultingInvoice.create().compose(s, ConsultingInvoiceFixtures.overflowInvoice())),
                invoice("luma_studio", -1,
                        s -> LumaStudioInvoice.create().compose(s, LumaStudioInvoiceFixtures.canonicalInvoice())),
                invoice("luma_studio_long", -1,
                        s -> LumaStudioInvoice.create().compose(s, LumaStudioInvoiceFixtures.overflowInvoice())),
                invoice("metered", -1, s -> MeteredInvoice.create().compose(s, MeteredInvoiceFixtures.invoice())),
                // Nine times its line items: the layout carries the gap above the closing row onto page 4.
                invoice("metered_long", -1, s -> MeteredInvoice.create()
                        .compose(s, withLinesRepeated(MeteredInvoiceFixtures.invoice(), 9))),
                invoice("merchant", -1, s -> MerchantInvoice.create().compose(s, MerchantInvoiceFixtures.invoice())),
                invoice("obsidian", -1, s -> ObsidianInvoice.create().compose(s, ObsidianInvoiceFixtures.invoice())),
                // Twelve times its line items: the card holding them runs over four pages, the pages
                // after the first set against other margins, and the layout breaks page 5 between
                // the info cards and the footer divider.
                invoice("obsidian_long", -1, s -> ObsidianInvoice.create()
                        .compose(s, withLinesRepeated(ObsidianInvoiceFixtures.invoice(), 12))),
                invoice("platform", -1, s -> PlatformInvoice.create().compose(s, PlatformInvoiceFixtures.invoice())),
                invoice("payments", -1,
                        s -> PaymentsInvoice.create().compose(s, PaymentsInvoiceFixtures.canonicalInvoice())),
                invoice("subscription", -1,
                        s -> SubscriptionInvoice.create().compose(s, SubscriptionInvoiceFixtures.invoice())),
                invoice("workspace", -1,
                        s -> WorkspaceInvoice.create().compose(s, WorkspaceInvoiceFixtures.canonicalInvoice())));
    }

    /** The invoice with its line items repeated {@code times}. */
    static StructuredInvoiceData withLinesRepeated(StructuredInvoiceData data, int times) {
        List<InvoiceServiceLines.Line> lines = new ArrayList<>();
        for (int time = 0; time < times; time++) {
            lines.addAll(data.serviceLines().lines());
        }
        return new StructuredInvoiceData(data.brand(), data.supplier(), data.masthead(),
                data.billTo(), data.shipTo(), data.summary(),
                new InvoiceServiceLines(data.serviceLines().columns(), lines), data.totals(), data.payment(),
                data.notes(), data.currencyCode());
    }

    private static DocxCorpusDocument invoice(String name, double margin,
                                              java.util.function.Consumer<com.demcha.compose.document.api.DocumentSession> compose) {
        return new DocxCorpusDocument("invoice", name, margin, compose);
    }
}
