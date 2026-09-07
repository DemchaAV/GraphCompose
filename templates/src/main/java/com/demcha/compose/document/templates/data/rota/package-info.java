/**
 * Shared, render-neutral rota document specs and supporting data records,
 * consumed by the layered {@code rota.presets} presets.
 *
 * <p>A rota is a grid the document owns rather than one a preset infers:
 * {@link com.demcha.compose.document.templates.data.rota.StructuredRotaData#days()}
 * is the columns and every
 * {@link com.demcha.compose.document.templates.data.rota.RotaStaff#days()}
 * runs in that same order, so a cell is found by position. A day holds a list
 * of entries rather than one, which makes an empty day and a split day ordinary
 * rather than special cases.</p>
 *
 * <p>This package supersedes {@code templates.data.schedule}, which modelled
 * the same document and which nothing ever rendered. The three differences are
 * the reasons: staff belong to bands rather than being ordered by a token,
 * an entry states how loudly it is drawn, and colour stays out of the document
 * — a schedule category carried three {@link java.awt.Color}s, which stops two
 * presets drawing the same rota in two palettes.</p>
 *
 * <p>What a cell means and what it prints are separate:
 * {@link com.demcha.compose.document.templates.data.rota.ShiftStatus} is the
 * meaning a preset colours by, and the text is the word a particular site uses
 * for it. Two rotas that print {@code A/L} and {@code HOL} colour alike.</p>
 *
 * @since 2.4.0
 */
package com.demcha.compose.document.templates.data.rota;
