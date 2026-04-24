/**
 * Public font catalog and registration helpers used by the canonical document API and PDF backend.
 *
 * <p>Ownership: Owned by the public authoring surface because applications may choose built-in fonts or register custom font families.</p>
 * <p>Extension rules: Extend by adding explicit font definitions; do not put backend render handlers in this package.</p>
 */
package com.demcha.compose.font;