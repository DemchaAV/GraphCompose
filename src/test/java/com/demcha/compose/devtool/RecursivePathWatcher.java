package com.demcha.compose.devtool;

import java.io.IOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Watches a set of roots recursively and emits resolved file-system changes.
 */
public final class RecursivePathWatcher implements AutoCloseable {
    private final WatchService watchService;
    private final Map<WatchKey, Path> registeredDirectories = new HashMap<>();
    private final List<Path> roots;
    private final Consumer<PathChange> onChange;
    private final Consumer<Exception> onFailure;
    private volatile boolean running;
    private Thread thread;

    public RecursivePathWatcher(List<Path> roots, Consumer<PathChange> onChange, Consumer<Exception> onFailure)
            throws IOException {
        this.watchService = FileSystems.getDefault().newWatchService();
        this.roots = List.copyOf(Objects.requireNonNull(roots, "roots"));
        this.onChange = Objects.requireNonNull(onChange, "onChange");
        this.onFailure = Objects.requireNonNull(onFailure, "onFailure");
    }

    public void start() throws IOException {
        for (Path root : roots) {
            if (Files.isDirectory(root)) {
                registerAll(root);
            }
        }

        running = true;
        thread = Thread.ofPlatform()
                .daemon()
                .name("graphcompose-devtool-watch", 0)
                .unstarted(this::processLoop);
        thread.start();
    }

    @Override
    public void close() throws IOException {
        running = false;
        watchService.close();
        if (thread != null) {
            thread.interrupt();
        }
    }

    private void processLoop() {
        try {
            while (running) {
                WatchKey key = watchService.take();
                Path directory = registeredDirectories.get(key);
                if (directory == null) {
                    key.reset();
                    continue;
                }

                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();
                    if (kind == StandardWatchEventKinds.OVERFLOW) {
                        continue;
                    }

                    Path child = directory.resolve(((WatchEvent<Path>) event).context()).toAbsolutePath().normalize();
                    if (kind == StandardWatchEventKinds.ENTRY_CREATE && Files.isDirectory(child)) {
                        registerAll(child);
                    }

                    onChange.accept(new PathChange(child, kind));
                }

                if (!key.reset()) {
                    registeredDirectories.remove(key);
                }
            }
        } catch (ClosedWatchServiceException ignored) {
            // Normal shutdown path.
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        } catch (Exception ex) {
            onFailure.accept(ex);
        }
    }

    private void registerAll(Path root) throws IOException {
        try (var stream = Files.walk(root)) {
            stream.filter(Files::isDirectory).forEach(directory -> {
                try {
                    WatchKey key = directory.register(
                            watchService,
                            StandardWatchEventKinds.ENTRY_CREATE,
                            StandardWatchEventKinds.ENTRY_DELETE,
                            StandardWatchEventKinds.ENTRY_MODIFY);
                    registeredDirectories.put(key, directory);
                } catch (IOException ex) {
                    throw new WatchRegistrationException(ex);
                }
            });
        } catch (WatchRegistrationException ex) {
            throw ex.unwrap();
        }
    }

    public record PathChange(Path path, WatchEvent.Kind<?> kind) {
    }

    private static final class WatchRegistrationException extends RuntimeException {
        private WatchRegistrationException(IOException cause) {
            super(cause);
        }

        private IOException unwrap() {
            return (IOException) getCause();
        }
    }
}
