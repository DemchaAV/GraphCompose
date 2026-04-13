package com.demcha.compose.layout_core.components.renderable;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Legacy chunked block-text marker kept for compatibility until the engine
 * fully converges on the main block-text path.
 */
@Slf4j
@Builder
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class ChunkedBlockText extends Container {
}
