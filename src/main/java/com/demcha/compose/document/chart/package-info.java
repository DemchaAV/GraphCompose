/**
 * Configurable, deterministic chart subsystem for canonical documents.
 *
 * <p>Charts are modelled as four independent layers — data
 * ({@link com.demcha.compose.document.chart.ChartData}), structure
 * ({@link com.demcha.compose.document.chart.ChartSpec}), style
 * ({@link com.demcha.compose.document.chart.ChartStyle} cascaded over
 * {@link com.demcha.compose.document.chart.ChartTheme}), and geometry
 * ({@link com.demcha.compose.document.chart.ChartLayoutResolver}). The resolver
 * is a pure function that compiles a chart into existing primitive nodes
 * (shapes, lines, paragraphs); a chart therefore never reaches a render backend
 * as a "chart", which keeps it snapshot-testable and byte-identical like the
 * rest of the engine.</p>
 *
 * <p><b>Ownership &amp; extension.</b> This package is canonical authoring
 * surface. Add a new chart kind by adding a permitted record to the sealed
 * {@link com.demcha.compose.document.chart.ChartSpec} and a matching branch in
 * {@link com.demcha.compose.document.chart.ChartLayoutResolver}; the compiler
 * enforces exhaustive handling. The semantic node
 * ({@link com.demcha.compose.document.node.ChartNode}) and its layout definition
 * ({@code com.demcha.compose.document.layout.definitions.ChartDefinition}) live
 * in their respective packages and should not be duplicated here.</p>
 *
 * @author Artem Demchyshyn
 * @since 1.8.0
 */
package com.demcha.compose.document.chart;
