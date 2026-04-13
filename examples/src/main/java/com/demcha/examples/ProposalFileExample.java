package com.demcha.examples;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.layout_core.core.DocumentComposer;
import com.demcha.examples.support.ExampleDataFactory;
import com.demcha.examples.support.ExampleOutputPaths;
import com.demcha.templates.builtins.ProposalTemplateV1;
import org.apache.pdfbox.pdmodel.common.PDRectangle;

import java.nio.file.Path;

public final class ProposalFileExample {

    private ProposalFileExample() {
    }

    public static Path generate() throws Exception {
        Path outputFile = ExampleOutputPaths.prepare("proposal.pdf");
        ProposalTemplateV1 template = new ProposalTemplateV1();

        try (DocumentComposer composer = GraphCompose.pdf(outputFile)
                .pageSize(PDRectangle.A4)
                .margin(22, 22, 22, 22)
                .markdown(true)
                .create()) {
            template.compose(composer, ExampleDataFactory.sampleProposal());
            composer.build();
        }

        return outputFile;
    }

    public static void main(String[] args) throws Exception {
        System.out.println("Generated: " + generate());
    }
}
