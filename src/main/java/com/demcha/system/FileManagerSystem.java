package com.demcha.system;

import java.io.IOException;
import java.nio.file.Path;

public interface FileManagerSystem extends System {

    Path outPath();

    boolean saveAtomic() throws IOException;
}
