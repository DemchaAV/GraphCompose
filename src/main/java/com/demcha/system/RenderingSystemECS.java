package com.demcha.system;

import com.demcha.components.components_builders.Canvas;
import com.demcha.components.containers.abstract_builders.GuidesRenderer;
import com.demcha.core.EntityManager;

public interface RenderingSystemECS extends SystemECS, GuidesRenderer {
    <T extends Canvas> T canvas();

}
