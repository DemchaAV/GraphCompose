package com.demcha.compose.loyaut_core.exceptions;


public class BigSizeElementException extends Throwable {

    // 💡 Best Practice: Use a constant for the template to avoid recreation
    private static final String ERROR_TEMPLATE = "Element is too large. Current position on the page: %s, Element height: %s, Available space: %s";

    // Standard constructor
    public BigSizeElementException(String message) {
        super(message);
    }
    public BigSizeElementException(Throwable e) {
        super(e);
    }

    // Constructor for chaining exceptions
    public BigSizeElementException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * 🚀 Specialized Constructor
     * Automatically formats the message using the context values.
     *
     * @param currentY The current Y position on the page
     * @param elementHeight The height of the element trying to fit
     * @param availableSpace The remaining space on the page
     */
    public BigSizeElementException(double currentY, double elementHeight, double availableSpace) {
        super(String.format(ERROR_TEMPLATE, currentY, elementHeight, availableSpace));
    }
    public BigSizeElementException(double currentY, double elementHeight, double availableSpace,Object args) {
        super(String.format(ERROR_TEMPLATE, currentY, elementHeight, availableSpace) + args.toString());
    }
}
