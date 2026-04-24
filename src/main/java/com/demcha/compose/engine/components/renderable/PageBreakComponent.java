package com.demcha.compose.engine.components.renderable;

import com.demcha.compose.engine.render.Render;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Render marker for a forced page break.
 *
 * <p>A page break entity forces the pagination system to start a new page
 * at the point where this entity appears in the layout flow. It has zero
 * visual content but signals the page breaker to advance.</p>
 *
 * @author Artem Demchyshyn
 */
@Slf4j
@EqualsAndHashCode
@NoArgsConstructor
public class PageBreakComponent implements Render {
}
