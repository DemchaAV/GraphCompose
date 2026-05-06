package com.demcha.examples;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.cv.presets.BlueBanner;
import com.demcha.compose.document.templates.cv.presets.BoxedSections;
import com.demcha.compose.document.templates.cv.presets.CenteredHeadline;
import com.demcha.compose.document.templates.cv.presets.ClassicSerif;
import com.demcha.compose.document.templates.cv.presets.CompactMono;
import com.demcha.compose.document.templates.cv.presets.EditorialBlue;
import com.demcha.compose.document.templates.cv.presets.EngineeringResume;
import com.demcha.compose.document.templates.cv.presets.Executive;
import com.demcha.compose.document.templates.cv.presets.ModernProfessional;
import com.demcha.compose.document.templates.cv.presets.MonogramSidebar;
import com.demcha.compose.document.templates.cv.presets.NordicClean;
import com.demcha.compose.document.templates.cv.presets.Panel;
import com.demcha.compose.document.templates.cv.presets.SidebarPortrait;
import com.demcha.compose.document.templates.cv.presets.TimelineMinimal;
import com.demcha.compose.document.templates.cv.spec.CvSpec;
import com.demcha.compose.document.theme.BusinessTheme;
import com.demcha.examples.support.ExampleDataFactory;
import com.demcha.examples.support.ExampleOutputPaths;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Renders all 14 Templates v2 CV presets against the same shared
 * sample data. Each PDF lands in
 * {@code examples/target/generated-pdfs/cv-<id>.pdf} where
 * {@code <id>} is the preset's stable identifier (e.g.
 * {@code cv-modern-professional.pdf}).
 *
 * <p>This is the single source of truth for the example CV gallery
 * in v2. The matching cover-letter gallery lives in
 * {@link CoverLetterTemplateGalleryFileExample}.</p>
 */
public final class CvTemplateGalleryFileExample {

    private static final BusinessTheme THEME = BusinessTheme.modern();

    private CvTemplateGalleryFileExample() {
    }

    /**
     * Renders all 14 v2 CV preset gallery PDFs.
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
                run(NordicClean.ID, 28.0, NordicClean::create),
                run(ClassicSerif.ID, 28.0, ClassicSerif::create),
                run(CompactMono.ID, 28.0, CompactMono::create),
                run(Executive.ID, 28.0, Executive::create),
                run(EngineeringResume.ID, 28.0, EngineeringResume::create),
                run(TimelineMinimal.ID, 28.0, TimelineMinimal::create),
                run(BoxedSections.ID, 28.0, BoxedSections::create),
                run(CenteredHeadline.ID, 28.0, CenteredHeadline::create),
                run(BlueBanner.ID, 28.0, BlueBanner::create),
                run(EditorialBlue.ID, 28.0, EditorialBlue::create),
                run(Panel.ID, 28.0, Panel::create),
                run(SidebarPortrait.ID, 28.0, SidebarPortrait::create),
                run(MonogramSidebar.ID, 28.0, MonogramSidebar::create));

        CvSpec spec = ExampleDataFactory.sampleCvSpecV2();
        List<Path> generated = new ArrayList<>();
        for (Run cv : runs) {
            if (presetId != null && !cv.id.equals(presetId)) {
                continue;
            }
            generated.add(renderOne(spec, cv));
        }
        return List.copyOf(generated);
    }

    /**
     * Renders all v2 CV preset gallery PDFs and prints each path.
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

    private static Path renderOne(CvSpec spec, Run cv) throws Exception {
        Path outputFile = ExampleOutputPaths.prepare("cv-" + cv.id + ".pdf");
        DocumentTemplate<CvSpec> template = cv.factory.apply(THEME);

        float m = (float) cv.margin;
        try (DocumentSession document = GraphCompose.document(outputFile)
                .pageSize(DocumentPageSize.A4)
                .margin(m, m, m, m)
                .create()) {
            template.compose(document, spec);
            document.buildPdf();
        }
        return outputFile;
    }

    private static Run run(String id, double margin, Function<BusinessTheme, DocumentTemplate<CvSpec>> factory) {
        return new Run(id, margin, factory);
    }

    private record Run(String id, double margin, Function<BusinessTheme, DocumentTemplate<CvSpec>> factory) {
    }
}
