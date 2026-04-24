/**
 * Internal file/output abstractions used by engine backends.
 *
 * <p>Ownership: Owned by backend infrastructure, not by the public authoring API.</p>
 * <p>Extension rules: Keep filesystem details behind backend contracts and avoid leaking user paths into logs.</p>
 */
package com.demcha.compose.engine.io;