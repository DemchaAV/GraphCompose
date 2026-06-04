package com.demcha.examples;

import com.demcha.examples.features.barcodes.BarcodeShowcaseExample;
import com.demcha.examples.features.canvas.CanvasLayerExample;
import com.demcha.examples.features.chrome.PdfChromeExample;
import com.demcha.examples.features.lists.NestedListExample;
import com.demcha.examples.features.shapes.ShapeContainerExample;
import com.demcha.examples.features.snapshots.LayoutSnapshotRegressionExample;
import com.demcha.examples.features.streaming.HttpStreamingExample;
import com.demcha.examples.features.tables.ComposedTableCellExample;
import com.demcha.examples.features.tables.TableAdvancedExample;
import com.demcha.examples.features.text.InlineShapesExample;
import com.demcha.examples.features.text.RichTextShowcaseExample;
import com.demcha.examples.features.text.SectionPresetsExample;
import com.demcha.examples.features.themes.CustomBusinessThemeExample;
import com.demcha.examples.features.transforms.TransformsExample;
import com.demcha.examples.flagships.BusinessReportExample;
import com.demcha.examples.flagships.MasterShowcaseExample;
import com.demcha.examples.flagships.ModuleFirstFileExample;
import com.demcha.examples.templates.coverletter.v2.CvBlueBannerLetterV2Example;
import com.demcha.examples.templates.coverletter.v2.CvBoxedSectionsLetterV2Example;
import com.demcha.examples.templates.coverletter.v2.CvCenteredHeadlineLetterV2Example;
import com.demcha.examples.templates.coverletter.v2.CvClassicSerifLetterV2Example;
import com.demcha.examples.templates.coverletter.v2.CvCompactMonoLetterV2Example;
import com.demcha.examples.templates.coverletter.v2.CvEditorialBlueLetterV2Example;
import com.demcha.examples.templates.coverletter.v2.CvEngineeringResumeLetterV2Example;
import com.demcha.examples.templates.coverletter.v2.CvExecutiveLetterV2Example;
import com.demcha.examples.templates.coverletter.v2.CvMintEditorialLetterV2Example;
import com.demcha.examples.templates.coverletter.v2.CvModernProfessionalLetterV2Example;
import com.demcha.examples.templates.coverletter.v2.CvMonogramSidebarLetterV2Example;
import com.demcha.examples.templates.coverletter.v2.CvNordicCleanLetterV2Example;
import com.demcha.examples.templates.coverletter.v2.CvPanelLetterV2Example;
import com.demcha.examples.templates.coverletter.v2.CvSidebarPortraitLetterV2Example;
import com.demcha.examples.templates.coverletter.v2.CvTimelineMinimalLetterV2Example;
import com.demcha.examples.templates.cv.v2.CvBlueBannerExample;
import com.demcha.examples.templates.cv.v2.CvBoxedV2Example;
import com.demcha.examples.templates.cv.v2.CvCenteredHeadlineExample;
import com.demcha.examples.templates.cv.v2.CvClassicSerifExample;
import com.demcha.examples.templates.cv.v2.CvCompactMonoExample;
import com.demcha.examples.templates.cv.v2.CvEditorialBlueExample;
import com.demcha.examples.templates.cv.v2.CvEngineeringResumeExample;
import com.demcha.examples.templates.cv.v2.CvExecutiveExample;
import com.demcha.examples.templates.cv.v2.CvMinimalUnderlinedExample;
import com.demcha.examples.templates.cv.v2.CvMintEditorialExample;
import com.demcha.examples.templates.cv.v2.CvModernV2Example;
import com.demcha.examples.templates.cv.v2.CvMonogramSidebarExample;
import com.demcha.examples.templates.cv.v2.CvNordicCleanExample;
import com.demcha.examples.templates.cv.v2.CvPanelExample;
import com.demcha.examples.templates.cv.v2.CvSidebarPortraitExample;
import com.demcha.examples.templates.cv.v2.CvTimelineMinimalExample;
import com.demcha.examples.templates.invoice.InvoiceCinematicFileExample;
import com.demcha.examples.templates.invoice.InvoiceFileExample;
import com.demcha.examples.templates.proposal.CinematicProposalFileExample;
import com.demcha.examples.templates.proposal.ProposalCinematicFileExample;
import com.demcha.examples.templates.proposal.ProposalFileExample;
import com.demcha.examples.templates.schedule.WeeklyScheduleFileExample;

/**
 * Orchestrator that runs every example in the showcase and prints
 * the absolute path of each generated PDF. After the v1.6 showcase
 * reorg, generated PDFs land under
 * {@code examples/target/generated-pdfs/<category>/} so the static
 * showcase site can consume them by category.
 */
public final class GenerateAllExamples {

    private GenerateAllExamples() {
    }

    public static void main(String[] args) throws Exception {
        // === Templates ===
        // CV / Resume (v2 layered — 16 presets, one example per preset)
        System.out.println("Generated: " + CvBlueBannerExample.generate());
        System.out.println("Generated: " + CvBoxedV2Example.generate());
        System.out.println("Generated: " + CvCenteredHeadlineExample.generate());
        System.out.println("Generated: " + CvClassicSerifExample.generate());
        System.out.println("Generated: " + CvCompactMonoExample.generate());
        System.out.println("Generated: " + CvEditorialBlueExample.generate());
        System.out.println("Generated: " + CvEngineeringResumeExample.generate());
        System.out.println("Generated: " + CvExecutiveExample.generate());
        System.out.println("Generated: " + CvMinimalUnderlinedExample.generate());
        System.out.println("Generated: " + CvMintEditorialExample.generate());
        System.out.println("Generated: " + CvModernV2Example.generate());
        System.out.println("Generated: " + CvMonogramSidebarExample.generate());
        System.out.println("Generated: " + CvNordicCleanExample.generate());
        System.out.println("Generated: " + CvPanelExample.generate());
        System.out.println("Generated: " + CvSidebarPortraitExample.generate());
        System.out.println("Generated: " + CvTimelineMinimalExample.generate());

        // Cover letters (v2 layered — 15 paired letters, one per CV preset)
        System.out.println("Generated: " + CvBlueBannerLetterV2Example.generate());
        System.out.println("Generated: " + CvBoxedSectionsLetterV2Example.generate());
        System.out.println("Generated: " + CvCenteredHeadlineLetterV2Example.generate());
        System.out.println("Generated: " + CvClassicSerifLetterV2Example.generate());
        System.out.println("Generated: " + CvCompactMonoLetterV2Example.generate());
        System.out.println("Generated: " + CvEditorialBlueLetterV2Example.generate());
        System.out.println("Generated: " + CvEngineeringResumeLetterV2Example.generate());
        System.out.println("Generated: " + CvExecutiveLetterV2Example.generate());
        System.out.println("Generated: " + CvMintEditorialLetterV2Example.generate());
        System.out.println("Generated: " + CvModernProfessionalLetterV2Example.generate());
        System.out.println("Generated: " + CvMonogramSidebarLetterV2Example.generate());
        System.out.println("Generated: " + CvNordicCleanLetterV2Example.generate());
        System.out.println("Generated: " + CvPanelLetterV2Example.generate());
        System.out.println("Generated: " + CvSidebarPortraitLetterV2Example.generate());
        System.out.println("Generated: " + CvTimelineMinimalLetterV2Example.generate());

        // Invoices
        System.out.println("Generated: " + InvoiceFileExample.generate());
        System.out.println("Generated: " + InvoiceCinematicFileExample.generate());

        // Proposals
        System.out.println("Generated: " + ProposalFileExample.generate());
        System.out.println("Generated: " + ProposalCinematicFileExample.generate());
        System.out.println("Generated: " + CinematicProposalFileExample.generate());

        // Schedule
        System.out.println("Generated: " + WeeklyScheduleFileExample.generate());

        // === Features ===
        // v1.6 layout primitives
        System.out.println("Generated: " + NestedListExample.generate());
        System.out.println("Generated: " + ComposedTableCellExample.generate());
        System.out.println("Generated: " + CanvasLayerExample.generate());

        // v1.5 visual primitives
        System.out.println("Generated: " + ShapeContainerExample.generate());
        System.out.println("Generated: " + TransformsExample.generate());
        System.out.println("Generated: " + TableAdvancedExample.generate());

        // Text + sections
        System.out.println("Generated: " + InlineShapesExample.generate());
        System.out.println("Generated: " + RichTextShowcaseExample.generate());
        System.out.println("Generated: " + SectionPresetsExample.generate());

        // Theming + chrome
        System.out.println("Generated: " + CustomBusinessThemeExample.generate());
        System.out.println("Generated: " + PdfChromeExample.generate());

        // Barcodes
        System.out.println("Generated: " + BarcodeShowcaseExample.generate());

        // Pipelines + tooling
        System.out.println("Generated: " + HttpStreamingExample.generate());
        System.out.println("Generated: " + LayoutSnapshotRegressionExample.generate());

        // === Flagships ===
        System.out.println("Generated: " + ModuleFirstFileExample.generate());
        System.out.println("Generated: " + MasterShowcaseExample.generate());
        System.out.println("Generated: " + BusinessReportExample.generate());
    }
}
