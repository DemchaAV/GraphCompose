/**
 * Internal ECS builders that materialize low-level entities and components.
 *
 * <p>Ownership: Owned by the shared engine; canonical users should prefer DocumentDsl and semantic nodes.</p>
 * <p>Extension rules: Extend only when a new engine primitive is required and has matching layout/render coverage.</p>
 */
package com.demcha.compose.engine.components.components_builders;