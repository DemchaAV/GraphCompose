package com.demcha.components.content.components_builders;

import com.demcha.components.core.Component;
import com.demcha.core.PdfDocument;

import java.util.Set;
import java.util.UUID;

public interface ComponentBuilder {

    String entityName();

    Set<Component> buildComponents();
    UUID buildInto(PdfDocument pdfDocument);
}
