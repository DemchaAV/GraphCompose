package com.demcha.components.layout;

import com.demcha.components.core.Component;

/**
 * Align = like Anchor, but used when local Position is absent.
 * If Position is present, Align acts as a base; Position is an offset from that base.
 */
public record Align(HAnchor h, VAnchor v, double spacing) implements Component { }

