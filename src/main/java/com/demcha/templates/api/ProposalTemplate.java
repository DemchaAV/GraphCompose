package com.demcha.templates.api;

import com.demcha.templates.data.ProposalData;
import org.apache.pdfbox.pdmodel.PDDocument;

import java.nio.file.Path;

/**
 * Public contract for reusable proposal PDF templates.
 */
public interface ProposalTemplate {

    String getTemplateId();

    String getTemplateName();

    default String getDescription() {
        return "";
    }

    PDDocument render(ProposalData data);

    PDDocument render(ProposalData data, boolean guideLines);

    void render(ProposalData data, Path path);

    void render(ProposalData data, Path path, boolean guideLines);
}
