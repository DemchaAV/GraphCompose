package com.demcha.loyaut_core.components.renderable;

import com.demcha.loyaut_core.components.core.Component;
import com.demcha.loyaut_core.components.core.Entity;
import com.demcha.loyaut_core.system.implemented_systems.pdf_systems.PdfRenderingSystemECS;

import java.io.IOException;

public record ImageComponent() implements Component {

    public boolean pdf(Entity e, PdfRenderingSystemECS renderingSystemECS, boolean guideLines) throws IOException {
        //TODO has to be implemented
        return false;
    }
}
