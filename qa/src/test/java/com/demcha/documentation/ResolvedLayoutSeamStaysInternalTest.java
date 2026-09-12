package com.demcha.documentation;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Keeps the resolved-layout seam off the public surface.
 *
 * <p>The seam exists so a built-in feature can draw from geometry it could not know during
 * layout. Making it public would mean settling, permanently, when passes run, in what
 * order, what one may see of another, whether one may change nodes or add pages, what
 * happens on failure, and how any of it behaves on a second compile. None of that is
 * needed by the feature that motivated it, and answering it by accident is worse than
 * leaving it open.</p>
 *
 * <p>Nothing enforces that on its own. The types sit in {@code @Internal} packages, which
 * excludes them today, but an {@code @Internal} package is one annotation away from not
 * being one, and the extension-SPI allow-list admits types out of exactly such a package
 * by name. So this reads the generated surfaces and says it directly.</p>
 *
 * <p>The list includes the two backend handlers. Each fixed backend does need one — its
 * {@code handlerFor} refuses a payload class it does not know — but a handler for an
 * internal payload is not something a caller can usefully hold, and a public class is
 * permanent. They are package-private, and sit beside their backend rather than in the
 * {@code handlers} package whose members are public precisely because callers register
 * them. The seam therefore adds <em>nothing</em> to the API.</p>
 */
class ResolvedLayoutSeamStaysInternalTest {

    private static final List<String> SEAM_TYPES = List.of(
            "LayoutAnchorId",
            "LayoutAnchorNode",
            "LayoutAnchorPayload",
            "LayoutAnchorDefinition",
            "LayoutDepth",
            "ResolvedLayoutAddition",
            "ResolvedLayoutAnchor",
            "ResolvedLayoutMetadata",
            "ResolvedLayoutPass",
            "ResolvedLayoutPasses",
            "PdfLayoutAnchorRenderHandler",
            "PptxLayoutAnchorRenderHandler");

    @Test
    void noSeamTypeIsAdmittedToAPublicSurface() throws IOException {
        Path apiDir = RepoRoot.get().resolve("knowledge/api");
        assertThat(apiDir).as("the knowledge pack must be present to check against").exists();

        List<String> admitted = new ArrayList<>();
        try (var files = Files.list(apiDir)) {
            for (Path surface : files.filter(p -> p.getFileName().toString().endsWith(".json")).toList()) {
                if (surface.getFileName().toString().equals("excluded.json")) {
                    continue;
                }
                String json = Files.readString(surface);
                for (String type : SEAM_TYPES) {
                    // A type entry, not a mention in a signature: the surfaces write an
                    // admitted type as "name": "Foo".
                    if (json.contains("\"name\": \"" + type + "\"")) {
                        admitted.add(surface.getFileName() + " admits " + type);
                    }
                }
            }
        }

        assertThat(admitted)
                .as("the resolved-layout seam is internal; promoting it needs its own design, "
                    + "not a side effect of the feature that first used it")
                .isEmpty();
    }

    @Test
    void theRegistrationMethodIsNotOnTheSession() throws IOException {
        Path authoring = RepoRoot.get().resolve("knowledge/api/authoring.json");
        assertThat(Files.readString(authoring))
                .as("registerLayoutPass is package-private and must not reach the authoring surface")
                .doesNotContain("registerLayoutPass");
    }

    @Test
    void theSeamAddsNoBackendSurfaceAtAll() throws IOException {
        // Separate from the sweep above because this is the one that was nearly got wrong:
        // a handler is the obvious place for internal machinery to leak, since every
        // sibling in the handlers package is public and copying one is the natural move.
        // The payload's own name would appear here too if a public handler declared
        // payloadType() — AnchorMarkerPayload and ShapeFragmentPayload are in this file for
        // exactly that reason — so its absence is the second signal that neither shipped.
        String json = Files.readString(RepoRoot.get().resolve("knowledge/api/backends.json"));

        assertThat(SEAM_TYPES.stream().filter(t -> json.contains("\"name\": \"" + t + "\"")).toList())
                .as("no seam type is admitted to the backend surface")
                .isEmpty();
        assertThat(json)
                .as("and no public signature mentions the anchor payload either")
                .doesNotContain("LayoutAnchorPayload");
    }
}
