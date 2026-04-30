package com.demcha.examples;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.templates.builtins.ProposalTemplateV2;
import com.demcha.compose.document.theme.BusinessTheme;
import com.demcha.examples.support.ExampleDataFactory;
import com.demcha.examples.support.ExampleOutputPaths;

import java.nio.file.Path;

/**
 * Phase E.2 — runnable showcase for {@code ProposalTemplateV2}, the
 * cinematic theme-driven proposal template.
 *
 * @author Artem Demchyshyn
 */
public final class ProposalCinematicFileExample {

    private ProposalCinematicFileExample() {
    }

    public static Path generate() throws Exception {
        Path outputFile = ExampleOutputPaths.prepare("proposal-cinematic.pdf");
        BusinessTheme theme = BusinessTheme.modern();
        ProposalTemplateV2 template = new ProposalTemplateV2(theme);

        try (DocumentSession document = GraphCompose.document(outputFile)
                .pageSize(DocumentPageSize.A4)
                .pageBackground(theme.pageBackground())
                .margin(28, 28, 28, 28)
                .create()) {
            template.compose(document, ExampleDataFactory.sampleProposal());
            document.buildPdf();
        }

        return outputFile;
    }

    public static void main(String[] args) throws Exception {
        System.out.println("Generated: " + generate());
    }
}
