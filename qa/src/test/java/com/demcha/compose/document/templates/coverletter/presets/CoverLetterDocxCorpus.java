package com.demcha.compose.document.templates.coverletter.presets;

import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.coverletter.data.CoverLetterDocument;
import com.demcha.compose.document.templates.fidelity.DocxCorpusDocument;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** The cover-letter presets of the DOCX fidelity corpus, each on the canonical letter. */
public final class CoverLetterDocxCorpus {

    private CoverLetterDocxCorpus() {
    }

    public static List<DocxCorpusDocument> documents() {
        List<DocxCorpusDocument> documents = new ArrayList<>();
        CoverLetterPresetFixtures.presets().forEach(arguments -> {
            Object[] preset = arguments.get();
            @SuppressWarnings("unchecked")
            Supplier<DocumentTemplate<CoverLetterDocument>> template =
                    (Supplier<DocumentTemplate<CoverLetterDocument>>) preset[2];
            documents.add(new DocxCorpusDocument("letter", (String) preset[0], (Double) preset[1],
                    session -> template.get().compose(session, CoverLetterPresetFixtures.canonicalLetter())));
        });
        return documents;
    }
}
