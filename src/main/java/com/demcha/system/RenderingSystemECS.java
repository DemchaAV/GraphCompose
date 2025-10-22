package com.demcha.system;

import com.demcha.components.components_builders.Canvas;

public interface RenderingSystemECS extends SystemECS {
    Canvas getCanvas();
}
