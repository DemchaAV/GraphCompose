/**
 * Weekly schedule document specs and supporting data records for canonical
 * templates.
 *
 * <p><strong>Superseded.</strong> Every type here is deprecated in favour of
 * {@link com.demcha.compose.document.templates.data.rota}, which models the
 * same document — who works when, over a span of days — and models it as the
 * sheet is actually drawn: staff in bands, an entry that states how loudly it
 * is drawn, and a day heading whose suffix a design can set apart. It also
 * keeps colour out of the document, which this package does not: a category
 * here carries three {@link java.awt.Color}s, so two presets cannot draw the
 * same schedule in two palettes. Nothing in the library ever rendered these
 * records.</p>
 *
 * @see com.demcha.compose.document.templates.data.rota
 */
package com.demcha.compose.document.templates.data.schedule;
