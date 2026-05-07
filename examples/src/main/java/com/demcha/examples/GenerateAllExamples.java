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
import com.demcha.examples.features.text.RichTextShowcaseExample;
import com.demcha.examples.features.text.SectionPresetsExample;
import com.demcha.examples.features.themes.CustomBusinessThemeExample;
import com.demcha.examples.features.transforms.TransformsExample;
import com.demcha.examples.flagships.BusinessReportExample;
import com.demcha.examples.flagships.MasterShowcaseExample;
import com.demcha.examples.flagships.ModuleFirstFileExample;
import com.demcha.examples.templates.coverletter.CoverLetterFileExample;
import com.demcha.examples.templates.coverletter.CoverLetterTemplateGalleryFileExample;
import com.demcha.examples.templates.cv.CvFileExample;
import com.demcha.examples.templates.cv.CvTemplateGalleryFileExample;
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
        // CV / Resume
        System.out.println("Generated: " + CvFileExample.generate());
        System.out.println("Generated: " + CvTemplateGalleryFileExample.generate());

        // Cover letters
        System.out.println("Generated: " + CoverLetterFileExample.generate());
        System.out.println("Generated: " + CoverLetterTemplateGalleryFileExample.generate());

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
