package com.demcha.compose.devtool;

/**
 * Plain Java entry point for IDE and Maven launches.
 * <p>
 * Run this class instead of {@link GraphComposeDevTool}. Launching a class that
 * directly extends {@code javafx.application.Application} often triggers the
 * JVM's JavaFX launcher path before our custom {@code main(...)} is invoked.
 * </p>
 */
public final class GraphComposeDevToolLauncher {

    private GraphComposeDevToolLauncher() {
        // Utility launcher
    }

    public static void main(String[] args) {
        GraphComposeDevTool.main(args);
    }
}
