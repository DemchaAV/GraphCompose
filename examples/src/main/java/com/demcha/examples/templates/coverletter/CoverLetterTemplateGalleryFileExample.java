package com.demcha.examples.templates.coverletter;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.coverletter.presets.BlueBannerLetter;
import com.demcha.compose.document.templates.coverletter.presets.BoxedSectionsLetter;
import com.demcha.compose.document.templates.coverletter.presets.CenteredHeadlineLetter;
import com.demcha.compose.document.templates.coverletter.presets.ClassicSerifLetter;
import com.demcha.compose.document.templates.coverletter.presets.CompactMonoLetter;
import com.demcha.compose.document.templates.coverletter.presets.EditorialBlueLetter;
import com.demcha.compose.document.templates.coverletter.presets.EngineeringResumeLetter;
import com.demcha.compose.document.templates.coverletter.presets.ExecutiveLetter;
import com.demcha.compose.document.templates.coverletter.presets.ModernProfessionalLetter;
import com.demcha.compose.document.templates.coverletter.presets.MonogramSidebarLetter;
import com.demcha.compose.document.templates.coverletter.presets.NordicCleanLetter;
import com.demcha.compose.document.templates.coverletter.presets.PanelLetter;
import com.demcha.compose.document.templates.coverletter.presets.SidebarPortraitLetter;
import com.demcha.compose.document.templates.coverletter.presets.TimelineMinimalLetter;
import com.demcha.compose.document.templates.coverletter.spec.CoverLetterSpec;
import com.demcha.compose.document.theme.BusinessTheme;
import com.demcha.examples.support.ExampleDataFactory;
import com.demcha.examples.support.ExampleOutputPaths;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Renders all 14 Templates v2 cover-letter presets against the same
 * shared sample data. Each PDF lands in
 * {@code examples/target/generated-pdfs/cover-letter-<id>.pdf}
 * where {@code <id>} is the paired CV preset's stable identifier
 * (e.g. {@code cover-letter-modern-professional.pdf}).
 *
 * <p>The 14 letter renders match the 14 CV renders in
 * {@link CvTemplateGalleryFileExample} — a writer can render both
 * galleries and pick a CV / cover-letter pair sharing the same
 * visual signature.</p>
 */
public final class CoverLetterTemplateGalleryFileExample {

    private static final BusinessTheme THEME = BusinessTheme.modern();

    private CoverLetterTemplateGalleryFileExample() {
    }

    /**
     * Renders all 14 v2 cover-letter preset gallery PDFs.
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
                // Stable id stripped of the "-letter" suffix so the
                // example file name matches the paired CV (e.g.
                // cover-letter-modern-professional.pdf pairs with
                // cv-modern-professional.pdf).
                run("modern-professional", ModernProfessionalLetter::create),
                run("nordic-clean", NordicCleanLetter::create),
                run("classic-serif", ClassicSerifLetter::create),
                run("compact-mono", CompactMonoLetter::create),
                run("executive", ExecutiveLetter::create),
                run("engineering-resume", EngineeringResumeLetter::create),
                run("timeline-minimal", TimelineMinimalLetter::create),
                run("boxed-sections", BoxedSectionsLetter::create),
                run("centered-headline", CenteredHeadlineLetter::create),
                run("blue-banner", BlueBannerLetter::create),
                run("editorial-blue", EditorialBlueLetter::create),
                run("panel", PanelLetter::create),
                run("sidebar-portrait", SidebarPortraitLetter::create),
                run("monogram-sidebar", MonogramSidebarLetter::create));

        CoverLetterSpec spec = ExampleDataFactory.sampleCoverLetterSpecV2();
        List<Path> generated = new ArrayList<>();
        for (Run letter : runs) {
            if (presetId != null && !letter.id.equals(presetId)) {
                continue;
            }
            generated.add(renderOne(spec, letter));
        }
        return List.copyOf(generated);
    }

    /**
     * Renders all v2 cover-letter preset gallery PDFs and prints each
     * path.
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

    private static Path renderOne(CoverLetterSpec spec, Run letter) throws Exception {
        Path outputFile = ExampleOutputPaths.prepare("templates/coverletter", "cover-letter-" + letter.id + ".pdf");
        DocumentTemplate<CoverLetterSpec> template = letter.factory.apply(THEME);

        try (DocumentSession document = GraphCompose.document(outputFile)
                .pageSize(DocumentPageSize.A4)
                .margin(48, 48, 48, 48)
                .create()) {
            template.compose(document, spec);
            document.buildPdf();
        }
        return outputFile;
    }

    private static Run run(String id, Function<BusinessTheme, DocumentTemplate<CoverLetterSpec>> factory) {
        return new Run(id, factory);
    }

    private record Run(String id, Function<BusinessTheme, DocumentTemplate<CoverLetterSpec>> factory) {
    }
}
