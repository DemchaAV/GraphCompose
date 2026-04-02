package com.demcha.examples;

import com.demcha.examples.support.ExampleDataFactory;
import com.demcha.examples.support.ExampleOutputPaths;
import com.demcha.templates.builtins.ProposalTemplateV1;

import java.nio.file.Path;

public final class ProposalFileExample {

    private ProposalFileExample() {
    }

    public static Path generate() throws Exception {
        Path outputFile = ExampleOutputPaths.prepare("proposal.pdf");
        new ProposalTemplateV1().render(ExampleDataFactory.sampleProposal(), outputFile);
        return outputFile;
    }

    public static void main(String[] args) throws Exception {
        System.out.println("Generated: " + generate());
    }
}
