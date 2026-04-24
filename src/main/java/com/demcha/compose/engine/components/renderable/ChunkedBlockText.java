package com.demcha.compose.engine.components.renderable;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Chunked block-text marker kept until the engine fully converges on the
 * main block-text path.
 */
@Slf4j
@Builder
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class ChunkedBlockText extends Container {
}
