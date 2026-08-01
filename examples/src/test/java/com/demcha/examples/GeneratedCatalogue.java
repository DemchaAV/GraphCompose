package com.demcha.examples;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

/**
 * The generated example tree, produced once per JVM for whichever test asks first.
 *
 * <p>Two suites assert against the same output — one on the files, one on the showcase
 * metadata that describes them — and running the whole catalogue twice would double the
 * cost of the examples job for nothing.</p>
 *
 * <p>The tree is emptied before it is rebuilt. The runner only writes, so without that
 * an artefact from an earlier build survives: delete an example and leave its showcase
 * entry behind, run without {@code clean}, and the coverage guard matches the entry
 * against yesterday's file and passes on a document the current code no longer writes.
 * That is invisible to a negative test, because the negative test starts from a tree the
 * runner just wrote. CI happens to be safe — it compiles clean first — which only means
 * the local run is the lenient one, and the local run is where the guard is read.</p>
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
     * Empties the tree and runs the whole catalogue into it. The path {@link #generateOnce}
     * takes, exposed so the guard covering the emptying can drive it from a known state
     * instead of depending on which test class happened to run first.
     */
    static synchronized Path regenerate() throws Exception {
        clear(ROOT);
        Files.createDirectories(ROOT);
        GenerateAllExamples.main(new String[0]);
        generated = true;
        return ROOT;
    }

    /**
     * Deletes everything under {@code root}, deepest entry first so a directory is empty
     * by the time it is removed. {@code root} itself stays.
     */
    private static void clear(Path root) throws IOException {
        if (!Files.isDirectory(root)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(root)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                if (!path.equals(root)) {
                    Files.delete(path);
                }
            }
        }
    }
}
