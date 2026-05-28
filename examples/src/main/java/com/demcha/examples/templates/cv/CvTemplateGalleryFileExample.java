package com.demcha.examples.templates.cv;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.cv.v2.data.CvDocument;
import com.demcha.compose.document.templates.cv.v2.presets.BlueBanner;
import com.demcha.compose.document.templates.cv.v2.presets.BoxedSections;
import com.demcha.compose.document.templates.cv.v2.presets.CenteredHeadline;
import com.demcha.compose.document.templates.cv.v2.presets.ClassicSerif;
import com.demcha.compose.document.templates.cv.v2.presets.CompactMono;
import com.demcha.compose.document.templates.cv.v2.presets.EditorialBlue;
import com.demcha.compose.document.templates.cv.v2.presets.EngineeringResume;
import com.demcha.compose.document.templates.cv.v2.presets.Executive;
import com.demcha.compose.document.templates.cv.v2.presets.ModernProfessional;
import com.demcha.compose.document.templates.cv.v2.presets.MonogramSidebar;
import com.demcha.compose.document.templates.cv.v2.presets.NordicClean;
import com.demcha.compose.document.templates.cv.v2.presets.Panel;
import com.demcha.compose.document.templates.cv.v2.presets.SidebarPortrait;
import com.demcha.compose.document.templates.cv.v2.presets.TimelineMinimal;
import com.demcha.examples.support.ExampleDataFactory;
import com.demcha.examples.support.ExampleOutputPaths;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Renders all 14 layered CV presets ({@code cv.v2.presets.*} — the
 * polished current standard) against the same shared sample
 * {@link CvDocument}. Each PDF lands in
 * {@code examples/target/generated-pdfs/templates/cv/cv-<id>.pdf}
 * where {@code <id>} is the preset's stable identifier (e.g.
 * {@code cv-modern-professional.pdf}).
 *
 * <p>This is the single source of truth for the CV showcase gallery.
 * The matching cover-letter gallery lives in
 * {@link com.demcha.examples.templates.coverletter.CoverLetterTemplateGalleryFileExample}.</p>
 */
public final class CvTemplateGalleryFileExample {

    private CvTemplateGalleryFileExample() {
    }

    /**
     * Renders all 14 layered CV preset gallery PDFs.
     *
     * @return list of absolute paths of the rendered PDFs in source
     *         order
     * @throws Exception if any rendering fails
     */
    public static List<Path> generate() throws Exception {
        return generate(null);
    }

    /**
     * Renders one preset (when {@code presetId} matches its stable id)
     * or all presets when {@code presetId} is null.
     *
     * @param presetId stable preset id to render exclusively, or null
     *                 to render all presets
     * @return list of absolute paths of the rendered PDFs
     * @throws Exception if any rendering fails
     */
    public static List<Path> generate(String presetId) throws Exception {
        List<Run> runs = List.of(
                run(ModernProfessional.ID, ModernProfessional.RECOMMENDED_MARGIN, ModernProfessional::create),
                run(NordicClean.ID, NordicClean.RECOMMENDED_MARGIN, NordicClean::create),
                run(ClassicSerif.ID, ClassicSerif.RECOMMENDED_MARGIN, ClassicSerif::create),
                run(CompactMono.ID, CompactMono.RECOMMENDED_MARGIN, CompactMono::create),
                run(Executive.ID, Executive.RECOMMENDED_MARGIN, Executive::create),
                run(EngineeringResume.ID, EngineeringResume.RECOMMENDED_MARGIN, EngineeringResume::create),
                run(TimelineMinimal.ID, TimelineMinimal.RECOMMENDED_MARGIN, TimelineMinimal::create),
                run(BoxedSections.ID, BoxedSections.RECOMMENDED_MARGIN, BoxedSections::create),
                run(CenteredHeadline.ID, CenteredHeadline.RECOMMENDED_MARGIN, CenteredHeadline::create),
                run(BlueBanner.ID, BlueBanner.RECOMMENDED_MARGIN, BlueBanner::create),
                run(EditorialBlue.ID, EditorialBlue.RECOMMENDED_MARGIN, EditorialBlue::create),
                run(Panel.ID, Panel.RECOMMENDED_MARGIN, Panel::create),
                run(SidebarPortrait.ID, SidebarPortrait.RECOMMENDED_MARGIN, SidebarPortrait::create),
                run(MonogramSidebar.ID, MonogramSidebar.RECOMMENDED_MARGIN, MonogramSidebar::create));

        CvDocument doc = ExampleDataFactory.sampleCvDocumentV2();
        List<Path> generated = new ArrayList<>();
        for (Run cv : runs) {
            if (presetId != null && !cv.id().equals(presetId)) {
                continue;
            }
            generated.add(renderOne(doc, cv));
        }
        return List.copyOf(generated);
    }

    /**
     * Renders all layered CV preset gallery PDFs and prints each path.
     *
     * @param args optional first arg = preset id filter
     * @throws Exception if any rendering fails
     */
    public static void main(String[] args) throws Exception {
        String presetId = args.length == 0 ? null : args[0];
        for (Path output : generate(presetId)) {
            System.out.println("Generated: " + output);
        }
    }

    private static Path renderOne(CvDocument doc, Run cv) throws Exception {
        Path outputFile = ExampleOutputPaths.prepare("templates/cv", "cv-" + cv.id() + ".pdf");
        DocumentTemplate<CvDocument> template = cv.factory().get();

        float m = (float) cv.margin();
        try (DocumentSession document = GraphCompose.document(outputFile)
                .pageSize(DocumentPageSize.A4)
                .margin(m, m, m, m)
                .create()) {
            template.compose(document, doc);
            document.buildPdf();
        }
        return outputFile;
    }

    private static Run run(String id, double margin,
                           Supplier<DocumentTemplate<CvDocument>> factory) {
        return new Run(id, margin, factory);
    }

    private record Run(String id, double margin,
                       Supplier<DocumentTemplate<CvDocument>> factory) {
    }
}
