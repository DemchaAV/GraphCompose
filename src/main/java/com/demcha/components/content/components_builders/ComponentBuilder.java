package com.demcha.components.content.components_builders;

import com.demcha.components.core.Component;
import com.demcha.components.core.Entity;
import com.demcha.core.PdfDocument;

import java.util.Set;
import java.util.UUID;

public interface ComponentBuilder {

    String entityName();

    Entity buildComponents();
    Entity buildInto(PdfDocument pdfDocument);
}
