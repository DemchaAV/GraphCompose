package com.demcha.compose;

import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.templates.data.cv.CvDocumentSpec;
import com.demcha.compose.document.templates.data.invoice.InvoiceDocumentSpec;
import com.demcha.compose.document.templates.data.proposal.ProposalDocumentSpec;
import com.demcha.compose.engine.components.style.Margin;
import com.demcha.mock.CvDocumentSpecMock;
import com.demcha.mock.InvoiceDataFixtures;
import com.demcha.mock.ProposalDataFixtures;
import org.apache.pdfbox.pdmodel.common.PDRectangle;

import java.awt.Color;

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

    static CvDocumentSpec canonicalCv() {
        return new CvDocumentSpecMock().getCv();
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
                .pageSize(pageSize)
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
