/**
 * Templates v2 reusable table styles — zebra, minimal grid, borderless, accent header, financial.
 *
 * <p>Table styles capture the visual presentation of a table independently
 * of the data it carries. An invoice template can pick a financial style
 * with totals; a CV technical-skills table can pick a zebra style; a
 * proposal can pick borderless. The same data can render with any style.</p>
 *
 * <p>This package will be populated during Phase F of the Templates v2
 * migration with at least:</p>
 *
 * <ul>
 *   <li>{@code presets/ZebraTable} — alternating row backgrounds.</li>
 *   <li>{@code presets/MinimalGridTable} — thin grid lines, no fill.</li>
 *   <li>{@code presets/BorderlessTable} — no grid, columns separated by spacing only.</li>
 *   <li>{@code presets/AccentHeaderTable} — coloured header row, plain body.</li>
 *   <li>{@code presets/FinancialTable} — column alignment for currency,
 *       totals row, optional tax row (used by invoice templates).</li>
 *   <li>{@code TableStyleBuilder} — fully custom table styles.</li>
 * </ul>
 *
 * @since 1.6.0
 */
package com.demcha.compose.document.templates.tables;
