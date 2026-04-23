package com.demcha.compose;

import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.templates.data.invoice.InvoiceData;
import com.demcha.compose.document.templates.data.cv.CvDocumentSpec;
import com.demcha.compose.document.templates.data.proposal.ProposalData;
import com.demcha.compose.layout_core.components.content.text.TextDecoration;
import com.demcha.compose.layout_core.components.content.text.TextStyle;
import com.demcha.compose.layout_core.components.style.ComponentColor;
import com.demcha.compose.layout_core.components.style.Margin;
import com.demcha.mock.CvDocumentSpecMock;
import com.demcha.mock.InvoiceDataFixtures;
import com.demcha.mock.ProposalDataFixtures;
import org.apache.pdfbox.pdmodel.common.PDRectangle;

import java.awt.Color;

final class CanonicalBenchmarkSupport {
    static final TextStyle TITLE_STYLE = TextStyle.builder()
            .size(20)
            .decoration(TextDecoration.BOLD)
            .color(new Color(18, 40, 74))
            .build();
    static final TextStyle BODY_STYLE = TextStyle.builder()
            .size(9.5)
            .decoration(TextDecoration.DEFAULT)
            .color(new Color(58, 69, 84))
            .build();

    private CanonicalBenchmarkSupport() {
    }

    static CvDocumentSpec canonicalCv() {
        return new CvDocumentSpecMock().getCv();
    }

    static InvoiceData canonicalInvoiceData() {
        return InvoiceDataFixtures.standardInvoice();
    }

    static ProposalData canonicalProposalData() {
        return ProposalDataFixtures.longProposal();
    }

    static byte[] renderSimpleBenchmarkDocument(PDRectangle pageSize,
                                                Margin margin,
                                                String rootName,
                                                String title,
                                                String body) throws Exception {
        try (DocumentSession document = GraphCompose.document()
                .pageSize(pageSize)
                .margin(margin)
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
                        .color(ComponentColor.LIGHT_GRAY))
                .build();
    }
}
