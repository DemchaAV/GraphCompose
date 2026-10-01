package com.demcha.compose.document.templates.invoice.presets;

import com.demcha.compose.document.templates.fidelity.DocxCorpusDocument;

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
                invoice("merchant", -1, s -> MerchantInvoice.create().compose(s, MerchantInvoiceFixtures.invoice())),
                invoice("obsidian", -1, s -> ObsidianInvoice.create().compose(s, ObsidianInvoiceFixtures.invoice())),
                invoice("platform", -1, s -> PlatformInvoice.create().compose(s, PlatformInvoiceFixtures.invoice())),
                invoice("payments", -1,
                        s -> PaymentsInvoice.create().compose(s, PaymentsInvoiceFixtures.canonicalInvoice())),
                invoice("subscription", -1,
                        s -> SubscriptionInvoice.create().compose(s, SubscriptionInvoiceFixtures.invoice())),
                invoice("workspace", -1,
                        s -> WorkspaceInvoice.create().compose(s, WorkspaceInvoiceFixtures.canonicalInvoice())));
    }

    private static DocxCorpusDocument invoice(String name, double margin,
                                              java.util.function.Consumer<com.demcha.compose.document.api.DocumentSession> compose) {
        return new DocxCorpusDocument("invoice", name, margin, compose);
    }
}
