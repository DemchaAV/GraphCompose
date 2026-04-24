package com.demcha.compose.document.backend.semantic;

import java.nio.file.Path;
import java.util.List;

/**
 * Skeleton semantic export result used by the initial DOCX/PPTX backends.
 *
 * @param backendName backend that produced the manifest
 * @param outputFile optional output file written by the backend
 * @param rootCount number of semantic roots exported
 * @param nodeKinds semantic node kinds encountered during export
 */
public record SemanticExportManifest(
        String backendName,
        Path outputFile,
        int rootCount,
        List<String> nodeKinds
) {
    /**
     * Freezes the exported node-kind list.
     */
    public SemanticExportManifest {
        nodeKinds = List.copyOf(nodeKinds);
    }
}


