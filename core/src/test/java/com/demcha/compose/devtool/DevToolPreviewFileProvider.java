package com.demcha.compose.devtool;

import java.nio.file.Path;

/**
 * Optional contract for preview providers that can export the current document
 * to a real PDF file on disk for external inspection.
 */
public interface DevToolPreviewFileProvider {

    Path savePreviewDocument() throws Exception;
}
