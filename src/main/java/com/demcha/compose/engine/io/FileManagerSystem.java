package com.demcha.compose.engine.io;

import com.demcha.compose.engine.core.SystemECS;

import java.io.IOException;
import java.nio.file.Path;

public interface FileManagerSystem extends SystemECS {

    Path outPath();

    boolean saveAtomic() throws IOException;
}
