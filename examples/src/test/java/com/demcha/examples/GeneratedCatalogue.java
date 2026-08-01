package com.demcha.examples;

import java.nio.file.Path;

/**
 * The generated example tree, produced once per JVM for whichever test asks first.
 *
 * <p>Two suites assert against the same output — one on the files, one on the showcase
 * metadata that describes them — and running the whole catalogue twice would double the
 * cost of the examples job for nothing.</p>
 *
 * <p>The emptying belongs to {@link GenerateAllExamples} rather than to this class. The
 * release script and CI run the runner directly, so a clean step that lived only here
 * would leave those two paths behind — and the failure it prevents, a document surviving
 * an example that no longer writes it, is exactly the kind that shows up in what gets
 * published rather than in what gets tested.</p>
 *
 * <p>Public because the metadata guard sits in the {@code support} package, beside the
 * package-private register it reads. Test scope only — nothing here is published.</p>
 */
public final class GeneratedCatalogue {

    /** Where {@link GenerateAllExamples} writes, relative to the module directory. */
    public static final Path ROOT = Path.of("target", "generated-pdfs");

    private static boolean generated;

    private GeneratedCatalogue() {
    }

    public static synchronized Path generateOnce() throws Exception {
        if (!generated) {
            regenerate();
        }
        return ROOT;
    }

    /**
     * Runs the catalogue, which empties the tree before writing it. Exposed so the guard
     * covering the emptying can drive it from a known state instead of depending on
     * which test class happened to run first.
     */
    static synchronized Path regenerate() throws Exception {
        GenerateAllExamples.main(new String[0]);
        generated = true;
        return ROOT;
    }
}
