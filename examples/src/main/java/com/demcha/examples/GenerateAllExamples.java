package com.demcha.examples;

public final class GenerateAllExamples {

    private GenerateAllExamples() {
    }

    public static void main(String[] args) throws Exception {
        // Document scenarios — built-in template + module-first authoring path.
        System.out.println("Generated: " + ModuleFirstFileExample.generate());
        System.out.println("Generated: " + CvFileExample.generate());
        System.out.println("Generated: " + CvTemplateGalleryFileExample.generate());
        System.out.println("Generated: " + CoverLetterTemplateGalleryFileExample.generate());
        System.out.println("Generated: " + CoverLetterFileExample.generate());
        System.out.println("Generated: " + InvoiceFileExample.generate());
        System.out.println("Generated: " + ProposalFileExample.generate());
        System.out.println("Generated: " + WeeklyScheduleFileExample.generate());

        // Cinematic templates (v1.5).
        System.out.println("Generated: " + InvoiceCinematicFileExample.generate());
        System.out.println("Generated: " + ProposalCinematicFileExample.generate());
        System.out.println("Generated: " + CinematicProposalFileExample.generate());

        // v1.5 feature showcases.
        System.out.println("Generated: " + ShapeContainerExample.generate());
        System.out.println("Generated: " + TransformsExample.generate());
        System.out.println("Generated: " + TableAdvancedExample.generate());
        System.out.println("Generated: " + CustomBusinessThemeExample.generate());
        System.out.println("Generated: " + HttpStreamingExample.generate());
        System.out.println("Generated: " + LayoutSnapshotRegressionExample.generate());

        // Public-API surface showcases.
        System.out.println("Generated: " + RichTextShowcaseExample.generate());
        System.out.println("Generated: " + SectionPresetsExample.generate());
        System.out.println("Generated: " + BarcodeShowcaseExample.generate());
        System.out.println("Generated: " + PdfChromeExample.generate());

        // v1.6 feature showcases.
        System.out.println("Generated: " + NestedListExample.generate());
        System.out.println("Generated: " + ComposedTableCellExample.generate());
        System.out.println("Generated: " + CanvasLayerExample.generate());

        // Kitchen-sink master demo + flagship business report cover.
        System.out.println("Generated: " + MasterShowcaseExample.generate());
        System.out.println("Generated: " + BusinessReportExample.generate());
    }
}
