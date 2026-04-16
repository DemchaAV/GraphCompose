/**
 * Public compose-first contracts for canonical GraphCompose document templates.
 *
 * <p>This package defines the stable template-facing API used by applications
 * that want to compose reusable CV, cover-letter, invoice, proposal, and
 * weekly schedule documents through
 * {@link com.demcha.compose.document.api.DocumentSession}.</p>
 *
 * <p>The contracts here are intentionally small:</p>
 * <ul>
 *   <li>template interfaces expose identity plus one {@code compose(...)} seam</li>
 *   <li>registries provide stable lookup and default selection helpers</li>
 *   <li>PDF-returning legacy conveniences stay in deprecated bridge packages
 *       under {@code com.demcha.templates.*}</li>
 * </ul>
 */
package com.demcha.compose.document.templates.api;
