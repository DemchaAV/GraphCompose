package com.demcha.compose.v2;

import java.nio.file.Path;
import java.util.List;

/**
 * Skeleton semantic export result used by the initial DOCX/PPTX backends.
 */
public record SemanticExportManifest(
        String backendName,
        Path outputFile,
        int rootCount,
        List<String> nodeKinds
) {
    public SemanticExportManifest {
        nodeKinds = List.copyOf(nodeKinds);
    }
}
