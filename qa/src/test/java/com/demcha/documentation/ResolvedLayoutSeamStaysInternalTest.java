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
 * <p>A payload's <em>name</em> legitimately appears in a render handler's signature —
 * {@code payloadType()} returns {@code Class<…Payload>} — the way
 * {@code AnchorMarkerPayload} and {@code ShapeFragmentPayload} already do. What must not
 * happen is the type being <em>admitted</em>, which is what this checks.</p>
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
            "ResolvedLayoutPasses");

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
    void theTwoRenderHandlersAreTheOnlySurfaceThisAdds() throws IOException {
        // Named rather than left out. The seam is internal, but a fragment payload needs a
        // handler in each fixed backend — handlerFor throws on a payload class it does not
        // know — and handlers are public here, as every sibling is. So two public types do
        // ship. A guard that simply omitted them could not say whether that was intended or
        // an oversight; this says it is intended, and goes red if a third one appears.
        Path backends = RepoRoot.get().resolve("knowledge/api/backends.json");
        String json = Files.readString(backends);

        assertThat(json).as("the pdf no-op handler ships, deliberately")
                .contains("\"name\": \"PdfLayoutAnchorRenderHandler\"");
        assertThat(json).as("and its pptx twin")
                .contains("\"name\": \"PptxLayoutAnchorRenderHandler\"");

        assertThat(SEAM_TYPES.stream().filter(t -> json.contains("\"name\": \"" + t + "\"")).toList())
                .as("nothing else from the seam follows them out")
                .isEmpty();
    }
}
