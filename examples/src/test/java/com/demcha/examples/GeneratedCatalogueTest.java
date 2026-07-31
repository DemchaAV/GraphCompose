package com.demcha.examples;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Covers the emptying, because everything asserted about the catalogue assumes it.
 *
 * <p>The guards downstream read the tree as the definitive list of what the runner
 * writes. A leftover from an earlier build is counted as current, and the entry it
 * matches stops looking orphaned — a false negative that survives precisely because a
 * negative test would plant its own file into a tree that was just rebuilt.</p>
 */
class GeneratedCatalogueTest {

    @Test
    void generationRemovesAnArtefactTheRunnerNoLongerWrites() throws Exception {
        Path stale = GeneratedCatalogue.ROOT.resolve("flagships").resolve("stale-example.pdf");
        Files.createDirectories(stale.getParent());
        Files.writeString(stale, "left behind by an earlier build");

        GeneratedCatalogue.regenerate();

        assertThat(stale)
                .describedAs("a document the runner no longer writes must not survive into the "
                        + "tree the guards read")
                .doesNotExist();
        assertThat(GeneratedCatalogue.ROOT.resolve("flagships").resolve("social-card.pdf"))
                .describedAs("emptying the tree must not cost the documents the runner does write")
                .exists();
    }
}
