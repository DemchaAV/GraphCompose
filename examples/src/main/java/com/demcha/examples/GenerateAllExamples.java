package com.demcha.examples;

import com.demcha.examples.features.barcodes.BarcodeShowcaseExample;
import com.demcha.examples.features.charts.ChartShowcaseExample;
import com.demcha.examples.features.canvas.CanvasLayerExample;
import com.demcha.examples.features.debug.DebugOverlayExample;
import com.demcha.examples.features.docx.WordExportExample;
import com.demcha.examples.features.chrome.PageNumberingExample;
import com.demcha.examples.features.chrome.PdfChromeExample;
import com.demcha.examples.features.chrome.ViewerPreferencesExample;
import com.demcha.examples.features.layout.BleedExample;
import com.demcha.examples.features.layout.PerPageMarginExample;
import com.demcha.examples.features.layout.RowColumnsExample;
import com.demcha.examples.features.layout.RowFlexExample;
import com.demcha.examples.features.layout.RowVerticalAlignExample;
import com.demcha.examples.features.layout.BlockAlignExample;
import com.demcha.examples.features.lists.NestedListExample;
import com.demcha.examples.features.shapes.LineCapExample;
import com.demcha.examples.features.shapes.LineFillExample;
import com.demcha.examples.features.shapes.PhotoClipExample;
import com.demcha.examples.features.shapes.ShapeContainerExample;
import com.demcha.examples.features.shapes.VectorPathExample;
import com.demcha.examples.features.snapshots.LayoutSnapshotRegressionExample;
import com.demcha.examples.features.streaming.HttpStreamingExample;
import com.demcha.examples.features.svg.SvgIconGalleryExample;
import com.demcha.examples.features.tables.ComposedTableCellExample;
import com.demcha.examples.features.tables.InlineCodeColumnWrapExample;
import com.demcha.examples.features.tables.TableAdvancedExample;
import com.demcha.examples.features.text.EmojiGalleryExample;
import com.demcha.examples.features.text.EmojiShortcodeExample;
import com.demcha.examples.features.text.EmojiSvgVsPngExample;
import com.demcha.examples.features.text.EmojiClipPathReportExample;
import com.demcha.examples.features.text.InlineShapesExample;
import com.demcha.examples.features.text.TextDirectionExample;
import com.demcha.examples.features.text.InlineSvgIconExample;
import com.demcha.examples.features.text.InlineHighlightExample;
import com.demcha.examples.features.navigation.InPdfNavigationExample;
import com.demcha.examples.features.navigation.PageReferenceExample;
import com.demcha.examples.features.navigation.ContainerBookmarkExample;
import com.demcha.examples.features.navigation.TocExample;
import com.demcha.examples.features.structure.MultiSectionExample;
import com.demcha.examples.features.text.RichTextShowcaseExample;
import com.demcha.examples.features.text.SectionPresetsExample;
import com.demcha.examples.features.title.BookTemplateExample;
import com.demcha.examples.features.title.PoetryTitlePageExample;
import com.demcha.examples.features.transforms.TransformsExample;
import com.demcha.examples.support.ExampleOutputPaths;
import com.demcha.examples.flagships.BusinessReportExample;
import com.demcha.examples.flagships.BusinessReportPptxExample;
import com.demcha.examples.flagships.EngineDeckExample;
import com.demcha.examples.flagships.EngineDeckV2Example;
import com.demcha.examples.flagships.EngineDeckPptxExample;
import com.demcha.examples.flagships.FeatureCatalogExample;
import com.demcha.examples.flagships.FinancialReportExample;
import com.demcha.examples.flagships.FinancialReportPptxExample;
import com.demcha.examples.flagships.MasterShowcaseExample;
import com.demcha.examples.flagships.MasterShowcasePptxExample;
import com.demcha.examples.flagships.LinkedInCarouselExample;
import com.demcha.examples.flagships.SocialCardExample;
import com.demcha.examples.flagships.MavenBannerPptxExample;
import com.demcha.examples.flagships.ModuleFirstFileExample;
import com.demcha.examples.flagships.TwinOutputExample;
import com.demcha.examples.templates.coverletter.CoverLetterFileExample;
import com.demcha.examples.templates.coverletter.CvBlueBannerLetterV2Example;
import com.demcha.examples.templates.coverletter.CvBoxedSectionsLetterV2Example;
import com.demcha.examples.templates.coverletter.CvCenteredHeadlineLetterV2Example;
import com.demcha.examples.templates.coverletter.CvClassicSerifLetterV2Example;
import com.demcha.examples.templates.coverletter.CvCompactMonoLetterV2Example;
import com.demcha.examples.templates.coverletter.CvEditorialBlueLetterV2Example;
import com.demcha.examples.templates.coverletter.CvEngineeringResumeLetterV2Example;
import com.demcha.examples.templates.coverletter.CvExecutiveLetterV2Example;
import com.demcha.examples.templates.coverletter.CvMintEditorialLetterV2Example;
import com.demcha.examples.templates.coverletter.CvModernProfessionalLetterV2Example;
import com.demcha.examples.templates.coverletter.CvMonogramSidebarLetterV2Example;
import com.demcha.examples.templates.coverletter.CvNordicCleanLetterV2Example;
import com.demcha.examples.templates.coverletter.CvPanelLetterV2Example;
import com.demcha.examples.templates.coverletter.CvSidebarPortraitLetterV2Example;
import com.demcha.examples.templates.coverletter.CvTimelineMinimalLetterV2Example;
import com.demcha.examples.templates.cv.v2.CvBlueBannerExample;
import com.demcha.examples.templates.cv.v2.CvBoxedV2Example;
import com.demcha.examples.templates.cv.v2.CvCenteredHeadlineExample;
import com.demcha.examples.templates.cv.v2.CvClassicSerifExample;
import com.demcha.examples.templates.cv.v2.CvCompactMonoExample;
import com.demcha.examples.templates.cv.v2.CvEditorialBlueExample;
import com.demcha.examples.templates.cv.v2.CvEngineeringResumeExample;
import com.demcha.examples.templates.cv.v2.CvExecutiveExample;
import com.demcha.examples.templates.cv.v2.CvMinimalUnderlinedExample;
import com.demcha.examples.templates.cv.v2.CvMintEditorialCustomExample;
import com.demcha.examples.templates.cv.v2.CvMintEditorialExample;
import com.demcha.examples.templates.cv.v2.CvModernV2Example;
import com.demcha.examples.templates.cv.v2.CvMonogramSidebarExample;
import com.demcha.examples.templates.cv.v2.CvNordicCleanExample;
import com.demcha.examples.templates.cv.v2.CvPanelExample;
import com.demcha.examples.templates.cv.v2.CvSidebarPortraitExample;
import com.demcha.examples.templates.cv.v2.CvTimelineMinimalExample;
import com.demcha.examples.templates.invoice.InvoiceCinematicFileExample;
import com.demcha.examples.templates.invoice.ModernInvoiceV2Example;
import com.demcha.examples.templates.proposal.CinematicProposalFileExample;
import com.demcha.examples.templates.proposal.ProposalCinematicFileExample;
import com.demcha.examples.templates.proposal.ModernProposalV2Example;
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
        // The catalogue is a statement about the code, so it starts empty: an
        // example renamed or deleted must not leave its old document behind for
        // the showcase to publish and the guards to count.
        ExampleOutputPaths.clean();

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
        System.out.println("Generated: " + CvMintEditorialCustomExample.generate());
        System.out.println("Generated: " + CoverLetterFileExample.generate());
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
        System.out.println("Generated: " + InvoiceCinematicFileExample.generate());
        System.out.println("Generated: " + ModernInvoiceV2Example.generate());

        // Proposals
        System.out.println("Generated: " + ProposalCinematicFileExample.generate());
        System.out.println("Generated: " + ModernProposalV2Example.generate());
        System.out.println("Generated: " + CinematicProposalFileExample.generate());

        // Schedule
        System.out.println("Generated: " + WeeklyScheduleFileExample.generate());

        // === Features ===
        // v1.6 layout primitives
        System.out.println("Generated: " + NestedListExample.generate());
        System.out.println("Generated: " + ComposedTableCellExample.generate());
        System.out.println("Generated: " + InlineCodeColumnWrapExample.generate());
        System.out.println("Generated: " + CanvasLayerExample.generate());

        // v1.5 visual primitives
        System.out.println("Generated: " + ShapeContainerExample.generate());
        System.out.println("Generated: " + VectorPathExample.generate());
        System.out.println("Generated: " + LineCapExample.generate());
        System.out.println("Generated: " + LineFillExample.generate());
        System.out.println("Generated: " + PhotoClipExample.generate());
        System.out.println("Generated: " + SvgIconGalleryExample.generate());
        System.out.println("Generated: " + BlockAlignExample.generate());
        System.out.println("Generated: " + BleedExample.generate());
        System.out.println("Generated: " + PerPageMarginExample.generate());
        System.out.println("Generated: " + RowColumnsExample.generate());
        System.out.println("Generated: " + RowVerticalAlignExample.generate());
        System.out.println("Generated: " + RowFlexExample.generate());
        System.out.println("Generated: " + TransformsExample.generate());
        System.out.println("Generated: " + TableAdvancedExample.generate());

        // Text + sections
        System.out.println("Generated: " + InlineShapesExample.generate());
        System.out.println("Generated: " + TextDirectionExample.generate());
        System.out.println("Generated: " + InlineSvgIconExample.generate());
        System.out.println("Generated: " + InlineHighlightExample.generate());
        System.out.println("Generated: " + EmojiShortcodeExample.generate());
        System.out.println("Generated: " + EmojiSvgVsPngExample.generate());
        System.out.println("Generated: " + EmojiGalleryExample.generate());
        System.out.println("Generated: " + EmojiClipPathReportExample.generate());
        System.out.println("Generated: " + RichTextShowcaseExample.generate());
        System.out.println("Generated: " + SectionPresetsExample.generate());
        System.out.println("Generated: " + InPdfNavigationExample.generate());
        System.out.println("Generated: " + PageReferenceExample.generate());
        System.out.println("Generated: " + TocExample.generate());
        System.out.println("Generated: " + ContainerBookmarkExample.generate());
        System.out.println("Generated: " + MultiSectionExample.generate());

        // Theming + chrome
        System.out.println("Generated: " + PdfChromeExample.generate());
        System.out.println("Generated: " + PageNumberingExample.generate());
        System.out.println("Generated: " + ViewerPreferencesExample.generate());

        // DOCX export
        System.out.println("Generated: " + WordExportExample.generate());

        // Barcodes
        System.out.println("Generated: " + BarcodeShowcaseExample.generate());

        // Charts
        System.out.println("Generated: " + ChartShowcaseExample.generate());

        // Pipelines + tooling
        System.out.println("Generated: " + HttpStreamingExample.generate());
        System.out.println("Generated: " + LayoutSnapshotRegressionExample.generate());
        System.out.println("Generated: " + DebugOverlayExample.generate());

        // === Flagships ===
        System.out.println("Generated: " + ModuleFirstFileExample.generate());
        System.out.println("Generated: " + MasterShowcaseExample.generate());
        System.out.println("Generated: " + MasterShowcasePptxExample.generate());
        System.out.println("Generated: " + FeatureCatalogExample.generate());
        System.out.println("Generated: " + BusinessReportExample.generate());
        System.out.println("Generated: " + BusinessReportPptxExample.generate());
        System.out.println("Generated: " + EngineDeckExample.generate());
        System.out.println("Generated: " + EngineDeckV2Example.generate());
        System.out.println("Generated: " + EngineDeckPptxExample.generate());
        // The PDF first: ShowcaseSync builds a card per PDF and publishes a PPTX only
        // as the same-named companion beside one, so a deck without its PDF twin never
        // reaches the site at all.
        System.out.println("Generated: " + MavenBannerPptxExample.generatePdf());
        System.out.println("Generated: " + MavenBannerPptxExample.generate());
        System.out.println("Generated: " + SocialCardExample.generate());
        System.out.println("Generated: " + SocialCardExample.generatePptx());
        System.out.println("Generated: " + LinkedInCarouselExample.generate());
        System.out.println("Generated: " + LinkedInCarouselExample.generatePptx());
        System.out.println("Generated: " + TwinOutputExample.generate());
        System.out.println("Generated: " + FinancialReportExample.generate());
        System.out.println("Generated: " + FinancialReportPptxExample.generate());
        System.out.println("Generated: " + BookTemplateExample.generate());
        System.out.println("Generated: " + PoetryTitlePageExample.generate());
    }
}
