package com.demcha.system.intarfaces;

import com.demcha.components.components_builders.Canvas;

public interface RenderingSystemECS extends SystemECS {
    <T extends Canvas> T canvas();
    <T extends GuidesRenderer> T guideRenderer();
    <T,S extends RenderStream<S>> T stream();

}
