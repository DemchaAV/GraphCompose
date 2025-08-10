package com.demcha.layout;

/**
 * Context passed to Layout during arrange pass.
 * This class encapsulates the information necessary for arranging child elements in a layout.
 * It contains the starting position (X and Y) and the allocated width and height for the element being arranged.
 *
 * <p>Instances of this class are used by layout managers to adjust the positioning and sizing of child components.</p>
 *
 * <h2>Fields:</h2>
 * <ul>
 *     <li><strong>startX</strong> - The starting X position of the element (horizontal position).</li>
 *     <li><strong>startY</strong> - The starting Y position of the element (vertical position).</li>
 *     <li><strong>allocatedWidth</strong> - The width allocated to the element during the arrangement process.</li>
 *     <li><strong>allocatedHeight</strong> - The height allocated to the element during the arrangement process.</li>
 * </ul>
 *
 * <h2>Usage Example:</h2>
 * <pre>
 * ArrangeCtx ctx = new ArrangeCtx(10, 20, 100, 200);
 * System.out.println("Start Position: (" + ctx.startX() + ", " + ctx.startY() + ")");
 * System.out.println("Allocated Size: " + ctx.allocatedWidth() + "x" + ctx.allocatedHeight());
 * </pre>
 *
 * <p>This class is immutable, ensuring that the provided arrangement data remains consistent throughout the layout pass.</p>
 */
public record ArrangeCtx(double startX, double startY, double allocatedWidth, double allocatedHeight) {}
