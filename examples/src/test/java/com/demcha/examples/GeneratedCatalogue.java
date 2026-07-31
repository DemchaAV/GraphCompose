package com.demcha.examples;

import java.nio.file.Path;

/**
 * The generated example tree, produced once per JVM for whichever test asks first.
 *
 * <p>Two suites assert against the same output — one on the files, one on the showcase
 * metadata that describes them — and running the whole catalogue twice would double the
 * cost of the examples job for nothing. Reusing a tree left behind by an earlier build
 * would be worse: a stale artefact makes a guard pass on a document the current code no
 * longer writes, which is the failure mode a negative test cannot see. So generation is
 * unconditional the first time and skipped only within the same JVM.</p>
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
            GenerateAllExamples.main(new String[0]);
            generated = true;
        }
        return ROOT;
    }
}
