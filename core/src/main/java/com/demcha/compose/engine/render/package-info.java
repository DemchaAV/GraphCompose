/**
 * Backend-neutral render contracts, handler registry, render ordering, and render-pass session lifetime.
 *
 * <p>Ownership: Owned by the shared engine render seam.</p>
 * <p>Extension rules: Extend with backend-neutral contracts first, then backend-specific handlers under backend packages.</p>
 */
package com.demcha.compose.engine.render;