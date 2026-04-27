package com.demcha.examples;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.templates.api.CvTemplate;
import com.demcha.compose.document.templates.builtins.ClassicSerifCvTemplate;
import com.demcha.compose.document.templates.builtins.CompactMonoCvTemplate;
import com.demcha.compose.document.templates.builtins.NordicCleanCvTemplate;
import com.demcha.compose.document.templates.builtins.ProductLeaderCvTemplate;
import com.demcha.compose.document.templates.builtins.TechLeadCvTemplate;
import com.demcha.compose.document.templates.data.cv.CvDocumentSpec;
import com.demcha.examples.support.ExampleDataFactory;
import com.demcha.examples.support.ExampleOutputPaths;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class CvTemplateGalleryFileExample {

    private CvTemplateGalleryFileExample() {
    }

    public static List<Path> generate() throws Exception {
        CvDocumentSpec cv = ExampleDataFactory.sampleCv();
        List<TemplateRun> templates = List.of(
                new TemplateRun(new NordicCleanCvTemplate(), 18),
                new TemplateRun(new CompactMonoCvTemplate(), 20),
                new TemplateRun(new ProductLeaderCvTemplate(), 18),
                new TemplateRun(new ClassicSerifCvTemplate(), 20),
                new TemplateRun(new TechLeadCvTemplate(), 20));

        List<Path> generated = new ArrayList<>();
        for (TemplateRun run : templates) {
            generated.add(generateOne(cv, run));
        }
        return List.copyOf(generated);
    }

    public static void main(String[] args) throws Exception {
        for (Path outputFile : generate()) {
            System.out.println("Generated: " + outputFile);
        }
    }

    private static Path generateOne(CvDocumentSpec cv, TemplateRun run) throws Exception {
        Path outputFile = ExampleOutputPaths.prepare("cv-" + run.template().getTemplateId() + ".pdf");

        try (DocumentSession document = GraphCompose.document(outputFile)
                .pageSize(DocumentPageSize.A4)
                .margin(run.margin(), run.margin(), run.margin(), run.margin())
                .create()) {
            run.template().compose(document, cv);
            document.buildPdf();
        }

        return outputFile;
    }

    private record TemplateRun(CvTemplate template, float margin) {
    }
}
