package com.demcha.compose.document.templates.data.rota;

/**
 * Public compose-first input for rota templates.
 *
 * <p><b>Authoring role:</b> the document-level object rota presets are
 * parameterised on, the way structured invoice presets are parameterised on
 * {@code StructuredInvoiceDocumentSpec}.</p>
 *
 * @param rota normalized rota content
 * @since 2.4.0
 */
public record StructuredRotaDocumentSpec(StructuredRotaData rota) {

    /**
     * Creates a normalized rota document spec.
     */
    public StructuredRotaDocumentSpec {
        rota = rota == null ? StructuredRotaData.builder().build() : rota;
    }

    /**
     * Wraps existing rota data in the document-level spec expected by rota
     * templates.
     *
     * @param rota rota data
     * @return document spec
     */
    public static StructuredRotaDocumentSpec from(StructuredRotaData rota) {
        return new StructuredRotaDocumentSpec(rota);
    }
}
