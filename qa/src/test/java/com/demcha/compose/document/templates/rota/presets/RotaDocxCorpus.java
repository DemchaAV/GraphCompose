package com.demcha.compose.document.templates.rota.presets;

import com.demcha.compose.document.templates.fidelity.DocxCorpusDocument;

import java.util.List;

/** The rota preset of the DOCX fidelity corpus. */
public final class RotaDocxCorpus {

    private RotaDocxCorpus() {
    }

    public static List<DocxCorpusDocument> documents() {
        return List.of(new DocxCorpusDocument("rota", "cobalt", -1,
                s -> CobaltRota.create().compose(s, CobaltRotaFixtures.canonicalRota())));
    }
}
