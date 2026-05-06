/**
 * Templates v2 invoice domain — layouts, presets, builder, and spec data types.
 *
 * <p>This package is the home of all invoice templates in the v2
 * architecture. Sub-packages partition the domain by concern:</p>
 *
 * <ul>
 *   <li>{@code invoice.layouts} — slot caркасы (Standard,
 *       DetailedLineItems, SummaryAtTop).</li>
 *   <li>{@code invoice.presets} — flat copy-and-tweak preset classes
 *       (ModernInvoice, ClassicInvoice). Presets accept a
 *       {@code TableStyle} parameter so the line-item table presentation
 *       is interchangeable.</li>
 *   <li>{@code invoice.builder} — {@code InvoiceBuilder} for users
 *       composing their own preset.</li>
 *   <li>{@code invoice.spec} — data records ({@code InvoiceSpec},
 *       {@code InvoiceHeader}, {@code LineItem}) describing the user's
 *       invoice content.</li>
 * </ul>
 *
 * <p>Sub-packages will be populated during Phase F of the Templates v2
 * migration.</p>
 *
 * @since 1.6.0
 */
package com.demcha.compose.document.templates.invoice;
