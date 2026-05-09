package com.demcha.compose;

import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.templates.blocks.BulletListBlock;
import com.demcha.compose.document.templates.blocks.IndentedBlock;
import com.demcha.compose.document.templates.blocks.KeyValueBlock;
import com.demcha.compose.document.templates.blocks.MultiParagraphBlock;
import com.demcha.compose.document.templates.blocks.ParagraphBlock;
import com.demcha.compose.document.templates.cv.spec.CvHeader;
import com.demcha.compose.document.templates.cv.spec.CvModule;
import com.demcha.compose.document.templates.cv.spec.CvSpec;
import com.demcha.compose.document.templates.data.invoice.InvoiceDocumentSpec;
import com.demcha.compose.document.templates.data.proposal.ProposalDocumentSpec;
import com.demcha.compose.engine.components.style.Margin;
import com.demcha.mock.InvoiceDataFixtures;
import com.demcha.mock.ProposalDataFixtures;
import org.apache.pdfbox.pdmodel.common.PDRectangle;

import java.awt.Color;
import java.util.List;

final class CanonicalBenchmarkSupport {
    static final DocumentTextStyle TITLE_STYLE = DocumentTextStyle.builder()
            .size(20)
            .decoration(DocumentTextDecoration.BOLD)
            .color(DocumentColor.of(new Color(18, 40, 74)))
            .build();
    static final DocumentTextStyle BODY_STYLE = DocumentTextStyle.builder()
            .size(9.5)
            .decoration(DocumentTextDecoration.DEFAULT)
            .color(DocumentColor.of(new Color(58, 69, 84)))
            .build();

    private CanonicalBenchmarkSupport() {
    }

    /**
     * Canonical CV spec used by Templates v2 benchmarks. Mirrors the
     * shape of {@code PresetVisualParityTest.canonicalCvSpec()} so the
     * benchmarks render the same realistic CV the visual parity gate
     * locks down. The legacy V1 {@code CvDocumentSpec} helper was
     * removed alongside {@code CvTemplateV1} when Templates v2 landed
     * in v1.6.
     */
    static CvSpec canonicalCv() {
        return CvSpec.builder()
                .header(CvHeader.builder()
                        .name("Artem Demchyshyn")
                        .jobTitle("Backend Java Developer")
                        .address("London, UK")
                        .phone("+44 20 5555 1000")
                        .email("artem@demo.dev")
                        .link("LinkedIn", "https://linkedin.com/in/graphcompose")
                        .link("GitHub", "https://github.com/DemchaAV")
                        .build())
                .module(CvModule.of("Professional Summary",
                        new ParagraphBlock(
                                "Platform engineer building resilient PDF and "
                                        + "document-generation workflows for reliable "
                                        + "business output.")))
                .module(CvModule.of("Technical Skills",
                        new BulletListBlock(List.of(
                                "Java 21, PDFBox, Maven, REST APIs",
                                "Template design systems, pagination, semantic layout composition",
                                "Testing strategy, CI pipelines, developer enablement"))))
                .module(CvModule.of("Education & Certifications",
                        new IndentedBlock(List.of(
                                new IndentedBlock.Item("MSc Computer Science",
                                        "University of Manchester | 2021"),
                                new IndentedBlock.Item("Oracle Java Certification",
                                        "Professional track | 2023")))))
                .module(CvModule.of("Projects",
                        new IndentedBlock(List.of(
                                new IndentedBlock.Item("GraphCompose",
                                        "Declarative PDF layout engine for reusable document generation"),
                                new IndentedBlock.Item("Template Studio",
                                        "Internal tool for evaluating CV, proposal, and invoice output")))))
                .module(CvModule.of("Professional Experience",
                        new MultiParagraphBlock(List.of(
                                "**Senior Platform Engineer**, Northwind Systems | "
                                        + "*2024-Present* — Led reusable document flows.",
                                "**Software Engineer**, BrightLeaf Labs | *2021-2024* "
                                        + "— Built backend services and rendering pipelines."))))
                .module(CvModule.of("Additional Information",
                        new KeyValueBlock(List.of(
                                new KeyValueBlock.Entry("Location",
                                        "Based in London and available for hybrid or remote collaboration"),
                                new KeyValueBlock.Entry("Interests",
                                        "Platform architecture, DX, and document-quality automation")))))
                .build();
    }

    static InvoiceDocumentSpec canonicalInvoice() {
        return InvoiceDocumentSpec.from(InvoiceDataFixtures.standardInvoice());
    }

    static ProposalDocumentSpec canonicalProposal() {
        return ProposalDocumentSpec.from(ProposalDataFixtures.longProposal());
    }

    static byte[] renderSimpleBenchmarkDocument(PDRectangle pageSize,
                                                Margin margin,
                                                String rootName,
                                                String title,
                                                String body) throws Exception {
        try (DocumentSession document = GraphCompose.document()
                .pageSize(DocumentPageSize.of(pageSize.getWidth(), pageSize.getHeight()))
                .margin(new DocumentInsets(margin.top(), margin.right(), margin.bottom(), margin.left()))
                .create()) {
            composeSimpleBenchmarkFlow(document, rootName, title, body);
            return document.toPdfBytes();
        }
    }

    static void composeSimpleBenchmarkFlow(DocumentSession document,
                                           String rootName,
                                           String title,
                                           String body) {
        double width = document.canvas().innerWidth();
        document.dsl()
                .pageFlow()
                .name(rootName)
                .spacing(8)
                .addParagraph(paragraph -> paragraph
                        .name(rootName + "Title")
                        .text(title)
                        .textStyle(TITLE_STYLE))
                .addParagraph(paragraph -> paragraph
                        .name(rootName + "Body")
                        .text(body)
                        .textStyle(BODY_STYLE)
                        .lineSpacing(2)
                        .padding(4, 4, 4, 4))
                .addDivider(divider -> divider
                        .name(rootName + "Divider")
                        .width(width)
                        .thickness(1)
                        .color(DocumentColor.LIGHT_GRAY))
                .build();
    }
}
