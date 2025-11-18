package com.demcha.system.intarfaces;

import java.io.IOException;
import java.nio.file.Path;

public interface FileManagerSystem extends SystemECS {

    Path outPath();

    boolean saveAtomic() throws IOException;
}
