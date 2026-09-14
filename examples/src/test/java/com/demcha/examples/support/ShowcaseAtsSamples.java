package com.demcha.examples.support;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.backend.fixed.pdf.PdfFixedLayoutBackend;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.cv.data.CvDocument;
import com.demcha.compose.document.templates.cv.presets.BlueBanner;
import com.demcha.compose.document.templates.cv.presets.BoxedSections;
import com.demcha.compose.document.templates.cv.presets.CenteredHeadline;
import com.demcha.compose.document.templates.cv.presets.ClassicSerif;
import com.demcha.compose.document.templates.cv.presets.EditorialBlue;
import com.demcha.compose.document.templates.cv.presets.Executive;
import com.demcha.compose.document.templates.cv.presets.MinimalUnderlined;
import com.demcha.compose.document.templates.cv.presets.ModernProfessional;

import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Renders an ATS-badged CV sample for certification: the document its showcase example composes,
 * written through a deterministic PDF backend so that unchanged code renders unchanged bytes.
 *
 * <p>Each badged example composes its preset's default template on A4 at the preset's recommended
 * margin, from {@link ExampleDataFactory#sampleCvDocumentV2()}, and writes it with
 * {@code buildPdf()}. That file carries a time-seeded document ID, so no two renders of it are
 * byte-identical. This class composes the same document and renders it with the dates pinned and
 * the ID derived from the metadata. ShowcaseAtsEvidenceTest holds each example's own file to this
 * render page by page, so the two cannot drift apart.</p>
 */
final class ShowcaseAtsSamples {

    /**
     * How a badged sample is composed.
     *
     * @param template the preset's default template
     * @param margin   the margin its example sets on every side
     */
    private record Sample(Supplier<DocumentTemplate<CvDocument>> template, double margin) {
    }

    private static final Map<String, Sample> SAMPLES = Map.of(
            "cv-blue-banner-v2", new Sample(BlueBanner::create, BlueBanner.RECOMMENDED_MARGIN),
            "cv-boxed-sections-v2", new Sample(BoxedSections::create, BoxedSections.RECOMMENDED_MARGIN),
            "cv-centered-headline-v2", new Sample(CenteredHeadline::create, CenteredHeadline.RECOMMENDED_MARGIN),
            "cv-classic-serif-v2", new Sample(ClassicSerif::create, ClassicSerif.RECOMMENDED_MARGIN),
            "cv-editorial-blue-v2", new Sample(EditorialBlue::create, EditorialBlue.RECOMMENDED_MARGIN),
            "cv-executive-v2", new Sample(Executive::create, Executive.RECOMMENDED_MARGIN),
            "cv-minimal-underlined-v2", new Sample(MinimalUnderlined::create, MinimalUnderlined.RECOMMENDED_MARGIN),
            "cv-modern-professional-v2", new Sample(ModernProfessional::create, ModernProfessional.RECOMMENDED_MARGIN));

    private ShowcaseAtsSamples() {
    }

    /**
     * The cards this class can render for certification.
     *
     * @return their card ids
     */
    static Set<String> renderable() {
        return SAMPLES.keySet();
    }

    /**
     * Renders a badged sample deterministically.
     *
     * @param cardId the showcase card id
     * @return the PDF bytes
     * @throws Exception when the sample cannot be composed or rendered
     */
    static byte[] render(String cardId) throws Exception {
        Sample sample = SAMPLES.get(cardId);
        if (sample == null) {
            throw new IllegalArgumentException(cardId + " has no certification render");
        }
        float margin = (float) sample.margin();
        try (DocumentSession document = GraphCompose.document()
                .pageSize(DocumentPageSize.A4)
                .margin(margin, margin, margin, margin)
                .create()) {
            sample.template().get().compose(document, ExampleDataFactory.sampleCvDocumentV2());
            return document.render(PdfFixedLayoutBackend.builder().deterministic(true).build());
        }
    }
}
