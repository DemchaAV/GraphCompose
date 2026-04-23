package com.demcha.examples;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.examples.support.ExampleOutputPaths;
import org.apache.pdfbox.pdmodel.common.PDRectangle;

import java.nio.file.Path;

/**
 * Minimal canonical authoring example for developers who are building their
 * own document instead of using a built-in template.
 */
public final class ModuleFirstFileExample {

    private ModuleFirstFileExample() {
    }

    public static Path generate() throws Exception {
        Path outputFile = ExampleOutputPaths.prepare("module-first-profile.pdf");

        try (DocumentSession document = GraphCompose.document(outputFile)
                .pageSize(PDRectangle.A4)
                .margin(24, 24, 24, 24)
                .create()) {

            document.pageFlow()
                    .name("CandidateProfile")
                    .spacing(12)
                    .module("Professional Summary", module -> module.paragraph(
                            "Backend engineer focused on clean Java APIs, stable document output, "
                                    + "and reusable template architecture."))
                    .module("Technical Skills", module -> module.bullets(
                            "Java 21 and Spring Boot",
                            "PDF document generation with GraphCompose",
                            "Layout snapshot testing and render regression checks",
                            "Modular template design for CV, invoice, and proposal flows"))
                    .module("Projects", module -> module.rows(
                            "GraphCompose - canonical engine migration, module-first DSL, and business template cleanup.",
                            "CVRewriter - profile-aware CV tailoring platform with secure Spring Boot backend and PDF generation."))
                    .build();

            document.buildPdf();
        }

        return outputFile;
    }

    public static void main(String[] args) throws Exception {
        System.out.println("Generated: " + generate());
    }
}
