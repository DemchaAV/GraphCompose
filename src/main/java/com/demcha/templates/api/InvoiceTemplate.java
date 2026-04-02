package com.demcha.templates.api;

import com.demcha.templates.data.InvoiceData;
import org.apache.pdfbox.pdmodel.PDDocument;

import java.nio.file.Path;

/**
 * Public contract for reusable invoice PDF templates.
 */
public interface InvoiceTemplate {

    String getTemplateId();

    String getTemplateName();

    default String getDescription() {
        return "";
    }

    PDDocument render(InvoiceData data);

    PDDocument render(InvoiceData data, boolean guideLines);

    void render(InvoiceData data, Path path);

    void render(InvoiceData data, Path path, boolean guideLines);
}
