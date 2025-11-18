package com.demcha.components.renderable;

import com.demcha.components.core.Component;
import com.demcha.components.core.Entity;
import com.demcha.system.implemented_systems.pdf_systems.PdfRenderingSystemECS;

import java.io.IOException;

public record ImageComponent() implements Component {

    public boolean pdf(Entity e, PdfRenderingSystemECS renderingSystemECS, boolean guideLines) throws IOException {
        //TODO has to be implemented
        return false;
    }
}
