package com.demcha.compose.document.templates.cv.presets;

import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.cv.data.CvDocument;
import com.demcha.compose.document.templates.fidelity.DocxCorpusDocument;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** The CV presets of the DOCX fidelity corpus, each on the fixture its own tests render. */
public final class CvDocxCorpus {

    private CvDocxCorpus() {
    }

    public static List<DocxCorpusDocument> documents() {
        List<DocxCorpusDocument> documents = new ArrayList<>();
        CvPresetFixtures.presets().forEach(arguments -> {
            Object[] preset = arguments.get();
            @SuppressWarnings("unchecked")
            Supplier<DocumentTemplate<CvDocument>> template = (Supplier<DocumentTemplate<CvDocument>>) preset[2];
            documents.add(cv((String) preset[0], (Double) preset[1], template, CvPresetFixtures.canonicalDocument()));
        });
        documents.add(cv("charcoal_gold", 0, CharcoalGold::create, CharcoalGoldFixtures.canonicalCv()));
        documents.add(cv("navy_sidebar", 0, NavySidebar::create, NavySidebarFixtures.canonicalCv()));
        documents.add(cv("slate_orange", -1, SlateOrange::create, SlateOrangeFixtures.canonicalCv()));
        documents.add(cv("midnight_navy", -1, MidnightNavy::create, MidnightNavyFixtures.canonicalCv()));
        documents.add(cv("orange_ops", -1, OrangeOps::create, OrangeOpsFixtures.canonicalCv()));
        documents.add(cv("professional_sidebar", -1, ProfessionalSidebar::create, ProfessionalSidebarFixtures.canonicalCv()));
        documents.add(cv("serif_headline", -1, SerifHeadline::create, SerifHeadlineFixtures.canonicalCv()));
        documents.add(cv("teal_pulse", -1, TealPulse::create, TealPulseFixtures.canonicalCv()));
        documents.add(cv("terracotta_rail", -1, TerracottaRail::create, TerracottaRailFixtures.canonicalCv()));
        documents.add(cv("violet_grid", -1, VioletGrid::create, VioletGridFixtures.canonicalCv()));
        return documents;
    }

    private static DocxCorpusDocument cv(String name, double margin,
                                         Supplier<DocumentTemplate<CvDocument>> template, CvDocument cv) {
        return new DocxCorpusDocument("cv", name, margin, session -> {
            OrangeOpsTestFont.register(session);
            template.get().compose(session, cv);
        });
    }
}
