package com.demcha.compose.layout_core.system.interfaces;

import java.io.IOException;
import java.nio.file.Path;

public interface FileManagerSystem extends SystemECS {

    Path outPath();

    boolean saveAtomic() throws IOException;
}
