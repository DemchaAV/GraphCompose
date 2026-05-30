package com.demcha.examples.templates.coverletter;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.coverletter.v2.data.CoverLetterDocument;
import com.demcha.compose.document.templates.coverletter.v2.presets.BlueBannerLetter;
import com.demcha.compose.document.templates.coverletter.v2.presets.BoxedSectionsLetter;
import com.demcha.compose.document.templates.coverletter.v2.presets.CenteredHeadlineLetter;
import com.demcha.compose.document.templates.coverletter.v2.presets.ClassicSerifLetter;
import com.demcha.compose.document.templates.coverletter.v2.presets.CompactMonoLetter;
import com.demcha.compose.document.templates.coverletter.v2.presets.EditorialBlueLetter;
import com.demcha.compose.document.templates.coverletter.v2.presets.EngineeringResumeLetter;
import com.demcha.compose.document.templates.coverletter.v2.presets.ExecutiveLetter;
import com.demcha.compose.document.templates.coverletter.v2.presets.MintEditorialLetter;
import com.demcha.compose.document.templates.coverletter.v2.presets.ModernProfessionalLetter;
import com.demcha.compose.document.templates.coverletter.v2.presets.MonogramSidebarLetter;
import com.demcha.compose.document.templates.coverletter.v2.presets.NordicCleanLetter;
import com.demcha.compose.document.templates.coverletter.v2.presets.PanelLetter;
import com.demcha.compose.document.templates.coverletter.v2.presets.SidebarPortraitLetter;
import com.demcha.compose.document.templates.coverletter.v2.presets.TimelineMinimalLetter;
import com.demcha.examples.support.ExampleDataFactory;
import com.demcha.examples.support.ExampleOutputPaths;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Renders all 15 layered cover-letter presets ({@code
 * coverletter.v2.presets.*} — the polished current standard) against
 * the same shared sample {@link CoverLetterDocument}. Each PDF lands in
 * {@code examples/target/generated-pdfs/templates/coverletter/cover-letter-<id>.pdf}
 * where {@code <id>} is the paired CV preset's stable identifier (e.g.
 * {@code cover-letter-modern-professional.pdf}).
 *
 * <p>The 15 letter renders match the 15 CV renders in
 * {@link com.demcha.examples.templates.cv.CvTemplateGalleryFileExample}
 * — a writer can render both galleries and pick a CV / cover-letter
 * pair sharing the same visual signature.</p>
 */
public final class CoverLetterTemplateGalleryFileExample {

    private CoverLetterTemplateGalleryFileExample() {
    }

    /**
     * Renders all 15 layered cover-letter preset gallery PDFs.
     *
     * @return list of absolute paths of the rendered PDFs in source
     *         order
     * @throws Exception if any rendering fails
     */
    public static List<Path> generate() throws Exception {
        return generate(null);
    }

    /**
     * Renders one preset (when {@code presetId} matches its slug) or all
     * presets when {@code presetId} is null.
     *
     * @param presetId slug to render exclusively, or null to render all
     * @return list of absolute paths of the rendered PDFs
     * @throws Exception if any rendering fails
     */
    public static List<Path> generate(String presetId) throws Exception {
        // The slug strips the "-letter" suffix so the example file name
        // matches the paired CV (cover-letter-modern-professional.pdf
        // pairs with cv-modern-professional.pdf).
        List<Run> runs = List.of(
                run("modern-professional", ModernProfessionalLetter.RECOMMENDED_MARGIN, ModernProfessionalLetter::create),
                run("nordic-clean", NordicCleanLetter.RECOMMENDED_MARGIN, NordicCleanLetter::create),
                run("classic-serif", ClassicSerifLetter.RECOMMENDED_MARGIN, ClassicSerifLetter::create),
                run("compact-mono", CompactMonoLetter.RECOMMENDED_MARGIN, CompactMonoLetter::create),
                run("executive", ExecutiveLetter.RECOMMENDED_MARGIN, ExecutiveLetter::create),
                run("engineering-resume", EngineeringResumeLetter.RECOMMENDED_MARGIN, EngineeringResumeLetter::create),
                run("timeline-minimal", TimelineMinimalLetter.RECOMMENDED_MARGIN, TimelineMinimalLetter::create),
                run("boxed-sections", BoxedSectionsLetter.RECOMMENDED_MARGIN, BoxedSectionsLetter::create),
                run("centered-headline", CenteredHeadlineLetter.RECOMMENDED_MARGIN, CenteredHeadlineLetter::create),
                run("blue-banner", BlueBannerLetter.RECOMMENDED_MARGIN, BlueBannerLetter::create),
                run("editorial-blue", EditorialBlueLetter.RECOMMENDED_MARGIN, EditorialBlueLetter::create),
                run("panel", PanelLetter.RECOMMENDED_MARGIN, PanelLetter::create),
                run("sidebar-portrait", SidebarPortraitLetter.RECOMMENDED_MARGIN, SidebarPortraitLetter::create),
                run("monogram-sidebar", MonogramSidebarLetter.RECOMMENDED_MARGIN, MonogramSidebarLetter::create),
                run("mint-editorial", MintEditorialLetter.RECOMMENDED_MARGIN, MintEditorialLetter::create));

        CoverLetterDocument doc = ExampleDataFactory.sampleCoverLetterDocumentV2();
        List<Path> generated = new ArrayList<>();
        for (Run letter : runs) {
            if (presetId != null && !letter.id().equals(presetId)) {
                continue;
            }
            generated.add(renderOne(doc, letter));
        }
        return List.copyOf(generated);
    }

    /**
     * Renders all layered cover-letter preset gallery PDFs and prints
     * each path.
     *
     * @param args optional first arg = slug filter
     * @throws Exception if any rendering fails
     */
    public static void main(String[] args) throws Exception {
        String presetId = args.length == 0 ? null : args[0];
        for (Path output : generate(presetId)) {
            System.out.println("Generated: " + output);
        }
    }

    private static Path renderOne(CoverLetterDocument doc, Run letter) throws Exception {
        Path outputFile = ExampleOutputPaths.prepare(
                "templates/coverletter", "cover-letter-" + letter.id() + ".pdf");
        DocumentTemplate<CoverLetterDocument> template = letter.factory().get();

        float m = (float) letter.margin();
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
                           Supplier<DocumentTemplate<CoverLetterDocument>> factory) {
        return new Run(id, margin, factory);
    }

    private record Run(String id, double margin,
                       Supplier<DocumentTemplate<CoverLetterDocument>> factory) {
    }
}
